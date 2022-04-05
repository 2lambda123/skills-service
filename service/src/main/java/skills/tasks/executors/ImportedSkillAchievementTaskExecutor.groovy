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
package skills.tasks.executors

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.services.LockingService
import skills.services.events.pointsAndAchievements.ImportedSkillsAchievementsHandler
import skills.storage.model.SkillDefMin
import skills.storage.repos.SkillDefRepo
import skills.tasks.config.TaskConfig
import skills.tasks.data.ImportedSkillAchievement

@Component
@Slf4j
class ImportedSkillAchievementTaskExecutor implements VoidExecutionHandler<ImportedSkillAchievement> {

    @Autowired
    ImportedSkillsAchievementsHandler importedSkillsAchievementsHandler

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    LockingService lockingService

    @Transactional
    @Override
    void execute(TaskInstance<ImportedSkillAchievement> taskInstance, ExecutionContext executionContext) {
        ImportedSkillAchievement data = taskInstance.getData()
        SkillDefMin min = getSkill(data.rawSkillId)
        log.debug("running imported skill achievement scheduled task for [{}-{}, {}], uuid=[{}]", min.projectId, min.skillId, data.userId, data.uuid)
        importedSkillsAchievementsHandler.handleAchievementsForImportedSkills(data.userId, min, data.incomingSkillDate, data.thisRequestCompletedOriginalSkill)
    }

    private SkillDefMin getSkill(int id) {
        try {
            SkillDefMin min = skillDefRepo.findSkillDefMinById(id)
            if (!min) {
                throw new TaskConfig.DoNotRetryAsyncTaskException("Failed to find skillId with id=[${id}]")
            }
            return min
        } catch (EmptyResultDataAccessException e) {
            throw new TaskConfig.DoNotRetryAsyncTaskException(e)
        }
    }
}
