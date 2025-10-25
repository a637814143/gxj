import { defineStore } from 'pinia'
import {
  fetchConsultations,
  createConsultation,
  fetchConsultationMessages,
  sendConsultationMessage,
  updateConsultation,
  markConsultationRead
} from '../services/consultation'

const toArray = value => {
  if (!value) {
    return []
  }
  if (Array.isArray(value)) {
    return value
  }
  return [value]
}

const normalizeConversation = item => ({
  id: item?.id ?? item?.consultationId ?? null,
  subject: item?.subject || item?.title || '未命名咨询',
  cropType: item?.cropType || item?.crop || '',
  status: item?.status || 'pending',
  priority: item?.priority || 'normal',
  createdAt: item?.createdAt || item?.created_at || null,
  updatedAt: item?.updatedAt || item?.updated_at || null,
  lastMessage: item?.lastMessage || item?.latestMessage || null,
  unreadCount: item?.unreadCount ?? item?.unread_count ?? 0,
  participants: toArray(item?.participants || item?.members)
})

const normalizeMessage = item => ({
  id: item?.id ?? item?.messageId ?? crypto.randomUUID?.() ?? Math.random().toString(36).slice(2),
  consultationId: item?.consultationId ?? item?.consultation_id ?? item?.conversationId ?? null,
  senderId: item?.senderId ?? item?.sender_id ?? null,
  senderName: item?.senderName || item?.sender_name || '未知用户',
  senderRole: item?.senderRole || item?.role || '',
  senderAvatar: item?.senderAvatar || item?.avatar || '',
  content: item?.content || item?.message || '',
  attachments: toArray(item?.attachments).map(attachment => ({
    id: attachment?.id ?? attachment?.fileId ?? crypto.randomUUID?.() ?? Math.random().toString(36).slice(2),
    name: attachment?.name || attachment?.fileName || '附件',
    url: attachment?.url || attachment?.fileUrl || attachment?.path || '',
    type: attachment?.type || attachment?.mimeType || ''
  })),
  createdAt: item?.createdAt || item?.created_at || item?.timestamp || null,
  metadata: item?.metadata || {}
})

export const useConsultationStore = defineStore('consultation', {
  state: () => ({
    conversations: [],
    activeConversationId: null,
    messages: {},
    pagination: {
      page: 1,
      pageSize: 20,
      total: 0
    },
    loadingConversations: false,
    loadingMessages: false,
    creating: false,
    sending: false,
    updatingStatus: false,
    poller: null
  }),
  getters: {
    activeConversation(state) {
      if (!state.activeConversationId) {
        return null
      }
      return state.conversations.find(item => item.id === state.activeConversationId) || null
    },
    activeMessages(state) {
      if (!state.activeConversationId) {
        return []
      }
      return state.messages[state.activeConversationId] || []
    }
  },
  actions: {
    setActiveConversation(id) {
      this.activeConversationId = id
    },
    updateConversationMeta(id, payload) {
      if (!id) {
        return
      }
      const index = this.conversations.findIndex(item => item.id === id)
      if (index === -1) {
        return
      }
      const current = this.conversations[index]
      const merged = {
        ...current,
        ...payload
      }
      this.conversations.splice(index, 1, merged)
    },
    upsertConversation(record) {
      const normalized = normalizeConversation(record)
      if (!normalized.id) {
        return
      }
      const index = this.conversations.findIndex(item => item.id === normalized.id)
      if (index === -1) {
        this.conversations.unshift(normalized)
      } else {
        this.conversations.splice(index, 1, {
          ...this.conversations[index],
          ...normalized
        })
      }
    },
    async loadConversations({ page, pageSize, silent = false } = {}) {
      if (!silent) {
        this.loadingConversations = true
      }
      try {
        const params = {
          page: page ?? this.pagination.page,
          pageSize: pageSize ?? this.pagination.pageSize
        }
        const { data } = await fetchConsultations(params)
        const payload = data?.data || data || {}
        const items = payload.items || payload.list || payload.records || []
        this.conversations = items.map(normalizeConversation)
        this.pagination = {
          page: payload.page ?? params.page ?? 1,
          pageSize: payload.pageSize ?? params.pageSize ?? 20,
          total: payload.total ?? payload.totalCount ?? items.length
        }
        if (!this.activeConversationId && this.conversations.length) {
          this.activeConversationId = this.conversations[0].id
        } else if (
          this.activeConversationId &&
          !this.conversations.some(item => item.id === this.activeConversationId)
        ) {
          this.activeConversationId = this.conversations[0]?.id ?? null
        }
      } catch (error) {
        console.error('Failed to load consultations', error)
        throw error
      } finally {
        if (!silent) {
          this.loadingConversations = false
        }
      }
    },
    async loadMessages(consultationId = this.activeConversationId, { silent = false } = {}) {
      if (!consultationId) {
        return
      }
      if (!silent) {
        this.loadingMessages = true
      }
      try {
        const { data } = await fetchConsultationMessages(consultationId, {
          pageSize: 100
        })
        const payload = data?.data || data || {}
        const items = payload.items || payload.list || payload.records || []
        this.messages[consultationId] = items.map(normalizeMessage)
        if (payload.conversation) {
          this.upsertConversation(payload.conversation)
        }
      } catch (error) {
        console.error('Failed to load consultation messages', error)
        throw error
      } finally {
        if (!silent) {
          this.loadingMessages = false
        }
      }
    },
    async ensureMessages(consultationId) {
      if (!consultationId) {
        return
      }
      if (!this.messages[consultationId] || !this.messages[consultationId].length) {
        await this.loadMessages(consultationId)
      }
    },
    async createConsultation(payload) {
      this.creating = true
      try {
        const { data } = await createConsultation(payload)
        const record = data?.data || data
        if (record) {
          const normalized = normalizeConversation(record)
          this.upsertConversation(normalized)
          this.activeConversationId = normalized.id
          await this.loadMessages(normalized.id)
        }
        return record
      } catch (error) {
        console.error('Failed to create consultation', error)
        throw error
      } finally {
        this.creating = false
      }
    },
    async sendReply({ consultationId = this.activeConversationId, content, attachments = [], metadata = {} }) {
      if (!consultationId) {
        throw new Error('未选择会话')
      }
      if (!content && (!attachments || !attachments.length)) {
        throw new Error('请输入消息内容或上传附件')
      }
      this.sending = true
      try {
        let payload
        if (attachments && attachments.length) {
          payload = new FormData()
          if (content) {
            payload.append('content', content)
          }
          Object.entries(metadata || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
              payload.append(key, value)
            }
          })
          attachments.forEach(file => {
            const actual = file?.raw || file
            payload.append('attachments', actual)
          })
        } else {
          payload = {
            content,
            ...metadata
          }
        }
        const { data } = await sendConsultationMessage(consultationId, payload)
        const record = data?.data || data
        if (record) {
          const normalized = normalizeMessage(record)
          const existing = this.messages[consultationId] ? [...this.messages[consultationId]] : []
          existing.push(normalized)
          this.messages[consultationId] = existing
          this.updateConversationMeta(consultationId, {
            lastMessage: normalized,
            updatedAt: normalized.createdAt,
            unreadCount: 0
          })
        }
        return record
      } catch (error) {
        console.error('Failed to send consultation message', error)
        throw error
      } finally {
        this.sending = false
      }
    },
    async updateStatus(consultationId, statusPayload) {
      if (!consultationId) {
        return
      }
      this.updatingStatus = true
      try {
        const { data } = await updateConsultation(consultationId, statusPayload)
        const record = data?.data || data
        if (record) {
          this.upsertConversation(record)
        } else {
          this.updateConversationMeta(consultationId, statusPayload)
        }
      } catch (error) {
        console.error('Failed to update consultation status', error)
        throw error
      } finally {
        this.updatingStatus = false
      }
    },
    async markAsRead(consultationId = this.activeConversationId) {
      if (!consultationId) {
        return
      }
      try {
        await markConsultationRead(consultationId)
      } catch (error) {
        console.warn('Failed to mark consultation as read', error)
      } finally {
        this.updateConversationMeta(consultationId, { unreadCount: 0 })
      }
    },
    startPolling(interval = 15000) {
      if (typeof window === 'undefined') {
        return
      }
      this.stopPolling()
      this.poller = window.setInterval(async () => {
        try {
          await this.loadConversations({ silent: true })
          if (this.activeConversationId) {
            await this.loadMessages(this.activeConversationId, { silent: true })
          }
        } catch (error) {
          console.warn('Consultation polling failed', error)
        }
      }, interval)
    },
    stopPolling() {
      if (typeof window === 'undefined') {
        return
      }
      if (this.poller) {
        window.clearInterval(this.poller)
        this.poller = null
      }
    },
    reset() {
      this.stopPolling()
      this.$reset()
    }
  }
})
