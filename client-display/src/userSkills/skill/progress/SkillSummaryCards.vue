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
    <div class="row mt-0 px-3 justify-content-center" data-cy="skillsSummaryCards">
        <div class="mb-2 px-1 col-md-6 col-lg">
          <progress-info-card :title="skill.points" label="Overall Points Earned" :sub-title="overallPtsSubTitle"
                              icon="fa fa-running" :icon-color="infoCards().iconColors[0]"
                              data-cy="overallPointsEarnedCard"/>
        </div>

        <div class="mb-2 px-1 col-md-6 col-lg">
          <progress-info-card :title="skill.todaysPoints" label="Points Achieved Today" :sub-title="todayPtsSubTitle"
                              icon="fa fa-clock"  :icon-color="infoCards().iconColors[1]"
                              data-cy="pointsAchievedTodayCard"/>
        </div>

        <div class="mb-2 px-1 col-md">
            <progress-info-card :title="skill.pointIncrement" label="Points per occurrence" :sub-title="occurrenceSubTitle"
                                icon="fas fa-flag-checkered" :animated="false"  :icon-color="infoCards().iconColors[2]"
                                data-cy="pointsPerOccurrenceCard"/>
        </div>

        <div v-if="!isTimeWindowDisabled" class="col-md mb-2 px-1">
            <progress-info-card :title="timeWindowTitle" :sub-title="timeWindowSubTitle" :label="timeWindowLabel"
                                :icon-color="infoCards().iconColors[3]"
                                icon="fas fa-hourglass-half" :animated="false"
                                data-cy="timeWindowPts"/>
        </div>

    </div>
</template>

<script>
  import ProgressInfoCard from '@/userSkills/skill/progress/ProgressInfoCard';
  import numberFormatter from '../../../common/filter/NumberFilter';
  import ThemePropsMixin from '../../../common/theme/ThemePropsMixin';

  export default {
    name: 'SkillSummaryCards',
    mixins: [ThemePropsMixin],
    components: { ProgressInfoCard },
    props: {
      skill: Object,
      shortSubTitles: {
        type: Boolean,
        default: false,
      },
    },
    computed: {
      overallPtsSubTitle() {
        return this.shortSubTitles ? 'Total' : 'Points Total';
      },
      todayPtsSubTitle() {
        return this.shortSubTitles ? 'Today' : 'Points Today';
      },
      occurrenceSubTitle() {
        return this.shortSubTitles ? 'Occurrence' : 'Points/Occurrence';
      },
      timeWindowSubTitle() {
        return this.shortSubTitles ? 'Window' : 'Time Window Pts.';
      },
      timeWindowTitle() {
        return this.skill.pointIncrement * this.skill.maxOccurrencesWithinIncrementInterval;
      },
      timeWindowLabel() {
        const hours = this.skill.pointIncrementInterval > 59 ? Math.floor(this.skill.pointIncrementInterval / 60) : 0;
        const minutes = this.skill.pointIncrementInterval > 60 ? this.skill.pointIncrementInterval % 60 : this.skill.pointIncrementInterval;
        const occur = this.skill.maxOccurrencesWithinIncrementInterval;
        const points = occur * this.skill.pointIncrement;
        let res = `Up-to ${numberFormatter(points, 1)} points within `;
        if (hours) {
          res = `${res} ${hours} hr${this.sOrNothing(hours)}`;
        }
        if (minutes) {
          if (hours) {
            res = ` ${res} and`;
          }
          res = `${res} ${minutes} min${this.sOrNothing(minutes)}`;
        }
        return res;
      },
      timeWindowPoints() {
        return this.skill.pointIncrement * this.skill.maxOccurrencesWithinIncrementInterval;
      },
      isTimeWindowDisabled() {
        return this.skill.pointIncrementInterval <= 0 || this.skill.pointIncrement === this.skill.totalPoints;
      },
    },
    methods: {
      sOrNothing(num) {
        return num > 1 ? 's' : '';
      },
    },
  };
</script>

<style scoped>

</style>
