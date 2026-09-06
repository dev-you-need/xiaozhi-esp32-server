<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationBarTitleText": "Агент",
    "navigationStyle": "custom"
  }
}
</route>

<script lang="ts" setup>
import { onLoad } from '@dcloudio/uni-app'
import { onMounted, ref } from 'vue'
import CustomTabs from '@/components/custom-tabs/index.vue'
import { t } from '@/i18n'
import ChatHistory from '@/pages/chat-history/index.vue'
import DeviceManagement from '@/pages/device/index.vue'
import VoiceprintManagement from '@/pages/voiceprint/index.vue'
import AgentEdit from './edit.vue'

defineOptions({
  name: 'AgentIndex',
})

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

// ID агента
const currentAgentId = ref('default')

// Текущая вкладка
const currentTab = ref('agent-config')

// Состояние обновления и загрузки
const refreshing = ref(false)
const refresherEnabled = ref(false)

// Ссылка на дочерний компонент
const deviceRef = ref()
const chatRef = ref()
const voiceprintRef = ref()

// Обновление состояния обновления
function updateRefresherEnabled(value: boolean) {
  refresherEnabled.value = value
}

// Конфигурация вкладок
const tabList = [
  {
    label: t('agent.roleConfig'),
    value: 'agent-config',
    icon: '/static/tabbar/robot.png',
    activeIcon: '/static/tabbar/robot_activate.png',
  },
  {
    label: t('agent.deviceManagement'),
    value: 'device-management',
    icon: '/static/tabbar/device.png',
    activeIcon: '/static/tabbar/device_activate.png',
  },
  {
    label: t('agent.chatHistory'),
    value: 'chat-history',
    icon: '/static/tabbar/chat.png',
    activeIcon: '/static/tabbar/chat_activate.png',
  },
  {
    label: t('agent.voiceprintManagement'),
    value: 'voiceprint-management',
    icon: '/static/tabbar/microphone.png',
    activeIcon: '/static/tabbar/microphone_activate.png',
  },
]

// Вернуться на предыдущую страницу
function goBack() {
  uni.navigateBack()
}

// Обработка переключения вкладок
function handleTabChange(item: any) {
  console.log('Tab changed:', item)
}

// Обновление по свайпу вниз
async function onRefresh() {
  // Страница редактирования роли не требует обновления
  if (currentTab.value === 'agent-config') {
    return
  }

  refreshing.value = true

  try {
    switch (currentTab.value) {
      case 'device-management':
        if (deviceRef.value?.refresh) {
          await deviceRef.value.refresh()
        }
        break
      case 'chat-history':
        if (chatRef.value?.refresh) {
          await chatRef.value.refresh()
        }
        break
      case 'voiceprint-management':
        if (voiceprintRef.value?.refresh) {
          await voiceprintRef.value.refresh()
        }
        break
    }
  }
  catch (error) {
    console.error('刷新失败:', error)
  }
  finally {
    refreshing.value = false
  }
}

// Загрузка дополнительных при достижении дна
async function onLoadMore() {
  // Загружать дополнительно нужно только для истории чата
  if (currentTab.value === 'chat-history' && chatRef.value?.loadMore) {
    await chatRef.value.loadMore()
  }
}

watch(() => currentTab.value, (newTab) => {
  updateRefresherEnabled(newTab !== 'agent-config')
})

// Принять параметры страницы
onLoad((options) => {
  if (options?.agentId) {
    currentAgentId.value = options.agentId
    console.log('接收到智能体ID:', options.agentId)
  }
})

onMounted(async () => {
  // Инициализация страницы
})
</script>

<template>
  <view class="h-screen flex flex-col bg-[#f5f7fb]">
    <!-- Панель навигации -->
    <wd-navbar :title="t('agent.pageTitle')" safe-area-inset-top>
      <template #left>
        <wd-icon name="arrow-left" size="18" @click="goBack" />
      </template>
    </wd-navbar>

    <!-- Пользовательские вкладки -->
    <CustomTabs
      v-model="currentTab"
      :tab-list="tabList"
      @change="handleTabChange"
    />

    <!-- Основная прокручиваемая область контента -->
    <scroll-view
      scroll-y
      :style="{ height: `calc(100vh - ${safeAreaInsets?.top || 0}px - 180rpx)` }"
      class="box-border flex-1 bg-[#f5f7fb]"
      enable-back-to-top
      :refresher-enabled="refresherEnabled"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <!-- Содержимое вкладки -->
      <view class="flex-1">
        <AgentEdit
          v-if="currentTab === 'agent-config'"
          :agent-id="currentAgentId"
        />
        <DeviceManagement
          v-else-if="currentTab === 'device-management'"
          ref="deviceRef"
          :agent-id="currentAgentId"
        />
        <ChatHistory
          v-else-if="currentTab === 'chat-history'"
          ref="chatRef"
          :agent-id="currentAgentId"
        />
        <VoiceprintManagement
          v-else-if="currentTab === 'voiceprint-management'"
          ref="voiceprintRef"
          :agent-id="currentAgentId"
          @update-refresher-enabled="updateRefresherEnabled"
        />
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
</style>
