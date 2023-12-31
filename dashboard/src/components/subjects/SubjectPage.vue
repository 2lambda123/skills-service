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
    <page-header :loading="isLoadingData" :options="headerOptions">
      <div slot="subSubTitle" v-if="subject && !isReadOnlyProj">
        <b-button @click="displayEditSubject"
                  ref="editSubjectButton"
                  class="btn btn-outline-primary mr-1"
                  size="sm"
                  variant="outline-primary"
                  data-cy="btn_edit-subject"
                  :aria-label="'edit Subject '+subject.subjectId">
          <span class="">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
        </b-button>
      </div>
      <div slot="footer">
        <import-finalize-alert />
      </div>
    </page-header>

    <navigation v-if="!isLoadingData" :nav-items="navItems">
    </navigation>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject"
                  :subject="subject" @subject-saved="subjectEdited"
                  :is-edit="true"
                  @hidden="handleHideSubjectEdit"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import ImportFinalizeAlert from '@/components/skills/catalog/ImportFinalizeAlert';
  import Navigation from '@/components/utils/Navigation';
  import PageHeader from '@/components/utils/pages/PageHeader';
  import EditSubject from '@/components/subjects/EditSubject';
  import SubjectsService from '@/components/subjects/SubjectsService';
  import projConfigMixin from '@/components/projects/ProjConfigMixin';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('subjects');

  export default {
    name: 'SubjectPage',
    mixins: [projConfigMixin],
    components: {
      ImportFinalizeAlert,
      PageHeader,
      Navigation,
      EditSubject,
    },
    data() {
      return {
        isLoadingSubjects: true,
        projectId: '',
        subjectId: '',
        showEditSubject: false,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
    },
    mounted() {
      this.loadSubject();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      isLoadingData() {
        return this.isLoadingSubjects || this.isLoadingProjConfig;
      },
      navItems() {
        const items = [
          { name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'SubjectSkills' },
        ];

        if (!this.isReadOnlyProj) {
          items.push({ name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'SubjectLevels' });
        }
        items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SubjectUsers' });
        items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SubjectMetrics' });

        return items;
      },
      headerOptions() {
        if (!this.subject) {
          return {};
        }
        return {
          icon: 'fas fa-cubes skills-color-subjects',
          title: `SUBJECT: ${this.subject.name}`,
          subTitle: `ID: ${this.subjectId}`,
          stats: [{
            label: 'Groups',
            count: this.subject.numGroups,
            disabledCount: this.subject.numGroupsDisabled,
            icon: 'fas fa-layer-group skills-color-groups',
          }, {
            label: 'Skills',
            count: this.subject.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
            secondaryStats: [{
              label: 'reused',
              count: this.subject.numSkillsReused,
              badgeVariant: 'info',
            }, {
              label: 'disabled',
              count: this.subject.numSkillsDisabled,
              badgeVariant: 'warning',
            }],
          }, {
            label: 'Points',
            count: this.subject.totalPoints,
            warn: this.subject.totalPoints < this.minimumPoints,
            warnMsg: (this.subject.totalPoints + this.subject.totalPointsReused) < this.minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${this.minimumPoints} points.` : null,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
            secondaryStats: [{
              label: 'reused',
              count: this.subject.totalPointsReused,
              badgeVariant: 'info',
            }],
          }],
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
      },
    },
    watch: {
      subject(newVal) {
        if (newVal && newVal.subjectId !== this.subjectId) {
          this.subjectId = newVal.subjectId;
        }
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      ...mapMutations([
        'setSubject',
      ]),
      loadSubject() {
        this.isLoadingSubjects = true;
        if (this.$route.params.subject) {
          this.setSubject(this.$route.params.subject);
          this.isLoadingSubjects = false;
        } else {
          this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subjectId })
            .finally(() => {
              this.isLoadingSubjects = false;
            });
        }
      },
      displayEditSubject() {
        this.showEditSubject = true;
      },
      subjectEdited(subject) {
        SubjectsService.saveSubject(subject).then((resp) => {
          const origId = this.subject.subjectId;
          this.setSubject(resp);
          if (resp.subjectId !== origId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, subjectId: resp.subjectId } });
          }
          this.$nextTick(() => {
            this.$announcer.polite(`Subject ${subject.name} has been edited`);
          });
        });
      },
      handleHideSubjectEdit() {
        this.showEditSubject = false;
        // if the id is edited, the route is reloaded which causes the focus to be moved to the container element
        // as such, if we want the edit button receive focus after the id has been altered, we need to double
        // the nextTick wait.
        this.$nextTick(() => {
          this.$nextTick(() => {
            const ref = this.$refs?.editSubjectButton;
            if (ref) {
              ref.focus();
            }
          });
        });
      },
    },
  };
</script>

<style scoped>
</style>
