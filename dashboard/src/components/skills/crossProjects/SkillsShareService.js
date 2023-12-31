/*
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
import axios from 'axios';

export default {
  shareSkillToAnotherProject(projectId, skillId, shareToProjectId) {
    return axios.put(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/shared/projects/${encodeURIComponent(shareToProjectId)}`);
  },
  deleteSkillShare(projectId, skillId, shareToProjectId) {
    return axios.delete(`/admin/projects/${encodeURIComponent(projectId)}/skills/${encodeURIComponent(skillId)}/shared/projects/${encodeURIComponent(shareToProjectId)}`);
  },
  getSharedSkills(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/shared`)
      .then((response) => response.data);
  },
  getSharedWithmeSkills(projectId) {
    return axios.get(`/admin/projects/${encodeURIComponent(projectId)}/sharedWithMe`)
      .then((response) => response.data);
  },

};
