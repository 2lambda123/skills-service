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
    <span v-if="!value" class="text-primary">
      <b-badge variant="warning">Never</b-badge>
    </span>
    <span v-else-if="isToday(value)" class="text-primary">
      <b-badge variant="info">Today</b-badge>
    </span>
    <span v-else class="text-primary small">
      {{ fromNow }}
    </span>
</template>

<script>
  import dayjs from '@/common-components/DayJsCustomizer';

  export default {
    name: 'SlimDateCell',
    props: {
      value: String,
      fromStartOfDay: {
        type: Boolean,
        default: false,
      },
      cssClass: {
        type: String,
        default: 'text-secondary small',
      },
    },
    computed: {
      fromNow() {
        if (this.fromStartOfDay) {
          return dayjs().startOf('day').to(dayjs(this.value));
        }
        return dayjs(this.value).startOf('seconds').fromNow();
      },
    },
    methods: {
      isToday(timestamp) {
        return dayjs().utc()
          .isSame(dayjs(timestamp), 'day');
      },
    },
  };
</script>

<style scoped>

</style>
