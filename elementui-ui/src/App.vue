<template>
  <div id="app">
    <file-manage></file-manage>
  </div>
</template>

<script>

import FileManage from "@/components/fileManage/index.vue";

export default {
  name: 'App',
  components: {
    FileManage
  }
}
</script>

<style>
body {
  margin: 0;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
  font-variant: tabular-nums;
  line-height: 1.5;
  background-color: #fff;
  font-feature-settings: 'tnum';
}

</style>
