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
  <div class="row">
    <div v-for="(navItem) in navCards" :key="navItem.title" class="col-md-6 col-xl-4 mt-2">
      <metric-nav-card :title="navItem.title" :subtitle="navItem.subtitle" :description="navItem.description"
                       :path-name="navItem.pathName"
                       :icon="navItem.icon"
      />
    </div>
  </div>
</template>

<script>
  import MetricNavCard from '@/components/metrics/projectNav/MetricNavCard';

  export default {
    name: 'ProjectMetricsNavCards',
    props: ['navCards'],
    components: { MetricNavCard },
  };
</script>

<style scoped>

</style>
