/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <metrics-card id="shared-skills-with-others-panel" title="Share skills from this project with other projects"
                :no-padding="true" data-cy="shareSkillsWithOtherProjectsCard">
      <loading-container :is-loading="loading.sharedSkillsInit || loading.allSkills || loading.projInfo">
        <no-content2 v-if="restrictedUserCommunity" title="Cannot Be Added" icon="fas fa-shield-alt"
                     class="my-5" data-cy="restrictedUserCommunityWarning">
          This project's access is
          restricted to <b class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users
          only and its skills <b class="text-primary">cannot</b> be added as dependencies in other Projects.
        </no-content2>
        <div v-if="!restrictedUserCommunity">
          <div class="row px-3 py-1">
            <div class="col-lg mt-2">
              <skills-selector2 :options="allSkills" v-on:added="onSelectedSkill" v-on:removed="onDeselectedSkill"
                                :selected="selectedSkills" :onlySingleSelectedValue="true"
                                data-cy="skillSelector"></skills-selector2>
            </div>
            <div class="col-lg mt-2">
              <project-selector :project-id="projectId" :selected="selectedProject"
                                v-on:selected="onSelectedProject"
                                v-on:unselected="onUnSelectedProject"
                                :only-single-selected-value="true"
                                :disabled="shareWithAllProjects">

              </project-selector>
              <b-form-checkbox v-model="shareWithAllProjects" @change="onShareWithAllProjects " class="mt-2"
                               data-cy="shareWithAllProjectsCheckbox">
                <small>Share With All Projects </small><inline-help msg="Select this checkbox to share the skill with ALL projects."/>
              </b-form-checkbox>
            </div>
          </div>

          <div class="row px-3">
            <div class="col text-center text-sm-left">
              <button class="btn btn-outline-hc h-100" v-on:click="shareSkill"
                      aria-label="Share skill with another project"
                      :disabled="!shareButtonEnabled" data-cy="shareButton">
                <i class="fas fa-share-alt mr-1"></i><span class="text-truncate">Share</span>
              </button>
            </div>
          </div>

          <b-alert v-if="displayError" variant="danger" class="mt-2" show dismissible>
            <i class="fa fa-exclamation-circle"></i> <span v-html="errorMessage"></span>
          </b-alert>

          <loading-container :is-loading="loading.sharedSkills">
          <div v-if="sharedSkills && sharedSkills.length > 0" class="my-4">
            <shared-skills-table :shared-skills="sharedSkills"
                                 v-on:skill-removed="deleteSharedSkill"></shared-skills-table>
          </div>
          <div v-else>
            <no-content2 title="Not Selected Yet..." icon="fas fa-share-alt" class="my-5"
                         message="To make your project's skills eligible please select a skill and then the project that you want to share this skill with."/>
          </div>
        </loading-container>
        </div>
      </loading-container>
  </metrics-card>
</template>

<script>
  import CommunityLabelsMixin from '@/components/utils/CommunityLabelsMixin';
  import SkillsSelector2 from '../SkillsSelector2';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SkillsService from '../SkillsService';
  import ProjectSelector from './ProjectSelector';
  import SharedSkillsTable from './SharedSkillsTable';
  import SkillsShareService from './SkillsShareService';
  import NoContent2 from '../../utils/NoContent2';
  import InlineHelp from '../../utils/InlineHelp';
  import MetricsCard from '../../metrics/utils/MetricsCard';
  import ProjectService from '../../projects/ProjectService';

  const allProjectsConstant = 'ALL_SKILLS_PROJECTS';

  export default {
    name: 'ShareSkillsWithOtherProjects',
    mixins: [CommunityLabelsMixin],
    props: ['projectId'],
    components: {
      MetricsCard,
      NoContent2,
      SharedSkillsTable,
      ProjectSelector,
      LoadingContainer,
      SkillsSelector2,
      InlineHelp,
    },
    data() {
      return {
        loading: {
          allSkills: true,
          sharedSkillsInit: true,
          sharedSkills: false,
          projInfo: true,
        },
        restrictedUserCommunity: false,
        isLoading: true,
        allSkills: [],
        selectedSkills: [],
        sharedSkills: [],
        selectedProject: null,
        displayError: false,
        errorMessage: '',
        shareWithAllProjects: false,
      };
    },
    mounted() {
      this.loadProjectInfo();
      this.loadAllSkills();
      this.loadSharedSkills();
    },
    computed: {
      shareButtonEnabled() {
        return (this.selectedProject || this.shareWithAllProjects) && this.selectedSkills && this.selectedSkills.length > 0 && !this.loading.sharedSkills;
      },
    },
    methods: {
      loadProjectInfo() {
        this.loading.projInfo = true;
        ProjectService.getProject(this.projectId)
          .then((projRes) => {
            this.restrictedUserCommunity = this.isRestrictedUserCommunity(projRes.userCommunity);
          }).finally(() => {
            this.loading.projInfo = false;
          });
      },
      loadAllSkills() {
        this.loading.allSkills = true;
        SkillsService.getProjectSkillsWithoutImportedSkills(this.projectId)
          .then((skills) => {
            this.allSkills = skills;
            this.loading.allSkills = false;
          });
      },
      loadSharedSkills() {
        this.loading.sharedSkills = true;
        return SkillsShareService.getSharedSkills(this.projectId)
          .then((data) => {
            this.sharedSkills = data;
            this.loading.sharedSkillsInit = false;
            this.loading.sharedSkills = false;
          });
      },

      shareSkill() {
        if (this.doesShareAlreadyExist()) {
          this.displayError = true;
        } else {
          this.displayError = false;
          this.loading.sharedSkills = true;
          const selectedSkill = this.selectedSkills[0];
          let sharedProjectId = allProjectsConstant;
          if (!this.shareWithAllProjects) {
            sharedProjectId = this.selectedProject.projectId;
          }
          SkillsShareService.shareSkillToAnotherProject(this.projectId, selectedSkill.skillId, sharedProjectId)
            .then(() => {
              this.loading.sharedSkills = true;
              this.selectedProject = null;
              this.selectedSkills = [];
              this.loadSharedSkills().then(() => {
                const sharedWith = sharedProjectId === allProjectsConstant ? 'All Projects' : sharedProjectId;
                this.$nextTick(() => this.$announcer.assertive(`Skill with id of ${selectedSkill.skillId} was shared with ${sharedWith}`));
              });
            });
        }
      },
      doesShareAlreadyExist() {
        const selectedSkill = this.selectedSkills[0];
        const alreadyExist = this.sharedSkills.find((entry) => entry.skillId === selectedSkill.skillId && (!entry.projectId || this.shareWithAllProjects || entry.projectId === this.selectedProject.projectId));
        if (alreadyExist) {
          if (alreadyExist.sharedWithAllProjects) {
            this.errorMessage = `Skill <strong>[${selectedSkill.name}]</strong> is already shared to <strong>[All Projects]</strong>.`;
          } else {
            this.errorMessage = `Skill <strong>[${selectedSkill.name}]</strong> is already shared to project <strong>[${alreadyExist.projectName}]</strong>.`;
          }
        }
        return alreadyExist;
      },
      deleteSharedSkill(itemToRemove) {
        this.loading.sharedSkills = true;
        let sharedProjectId = allProjectsConstant;
        if (!itemToRemove.sharedWithAllProjects) {
          sharedProjectId = itemToRemove.projectId;
        }
        SkillsShareService.deleteSkillShare(this.projectId, itemToRemove.skillId, sharedProjectId)
          .then(() => {
            this.loadSharedSkills();
          }).finally(() => {
            const sharedWith = sharedProjectId === allProjectsConstant ? 'All Projects' : sharedProjectId;
            this.$nextTick(() => this.$announcer.assertive(`Removed shared skill ${itemToRemove.skillId} from ${sharedWith}`));
          });
      },
      onSelectedProject(item) {
        this.displayError = false;
        this.selectedProject = item;
      },
      onUnSelectedProject() {
        this.displayError = false;
        this.selectedProject = null;
      },
      onSelectedSkill(item) {
        this.displayError = false;
        this.selectedSkills = [item];
      },
      onDeselectedSkill() {
        this.displayError = false;
        this.selectedSkills = [];
      },
      onShareWithAllProjects(checked) {
        this.displayError = false;
        if (checked) {
          this.selectedProject = null;
        }
      },
    },

  };
</script>

<style>

</style>
