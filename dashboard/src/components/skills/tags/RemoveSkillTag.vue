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
  <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
    <b-modal :id="projectId" size="md" title="Remove Tag From Selected Skills" v-model="show"
             :no-close-on-backdrop="true" :centered="true"
             header-bg-variant="info"
             @hide="cancel"
             header-text-variant="light" no-fade
             role="dialog"
             aria-label="'Remove Tag from Skills in this project'">
      <b-container fluid>
        <div v-if="hasExistingTags">
          <label for="existingTag">Select Tag to Remove</label>
          <div id="existingTag">
            <v-select v-model="selectedTagValue" :options="existingTags" label="tagValue"
                      data-cy="existingTagDropdown"
                      placeholder="Select Tag" :loading="isLoading">
            </v-select>
          </div>
        </div>
        <div v-else>
          <div>
            The selected skills do not have any tags.
          </div>
        </div>
      </b-container>
      <div slot="modal-footer" class="w-100">
        <div v-if="hasExistingTags">
          <b-button variant="success"
                    size="sm"
                    class="float-right"
                    @click="handleSubmit(deleteTagForSkills)"
                    :disabled="invalid || !selectedTagValue"
                    v-skills="'AddOrModifyTags'"
                    data-cy="deleteTagsButton">
            Delete
          </b-button>
          <b-button variant="secondary" size="sm" class="float-right mr-2" @click="cancel" data-cy="cancelDeleteTagsButton">
            Cancel
          </b-button>
        </div>
        <div v-else>
          <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close" data-cy="cancelDeleteTagsButton">
            OK
          </b-button>
        </div>
      </div>
    </b-modal>
  </ValidationObserver>
</template>

<script>
  import vSelect from 'vue-select';
  import SkillsService from '@/components/skills/SkillsService';

  export default {
    name: '',
    props: {
      skills: {
        type: Array,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    components: { vSelect },
    data() {
      return {
        isLoading: false,
        show: this.value,
        selectedTagValue: null,
        existingTags: [],
      };
    },
    mounted() {
      this.loadExistingTags();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      projectId() {
        return this.$route.params.projectId;
      },
      hasExistingTags() {
        return this.existingTags && this.existingTags.length;
      },
    },
    methods: {
      deleteTagForSkills() {
        if (this.selectedTagValue) {
          const skillIds = this.skills.map((skill) => skill.skillId);
          SkillsService.deleteTagForSkills(this.$route.params.projectId, skillIds, this.selectedTagValue.tagId)
            .then(() => {
              this.$emit('action-success', {
                skills: this.skills,
                tag: this.selectedTagValue,
              });
              this.show = false;
            });
        }
      },
      cancel(e) {
        this.show = false;
        this.publishHidden(e, true);
      },
      close(e) {
        this.show = false;
        this.publishHidden(e, false);
      },
      publishHidden(e, cancelled) {
        this.$emit('hidden', {
          ...e,
          cancelled,
        });
      },
      loadExistingTags() {
        this.isLoading = true;
        const skillIds = this.skills.map((skill) => skill.skillId);
        SkillsService.getTagsForSkills(this.projectId, skillIds)
          .then((res) => {
            this.existingTags = res;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>

<style scoped>
</style>
