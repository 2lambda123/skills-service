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
  <b-card body-class="p-0 d-flex flex-column">
    <div class="flex-grow-1">
      <b-row class="justify-content-between no-gutters px-3 pt-3 pb-1">
        <b-col cols="5">
          <div class="text-uppercase text-secondary">Skills</div>
          <div class="mt-2 ml-1 text-dark">
            <span style="font-size: 2.5rem;" data-cy="numAchievedSkills">{{ numAchievedSkills }}</span>
          </div>
          <div><b-badge variant="info" data-cy="numSkillsAvailable">Total: {{ totalSkills }}</b-badge></div>
        </b-col>
        <b-col cols="7">
          <apexchart type="line" height="158" :options="chartOptions" :series="series"></apexchart>
        </b-col>
      </b-row>
      </div>
      <b-row class="justify-content-between no-gutters border-top text-muted small" style="min-height: 3rem;">
        <b-col class="p-2">
          <span data-cy="num-skills-footer">So many skills... so little time! Good luck!</span>
        </b-col>
      </b-row>
  </b-card>
</template>

<script>
  export default {
    name: 'NumSkills',
    props: {
      totalSkills: {
        type: Number,
        required: true,
      },
      numAchievedSkills: {
        type: Number,
        required: true,
      },
    },
    data() {
      return {
        series: [{
          data: [10, 41, 35, 51, 49, 62, 69, 91, 148],
        }],
        chartOptions: {
          chart: {
            height: 135,
            type: 'line',
            zoom: {
              enabled: false,
            },
            toolbar: {
              show: false,
            },
          },
          dataLabels: {
            enabled: false,
          },
          stroke: {
            curve: 'straight',
          },
          grid: {
            padding: {
              top: -10,
              bottom: -15,
            },
            row: {
              colors: ['transparent', 'transparent'], // takes an array which will be repeated on columns
              opacity: 0.5,
            },
          },
          xaxis: {
            labels: {
              show: false,
            },
          },
          yaxis: {
            labels: {
              show: false,
            },
          },
          tooltip: {
            enabled: false,
          },
        },
      };
    },
  };
</script>

<style scoped>

</style>
