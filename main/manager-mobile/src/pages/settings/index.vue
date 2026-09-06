<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationBarTitleText": "настраивать",
    "navigationStyle": "custom"
  }
}
</route>

<script lang="ts" setup>
import type { Language } from '@/store/lang'
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from 'wot-design-uni/components/wd-toast'
import { changeLanguage, getCurrentLanguage, getSupportedLanguages, t } from '@/i18n'
import { useConfigStore } from '@/store'
import { clearServerBaseUrlOverride, getEnvBaseUrl, getServerBaseUrlOverride, setServerBaseUrlOverride } from '@/utils'
import { isMp } from '@/utils/platform'

defineOptions({
  name: 'SettingsPage',
})

const toast = useToast()

// информация кэша
const cacheInfo = reactive({
  storageSize: '0MB',
  imageCache: '0MB',
  dataCache: '0MB',
})

const configStore = useConfigStore()

// Настройки адреса сервера
const baseUrlInput = ref('')
const urlError = ref('')

// Информация о системе（бронировать）
const systemInfo = computed(() => {
  const info = uni.getSystemInfoSync()
  return `${info.platform} ${info.system}`
})

// Чтение локального перекрывающего адреса
function loadServerBaseUrl() {
  const override = getServerBaseUrlOverride()
  baseUrlInput.value = override || getEnvBaseUrl()
}

// Получить информацию о кэше
function getCacheInfo() {
  try {
    const info = uni.getStorageInfoSync()
    const totalSize = (info.currentSize || 0) / 1024 // KB to MB
    cacheInfo.storageSize = `${totalSize.toFixed(2)}MB`
  }
  catch (error) {
    console.error('Ошибка получения информации о кэше:', error)
  }
}

// проверятьURLФормат
function validateUrl() {
  urlError.value = ''

  if (!baseUrlInput.value) {
    return
  }

  if (!/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    urlError.value = t('settings.validServerUrl')
  }
}

// Адрес тестового сервера
async function testServerBaseUrl() {
  // Сначала удалите сообщение об ошибке
  urlError.value = ''

  if (!baseUrlInput.value || !/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    return false
  }

  try {
    const response = await uni.request({
      url: `${baseUrlInput.value}/api/ping`,
      method: 'GET',
      timeout: 3000,
    })

    if (response.statusCode === 200) {
      return true
    }
    else {
      toast.error({
        msg: t('message.invalidAddress'),
        duration: 3000,
      })
      return false
    }
  }
  catch (error) {
    console.error('Адрес тестового сервера не удался:', error)
    toast.error({
      msg: t('message.invalidAddress'),
      duration: 3000,
    })
    return false
  }
}

// Сохранить адрес сервера
async function saveServerBaseUrl() {
  if (!baseUrlInput.value || !/^https?:\/\/.+\/xiaozhi$/.test(baseUrlInput.value)) {
    toast.warning(t('settings.validServerUrl'))
    return
  }

  // Проверка достоверности адреса
  const isServerValid = await testServerBaseUrl()
  if (!isServerValid) {
    return
  }
  setServerBaseUrlOverride(baseUrlInput.value)
  // иметь дело сconfigКэш не может быть обновлен
  uni.request({
    url: `${getEnvBaseUrl()}/user/pub-config`,
    method: 'GET',
    success: (res: any) => {
      if (res.statusCode === 200) {
        configStore.setConfig(res.data.data)
        uni.setStorageSync('config', res.data.data)
      }
    },
    fail: (err) => {
      console.error('ПолучатьSM2Открытый ключ не удался:', err)
    },
  })

  // Очистка всего кэша после смены адреса запроса
  clearAllCacheAfterUrlChange()

  uni.showModal({
    title: t('settings.restartApp'),
    content: t('settings.serverUrlSavedAndCacheCleared'),
    confirmText: t('settings.restartNow'),
    cancelText: t('settings.restartLater'),
    success: (res) => {
      if (res.confirm) {
        restartApp()
      }
      else {
        toast.success(t('settings.restartSuccess'))
      }
    },
  })
}

// переключатель языка
const supportedLanguages = getSupportedLanguages()
const currentLanguage = ref<Language>(getCurrentLanguage())
const showLanguageSheet = ref(false)

function handleLanguageChange(lang: Language) {
  changeLanguage(lang)
  showLanguageSheet.value = false
  currentLanguage.value = lang
  toast.success(t('settings.languageChanged'))
}

// сбросить до env по умолчанию
function resetServerBaseUrl() {
  clearServerBaseUrlOverride()
  baseUrlInput.value = getEnvBaseUrl()

  // Очистка всего кэша после смены адреса запроса
  clearAllCacheAfterUrlChange()

  uni.showModal({
    title: t('settings.restartApp'),
    content: t('settings.resetToDefaultAndCacheCleared'),
    confirmText: t('settings.restartNow'),
    cancelText: t('settings.restartLater'),
    success: (res) => {
      if (res.confirm) {
        restartApp()
      }
      else {
        toast.success(t('settings.resetSuccess'))
      }
    },
  })
}

// Перезапустить приложение（App собственный перезапуск；Вернуться на домашнюю страницу на других терминалах）
function restartApp() {
  // #ifdef APP-PLUS
  plus.runtime.restart()
  // #endif
  // #ifndef APP-PLUS
  uni.reLaunch({ url: '/pages/index/index' })
  // #endif
}

// Автоматическая очистка всего кэша после смены адреса
function clearAllCacheAfterUrlChange() {
  try {
    // Резервное копирование перекрывающего адреса времени выполнения，Обеспечение восстановления после очистки
    const preservedOverride = getServerBaseUrlOverride()

    // Полная очистка всего кэша，включатьtoken
    uni.clearStorageSync()

    // ПрозрачныйlocalStorage（H5среда）
    // #ifdef H5
    if (typeof localStorage !== 'undefined') {
      localStorage.clear()
    }
    // #endif

    // Восстановление перекрывающего адреса времени выполнения（Если есть），Необходимо написать после завершения уборки
    if (preservedOverride) {
      setServerBaseUrlOverride(preservedOverride)
    }

    // Повторное получение информации о кэше
    getCacheInfo()
  }
  catch (error) {
    console.error('Ошибка очистки кэша:', error)
  }
}

// Очистить кэш
async function clearCache() {
  try {
    uni.showModal({
      title: t('settings.confirmClear'),
      content: t('settings.confirmClearMessage'),
      confirmText: t('common.confirm'),
      cancelText: t('common.cancel'),
      success: (res) => {
        if (res.confirm) {
          clearAllCacheAfterUrlChange()
          toast.success(t('settings.cacheCleared'))

          // Задержка перехода на страницу входа
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/index' })
          }, 1500)
        }
      },
    })
  }
  catch (error) {
    console.error('Ошибка очистки кэша:', error)
    toast.error(t('settings.clearCacheFailed'))
  }
}

// о нас
function showAbout() {
  uni.showModal({
    title: t('settings.aboutApp', { appName: import.meta.env.VITE_APP_TITLE }),
    content: t('settings.aboutContent', {
      appName: import.meta.env.VITE_APP_TITLE,
      version: '0.9.6',
    }),
    showCancel: false,
    confirmText: t('common.confirm'),
  })
}

onMounted(async () => {
  // Загрузка настроек адреса сервера только в не-miniprogram окружении
  if (!isMp) {
    loadServerBaseUrl()
  }
  getCacheInfo()

  // Динамическая установка заголовка панели навигации на интернационализированный текст
  uni.setNavigationBarTitle({
    title: t('settings.title'),
  })
})
</script>

<template>
  <view class="min-h-screen bg-[#f5f7fb]">
    <wd-navbar :title="t('settings.title')" placeholder safe-area-inset-top fixed />

    <view class="p-[24rpx]">
      <!-- Настройки сети - Отображается только в средах, отличных от мини-программ. -->
      <view v-if="!isMp" class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.networkSettings') }}
          </text>
        </view>

        <view
          class="overflow-hidden border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view class="mb-[24rpx]">
            <text class="text-[28rpx] text-[#232338] font-semibold">
              {{ t('settings.serverApiUrl') }}
            </text>
            <text class="mt-[8rpx] block text-[24rpx] text-[#9d9ea3]">
              {{ t('settings.modifyWillClearCache') }}
            </text>
          </view>

          <view class="mb-[24rpx]">
            <view class="w-full overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
              <wd-input
                v-model="baseUrlInput" type="text" clearable :maxlength="200"
                :placeholder="t('settings.enterServerUrl')"
                custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
                input-class="text-[28rpx] text-[#232338]" @input="validateUrl" @blur="validateUrl"
              />
            </view>
            <text v-if="urlError" class="mt-[8rpx] block text-[24rpx] text-[#ff4d4f]">
              {{ urlError }}
            </text>
          </view>

          <view class="flex gap-[16rpx]">
            <wd-button
              type="primary"
              custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-[#336cff] border-none shadow-[0_4rpx_16rpx_rgba(51,108,255,0.3)] active:shadow-[0_2rpx_8rpx_rgba(51,108,255,0.4)] active:scale-98"
              @click="saveServerBaseUrl"
            >
              {{ t('settings.saveSettings') }}
            </wd-button>
            <wd-button
              type="default"
              custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#eeeeee] text-[#65686f] active:bg-[#f5f7fb]"
              @click="resetServerBaseUrl"
            >
              {{ t('settings.resetDefault') }}
            </wd-button>
          </view>
        </view>
      </view>

      <!-- Управление кэшем -->
      <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.cacheManagement') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view class="space-y-[16rpx]">
            <!-- Отображение информации о кэше，Справочный стиль плагина -->
            <view
              class="flex items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            >
              <view>
                <text class="text-[28rpx] text-[#232338] font-medium">
                  {{ t('settings.totalCacheSize') }}
                </text>
                <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                  {{ t('settings.appDataSize') }}
                </text>
              </view>
              <text class="text-[28rpx] text-[#65686f] font-semibold">
                {{ cacheInfo.storageSize }}
              </text>
            </view>

            <!-- кнопка очистки кэша，Стиль кнопки редактирования эталонного плагина -->
            <view
              class="flex items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx]"
            >
              <view>
                <text class="text-[28rpx] text-[#232338] font-medium">
                  {{ t('settings.cacheClear') }}
                </text>
                <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                  {{ t('settings.clearAllCache') }}
                </text>
              </view>
              <view
                class="cursor-pointer rounded-[24rpx] bg-[rgba(255,107,107,0.1)] px-[28rpx] py-[16rpx] text-[24rpx] text-[#ff6b6b] font-semibold transition-all duration-300 active:scale-95 active:bg-[#ff6b6b] active:text-white"
                @click="clearCache"
              >
                {{ t('settings.clearCache') }}
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Информация о приложении -->
      <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.appInfo') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view
            class="flex cursor-pointer items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            @click="showAbout"
          >
            <view>
              <text class="text-[28rpx] text-[#232338] font-medium">
                {{ t('settings.aboutUs') }}
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                {{ t('settings.appVersion') }}
              </text>
            </view>
            <wd-icon name="arrow-right" custom-class="text-[32rpx] text-[#9d9ea3]" />
          </view>
        </view>
      </view>

      <!-- Языковые настройки -->
      <view class="mb-[32rpx]">
        <view class="mb-[24rpx] flex items-center">
          <text class="text-[32rpx] text-[#232338] font-bold">
            {{ t('settings.languageSettings') }}
          </text>
        </view>

        <view
          class="border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
        >
          <view
            class="flex cursor-pointer items-center justify-between border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb] p-[24rpx] transition-all active:bg-[#eef3ff]"
            @click="showLanguageSheet = true"
          >
            <view>
              <text class="text-[32rpx] text-[#232338] font-medium">
                {{ t('settings.language') }}
              </text>
              <text class="mt-[4rpx] block text-[24rpx] text-[#9d9ea3]">
                {{ t('settings.selectLanguage') }}
              </text>
            </view>
            <view class="flex items-center">
              <text class="mr-[16rpx] text-[32rpx] text-[#9d9ea3] font-semibold">
                {{ supportedLanguages.find(lang => lang.code === currentLanguage)?.name }}
              </text>
              <wd-icon name="arrow-right" custom-class="text-[32rpx] text-[#9d9ea3]" />
            </view>
          </view>
        </view>
      </view>

      <!-- Всплывающее окно выбора языка -->
      <wd-action-sheet v-model="showLanguageSheet" :title="t('settings.selectLanguage')" :close-on-click-modal="true">
        <view class="language-sheet">
          <scroll-view scroll-y class="language-list">
            <view
              v-for="lang in supportedLanguages" :key="lang.code" class="language-item"
              @click="handleLanguageChange(lang.code)"
            >
              <text class="language-name">
                {{ lang.name }}
              </text>
            </view>
          </scroll-view>
        </view>
      </wd-action-sheet>

      <!-- Нижнее безопасное расстояние -->
      <!-- Нижнее безопасное расстояние -->
      <view style="height: env(safe-area-inset-bottom);" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
// держаться с edit.vue последовательный стиль，Стили в основном контролируются через имена классов.

// Стиль всплывающего окна выбора языка
.language-sheet {
  .language-list {
    max-height: 50vh;

    .language-item {
      padding: 30rpx 0;
      text-align: center;
      border-bottom: 1rpx solid #f0f0f0;

      .language-name {
        font-size: 28rpx;
        color: #333;
      }

      &:last-child {
        border-bottom: none;
      }

      &:active {
        background-color: #f5f7fb;
      }
    }
  }
}
</style>
