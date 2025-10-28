import { defineStore } from 'pinia'
import {
  fetchConsultations,
  createConsultation,
  fetchConsultationMessages,
  sendConsultationMessage,
  updateConsultation,
  markConsultationRead,
  closeConsultation,
  fetchConsultationDepartments
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

const normalizeParticipant = participant => ({
  userId: participant?.userId ?? participant?.id ?? participant?.user_id ?? null,
  name: participant?.name || participant?.fullName || participant?.username || '',
  role: participant?.role || participant?.roleCode || participant?.role_code || '',
  owner: Boolean(participant?.owner),
  self: Boolean(participant?.self)
})

const buildUserRef = (id, name, fallback) => {
  if (fallback && (fallback.userId || fallback.name)) {
    return {
      id: fallback.userId ?? null,
      name: fallback.name || '',
      role: fallback.role || ''
    }
  }
  if (!id && !name) {
    return null
  }
  return {
    id: id ?? null,
    name: name || '',
    role: fallback?.role || ''
  }
}

const normalizeConversation = item => {
  const participantsRaw = toArray(item?.participants || item?.members)
  const participants = participantsRaw.map(normalizeParticipant)
  const ownerParticipant = participants.find(participant => participant.owner)
  const ownerId = item?.ownerId ?? item?.owner_id
  const ownerName = item?.ownerName || item?.owner_name
  const assignedId = item?.assignedToId ?? item?.assigned_to_id
  const assignedName = item?.assignedToName || item?.assigned_to_name
  const messageCountValue =
    typeof item?.messageCount === 'number'
      ? item.messageCount
      : Number(item?.messageCount ?? item?.message_count ?? 0)

  return {
    id: item?.id ?? item?.consultationId ?? null,
    subject: item?.subject || item?.title || '未命名咨询',
    cropType: item?.cropType || item?.crop || '',
    status: item?.status || 'pending',
    priority: item?.priority || 'normal',
    createdAt: item?.createdAt || item?.created_at || null,
    updatedAt: item?.updatedAt || item?.updated_at || null,
    closedAt: item?.closedAt || item?.closed_at || null,
    lastMessage: item?.lastMessage || item?.latestMessage || null,
    unreadCount: item?.unreadCount ?? item?.unread_count ?? 0,
    participants,
    description: item?.description || '',
    owner: buildUserRef(ownerId, ownerName, ownerParticipant),
    assignedTo: buildUserRef(assignedId, assignedName),
    messageCount: Number.isFinite(messageCountValue) ? messageCountValue : 0
  }
}

const normalizeMessage = item => ({
  id: item?.id ?? item?.messageId ?? crypto.randomUUID?.() ?? Math.random().toString(36).slice(2),
  consultationId: item?.consultationId ?? item?.consultation_id ?? item?.conversationId ?? null,
  senderId: item?.senderId ?? item?.sender_id ?? null,
  senderName: item?.senderName || item?.sender_name || '未知用户',
  senderRole: item?.senderRole || item?.role || '',
  senderAvatar: item?.senderAvatar || item?.avatar || '',
  content: item?.content || item?.message || '',
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
    loadingDepartments: false,
    departments: [],
    creating: false,
    sending: false,
    updatingStatus: false,
    closing: false,
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
    async loadDepartments({ force = false } = {}) {
      if (!force && Array.isArray(this.departments) && this.departments.length) {
        return this.departments
      }
      this.loadingDepartments = true
      try {
        const { data } = await fetchConsultationDepartments()
        const payload = data?.data || data || []
        this.departments = Array.isArray(payload)
          ? payload.map(item => ({
              code: item?.code || item?.departmentCode || '',
              name: item?.name || item?.departmentName || '',
              description: item?.description || '',
              contactName: item?.contactName || item?.contact_name || '',
              contactUsername: item?.contactUsername || item?.contact_username || '',
              contactUserId: item?.contactUserId ?? item?.contact_user_id ?? null
            }))
          : []
        return this.departments
      } catch (error) {
        console.error('Failed to load consultation departments', error)
        throw error
      } finally {
        this.loadingDepartments = false
      }
    },
    async loadConversations({ page, pageSize, silent = false, ...rest } = {}) {
      if (!silent) {
        this.loadingConversations = true
      }
      try {
        const filters = { ...rest }
        Object.keys(filters).forEach(key => {
          const value = filters[key]
          if (
            value === undefined ||
            value === null ||
            (typeof value === 'string' && (!value.trim() || value.trim().toLowerCase() === 'all'))
          ) {
            delete filters[key]
          }
        })
        const params = {
          page: page ?? this.pagination.page,
          pageSize: pageSize ?? this.pagination.pageSize,
          ...filters
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
    async sendReply({ consultationId = this.activeConversationId, content }) {
      if (!consultationId) {
        throw new Error('未选择会话')
      }
      const target = this.conversations.find(item => item.id === consultationId)
      if (target && (target.status || '').toLowerCase() === 'closed') {
        throw new Error('该对话已结束，无法发送新消息')
      }
      if (!content || !content.trim()) {
        throw new Error('请输入消息内容')
      }
      this.sending = true
      try {
        const payload = {
          content: content.trim()
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
    async closeConversation(consultationId = this.activeConversationId) {
      if (!consultationId) {
        return
      }
      this.closing = true
      try {
        const { data } = await closeConsultation(consultationId)
        const record = data?.data || data
        if (record) {
          this.upsertConversation(record)
        } else {
          this.updateConversationMeta(consultationId, { status: 'closed', closedAt: new Date().toISOString() })
        }
      } catch (error) {
        console.error('Failed to close consultation', error)
        throw error
      } finally {
        this.closing = false
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
