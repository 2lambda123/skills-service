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
  <div class="mt-5 container-fluid" data-cy="upgradeInProgressError">
    <div class="text-center text-secondary" aria-hidden="true">
      <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-hammer fa-stack-1x fa-inverse"></i>
                    </span>
    </div>
    <div class="text-center text-secondary">
      <div class="h4">A database upgrade is in progress!</div>
    </div>

    <div class="row justify-content-center text-info mt-3">
      <div class="col col-sm-8 col-md-6 col-lg-4 text-center">
        <p>
          A database upgrade is currently in progress, no modifications or changes to Projects/Subjects/Skills/Badges are allowed until the upgrade has completed. Please try again later.
        </p>
      </div>
    </div>

    <div class="text-center">
      <b-button href="/" variant="outline-primary" class="p-2" aria-label="navigate to home page"><i class="fas fa-home mr-1"/>Take Me Home</b-button>
    </div>
  </div>
</template>

<script>

  export default {
    name: 'ErrorPage',
    computed: {
      errorMessage() {
        return this.$route.query.errorMessage;
      },
      previousUrl() {
        return this.$store.state.previousUrl;
      },
    },
    mounted() {
      if (this.previousUrl) {
        // eslint-disable-next-line
        history.replaceState(history.state, '', this.previousUrl);
      }
    },
  };
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  @import "../../styles/palette";

  .error-header {
    background-color: $red-palette-color3;
    border-top-left-radius: 7px;
    border-top-right-radius: 7px;
    padding: 1rem;
  }

  .error-title {
    color: whitesmoke;
    font-size: 1.5rem;
  }

  .error-body {
    border: 1px solid #ddd;
    border-bottom-left-radius: 7px;
    border-bottom-right-radius: 7px;
    padding: 1rem;
  }

</style>
