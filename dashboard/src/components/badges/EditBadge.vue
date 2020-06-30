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
  <b-modal :id="badgeInternal.badgeId" size="xl" :title="title" v-model="show" :no-close-on-backdrop="true"
           header-bg-variant="info" header-text-variant="light" no-fade >
    <ValidationObserver ref="observer" v-slot="{invalid}" slim>
      <b-container fluid>
        <div v-if="displayIconManager === false" class="text-left">
          <div class="media">
            <icon-picker :startIcon="badgeInternal.iconClass" @select-icon="toggleIconDisplay(true)" class="mr-3"></icon-picker>
            <div class="media-body">
              <div class="form-group">
                <label for="badgeName">Badge Name</label>
                <ValidationProvider rules="required|minNameLength|maxBadgeNameLength|uniqueName|customNameValidator" v-slot="{errors}" name="Badge Name">
                  <input v-focus class="form-control" id="badgeName" type="text" v-model="badgeInternal.name"
                         @input="updateBadgeId"
                         data-vv-name="badgeName"/>
                  <small class="form-text text-danger" v-show="errors[0]">{{ errors[0] }}
                  </small>
                </ValidationProvider>
              </div>
            </div>
          </div>

          <id-input type="text" label="Badge ID" v-model="badgeInternal.badgeId" @input="canAutoGenerateId=false"
                    additional-validation-rules="uniqueId"/>

          <div class="mt-2">
            <label>Description</label>
            <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" v-slot="{errors}" name="Badge Description">
              <markdown-editor v-model="badgeInternal.description" @input="updateDescription"></markdown-editor>
              <small class="form-text text-danger mb-3">{{ errors[0] }}</small>
            </ValidationProvider>
          </div>

          <div>
            <label>Help URL/Path
              <inline-help
                msg="If project level 'Root Help Url' is specified then this path will be relative to 'Root Help Url'"/>
            </label>
            <input class="form-control" type="text" v-model="badgeInternal.helpUrl" data-vv-name="helpUrl"/>
            <small class="form-text text-danger">{{ errors.first('helpUrl')}}</small>
          </div>

          <div v-if="!global" data-cy="gemEditContainer">
            <b-form-checkbox v-model="limitTimeframe" class="mt-4"
                             @change="onEnableGemFeature" data-cy="enableGemCheckbox">
                Enable Gem Feature <inline-help msg="The Gem feature allows for the badge to only be achievable during the specified time frame."/>
            </b-form-checkbox>

            <b-collapse id="gemCollapse" v-model="limitTimeframe">
                <b-row v-if="limitTimeframe" no-gutters class="justify-content-md-center mt-3" key="gemTimeFields">
                  <b-col cols="12" md="4" style="min-width: 20rem;">
                    <label class="label mt-2">Start Date</label>
                    <ValidationProvider rules="required|dateOrder" v-slot="{errors}" name="Start Date" ref="startDateValidationProvider">
                      <datepicker :inline="true" v-model="badgeInternal.startDate" name="startDate" key="gemFrom" data-cy="startDatePicker"></datepicker>
                      <small class="form-text text-danger" v-show="errors[0]">{{ errors[0] }}
                      </small>
                    </ValidationProvider>
                  </b-col>
                  <b-col cols="12" md="4"  style="min-width: 20rem;">
                    <label class="label mt-2">End Date</label>
                    <ValidationProvider rules="required|dateOrder|noHistoricalEnd" v-slot="{errors}" name="End Date" ref="endDateValidationProvider">
                      <datepicker :inline="true" v-model="badgeInternal.endDate" name="endDate"
                                  key="gemTo" data-cy="endDatePicker"></datepicker>
                      <small class="form-text text-danger" v-show="errors[0]">{{ errors[0] }}</small>
                    </ValidationProvider>
                  </b-col>
                </b-row>
            </b-collapse>
          </div>
          <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-3">***{{ overallErrMsg }}***</p>
        </div>
        <div v-else>
          <icon-manager @selected-icon="onSelectedIcon"></icon-manager>
          <div class="text-right mr-2">
            <b-button variant="secondary" @click="toggleIconDisplay(false)" class="mt-4">Cancel Icon Selection</b-button>
          </div>
        </div>
      </b-container>
    </ValidationObserver>

    <div slot="modal-footer" class="w-100">
      <div v-if="displayIconManager === false">
        <b-button variant="success" size="sm" class="float-right" @click="updateBadge">
          Save
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="closeMe">
          Cancel
        </b-button>
      </div>
    </div>
  </b-modal>
</template>

<script>
  import { Validator, ValidationProvider, ValidationObserver } from 'vee-validate';
  import Datepicker from 'vuejs-datepicker';
  import MarkdownEditor from '../utils/MarkdownEditor';
  import IconPicker from '../utils/iconPicker/IconPicker';
  import IconManager from '../utils/iconPicker/IconManager';
  import IdInput from '../utils/inputForm/IdInput';
  import InlineHelp from '../utils/InlineHelp';
  import BadgesService from './BadgesService';
  import GlobalBadgeService from './global/GlobalBadgeService';
  import InputSanitizer from '../utils/InputSanitizer';


  const dictionary = {
    en: {
      attributes: {
        badgeName: 'Badge Name',
        badgeId: 'ID',
        requiredSkills: 'Required Skills',
        startDate: 'Start Date',
        endDate: 'End Date',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'EditBadge',
    components: {
      InlineHelp,
      IconPicker,
      MarkdownEditor,
      Datepicker,
      IconManager,
      IdInput,
      ValidationProvider,
      ValidationObserver,
    },
    props: {
      badge: Object,
      isEdit: Boolean,
      value: Boolean,
      global: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      // convert string to Date objects
      this.badge.startDate = this.toDate(this.badge.startDate);
      this.badge.endDate = this.toDate(this.badge.endDate);
      const limitedTimeframe = !!(this.badge.startDate && this.badge.endDate);
      return {
        canAutoGenerateId: true,
        canEditBadgeId: false,
        badgeInternal: Object.assign({ originalBadgeId: this.badge.badgeId, isEdit: this.isEdit }, this.badge),
        overallErrMsg: '',
        limitTimeframe: limitedTimeframe,
        show: this.value,
        displayIconManager: false,
      };
    },
    created() {
      this.assignCustomValidation();
    },
    computed: {
      title() {
        return this.isEdit ? 'Editing Existing Badge' : 'New Badge';
      },
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      closeMe() {
        this.show = false;
      },
      updateDescription(event) {
        this.badgeInternal.description = event;
      },
      updateBadge() {
        this.$refs.observer.validate().then((res) => {
          if (!res) {
            this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
          } else {
            this.show = false;
            this.badgeInternal.badgeId = InputSanitizer.sanitize(this.badgeInternal.badgeId);
            this.badgeInternal.name = InputSanitizer.sanitize(this.badgeInternal.name);
            this.$emit('badge-updated', this.badgeInternal);
          }
        });
      },
      updateBadgeId() {
        if (!this.isEdit && this.canAutoGenerateId) {
          let id = InputSanitizer.removeSpecialChars(this.badgeInternal.name);
          // Subjects, skills and badges can not have same id under a project
          // by default append Badge to avoid id collision with other entities,
          // user can always override in edit mode
          if (id) {
            id = `${id}Badge`;
          }
          this.badgeInternal.badgeId = id;
        }
      },
      onSelectedIcon(selectedIcon) {
        this.badgeInternal.iconClass = `${selectedIcon.css}`;
        this.displayIconManager = false;
      },
      onEnableGemFeature(value) {
        if (!value) {
          this.$nextTick(() => {
            this.badgeInternal.startDate = null;
            this.badgeInternal.endDate = null;
          });
        }
      },
      toDate(value) {
        let dateVal = value;
        if (value && !(value instanceof Date)) {
          dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
        }
        return dateVal;
      },
      toggleEditId() {
        this.canEditBadgeId = !this.canEditBadgeId && !this.isEdit;
        this.updateBadgeId();
      },
      toggleIconDisplay(shouldDisplay) {
        this.displayIconManager = shouldDisplay;
      },
      assignCustomValidation() {
        // only want to validate for a new badge, existing subjects will override
        // name and badge id
        const self = this;
        Validator.extend('uniqueName', {
          getMessage: field => `The value for ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && (value === self.badge.name || self.badge.name.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
              return true;
            }
            if (self.global) {
              return GlobalBadgeService.badgeWithNameExists(value);
            }
            return BadgesService.badgeWithNameExists(self.badgeInternal.projectId, value);
          },
        }, {
          immediate: false,
        });

        Validator.extend('uniqueId', {
          getMessage: field => `The value for ${field} is already taken.`,
          validate(value) {
            if (self.isEdit && self.badge.badgeId === value) {
              return true;
            }
            if (self.global) {
              return GlobalBadgeService.badgeWithIdExists(value);
            }
            return BadgesService.badgeWithIdExists(self.badgeInternal.projectId, value);
          },
        }, {
          immediate: false,
        });

        Validator.extend('dateOrder', {
          getMessage: 'Start Date must come before End Date',
          validate() {
            let valid = true;
            if (self.limitTimeframe && self.badgeInternal.startDate && self.badgeInternal.endDate) {
              valid = window.moment(self.badgeInternal.startDate).isBefore(self.badgeInternal.endDate);
              if (valid) {
                // manually clear errors in case the orig error occurred when setting startDate,
                // but was fixed by updating endDate (or vise-versa)
                if (self.$refs.startDateValidationProvider) {
                  self.$refs.startDateValidationProvider.reset();
                }
                if (self.$refs.endDateValidationProvider) {
                  self.$refs.endDateValidationProvider.reset();
                }
              }
            }
            return valid;
          },
        }, {
          immediate: false,
        });

        Validator.extend('noHistoricalEnd', {
          getMessage: 'End Date cannot be in the past',
          validate() {
            let valid = true;
            // only trigger this validation on new badge entry, not edits
            if (self.limitTimeframe && self.badgeInternal.endDate && !self.badge.badgeId) {
              valid = window.moment(self.badgeInternal.endDate).isAfter(new Date());
            }
            return valid;
          },
        }, {
          immediate: false,
        });
      },
    },
  };

</script>

<style scoped>
</style>