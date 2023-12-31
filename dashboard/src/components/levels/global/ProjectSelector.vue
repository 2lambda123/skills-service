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
  <div id="project-selector">
    <v-select :options="projectsInternal"
              v-model="selectedInternal"
              placeholder="Select Project..."
              :filterable="internalSearch"
              label="name"
              v-on:search="searchChanged"
              v-on:input="inputChanged"
              :loading="isLoading">
      <template #option="{ name, projectId }">
        <div :data-cy="`${projectId}_option`">
          <div class="h6">{{ name }}</div>
          <div class="text-secondary">ID: {{ projectId }}</div>
        </div>
      </template>
      <template v-if="afterListSlotText" #list-footer>
        <li>
          <div class="h6 ml-1" data-cy="projectSelectorCountMsg"> {{ afterListSlotText }}</div>
        </li>
      </template>
    </v-select>
  </div>
</template>

<script>
  import vSelect from 'vue-select';

  export default {
    name: 'ProjectSelector',
    components: { vSelect },
    props: {
      value: {
        type: Object,
      },
      projects: {
        type: Array,
        required: true,
      },
      internalSearch: {
        type: Boolean,
        default: true,
      },
      afterListSlotText: {
        type: String,
        default: '',
      },
      isLoading: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        selectedInternal: null,
        badgeId: null,
        projectsInternal: [],
      };
    },
    mounted() {
      this.badgeId = this.$route.params.badgeId;
      this.setSelectedInternal();
    },
    watch: {
      value: function watchUpdatesToSelected() {
        this.setSelectedInternal();
      },
      projects: function updateProjects() {
        this.projectsInternal = this.projects;
      },
    },
    methods: {
      setSelectedInternal() {
        if (this.value) {
          this.selectedInternal = { ...this.value };
        } else {
          this.selectedInternal = null;
        }
      },
      removed(removedItem) {
        this.$emit('removed', removedItem);
      },
      added(addedItem) {
        this.$emit('input', addedItem);
        this.$emit('added', addedItem);
      },
      inputChanged(inputItem) {
        if (inputItem != null) {
          this.added(inputItem);
        } else {
          this.removed(null);
        }
      },
      searchChanged(query, loadingFunction) {
        this.$emit('search-change', query, loadingFunction);
      },
    },
  };
</script>

<style>
</style>
