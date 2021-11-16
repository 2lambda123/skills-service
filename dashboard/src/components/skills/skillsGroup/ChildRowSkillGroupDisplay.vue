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
<div>
  <loading-container v-bind:is-loading="isLoading" :data-cy="`ChildRowSkillGroupDisplay_${group.skillId}`">
    <div class="ml-4 mb-3">
      <b-card v-if="description" header="Description" class="mb-3" body-class="card-bg" data-cy="description">
        <markdown-text :text="description" />
      </b-card>

      <b-card body-class="p-0 card-bg" >
        <div class="row px-3 my-2">
          <div class="col">
            <div class="row align-items-center">
              <div class="col-lg-auto border-right" data-cy="skillGroupStatus">
                <div v-if="this.group.enabled">
                  <span class="font-italic">Status: </span> <span class="text-uppercase"><b-badge variant="success">Live <span class="far fa-check-circle" aria-hidden="true"/></b-badge></span>
                </div>
                <div v-if="!this.group.enabled">
                  <span class="font-italic">Status: </span>
                  <span class="text-uppercase mr-1"><b-badge variant="warning">Disabled</b-badge></span>
                  <span v-b-tooltip.hover="goLiveToolTipText">
                    <b-button variant="outline-info" size="sm" data-cy="goLiveBtn"
                              @click="enableGroup"
                              :disabled="lessThanTwoSkills">
                      <i class="fas fa-glass-cheers"></i> Go Live
                    </b-button>
                  </span>
                </div>
              </div>
              <div class="col-lg mt-2 mt-lg-0" data-cy="requiredSkillsSection">
                <b-form inline>
                  <span>
                    <span class="mr-1 font-italic">Required: </span>
                    <span v-if="!allSkillsRequired">
                      <b-badge variant="info" data-cy="requiredSkillsNum">{{ requiredSkillsNum }}</b-badge>
                      <span class="ml-1">out <b-badge data-cy="numSkillsInGroup">{{ group.numSkillsInGroup }}</b-badge> skills</span>
                    </span>
                    <span v-if="allSkillsRequired" data-cy="requiredAllSkills">
                      <b-badge variant="info" class="text-uppercase">all skills</b-badge>
                    </span>
                  </span>
                  <span v-b-tooltip.hover="editRequiredNumSkillsToolTipText">
                  <b-button variant="outline-info" size="sm"
                            @click="showEditRequiredSkillsDialog"
                            :disabled="lessThanTwoSkills"
                            :aria-label="'Edit Number of Required skills for '+ group.name + ' group'"
                            data-cy="editRequired" class="ml-2"><i class="far fa-edit"  aria-hidden="true"></i></b-button>
                  </span>

                </b-form>

              </div>
            </div>
          </div>
          <div class="col-auto text-right">
            <b-button :id="`group-${group.skillId}_newSkillBtn`" :ref="`group-${group.skillId}_newSkillBtn`" variant="outline-info" size="sm"
                      @click="showNewSkillDialog"
                    :data-cy="`addSkillToGroupBtn-${group.skillId}`" class="ml-1">
              <span class="">Add Skill to Group</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
            </b-button>
          </div>
        </div>
        <div class="mt-3">
          <skills-table :table-id="`groupSkills_${this.group.skillId}`" :ref="`groupSkills_${this.group.skillId}`"
                    :skills-prop="skills" :is-top-level="true"
                    :project-id="this.$route.params.projectId"
                    :subject-id="this.$route.params.subjectId"
                    @skill-removed="skillRemoved"
                    @skills-change="skillChanged"
                    :disableDeleteButtonsInfo="disableDeleteButtonInfo"
                    :can-edit-points="canEditPoints" :can-edit-points-msg="canEditPointsMsg()"
                        :show-search="false" :show-header="false" :show-paging="false"/>
        </div>
      </b-card>
    </div>
  </loading-container>

  <edit-skill v-if="editSkillInfo.show" v-model="editSkillInfo.show" :is-copy="editSkillInfo.isCopy" :is-edit="editSkillInfo.isEdit"
              :project-id="editSkillInfo.skill.projectId" :subject-id="editSkillInfo.skill.subjectId" :group-id="this.group.skillId"
              :can-edit-points="canEditPoints" :can-edit-points-msg="canEditPointsMsg()" :new-skill-default-values="defaultNewSkillValues()"
              @skill-saved="saveSkill" @hidden="focusOnNewSkillButton"/>
  <edit-num-required-skills v-if="editRequiredSkillsInfo.show" v-model="editRequiredSkillsInfo.show"
                            :group="group" :skills="skills" @group-changed="handleNumRequiredSkillsChanged"
              @skills-updated="handleSkillsUpdate"/>
</div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SkillsService from '../SkillsService';
  import EditSkill from '../EditSkill';
  import LoadingContainer from '../../utils/LoadingContainer';
  import MsgBoxMixin from '../../utils/modal/MsgBoxMixin';
  import EditNumRequiredSkills from './EditNumRequiredSkills';
  import MarkdownText from '../../utils/MarkdownText';

  const { mapActions } = createNamespacedHelpers('subjects');

  export default {
    name: 'ChildRowSkillGroupDisplay',
    mixins: [MsgBoxMixin],
    components: {
      MarkdownText,
      EditNumRequiredSkills,
      LoadingContainer,
      SkillsTable: () => import('../SkillsTable'),
      EditSkill,
    },
    props: {
      group: Object,
    },
    data() {
      return {
        loading: {
          details: true,
          skills: true,
        },
        numSkills: 0,
        editSkillInfo: {},
        skills: [],
        description: null,
        editRequiredSkillsInfo: {
          show: false,
        },
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      isLoading() {
        return this.loading.details || this.loading.skills;
      },
      lessThanTwoSkills() {
        return this.numSkills < 2;
      },
      goLiveToolTipText() {
        const disabled = this.numSkills < 2;
        if (disabled) {
          return 'Must have at least 2 skills to go live!';
        }
        return '';
      },
      editRequiredNumSkillsToolTipText() {
        const disabled = this.numSkills < 2;
        if (disabled) {
          return 'Must have at least 2 skills to modify!';
        }
        return '';
      },
      requiredSkillsNum() {
        // -1 == all skills required
        return (this.group.numSkillsRequired === -1) ? this.skills.length : this.group.numSkillsRequired;
      },
      allSkillsRequired() {
        // -1 == all skills required
        return (this.group.numSkillsRequired < 0);
      },
      canEditPoints() {
        return (this.group.numSkillsRequired === -1);
      },
      disableDeleteButtonInfo() {
        let res = null;
        if (this.group.enabled) {
          if (this.group.numSkillsRequired > 0 && this.group.numSkillsRequired === this.skills.length) {
            res = {
              minNumSkills: this.group.numSkillsRequired,
              tooltip: 'Cannot delete! Cannot go below the number of the required skills.',
            };
          } else {
            res = { minNumSkills: 2, tooltip: 'Cannot delete! Groups that went Live must have at least 2 skill.' };
          }
        }
        return res;
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      canEditPointsMsg() {
        return (this.group.numSkillsRequired === -1) ? null : 'Points CANNOT be modified when group\'s number of the required skill is set.';
      },
      loadData() {
        this.loading.skills = true;
        this.loading.details = true;

        SkillsService.getSkillDetails(this.group.projectId, this.group.subjectId, this.group.skillId)
          .then((res) => {
            this.description = res.description;
          }).finally(() => {
            this.loading.details = false;
          });

        SkillsService.getGroupSkills(this.group.projectId, this.group.skillId)
          .then((res) => {
            this.setInternalSkills(res);
          }).finally(() => {
            this.loading.skills = false;
          });
      },
      handleSkillsUpdate(skills) {
        this.setInternalSkills(skills);
        this.$refs[`groupSkills_${this.group.skillId}`].loadDataFromParams(skills);
        this.refreshSubjectState();
      },
      setInternalSkills(skillsParam) {
        this.numSkills = skillsParam.length;
        this.skills = skillsParam.map((skill) => ({ ...skill, subjectId: this.group.subjectId }));
      },
      showNewSkillDialog() {
        this.editSkillInfo = {
          skill: {
            projectId: this.group.projectId,
            subjectId: this.group.subjectId,
            type: 'Skill',
          },
          show: true,
          isEdit: false,
          isCopy: false,
        };
      },
      showEditRequiredSkillsDialog() {
        this.editRequiredSkillsInfo = {
          show: true,
        };
      },
      saveSkill(skill) {
        const copy = { groupId: this.group.skillId, ...skill };
        this.$refs[`groupSkills_${this.group.skillId}`].skillCreatedOrUpdated(copy)
          .then(() => {
            this.numSkills += 1;
            const updatedGroup = {
              ...this.group,
              numSkillsInGroup: this.group.numSkillsInGroup + 1,
              numSkillsRequired: this.group.numSkillsRequired,
              totalPoints: this.group.totalPoints + (skill.pointIncrement * skill.numPerformToCompletion),
            };
            this.$emit('group-changed', updatedGroup);
            this.refreshSubjectState();
          });
      },
      refreshSubjectState(forceRefresh = false) {
        if (this.group.enabled || forceRefresh) {
          this.loadSubjectDetailsState({ projectId: this.group.projectId, subjectId: this.group.subjectId });
        }
      },
      skillRemoved(skill) {
        this.numSkills -= 1;
        this.skills = this.skills.filter((item) => item.skillId !== skill.skillId);
        const updatedGroup = {
          ...this.group,
          numSkillsInGroup: this.group.numSkillsInGroup - 1,
          numSkillsRequired: this.group.numSkillsRequired === this.numSkills ? -1 : this.group.numSkillsRequired,
          totalPoints: this.group.totalPoints - (skill.pointIncrement * skill.numPerformToCompletion),
        };
        this.$emit('group-changed', updatedGroup);
        this.refreshSubjectState();
      },
      skillChanged(skill) {
        const item1Index = this.skills.findIndex((item) => item.skillId === skill.originalSkillId);
        if (item1Index >= 0) {
          const removedSkill = this.skills[item1Index];
          this.skills.splice(item1Index, 1, skill);
          const updatedGroup = {
            ...this.group,
            totalPoints: this.group.totalPoints - (removedSkill.pointIncrement * removedSkill.numPerformToCompletion) + (skill.pointIncrement * skill.numPerformToCompletion),
          };
          this.$emit('group-changed', updatedGroup);
        } else {
          this.skills.push(skill);
        }
        this.refreshSubjectState();
      },
      handleNumRequiredSkillsChanged(updatedGroup) {
        SkillsService.saveSkill(updatedGroup).then(() => {
          this.$emit('group-changed', updatedGroup);
        });
      },
      focusOnNewSkillButton() {
        const ref = this.$refs[`group-${this.group.skillId}_newSkillBtn`];
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
      enableGroup() {
        const msg = `While this Group is disabled, user's cannot see the group or achieve it. Once the group is live, it will be visible to users.
        Please note that once the group is live, it cannot be disabled.`;
        this.msgConfirm(msg, 'Please Confirm!', 'Yes, Go Live!')
          .then((res) => {
            if (res) {
              const copy = { ...this.group, enabled: true };
              SkillsService.saveSkill(copy).then((savedGroup) => {
                this.$emit('group-changed', savedGroup);
                this.refreshSubjectState(true);
              });
            }
          });
      },
      defaultNewSkillValues() {
        if (this.group.numSkillsRequired === -1) {
          return null;
        }
        return {
          pointIncrement: this.skills[0].pointIncrement,
          numPerformToCompletion: this.skills[0].numPerformToCompletion,
        };
      },
    },
  };
</script>

<style scoped>
.card-bg {
  background-color: rgba(0,124,73,0.04) !important;
}
</style>