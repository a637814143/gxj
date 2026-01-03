<template>
  <el-select v-model="locale" size="small" class="lang-switch" @change="changeLocale">
    <el-option label="中文" value="zh" />
    <el-option label="English" value="en" />
  </el-select>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed } from 'vue'

const { locale: i18nLocale } = useI18n()

const locale = computed({
  get: () => i18nLocale.value,
  set: (val) => {
    i18nLocale.value = val
    localStorage.setItem('app-locale', val)
  }
})

const changeLocale = (val) => {
  locale.value = val
}
</script>

<style scoped>
.lang-switch {
  width: 120px;
}
</style>
