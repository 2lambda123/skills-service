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
<script>
  import SelfReportService from '@/components/skills/selfReport/SelfReportService';

  export default {
    name: 'SelfReportApprovalConfMixin',
    methods: {
      removeTagConf(removedIem) {
        this.table.items = this.table.items.map((i) => ({ ...i, deleteInProgress: i.id === removedIem.id }));
        return SelfReportService.removeApproverConfig(this.projectId, removedIem.id)
          .then(() => {
            this.table.items = this.table.items.filter((i) => i.id !== removedIem.id);
            this.$emit('conf-removed', removedIem);
            this.$nextTick(() => this.$announcer.polite('Removed workload configuration successfully.'));
          });
      },
    },
  };
</script>

<style scoped>

</style>
