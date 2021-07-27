/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.notify

import callStack.profiler.Profile
import groovy.json.JsonOutput
import groovy.time.TimeCategory
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.StopWatch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.controller.result.model.SettingsResult
import skills.notify.builders.Formatting
import skills.notify.builders.NotificationEmailBuilder
import skills.notify.builders.NotificationEmailBuilderManager
import skills.services.EmailSendingService
import skills.services.FeatureService
import skills.services.LockingService
import skills.services.settings.SettingsService
import skills.settings.EmailSettingsService
import skills.storage.model.Notification
import skills.storage.model.UserAttrs
import skills.storage.repos.NotificationsRepo
import skills.storage.repos.UserAttrsRepo

import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@Component
@Slf4j
class EmailNotifier implements Notifier {

    @Value('#{"${skills.config.notifications.retainFailedNotificationsForNumSecs}"}')
    int retainFailedNotificationsForNumSecs

    @Autowired
    EmailSendingService sendingService

    @Autowired
    UserAttrsRepo userAttrs

    @Autowired
    SettingsService settingsService

    @Autowired
    NotificationsRepo notificationsRepo

    @Autowired
    LockingService lockingService

    @Autowired
    FeatureService featureService

    @Autowired
    NotificationEmailBuilderManager notificationEmailBuilderManager

    @Transactional
    @Profile
    void sendNotification(Notifier.NotificationRequest notificationRequest) {

        assert notificationRequest.userIds
        assert notificationRequest.requestedOn
        assert notificationRequest.type
        assert notificationRequest.keyValParams

        if (!featureService.isEmailServiceFeatureEnabled()) {
            return
        }

        String serParams = JsonOutput.toJson(notificationRequest.keyValParams)
        notificationRequest.userIds.each { String userId ->
            Notification notification = new Notification(
                    requestedOn: notificationRequest.requestedOn,
                    userId: userId?.toLowerCase(),
                    failedCount: 0,
                    encodedParams: serParams,
                    type: notificationRequest.type,
            )

            notificationsRepo.save(notification)
        }

    }

    @Transactional
    void dispatchNotifications() {
        log.debug("Checking notifications to dispatch.")
        doDispatchNotifications { notificationsRepo.streamNewNotifications() }
    }

    @Transactional
    void attemptToDispatchErroredNotifications() {
        log.debug("Checking for errored notifications.")
        doDispatchNotifications("Retry: ") { notificationsRepo.streamFailedNotifications() }
    }

    private void doDispatchNotifications(String prependToLogs = "", Closure<Stream<Notification>> streamCreator) {
        lockingService.lockForNotifying()
        List<SettingsResult> emailSettings = settingsService.getGlobalSettingsByGroup(EmailSettingsService.settingsGroup);

        Formatting formatting = new Formatting(
                htmlHeader: emailSettings.find {it.setting == EmailSettingsService.htmlHeader }?.value ?: null,
                plaintextHeader: emailSettings.find { it.setting == EmailSettingsService.plaintextHeader }?.value ?: null,
                htmlFooter: emailSettings.find { it.setting == EmailSettingsService.htmlFooter }?.value ?: null,
                plaintextFooter: emailSettings.find { it.setting == EmailSettingsService.plaintextFooter }?.value ?:null
        )
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()

        int count = 0
        int errCount = 0
        String lastErrMsg
        try (Stream<Notification> notifications = streamCreator.call()) {
            notifications.forEach({ Notification notification ->
                NotificationEmailBuilder.Res emailRes = notificationEmailBuilderManager.build(notification, formatting)
                UserAttrs userAttrs = userAttrs.findByUserId(notification.userId)
                boolean failed = false;
                try {
                    sendingService.sendEmail(emailRes.subject, userAttrs.email, emailRes.html, emailRes.plainText, notification.requestedOn)
                } catch (Throwable t) {
                    // don't print the same message over and over again
                    if (!lastErrMsg?.equalsIgnoreCase(t.message)) {
                        log.error("${prependToLogs}Failed to sent notification with id [${notification.id}] and type [${notification.type}]. Updating notification to retry", t)
                        lastErrMsg = t.message
                    }
                    notification.failedCount = notification.failedCount + 1

                    boolean removed = removeIfOlderThanConfiguredRetainPeriod(notification)
                    if (!removed) {
                        notificationsRepo.save(notification)
                    }
                    failed = true;
                }
                if (!failed) {
                    removeNotificationImmediately(notification.id)
                    count++
                } else {
                    errCount++
                }
            })
        }

        stopWatch.stop()
        if (count > 0 || errCount > 0) {
            int seconds = stopWatch.getTime(TimeUnit.SECONDS)
            log.info("${prependToLogs}Dispatched [${count}] notification(s) with [${errCount}] error(s) in [${seconds}] seconds")
        }
    }

    private boolean removeIfOlderThanConfiguredRetainPeriod(Notification notification) {
        boolean removed = false
        use(TimeCategory) {
            Date keepAfterDate = retainFailedNotificationsForNumSecs.seconds.ago
            if (notification.created.before(keepAfterDate)) {
                removeNotificationImmediately(notification.id)
                log.error("Removed notification because it failed and exceeded max retention of [${retainFailedNotificationsForNumSecs}] seconds! Notificaiton=[{}]", notification)
                removed = true
            }
        }

        return removed;
    }

    private void removeNotificationImmediately(Integer id) {
        assert id
        notificationsRepo.deleteById(id)
        notificationsRepo.flush()
    }


}
