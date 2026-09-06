<script setup lang="ts">
import { onHide, onLaunch, onShow } from '@dcloudio/uni-app'
import { onMounted, watch } from 'vue'
import { usePageAuth } from '@/hooks/usePageAuth'
import { t } from '@/i18n'
import { useConfigStore } from '@/store'
import { useLangStore } from '@/store/lang'
import 'abortcontroller-polyfill/dist/abortcontroller-polyfill-only'

usePageAuth()

const configStore = useConfigStore()
const langStore = useLangStore()

onLaunch(() => {
  console.log('App Launch')
  // Получить общедоступную конфигурацию
  configStore.fetchPublicConfig().catch((error) => {
    console.error('Ошибка получения общих настроек:', error)
  })
})
onShow(() => {
  console.log('App Show')
  // использоватьsetTimeoutОтложенное выполнение，убеждатьсяtabBarУже инициализировано
  setTimeout(() => {
    updateTabBarText()
  }, 100)
})

// Динамические обновленияtabBarтекст
function updateTabBarText() {
  try {
    // Настроить домашнюю страницуtabBarтекст
    uni.setTabBarItem({
      index: 0,
      text: t('tabBar.home'),
      success: () => {},
      fail: (err) => {
        console.log('Настроить домашнюю страницуtabBarтекст не удался:', err)
      },
    })

    // Установка настройки сетиtabBarтекст
    uni.setTabBarItem({
      index: 1,
      text: t('tabBar.deviceConfig'),
      success: () => {},
      fail: (err) => {
        console.log('Установка настройки сетиtabBarтекст не удался:', err)
      },
    })

    // Настройте системуtabBarтекст
    uni.setTabBarItem({
      index: 2,
      text: t('tabBar.settings'),
      success: () => {},
      fail: (err) => {
        console.log('Настройте системуtabBarтекст не удался:', err)
      },
    })
  }
  catch (error) {
    console.log('возобновлятьtabBarПри отправке текста произошла ошибка:', error)
  }
}
// Отслеживание события смены языка
onMounted(() => {
  // Слушайте изменения языка，когда язык меняетсяАвтообновлениеtabBarтекст
  watch(() => langStore.currentLang, () => {
    console.log('Язык был переключен，возобновлятьtabBarтекст')
    // Обновление сразу после переключения языкаtabBarтекст
    updateTabBarText()
  })
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
swiper,
scroll-view {
  flex: 1;
  height: 100%;
  overflow: hidden;
}

image {
  width: 100%;
  height: 100%;
  vertical-align: middle;
}
</style>
