/**
 * Copyright 2022 SkillTree
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
package skills.storage.model

class DayCount implements DayCountItem{
    Date day
    Long count
    String projectId // optional, used when counting items for multiple projects in the same query

    DayCount(Date day, Long count){
        this.day = day
        this.count = count
    }

    DayCount(String projectId, Date day, Long count){
        this.projectId = projectId
        this.day = day
        this.count = count
    }
}
