import { defineStore } from 'pinia'
import apiClient from '../services/http'

const STATUS_LABELS = {
  ACTIVE: '生效中',
  INACTIVE: '未启用',
  SCHEDULED: '计划发布'
}

export const useAnnouncementStore = defineStore('announcement', {
  state: () => ({
    loading: false,
    error: null,
    announcement: null,
    lastLoaded: 0
  }),
  getters: {
    isActive: state => (state.announcement?.status || '').toUpperCase() === 'ACTIVE',
    statusLabel: state => STATUS_LABELS[(state.announcement?.status || '').toUpperCase()] || '未启用'
  },
  actions: {
    async fetch(force = false) {
      if (this.loading) return
      const now = Date.now()
      if (!force && this.announcement && now - this.lastLoaded < 5 * 60 * 1000) {
        return
      }

      this.loading = true
      this.error = null
      try {
        const { data } = await apiClient.get('/api/system/settings')
        const payload = data?.data ?? data ?? {}
        this.announcement = {
          title: payload.announcementTitle || '平台通知',
          message: payload.announcementMessage || '暂无通知内容',
          status: (payload.announcementStatus || 'INACTIVE').toUpperCase(),
          updatedAt: payload.updatedAt ?? payload.updated_at ?? null
        }
        this.lastLoaded = now
      } catch (error) {
        this.error = error?.response?.data?.message || '通知获取失败'
      } finally {
        this.loading = false
      }
    }
  }
})
