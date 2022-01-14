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
package skills.skillLoading

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.skillLoading.model.SkillDependencySummary
import skills.storage.model.SkillDef
import skills.storage.model.SkillRelDef
import skills.storage.model.UserPoints
import skills.storage.repos.UserPointsRepo

@Component
@Slf4j
@CompileStatic
class SubjectDataLoader {

    @Autowired
    UserPointsRepo userPointsRepo

    @Autowired
    DependencySummaryLoader dependencySummaryLoader


    static class SkillsAndPoints {
        SkillDef skillDef
        int points
        int todaysPoints
        String copiedFromProjectName

        SkillDependencySummary dependencyInfo
    }

    static class SkillsData {
        List<SkillsAndPoints> childrenWithPoints
    }

    @Profile
    SkillsData loadData(String userId, String projectId, String skillId, Integer version = Integer.MAX_VALUE) {
        return loadData(userId, projectId, skillId, version, [SkillRelDef.RelationshipType.RuleSetDefinition, SkillRelDef.RelationshipType.SkillsGroupRequirement])
    }

    @Profile
    SkillsData loadData(String userId, String projectId, String skillId, Integer version = Integer.MAX_VALUE, List<SkillRelDef.RelationshipType> relationshipTypes) {
        List<SkillDefAndUserPoints> childrenWithUserPoints = loadChildren(userId, projectId, skillId, relationshipTypes, version)
        childrenWithUserPoints = childrenWithUserPoints?.sort({ it.skillDef.displayOrder })

        List<SkillDefAndUserPoints> todaysUserPoints = loadChildren(userId, projectId, skillId, relationshipTypes, version, new Date().clearTime())

        List<UserPointsRepo.SkillWithChildAndAchievementIndicator> allProjectDepsAndAchievements = loadAllDepsWithAchievementIndicator(userId, projectId, version)
        Map<Integer, List<UserPointsRepo.SkillWithChildAndAchievementIndicator>> byParentId = allProjectDepsAndAchievements.groupBy { it.parentId }

        List<SkillsAndPoints> skillsAndPoints = childrenWithUserPoints.collect { SkillDefAndUserPoints skillDefAndUserPoints ->
            SkillDefAndUserPoints todaysPoints = todaysUserPoints.find({
                it.skillDef.id == skillDefAndUserPoints.skillDef.id
            })
            int todayPoints = todaysPoints?.points ? todaysPoints.points?.points : 0
            int points = skillDefAndUserPoints?.points ? skillDefAndUserPoints.points.points : 0

            List<UserPointsRepo.SkillWithChildAndAchievementIndicator> dependents = byParentId[skillDefAndUserPoints.skillDef.id]
            SkillDependencySummary dependencyInfo
            if (dependents) {
                dependencyInfo = dependents ? new SkillDependencySummary(
                        numDirectDependents: dependents.size(),
                        achieved: !dependents.find { it.getAchievementId() == null }
                ) : null
            }
            new SkillsAndPoints(skillDef: skillDefAndUserPoints.skillDef, points: points, todaysPoints: todayPoints, dependencyInfo: dependencyInfo,
                    copiedFromProjectName: skillDefAndUserPoints.copiedFromProjectName)
        }
        new SkillsData(childrenWithPoints: skillsAndPoints)
    }

    @Profile
    private List<UserPointsRepo.SkillWithChildAndAchievementIndicator> loadAllDepsWithAchievementIndicator(String userId, String projectId, int version) {
        if (projectId) {
            userPointsRepo.findAllChildrenAndTheirAchievementsForProject(userId, projectId, SkillRelDef.RelationshipType.Dependence, version)
        } else {
            userPointsRepo.findAllChildrenAndTheirAchievementsForGlobal(userId, SkillRelDef.RelationshipType.Dependence, version)
        }
    }

    private static class SkillDefAndUserPoints {
        SkillDef skillDef
        UserPoints points
        String copiedFromProjectName
    }

    @Profile
    private List<SkillDefAndUserPoints> loadChildren(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, Integer version = Integer.MAX_VALUE, Date day = null) {

        List<Object[]> childrenWithUserPoints = day ?
                findChildrenPointsByDay(userId, projectId, skillId, relationshipTypes, version, day) :
                findChildrenPoints(userId, projectId, skillId, relationshipTypes, version)

        List<SkillDefAndUserPoints> res = childrenWithUserPoints.collect {
            UserPoints userPoints = (it.length > 1 ? it[1] : null) as UserPoints
            return new SkillDefAndUserPoints(
                    skillDef: it[0] as SkillDef, points: userPoints, copiedFromProjectName: it.length > 2 ? (String)it[2] : null
            )
        }
        return res?.sort { it.skillDef.displayOrder }
    }

    @Profile
    private List<Object[]> findChildrenPoints(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, int version) {
        if (projectId) {
            return userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipTypes, version)
        } else {
            return userPointsRepo.findGlobalChildrenAndTheirUserPoints(userId, skillId, relationshipTypes, version)
        }
    }

    @Profile
    private List<Object[]> findChildrenPointsByDay(String userId, String projectId, String skillId, List<SkillRelDef.RelationshipType> relationshipTypes, int version, Date day) {
        if (projectId) {
            return userPointsRepo.findChildrenAndTheirUserPoints(userId, projectId, skillId, relationshipTypes, version, day)
        } else {
            return userPointsRepo.findGlobalChildrenAndTheirUserPoints(userId, skillId, relationshipTypes, version, day)
        }
    }

}
