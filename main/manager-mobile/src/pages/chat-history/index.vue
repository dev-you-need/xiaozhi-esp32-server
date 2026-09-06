<script lang="ts" setup>
import type { ChatSession } from '@/api/chat-history/types'
import { computed, onMounted, ref } from 'vue'
import { getChatSessions } from '@/api/chat-history/chat-history'
import { t } from '@/i18n'
import { deepClone } from '@/utils'

defineOptions({
  name: 'ChatHistory',
})

const props = withDefaults(defineProps<Props>(), {
  agentId: 'default',
})

// перениматьprops
interface Props {
  agentId?: string
}

// Получение расстояния от края экрана до безопасной области
let safeAreaInsets: any
let systemInfo: any

// #ifdef MP-WEIXIN
systemInfo = uni.getWindowInfo()
safeAreaInsets = systemInfo.safeArea
  ? {
      top: systemInfo.safeArea.top,
      right: systemInfo.windowWidth - systemInfo.safeArea.right,
      bottom: systemInfo.windowHeight - systemInfo.safeArea.bottom,
      left: systemInfo.safeArea.left,
    }
  : null
// #endif

// #ifndef MP-WEIXIN
systemInfo = uni.getSystemInfoSync()
safeAreaInsets = systemInfo.safeAreaInsets
// #endif

// Данные сеанса чата
const sessionList = ref<ChatSession[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(0)
const pageSize = 10

// Использование переданного агентаID
const currentAgentId = computed(() => {
  return props.agentId
})

// Загрузка списка сессий чата
async function loadChatSessions(page = 1, isUpdate = false) {
  try {
    // Проверка наличия текущего выбранного агента
    if (!currentAgentId.value) {
      console.warn(t('chatHistory.noSelectedAgent'))
      sessionList.value = []
      return
    }

    if (page === 1) {
      loading.value = true
    }
    else {
      loadingMore.value = true
    }

    const response = await getChatSessions(currentAgentId.value, {
      page,
      limit: pageSize,
    })

    if (page === 1) {
      const oldSessionList = deepClone(sessionList.value)
      oldSessionList.splice(0, 10)
      oldSessionList.unshift(...(response.list || []))
      sessionList.value = isUpdate ? oldSessionList : response.list || []
    }
    else {
      sessionList.value.push(...(response.list || []))
    }

    // Обновление информации о постраничном разбиении
    hasMore.value = (response.list?.length || 0) === pageSize
    currentPage.value = page
  }
  catch (error) {
    console.error(t('chatHistory.getChatSessionsFailed'), error)
    if (page === 1) {
      sessionList.value = []
    }
  }
  finally {
    loading.value = false
    loadingMore.value = false
  }
}

// Метод обновления, доступный родительскому компоненту
async function refresh() {
  currentPage.value = 1
  hasMore.value = true
  await loadChatSessions(1, true)
}

// Загрузка дополнительных методов, доступных родительскому компоненту
async function loadMore() {
  if (!hasMore.value || loadingMore.value) {
    return
  }
  await loadChatSessions(currentPage.value + 1)
}

// Формат времени
function formatTime(timeStr: string) {
  if (!timeStr)
    return t('chatHistory.unknownTime')

  // Обработка строки времени，Обеспечение правильного формата
  const date = new Date(timeStr.replace(' ', 'T')) // Преобразовать вISOФормат
  const now = new Date()

  // Проверка действительности даты
  if (Number.isNaN(date.getTime())) {
    return timeStr // Если разбор не удался，Вернуть исходную строку напрямую
  }

  const diff = now.getTime() - date.getTime()

  // меньше, чем1минута
  if (diff < 60000)
    return t('chatHistory.justNow')

  // меньше, чем1Час
  if (diff < 3600000)
    return t('chatHistory.minutesAgo', { minutes: Math.floor(diff / 60000) })

  // меньше, чем1небо（24Час）
  if (diff < 86400000)
    return t('chatHistory.hoursAgo', { hours: Math.floor(diff / 3600000) })

  // меньше, чем7небо
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000)
    return t('chatHistory.daysAgo', { days })
  }

  // Превосходить7небо，Показать конкретную дату
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const currentYear = now.getFullYear()

  // Если текущий год，Не показывать год
  if (year === currentYear) {
    return `${month}-${day}`
  }

  return `${year}-${month}-${day}`
}

// Введите данные чата
function goToChatDetail(session: ChatSession) {
  uni.navigateTo({
    url: `/pages/chat-history/detail?sessionId=${session.sessionId}&agentId=${currentAgentId.value}`,
  })
}

onMounted(async () => {
  // Агент был упрощен до значения по умолчанию.
  loadChatSessions(1)
})

onShow(() => {
  if (currentPage.value !== 0) {
    loadChatSessions(1, true)
  }
})

// Предоставлять методы родительскому компоненту
defineExpose({
  refresh,
  loadMore,
})
</script>

<template>
  <view class="chat-history-container" style="background: #f5f7fb; min-height: 100%;">
    <!-- Статус загрузки -->
    <view v-if="loading && sessionList.length === 0" class="loading-container">
      <wd-loading color="#336cff" />
      <text class="loading-text">
        {{ t('chatHistory.loading') }}
      </text>
    </view>

    <!-- Список разговоров -->
    <view v-else-if="sessionList.length > 0" class="session-container">
      <!-- Список сеансов чата -->
      <view class="session-list">
        <view
          v-for="session in sessionList"
          :key="session.sessionId"
          class="session-item"
          @click="goToChatDetail(session)"
        >
          <view class="session-card">
            <view class="session-info">
              <view class="session-header">
                <text class="session-title">
                  {{ session.title || `${t('chatHistory.conversationRecord')} ${session.sessionId.substring(0, 8)}...` }}
                </text>
                <text class="session-time">
                  {{ formatTime(session.createdAt) }}
                </text>
              </view>
              <view class="session-meta">
                <text class="chat-count">
                  {{ t('chatHistory.totalChats', { count: session.chatCount }) }}
                </text>
              </view>
            </view>
            <wd-icon name="arrow-right" custom-class="arrow-icon" />
          </view>
        </view>
      </view>

      <!-- Состояние загрузки дополнительных -->
      <view v-if="loadingMore" class="loading-more">
        <wd-loading color="#336cff" size="24" />
        <text class="loading-more-text">
          {{ t('chatHistory.loading') }}
        </text>
      </view>

      <!-- больше нет данных -->
      <view v-else-if="!hasMore && sessionList.length > 0" class="no-more">
        <text class="no-more-text">
          {{ t('chatHistory.noMoreData') }}
        </text>
      </view>
    </view>

    <!-- Пустое состояние -->
    <view v-else-if="!loading" class="empty-state">
      <wd-icon name="chat" custom-class="empty-icon" />
      <text class="empty-text">
        {{ t('chatHistory.noChatRecords') }}
      </text>
      <text class="empty-desc">
        {{ t('chatHistory.chatRecordsDescription') }}
      </text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.chat-history-container {
  position: relative;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
}

.loading-text {
  margin-top: 20rpx;
  font-size: 28rpx;
  color: #666666;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  gap: 16rpx;

  .loading-more-text {
    font-size: 26rpx;
    color: #666666;
  }
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;

  .no-more-text {
    font-size: 26rpx;
    color: #999999;
  }
}

.navbar-section {
  background: #ffffff;
}

.status-bar {
  background: #ffffff;
  width: 100%;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 20rpx;
  box-sizing: border-box;
}

.session-item {
  background: #fbfbfb;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  border: 1rpx solid #eeeeee;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;

  &:active {
    background: #f8f9fa;
  }
}

.session-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;

  .session-info {
    width: 94%;

    .session-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12rpx;

      .session-title {
        font-size: 32rpx;
        font-weight: 600;
        color: #232338;
        width: 70%;
        word-break: break-all;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .session-time {
        font-size: 24rpx;
        color: #9d9ea3;
      }
    }

    .session-meta {
      .chat-count {
        font-size: 28rpx;
        color: #65686f;
      }
    }
  }

  :deep(.arrow-icon) {
    font-size: 24rpx;
    color: #c7c7cc;
    margin-left: 16rpx;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
  text-align: center;

  :deep(.empty-icon) {
    font-size: 120rpx;
    color: #d9d9d9;
    margin-bottom: 32rpx;
  }

  .empty-text {
    font-size: 32rpx;
    color: #666666;
    margin-bottom: 16rpx;
    font-weight: 500;
  }

  .empty-desc {
    font-size: 26rpx;
    color: #999999;
    line-height: 1.5;
  }
}
</style>
