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
package skills.services.admin

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.SkillsAuthorizationException
import skills.auth.UserInfo
import skills.auth.UserInfoService
import skills.auth.UserSkillsGrantedAuthority
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.controller.result.model.CustomIconResult
import skills.controller.result.model.LatestEvent
import skills.controller.result.model.ProjectDescription
import skills.controller.result.model.ProjectResult
import skills.controller.result.model.SettingsResult
import skills.controller.result.model.SimpleProjectResult
import skills.icons.IconCssNameUtil
import skills.services.*
import skills.services.inception.InceptionProjectService
import skills.services.settings.Settings
import skills.services.settings.SettingsService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.*
import skills.storage.model.auth.RoleName
import skills.storage.model.auth.User
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserEventsRepo
import skills.storage.repos.UserRepo
import skills.utils.ClientSecretGenerator
import skills.utils.InputSanitizer
import skills.utils.Props

@Service
@Slf4j
class ProjAdminService {

    static final String rootUserPinnedProjectGroup = "pinned_project"
    private static final String myProjectGroup = "my_projects"
    private static final String myProjectSetting = "my_project"
    public static final String PINNED = "pinned"

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    LevelDefinitionStorageService levelDefService

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    LockingService lockingService

    @Autowired
    CustomValidator customValidator

    @Autowired
    CreatedResourceLimitsValidator createdResourceLimitsValidator

    @Autowired
    UserInfoService userInfoService

    @Autowired
    GlobalBadgesService globalBadgesService

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    ProjectSortingService sortingService

    @Autowired
    SettingsService settingsService

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjectErrorService errorService

    @Autowired
    UserEventsRepo eventsRepo

    @Autowired
    ProjectExpirationService projectExpirationService

    @Autowired
    UserRepo userRepo

    @Autowired
    BatchOperationsTransactionalAccessor batchOperationsTransactionalAccessor

    @Autowired
    SkillsAdminService skillsAdminService

    @Autowired
    ServiceValidatorHelper serviceValidatorHelper

    @Transactional()
    void saveProject(String originalProjectId, ProjectRequest projectRequest, String userIdParam = null) {
        assert projectRequest?.projectId
        assert projectRequest?.name

        lockingService.lockProjects()

        CustomValidationResult customValidationResult = customValidator.validate(projectRequest)
        if (!customValidationResult.valid) {
            throw new SkillException(customValidationResult.msg)
        }

        ProjDef projectDefinition = originalProjectId ? projDefRepo.findByProjectIdIgnoreCase(originalProjectId) : null
        if (!projectDefinition || !projectRequest.projectId.equalsIgnoreCase(originalProjectId)) {
            serviceValidatorHelper.validateProjectIdDoesNotExist(projectRequest.projectId)
        }
        if (!projectDefinition || !projectRequest.name.equalsIgnoreCase(projectDefinition.name)) {
            serviceValidatorHelper.validateProjectNameDoesNotExist(projectRequest.name, projectRequest.projectId)
        }
        if (projectDefinition) {
            Props.copy(projectRequest, projectDefinition)
            log.debug("Updating [{}]", projectDefinition)

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId) {
                projectDefinition = projDefRepo.save(projectDefinition)
            }
            log.debug("Saved [{}]", projectDefinition)
        } else {
            // TODO: temp hack around since user is not yet defined when Inception project is created
            // This will be addressed in ticket #139
            String clientSecret = new ClientSecretGenerator().generateClientSecret()

            projectDefinition = new ProjDef(projectId: projectRequest.projectId, name: projectRequest.name,
                    clientSecret: clientSecret, description: projectRequest.description)
            log.debug("Created project [{}]", projectDefinition)

            if (!userInfoService.isCurrentUserASuperDuperUser()) {
                createdResourceLimitsValidator.validateNumProjectsCreated(userIdParam ?: userInfoService.getCurrentUserId())
            }

            DataIntegrityExceptionHandlers.dataIntegrityViolationExceptionHandler.handle(projectDefinition.projectId) {
                projectDefinition = projDefRepo.save(projectDefinition)
            }

            log.debug("Saved [{}]", projectDefinition)

            levelDefService.createDefault(projectRequest.projectId, projectDefinition)

            String userId = userIdParam ?: userInfoService.getCurrentUserId()
            accessSettingsStorageService.addUserRole(userId, projectRequest.projectId, RoleName.ROLE_PROJECT_ADMIN)
            log.debug("Added user role [{}] to [{}]", RoleName.ROLE_PROJECT_ADMIN, userId)
        }
    }

    @Transactional()
    void deleteProject(String projectId) {
        log.debug("Deleting project with id [{}]", projectId)
        if (!existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        if (globalBadgesService.isProjectUsedInGlobalBadge(projectId)) {
            throw new SkillException("Project with id [${projectId}] cannot be deleted as it is currently referenced by one or more global badges")
        }

        List<SkillDef> childSkills = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Skill)
        childSkills.each {
            skillsAdminService.removeCatalogImportedSkills(it)
        }

        projDefRepo.deleteByProjectIdIgnoreCase(projectId)
        log.debug("Deleted project with id [{}]", projectId)
    }

    @Transactional()
    void pinProjectForRootUser(String projectId, User user=null) {
        if (!existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }
        UserProjectSettingsRequest userSettingsRequest = new UserProjectSettingsRequest(
                projectId: projectId,
                settingGroup: rootUserPinnedProjectGroup,
                setting: PINNED,
                value: projectId
        )
        settingsService.saveSetting(userSettingsRequest, user)

        String userId = user ? user.userId : loadCurrentUser(true)?.username
        sortingService.setNewProjectDisplayOrder(projectId, userId?.toLowerCase())
    }

    void pinAllExistingProjectsWhereUserIsAdminExceptInception(String userId) {
        List<String> existingProjectIdsWhereUserIsAdmin = projDefRepo.getProjectIdsByUser(userId.toLowerCase())
        if (existingProjectIdsWhereUserIsAdmin) {
            User user = userRepo.findByUserId(userId.toLowerCase())
            existingProjectIdsWhereUserIsAdmin.each { projectId ->
                if (projectId != InceptionProjectService.inceptionProjectId) {
                    pinProjectForRootUser(projectId, user)
                }
            }
        }
    }

    @Transactional()
    void unpinProjectForRootUser(String projectId, User user=null) {
        if (existsByProjectId(projectId)) {
            String currentUserIdLower = user ? user.userId.toLowerCase() : userInfoService.getCurrentUserId().toLowerCase()
            settingsService.deleteUserProjectSetting(
                    currentUserIdLower,
                    rootUserPinnedProjectGroup,
                    PINNED,
                    projectId
            )
            sortingService.deleteProjectDisplayOrder(projectId, currentUserIdLower)
        }
    }
    void unpinAllProjectsForRootUser(String userId) {
        List<SettingsResult> pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(userId.toLowerCase(), ProjAdminService.rootUserPinnedProjectGroup)
        List<String> existingPinnedProjectIds = pinnedProjectSettings.collect { it.projectId }
        if (existingPinnedProjectIds) {
            User user = userRepo.findByUserId(userId.toLowerCase())
            existingPinnedProjectIds.each { projectId ->
                unpinProjectForRootUser(projectId, user)
            }
        }
    }

    @Transactional()
    void addMyProject(String projectId, AddMyProjectRequest addMyProjectRequest) {
        if (!existsByProjectId(projectId)) {
            throw new SkillException("Project with id [${projectId}] does NOT exist")
        }

        String currentUserIdLower = userInfoService.getCurrentUserId().toLowerCase()
        List<SettingsResult> allExistingProjects = settingsService.getUserProjectSettingsForAllProjectsForGroup(currentUserIdLower, myProjectGroup)
        List<SettingsRequest> finalRes = allExistingProjects.collect({
            new UserProjectSettingsRequest(
                    projectId: it.projectId,
                    settingGroup: it.settingGroup,
                    setting: it.setting,
                    value: it.value
            )
        }).sort { it.value }

        SettingsRequest alreadyExistProjectRequest =  finalRes.find({ ((UserProjectSettingsRequest)it).getProjectId() == projectId})
        if(!alreadyExistProjectRequest) {
            UserProjectSettingsRequest newProjectAdded = new UserProjectSettingsRequest(
                    projectId: projectId,
                    settingGroup: myProjectGroup,
                    setting: myProjectSetting,
            )

            // new project should always appear first in the list
            int insertIndex = 0;
            if (addMyProjectRequest?.newSortIndex != null) {
                insertIndex = Math.min(addMyProjectRequest.newSortIndex, finalRes.size() - 1)
            }
            finalRes.add(insertIndex, newProjectAdded)
        } else if (alreadyExistProjectRequest && addMyProjectRequest?.newSortIndex != null && addMyProjectRequest.newSortIndex < finalRes.size()) {
            finalRes.remove(alreadyExistProjectRequest)
            finalRes.add(addMyProjectRequest.newSortIndex, alreadyExistProjectRequest)
        }

        finalRes.eachWithIndex{ def entry, int i ->
            entry.value = "sort_${StringUtils.leftPad(i.toString(), 4, "0")}"
        }

        User user = userRepo.findByUserId(currentUserIdLower)
        if (!user) {
            throw new SkillException("Failed to find user with id [${currentUserIdLower}]")
        }
        settingsService.saveSettings(finalRes, user)
    }

    @Transactional()
    void removeMyProject(String projectId) {
        if (existsByProjectId(projectId)) {
            String currentUserIdLower = userInfoService.getCurrentUserId().toLowerCase()
            settingsService.deleteUserProjectSetting(
                    currentUserIdLower,
                    myProjectGroup,
                    myProjectSetting,
                    projectId
            )
        }
    }

    private  List<ProjectResult> loadProjectsForRoot(Map<String, Integer> projectIdSortOrder, String userId) {
        List<SettingsResult> pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(userId, rootUserPinnedProjectGroup)
        List<String> pinnedProjects = pinnedProjectSettings.collect { it.projectId }

        List<ProjSummaryResult> projects = projDefRepo.getAllSummariesByProjectIdIn(pinnedProjects)
        Set<String> pinnedProjectIds = pinnedProjects.toSet()

        List<ProjectResult> finalRes = projects?.unique({ it.projectId })?.collect({
            ProjectResult res = convert(it, projectIdSortOrder, pinnedProjectIds)
            return res
        })

        return finalRes
    }

    @Transactional(readOnly = true)
    List<ProjectResult> searchByProjectName(String search) {
        validateRootUser();
        List<ProjSummaryResult> projects = projDefRepo.getSummariesByNameLike(search)
        return convertProjectsWithPinnedIndicator(projects)
    }

    @Transactional(readOnly = true)
    List<ProjectResult> getAllProjects() {
        validateRootUser();
        List<ProjSummaryResult> projects = projDefRepo.getAllSummaries()
        return convertProjectsWithPinnedIndicator(projects)
    }

    private List<ProjectResult> convertProjectsWithPinnedIndicator(List<ProjSummaryResult> projects) {
        Map<String, Integer> projectIdSortOrder = [:]
        List<SettingsResult> pinnedProjectSettings = settingsService.getUserProjectSettingsForGroup(userInfoService.getCurrentUserId(), rootUserPinnedProjectGroup)
        List<String> pinnedProjects = pinnedProjectSettings.collect { it.value }
        return projects?.unique({ it.projectId })?.collect({
            return convert(it, projectIdSortOrder, pinnedProjects?.toSet())
        })
    }

    private validateRootUser(){
        UserInfo userInfo = userInfoService.getCurrentUser()
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority &&
                    (RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName || RoleName.ROLE_SUPERVISOR == it.role?.roleName)
        }
        assert isRoot
    }

    @Transactional(readOnly = true)
    List<ProjectResult> getProjects() {
        UserInfo userInfo = userInfoService.getCurrentUser()
        boolean isRoot = userInfo.authorities?.find() {
            it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
        }

        String userId = userInfo.username
        Map<String, Integer> projectIdSortOrder = sortingService.getUserProjectsOrder(userId)

        List<ProjectResult> finalRes
        if (isRoot) {
            finalRes = loadProjectsForRoot(projectIdSortOrder, userId)
        } else {
            // sql join with UserRoles and there is 1-many relationship that needs to be normalized
            List<ProjSummaryResult> projects = projDefRepo.getProjectSummariesByUser(userId)
            finalRes = projects?.unique({ it.projectId })?.collect({
                ProjectResult res = convert(it, projectIdSortOrder)
                return res
            })
        }

        finalRes.sort() { it.displayOrder }

        if (finalRes) {
            finalRes.first().isFirst = true
            finalRes.last().isLast = true
        }

        return finalRes
    }

    @Transactional(readOnly = true)
    ProjectResult getProject(String projectId) {
        ProjSummaryResult projectDefinition = projDefAccessor.getProjSummaryResult(projectId)
        Integer order = sortingService.getProjectSortOrder(projectId)
        ProjectResult res = convert(projectDefinition, [(projectId): order])
        return res
    }

    @Transactional(readOnly = true)
    ProjectDescription getProjectDescription(String projectId) {
        String description = projDefAccessor.getProjDescription(projectId)
        return new ProjectDescription(projectId: projectId, description: InputSanitizer.unsanitizeForMarkdown(description))
    }

    @Transactional(readOnly = true)
    boolean existsByProjectId(String projectId) {
        return projDefRepo.existsByProjectIdIgnoreCase(projectId)
    }

    @Transactional(readOnly = true)
    boolean existsByProjectName(String projectName) {
        return projDefRepo.existsByNameIgnoreCase(projectName)
    }


    @Transactional()
    void setProjectDisplayOrder(String projectId, ActionPatchRequest projectPatchRequest) {
        assert projectPatchRequest.action

        if (ActionPatchRequest.ActionType.NewDisplayOrderIndex == projectPatchRequest.action) {
            UserInfo userInfo = userInfoService.getCurrentUser()
            String userId = userInfo.username
            boolean isRootUser = userInfo.authorities?.find() {
                it instanceof UserSkillsGrantedAuthority && RoleName.ROLE_SUPER_DUPER_USER == it.role?.roleName
            }
            sortingService.updateDisplayOrderByUsingNewIndex(userId, isRootUser, projectId, projectPatchRequest)
        }
    }

    @Transactional()
    List<SimpleProjectResult> searchProjects(String projectId, String nameQuery) {
        List<ProjSummaryResult> projDefs = projDefRepo.queryProjectSummariesByNameAndNotProjectId(nameQuery.toLowerCase(), projectId, PageRequest.of(0, 5, Sort.Direction.ASC, "name"))
        return projDefs.collect {
            new SimpleProjectResult(name: it.name,
                    projectId: it.projectId,
                    created: it.created,
                    lastReportedSkill: it.lastReportedSkill)
        }
    }

    @Transactional(readOnly = true)
    String getProjectSecret(String projectId) {
        ProjDef projectDefinition = projDefAccessor.getProjDef(projectId)
        return projectDefinition.clientSecret
    }

    @Transactional
    void updateClientSecret(String projectId, String clientSecret) {
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        projDef.clientSecret = clientSecret
    }

    @Transactional(readOnly = true)
    List<CustomIconResult> getCustomIcons(String projectId){
        ProjDef project = projDefAccessor.getProjDef(projectId)
        return project.getCustomIcons().collect { CustomIcon icon ->
            String cssClassname = IconCssNameUtil.getCssClass(icon.projectId, icon.filename)
            return new CustomIconResult(filename: icon.filename, cssClassname: cssClassname)
        }
    }

    @Transactional
    void cancelProjectExpiration(String projectId) {
        projectExpirationService.cancelExpiration(projectId)
    }

    @Transactional(readOnly = true)
    String lookupMyProjectName(String userId, String projectId){
        ProjectSummaryResult mySummary = projDefRepo.getMyProjectName(userId, projectId)
        if (!mySummary) {
            throw new SkillException("Project not found", projectId, null, ErrorCode.ProjectNotFound)
        }

        return mySummary.projectName
    }

    @Transactional(readOnly = true)
    String lookupProjectName(String projectId){
        ProjectSummaryResult mySummary = projDefRepo.getProjectName(projectId)
        if (!mySummary) {
            throw new SkillException("Project not found", projectId, null, ErrorCode.ProjectNotFound)
        }

        return InputSanitizer.unsanitizeName(mySummary.projectName)
    }

    void rebuildUserAndProjectPoints(String projectId) {
        log.info("Rebuilding user and project points for project [{}]", projectId)

        // update `skill_definition.total_points` for skill groups, subjects and the project
        log.info("Updating skill_definition.total_points for [{}] project", projectId)
        List<SkillDef> skillGroups = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.SkillsGroup)
        skillGroups.each { SkillDef skillGroup ->
            batchOperationsTransactionalAccessor.updateGroupTotalPoints(projectId, skillGroup.skillId)
        }
        List<SkillDef> subjects = skillDefRepo.findAllByProjectIdAndType(projectId, SkillDef.ContainerType.Subject)
        subjects.each { SkillDef subject ->
            batchOperationsTransactionalAccessor.updateSubjectTotalPoints(projectId, subject.skillId)
        }
        batchOperationsTransactionalAccessor.updateProjectsTotalPoints(projectId)

        // update `user_points.points` for all existing users
        log.info("Updating user_points.points for [{}] project", projectId)
        subjects.each { subject ->
            log.info("Updating UserPoints for the existing users for [{}-{}] subject", projectId, subject.skillId)
            batchOperationsTransactionalAccessor.updateUserPointsForSubject(projectId, subject.skillId)

            log.info("Identifying subject level achievements for [{}-{}] subject", projectId, subject.skillId)
            batchOperationsTransactionalAccessor.identifyAndAddSubjectLevelAchievements(subject.projectId, subject.skillId)
        }

        log.info("Identifying group achievements for [{}] groups in project [{}]", skillGroups.size(), projectId)
        batchOperationsTransactionalAccessor.identifyAndAddGroupAchievements(skillGroups)

        SettingsResult settingsResult = settingsService.getProjectSetting(projectId, Settings.LEVEL_AS_POINTS.settingName)
        boolean pointsBased = settingsResult ? settingsResult.isEnabled() : false
        log.info("Updating UserPoints for the existing users for [{}] project", projectId)
        batchOperationsTransactionalAccessor.updateUserPointsForProject(projectId)
        log.info("Identifying and adding project level achievements for [{}] project, pointsBased=[{}]", projectId, pointsBased)
        batchOperationsTransactionalAccessor.identifyAndAddProjectLevelAchievements(projectId, pointsBased)

        log.info("Completed rebuilding user and project points for project [{}]", projectId)
    }

    @Profile
    private ProjectResult convert(ProjSummaryResult definition, Map<String, Integer> projectIdSortOrder, Set<String> pinnedProjectIds = []) {
        Integer order = projectIdSortOrder?.get(definition.getProjectId())
        ProjectResult res = new ProjectResult(
                projectId: definition.getProjectId(),
                name: InputSanitizer.unsanitizeName(definition.getName()),
                totalPoints: definition.getTotalPoints() - (definition.getTotalPointsReused() ?: 0),
                numSubjects: definition.getNumSubjects(),
                numGroups: definition.getNumGroups(),
                displayOrder: order != null ? order : 0,
                pinned: pinnedProjectIds?.contains(definition.getProjectId()),
                created: definition.getCreated(),
                expiring: definition.getExpiring(),
                expirationTriggered: definition.getExpirationTriggered(),
                numSkillsDisabled: definition.getNumSkillsDisabled(),
                numSkillsReused: definition.getNumSkillsReused() ?: 0,
                totalPointsReused: definition.getTotalPointsReused() ?: 0,
        )
        res.numBadges = definition.numBadges
        res.numSkills = definition.numSkills
        res.numSkillsDisabled = definition.getNumSkillsDisabled()
        res.numErrors = definition.numErrors
        res.lastReportedSkill = definition.lastReportedSkill

        res
    }

    @Profile
    private long countNumSkillsForProject(ProjDef definition) {
        skillDefRepo.countByProjectIdAndType(definition.projectId, SkillDef.ContainerType.Skill)
    }

    @Profile
    private UserInfo loadCurrentUser(boolean failIfNoCurrentUser = true) {
        UserInfo currentUser = userInfoService.getCurrentUser()
        if (!currentUser && failIfNoCurrentUser) {
            throw new SkillsAuthorizationException('No current user found')
        }
        return currentUser
    }

    LatestEvent getLastReportedSkillEvent(String projectId) {
        Date date = eventsRepo.getLatestEventDateForProject(projectId)
        new LatestEvent(lastReportedSkillDate: date)
    }
}
