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
  <metrics-card title="Find users across multiple projects" data-cy="multiProjectUsersInCommon" :no-padding="true">
    <skills-spinner :is-loading="projects.loading" class="mb-5"/>
    <div v-if="enoughOverallProjects && !projects.loading">
      <div class="p-2 px-4">
        <v-select :options="projects.available"
                  v-model="projects.selected"
                  :loading="projects.loading"
                  :multiple="true"
                  :close-on-select="false"
                  label="name"
                  placeholder="Select option"
                  :selectable="() => projects.selected.length < 5"
                  v-on:option:selected="projAdded"
                  v-on:option:deselecting="projRemoved"
                  data-cy="projectSelector">
          <template v-if="beforeListSlotText" #list-header>
            <li>
              <div class="h6 ml-1"> {{ beforeListSlotText }}</div>
            </li>
          </template>
        </v-select>
      </div>
      <div class="my-3">
      <no-content2 v-if="!atLeast1Proj" title="No Projects Selected"
                   message="Please select at least 2 projects using search above then click 'Find Users' button below"></no-content2>

        <b-table v-if="atLeast1Proj"
                 striped
                 :items="projects.selected"
                 :fields="fields"
                 stacked="md"
                 :no-sort-reset="true"
                 aria-label="Find users across multiple projects"
                 data-cy="multiProjectUsersInCommon-inputProjs">
          <template v-slot:cell(minLevel)="data">
            <b-row>
              <b-col>
                <b-form-select v-if="!data.item.loadingLevels"
                               v-model="data.item.minLevel"
                               :options="data.item.availableLevels"
                               data-cy="minLevelSelector" aria-label="minimum level select"></b-form-select>
              </b-col>
              <b-col cols="auto">
                <b-button variant="outline-info"
                          v-b-tooltip.hover title="Sync other levels"
                          aria-label="Sync other levels"
                          @click="syncOtherLevels(data.item.minLevel)"
                          data-cy="syncLevelButton">
                  <i class="fas fa-sync"></i>
                </b-button>
              </b-col>
            </b-row>
          </template>
          <template v-slot:cell(numSkills)="data">
            {{ data.value | number }}
          </template>
          <template v-slot:cell(totalPoints)="data">
            {{ data.value | number }}
          </template>
        </b-table>
      </div>
      <div class="text-center my-2 mb-4"
           v-b-tooltip.hover
           :title="atLeast2Proj ? '' : 'Please select at least 2 projects'">
        <b-button variant="outline-info"
                  :disabled="!atLeast2Proj"
                  @click="locateUsers"
                  data-cy="findUsersBtn">
          Find Users <i class="fas fa-search-plus"></i>
        </b-button>
      </div>

      <skills-b-table v-if="hasResults || resultsLoaded"
                      :items="results"
                      :options="resultTableOptions"
                      @sort-changed="sortTable"
                      @page-changed="pageChanged"
                      @page-size-changed="pageSizeChanged"
                      tabindex="0"
                      aria-label="Find users across multiple projects results"
                      tableStoredStateId="usersInCommonResultTable"
                      data-cy="usersInCommonResultTable">
        <template v-slot:cell(0)="data">
          <level-badge :level="data.value" />
        </template>
        <template v-slot:cell(1)="data">
          <level-badge :level="data.value" />
        </template>
        <template v-slot:cell(2)="data">
          <level-badge :level="data.value" />
        </template>
        <template v-slot:cell(3)="data">
          <level-badge :level="data.value" />
        </template>
        <template v-slot:cell(4)="data">
          <level-badge :level="data.value" />
        </template>
      </skills-b-table>
    </div>
    <no-content2 v-if="!enoughOverallProjects"
                 class="my-5"
                 title="Feature is disabled"
                 icon="fas fa-poo"
                 message="At least 2 projects must exist for this feature to work. Please create more projects to enable this feature."/>
  </metrics-card>
</template>

<script>
  import vSelect from 'vue-select';
  import MetricsCard from '../utils/MetricsCard';
  import NoContent2 from '../../utils/NoContent2';
  import SkillsBTable from '../../utils/table/SkillsBTable';
  import SupervisorService from '../../utils/SupervisorService';
  import SkillsSpinner from '../../utils/SkillsSpinner';
  import MetricsService from '../MetricsService';
  import LevelBadge from './LevelBadge';

  export default {
    name: 'MultipleProjUsersInCommon',
    components: {
      LevelBadge,
      SkillsSpinner,
      SkillsBTable,
      NoContent2,
      MetricsCard,
      vSelect,
    },
    props: ['availableProjects'],
    data() {
      return {
        fields: ['name', 'numSubjects', 'numBadges', 'numSkills', 'totalPoints', 'minLevel'],
        projects: {
          loading: true,
          available: [],
          selected: [],
        },
        selectProjects: [],
        results: [],
        resultsLoaded: false,
        resultTableOptions: {
          busy: false,
          sortBy: 'userId',
          sortDesc: false,
          bordered: true,
          outlined: true,
          rowDetailsControls: false,
          stacked: 'md',
          fields: [
            {
              key: 'userId',
              label: 'User',
              sortable: true,
            },
          ],
          pagination: {
            server: true,
            currentPage: 1,
            totalRows: 1,
            pageSize: 5,
            possiblePageSizes: [5, 10, 15, 20, 50],
          },
        },
      };
    },
    computed: {
      atLeast1Proj() {
        return this.projects.selected && this.projects.selected.length > 0;
      },
      atLeast2Proj() {
        return this.projects.selected && this.projects.selected.length > 1;
      },
      hasResults() {
        return this.results && this.results.length > 0;
      },
      enoughOverallProjects() {
        return this.availableProjects && this.availableProjects.length >= 2;
      },
      beforeListSlotText() {
        if (this.projects.selected.length >= 5) {
          return 'Maximum of 5 options selected. First remove a selected option to select another.';
        }
        return '';
      },
    },
    mounted() {
      this.loadProjects();
    },
    watch: {
      'projects.selected': function rebuild() {
        this.rebuildFields();
      },
    },
    methods: {
      pageChanged(pageNum) {
        this.resultTableOptions.pagination.currentPage = pageNum;
        this.locateUsers();
      },
      pageSizeChanged(newSize) {
        this.resultTableOptions.pagination.pageSize = newSize;
        this.locateUsers();
      },
      sortTable(sortContext) {
        this.resultTableOptions.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.resultTableOptions.pagination.currentPage = 1;
        this.locateUsers();
      },
      projAdded(addedItem) {
        this.clearRes();

        const refProj = addedItem[addedItem.length - 1];
        SupervisorService.getProjectLevels(refProj.projectId)
          .then((res) => {
            refProj.availableLevels = res.map((r) => r.level);
          }).finally(() => {
            refProj.loadingLevels = false;
          });
      },
      projRemoved() {
        this.clearRes();
      },
      rebuildFields() {
        this.resultTableOptions.fields = this.projects.selected.map((projItem, index) => ({
          projectId: projItem.projectId,
          key: `${index}`,
          label: projItem.name,
          sortable: false,
        }));
        this.resultTableOptions.fields.splice(0, 0, {
          key: 'userId',
          label: 'User',
          sortable: true,
        });
        this.loadProjects();
        this.projects.available = this.projects.available.filter((el) => !this.projects.selected.some((sel) => sel.projectId === el.projectId));
      },
      clearRes() {
        this.results = [];
        this.resultsLoaded = false;
      },
      locateUsers() {
        this.resultTableOptions.busy = true;
        this.resultsLoaded = true;

        const params = {
          pageSize: this.resultTableOptions.pagination.pageSize,
          currentPage: this.resultTableOptions.pagination.currentPage,
          sortDesc: this.resultTableOptions.sortDesc,
          projIdsAndLevel: this.projects.selected.map((item) => `${item.projectId}AndLevel${item.minLevel}`).join(','),
        };
        MetricsService.loadGlobalMetrics('findExpertsForMultipleProjectsChartBuilder', params)
          .then((dataFromServer) => {
            this.resultTableOptions.busy = false;
            this.resultTableOptions.pagination.totalRows = dataFromServer.totalNum;
            this.results = dataFromServer.data.map((item) => {
              const res = { userId: item.userId };
              item.levels.forEach((level) => {
                const keyForLevel = this.resultTableOptions.fields.find((p) => p.projectId === level.projectId);
                res[`${keyForLevel.key}`] = level.level;
              });
              return res;
            });
          });
      },
      loadProjects() {
        this.projects.available = this.availableProjects.map((proj) => ({
          loadingLevels: true,
          minLevel: 1,
          ...proj,
        }));
        this.projects.loading = false;
      },
      syncOtherLevels(level) {
        for (let i = 0; i < this.projects.selected.length; i += 1) {
          const maxLevel = Math.max(...this.projects.selected[i].availableLevels);
          this.projects.selected[i].minLevel = level > maxLevel ? maxLevel : level;
        }
      },
    },
  };
</script>

<style scoped>

</style>
