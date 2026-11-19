<template>
  <el-tag :type="type" size="mini" effect="light">{{ text }}</el-tag>
</template>

<script>
import { ACTIVITY_STATUS, REG_STATUS } from '@/utils/format'

export default {
  name: 'StatusTag',
  props: {
    status: { type: [Number, String], default: null },
    kind: { type: String, default: 'activity' } // activity | registration
  },
  computed: {
    map () {
      return this.kind === 'registration' ? REG_STATUS : ACTIVITY_STATUS
    },
    info () {
      return this.map[this.status] || { text: '未知', type: 'info' }
    },
    text () {
      return this.info.text
    },
    type () {
      return this.info.type || undefined
    }
  }
}
</script>
