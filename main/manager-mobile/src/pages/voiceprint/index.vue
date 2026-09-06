<script lang="ts" setup>
import type { ChatHistory, CreateSpeakerData, VoicePrint } from '@/api/voiceprint'
import { computed, onMounted, ref } from 'vue'
import { useMessage } from 'wot-design-uni/components/wd-message-box'
import { useToast } from 'wot-design-uni/components/wd-toast'
import { createVoicePrint, deleteVoicePrint, getAudioDownloadId, getChatHistory, getVoicePrintList, updateVoicePrint } from '@/api/voiceprint'
import { t } from '@/i18n'
import { getEnvBaseUrl } from '@/utils'

defineOptions({
  name: 'VoicePrintManage',
})

const props = withDefaults(defineProps<Props>(), {
  agentId: 'default',
})

const emits = defineEmits(['update-refresher-enabled'])

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

const message = useMessage()
const toast = useToast()

// Данные страницы
const voicePrintList = ref<VoicePrint[]>([])
const chatHistoryList = ref<ChatHistory[]>([])
const chatHistoryActions = ref<any[]>([])
const swipeStates = ref<Record<string, 'left' | 'close' | 'right'>>({})
const loading = ref(false)

// Настройки воспроизведения аудио
const audioRef = ref<UniApp.InnerAudioContext | null>(null)
const playingAudioId = ref<string>('')

// Использование переданного агентаID
const currentAgentId = computed(() => {
  return props.agentId
})

// Функция, связанная с выбором агента, удалена

// Связанное всплывающее окно
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const showChatHistoryDialog = ref(false)
const addForm = ref<CreateSpeakerData>({
  agentId: '',
  audioId: '',
  sourceName: '',
  introduce: '',
})
const editForm = ref<VoicePrint>({
  id: '',
  audioId: '',
  sourceName: '',
  introduce: '',
  createDate: '',
})

// Получить список голосовых отпечатков
async function loadVoicePrintList() {
  try {
    console.log('Получить список голосовых отпечатков')

    // Проверка наличия текущего выбранного агента
    if (!currentAgentId.value) {
      console.warn(t('voiceprint.noSelectedAgent'))
      voicePrintList.value = []
      return
    }

    loading.value = true
    const data = await getVoicePrintList(currentAgentId.value)

    // Инициализация состояния прокрутки
    const list = data || []
    list.forEach((item) => {
      if (!swipeStates.value[item.id]) {
        swipeStates.value[item.id] = 'close'
      }
    })

    voicePrintList.value = list
  }
  catch (error) {
    console.error('Ошибка получения списка голосовых отпечатков:', error)
    voicePrintList.value = []
  }
  finally {
    loading.value = false
  }
}

// Воздействие на отцакомпонентыОбновить метод для
async function refresh() {
  await loadVoicePrintList()
}

// Получение голосовых записей диалога
async function loadChatHistory() {
  try {
    if (!currentAgentId.value) {
      toast.error(t('voiceprint.pleaseSelectAgent'))
      return
    }

    const data = await getChatHistory(currentAgentId.value)
    chatHistoryList.value = data || []
    // СтоимостьActionSheetФорма
    chatHistoryActions.value = chatHistoryList.value.map((item, index) => ({
      name: item.content,
      audioId: item.audioId,
      index,
    }))
  }
  catch (error) {
    console.error('Ошибка получения записей диалога:', error)
    toast.error(t('voiceprint.fetchHistoryFailed'))
  }
}

// Открыть всплывающее окно «Добавить»
function openAddDialog() {
  if (!currentAgentId.value) {
    toast.error(t('voiceprint.pleaseSelectAgent'))
    return
  }

  // Проверка конфигурации интерфейса голосовых отпечатков（Обнаружить, пытаясь получить список голосовых отпечатков）
  const checkVoicePrintConfig = async () => {
    try {
      await getVoicePrintList(currentAgentId.value)
      // Интерфейс ОК，Продолжайте открывать всплывающее окно добавления.
      addForm.value = {
        agentId: currentAgentId.value,
        audioId: '',
        sourceName: '',
        introduce: '',
      }
      showAddDialog.value = true
    }
    catch (error: any) {
      // Интерфейс захвата голосового отпечатка настроен некорректно
      if (error.message && error.message.includes('Ошибка запроса[10054]')) {
        toast.error(t('voiceprint.voiceprintInterfaceNotConfigured'))
      }
      else {
        // Другая ошибка，Продолжайте открывать всплывающее окно
        addForm.value = {
          agentId: currentAgentId.value,
          audioId: '',
          sourceName: '',
          introduce: '',
        }
        showAddDialog.value = true
      }
    }
  }

  checkVoicePrintConfig()
}

// Откройте всплывающее окно редактирования
function openEditDialog(item: VoicePrint) {
  editForm.value = { ...item }
  showEditDialog.value = true
}

// Получение отображаемого содержимого выбранного аудио
function getSelectedAudioContent(audioId: string) {
  if (!audioId)
    return t('voiceprint.clickToSelectVector')
  const chatItem = chatHistoryList.value.find(item => item.audioId === audioId)
  return chatItem ? chatItem.content : `Выбрано: ${audioId.substring(0, 8)}...`
}

// Выбрать вектор голосового отпечатка
function selectAudioId({ item }: { item: any }) {
  if (showAddDialog.value) {
    addForm.value.audioId = item.audioId
  }
  else if (showEditDialog.value) {
    editForm.value.audioId = item.audioId
  }
  showChatHistoryDialog.value = false
}

// Нажмите, чтобы выбрать
function handleItemClick(item: any) {
  selectAudioId({ item })
}

// Отправка добавления говорящего
async function submitAdd() {
  if (!addForm.value.sourceName.trim()) {
    toast.error(t('voiceprint.pleaseInputName'))
    return
  }
  if (!addForm.value.audioId) {
    toast.error(t('voiceprint.pleaseSelectVector'))
    return
  }

  try {
    await createVoicePrint(addForm.value)
    toast.success(t('voiceprint.addSuccess'))
    showAddDialog.value = false
    await loadVoicePrintList()
  }
  catch (error) {
    console.error('Ошибка добавления говорящего:', error)
    toast.error(t('voiceprint.addFailed'))
  }
}

// Отправка редактирования говорящего
async function submitEdit() {
  if (!editForm.value.sourceName.trim()) {
    toast.error(t('voiceprint.pleaseInputName'))
    return
  }
  if (!editForm.value.audioId) {
    toast.error(t('voiceprint.pleaseSelectVector'))
    return
  }

  try {
    await updateVoicePrint({
      id: editForm.value.id,
      audioId: editForm.value.audioId,
      sourceName: editForm.value.sourceName,
      introduce: editForm.value.introduce,
      createDate: editForm.value.createDate,
    })
    toast.success(t('voiceprint.editSuccess'))
    showEditDialog.value = false
    await loadVoicePrintList()
  }
  catch (error) {
    console.error('Не удалось изменить докладчика.:', error)
    toast.error(t('voiceprint.editFailed'))
  }
}

// Обработка операции редактирования
function handleEdit(item: VoicePrint) {
  openEditDialog(item)
  swipeStates.value[item.id] = 'close'
}

// Удаление голосового отпечатка
async function handleDelete(id: string) {
  message.confirm({
    msg: t('voiceprint.deleteConfirmMsg'),
    title: t('voiceprint.deleteConfirmTitle'),
  }).then(async () => {
    await deleteVoicePrint(id)
    toast.success(t('voiceprint.deleteSuccess'))
    await loadVoicePrintList()
  }).catch(() => {
    console.log('Нажата кнопка отмены')
  })
}

// Воспроизвести аудио
async function playAudio(audioId: string, event: Event) {
  event.stopPropagation() // Остановить пузырящееся событие，Предотвратить закрытие раскрывающегося списка

  if (!audioId) {
    toast.warning(t('voiceprint.audioNotExist'))
    return
  }

  // Если воспроизводится то же аудио，Затем остановитесь
  if (playingAudioId.value === audioId) {
    stopAudio()
    return
  }

  // Остановить предыдущий звук
  stopAudio()

  try {
    // Сначала получить ID скачивания аудио
    playingAudioId.value = audioId
    const downloadId = await getAudioDownloadId(audioId)

    if (!downloadId) {
      toast.error(t('voiceprint.getAudioFailed'))
      playingAudioId.value = ''
      return
    }

    // ПолучитьbaseURL
    const baseURL = getEnvBaseUrl()
    const audioUrl = `${baseURL}/agent/play/${downloadId}`

    // Создание нового экземпляра аудио
    audioRef.value = uni.createInnerAudioContext()
    audioRef.value.src = audioUrl
    audioRef.value.autoplay = true

    // Отслеживание окончания воспроизведения
    audioRef.value.onEnded(() => {
      playingAudioId.value = ''
    })

    // Отслеживание ошибок воспроизведения
    audioRef.value.onError((error) => {
      console.error('Ошибка воспроизведения аудио:', error)
      toast.error(t('voiceprint.audioPlayFailed'))
      playingAudioId.value = ''
    })
  }
  catch (error) {
    console.error('Не удалось воспроизвести аудио:', error)
    toast.error(t('voiceprint.audioPlayFailed'))
    playingAudioId.value = ''
  }
}

// Остановка аудио
function stopAudio() {
  if (audioRef.value) {
    audioRef.value.stop()
    audioRef.value.destroy()
    audioRef.value = null
  }
  playingAudioId.value = ''
}

watch(() => [showAddDialog.value, showEditDialog.value], (newValues) => {
  if (newValues.some((value: boolean) => value)) {
    emits('update-refresher-enabled', false)
  }
  else {
    emits('update-refresher-enabled', true)
  }
})

onMounted(async () => {
  // Агент был сведен к дефолту

  loadVoicePrintList()
  loadChatHistory()
})

// Метод воздействия на отцакомпоненты
defineExpose({
  showAddDialog,
  showEditDialog,
  refresh,
})
</script>

<template>
  <view class="voiceprint-container" style="background: #f5f7fb; min-height: 100%;">
    <!-- Статус загрузки -->
    <view v-if="loading && voicePrintList.length === 0" class="loading-container">
      <wd-loading color="#336cff" />
      <text class="loading-text">
        {{ t('voiceprint.loading') }}
      </text>
    </view>

    <!-- Список голосовых отпечатков -->
    <view v-else-if="voicePrintList.length > 0" class="voiceprint-list">
      <!-- Список карточек Voiceprint -->
      <view class="box-border flex flex-col gap-[24rpx] p-[20rpx]">
        <view v-for="item in voicePrintList" :key="item.id">
          <wd-swipe-action
            :model-value="swipeStates[item.id] || 'close'"
            @update:model-value="swipeStates[item.id] = $event"
          >
            <view class="bg-[#fbfbfb] p-[32rpx]" @click="handleEdit(item)">
              <view>
                <text class="mb-[12rpx] block text-[32rpx] text-[#232338] font-semibold">
                  {{ item.sourceName }}
                </text>
                <text class="mb-[12rpx] block text-[28rpx] text-[#65686f] leading-[1.4]">
                  {{ item.introduce || 'Описания пока нет' }}
                </text>
                <text class="block text-[24rpx] text-[#9d9ea3]">
                  {{ item.createDate }}
                </text>
              </view>
            </view>

            <template #right>
              <view class="h-full flex">
                <view
                  class="h-full min-w-[120rpx] flex items-center justify-center bg-[#ff4d4f] p-x-[32rpx] text-[28rpx] text-white font-medium"
                  @click="handleDelete(item.id)"
                >
                  <wd-icon name="delete" />
                  {{ t('voiceprint.delete') }}
                </view>
              </view>
            </template>
          </wd-swipe-action>
        </view>
      </view>
    </view>

    <!-- Пустое состояние -->
    <view v-else-if="!loading" class="empty-container">
      <view class="flex flex-col items-center justify-center p-[100rpx_40rpx] text-center">
        <wd-icon name="voice" custom-class="text-[120rpx] text-[#d9d9d9] mb-[32rpx]" />
        <text class="mb-[32rpx] text-[32rpx] text-[#666666] font-medium">
          {{ t('voiceprint.emptyTitle') }}
        </text>
        <text class="text-[26rpx] text-[#999999] leading-[1.5]">
          {{ t('voiceprint.emptyDesc') }}
        </text>
      </view>
    </view>

    <!-- Плавающая кнопка действия -->
    <wd-fab custom-style="z-index:10" type="primary" size="small" :draggable="true" :expandable="false" @click="openAddDialog">
      <wd-icon name="add" />
    </wd-fab>

    <!-- MessageBox компоненты -->
    <wd-message-box />
  </view>

  <!-- Диалог добавления говорящего -->
  <wd-popup
    v-model="showAddDialog"
    position="center"
    custom-style="width: 90%; max-width: 400px; border-radius: 16px;"
    safe-area-inset-bottom
  >
    <view>
      <view class="p-[32rpx]">
        <!-- Выбор вектора голосового отпечатка -->
        <view class="mb-[32rpx]">
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.voiceVector') }}
          </text>
          <view
            class="flex cursor-pointer items-center justify-between border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[20rpx] transition-all duration-300 active:bg-[#eef3ff]"
            @click="showChatHistoryDialog = true"
          >
            <text
              class="m-r-[16rpx] flex-1 text-left text-[26rpx] text-[#232338]"
              :class="{ 'text-[#9d9ea3]': !addForm.audioId }"
            >
              {{ getSelectedAudioContent(addForm.audioId) }}
            </text>
            <wd-icon name="arrow-down" custom-class="text-[20rpx] text-[#9d9ea3]" />
          </view>
        </view>

        <!-- (ФИО) -->
        <view class="mb-[32rpx]">
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.name') }}
          </text>
          <input
            v-model="addForm.sourceName"
            class="box-border h-[80rpx] w-full border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[16rpx_20rpx] text-[28rpx] text-[#232338] leading-[1.4] outline-none focus:border-[#336cff] focus:bg-white placeholder:text-[#9d9ea3]"
            type="text" :placeholder="t('voiceprint.pleaseInputName')"
          >
        </view>

        <!-- ОПИСАНИЕ -->
        <view>
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.description') }}
          </text>
          <textarea
            v-model="addForm.introduce" :maxlength="100" :placeholder="t('voiceprint.pleaseInputDescription')"
            class="box-border h-[200rpx] w-full resize-none border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[20rpx] text-[26rpx] text-[#232338] leading-[1.6] outline-none focus:border-[#336cff] focus:bg-white placeholder:text-[#9d9ea3]"
          />
          <view class="mt-[8rpx] text-right text-[22rpx] text-[#9d9ea3]">
            {{ (addForm.introduce || '').length }}/100
          </view>
        </view>
      </view>

      <view class="flex gap-[16rpx] border-t-[2rpx] border-[#eeeeee] p-[24rpx_32rpx_32rpx]">
        <wd-button type="info" custom-class="flex-1" @click="showAddDialog = false">
          {{ t('voiceprint.cancel') }}
        </wd-button>
        <wd-button type="primary" custom-class="flex-1" @click="submitAdd">
          {{ t('voiceprint.save') }}
        </wd-button>
      </view>
    </view>
  </wd-popup>

  <!-- Редактировать всплывающее окно динамика -->
  <wd-popup
    v-model="showEditDialog" position="center" custom-style="width: 90%; max-width: 400px; border-radius: 16px;"
    safe-area-inset-bottom
  >
    <view>
      <view class="box-border w-full flex items-center justify-between border-b-[2rpx] border-[#eeeeee] p-[32rpx_32rpx_24rpx]">
        <text class="w-full text-center text-[32rpx] text-[#232338] font-semibold">
          {{ t('voiceprint.editSpeaker') }}
        </text>
      </view>

      <view class="p-[32rpx]">
        <!-- Выбор вектора голосового отпечатка -->
        <view class="mb-[32rpx]">
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.voiceVector') }}
          </text>
          <view
            class="flex cursor-pointer items-center justify-between border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[20rpx] transition-all duration-300 active:bg-[#eef3ff]"
            @click="showChatHistoryDialog = true"
          >
            <text
              class="m-r-[16rpx] flex-1 text-left text-[26rpx] text-[#232338]"
              :class="{ 'text-[#9d9ea3]': !editForm.audioId }"
            >
              {{ getSelectedAudioContent(editForm.audioId) }}
            </text>
            <wd-icon name="arrow-down" custom-class="text-[20rpx] text-[#9d9ea3]" />
          </view>
        </view>

        <!-- (ФИО) -->
        <view class="mb-[32rpx]">
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.name') }}
          </text>
          <input
            v-model="editForm.sourceName"
            class="box-border h-[80rpx] w-full border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[16rpx_20rpx] text-[28rpx] text-[#232338] leading-[1.4] outline-none focus:border-[#336cff] focus:bg-white placeholder:text-[#9d9ea3]"
            type="text" :placeholder="t('voiceprint.pleaseInputName')"
          >
        </view>

        <!-- ОПИСАНИЕ -->
        <view>
          <text class="mb-[16rpx] block text-[28rpx] text-[#232338] font-medium">
            <text class="text-red">
              *
            </text>
            {{ t('voiceprint.description') }}
          </text>
          <textarea
            v-model="editForm.introduce" :maxlength="100" :placeholder="t('voiceprint.pleaseInputDescription')"
            class="box-border h-[200rpx] w-full resize-none border-[1rpx] border-[#eeeeee] rounded-[12rpx] bg-[#f5f7fb] p-[20rpx] text-[26rpx] text-[#232338] leading-[1.6] outline-none focus:border-[#336cff] focus:bg-white placeholder:text-[#9d9ea3]"
          />
          <view class="mt-[8rpx] text-right text-[22rpx] text-[#9d9ea3]">
            {{ (editForm.introduce || '').length }}/100
          </view>
        </view>
      </view>

      <view class="flex gap-[16rpx] border-t-[2rpx] border-[#eeeeee] p-[24rpx_32rpx_32rpx]">
        <wd-button type="info" custom-class="flex-1" @click="showEditDialog = false">
          {{ t('voiceprint.cancel') }}
        </wd-button>
        <wd-button type="primary" custom-class="flex-1" @click="submitEdit">
          {{ t('voiceprint.save') }}
        </wd-button>
      </view>
    </view>
  </wd-popup>

  <!-- Настройте всплывающий слой выбора записи голосового разговора -->
  <wd-popup v-model="showChatHistoryDialog" class="custom-popup" position="bottom" @close="stopAudio">
    <view class="rounded-[20rpx] bg-white pb-[20rpx] pt-[20rpx]">
      <view class="max-h-[600rpx] overflow-y-auto rounded-[20rpx]">
        <view
          v-for="item in chatHistoryActions"
          :key="item.audioId"
          class="flex items-center justify-between border-b border-[#f5f5f5] p-[32rpx] transition-all active:bg-[#f5f7fb]"
          @click="handleItemClick(item)"
        >
          <text class="flex-1 text-[28rpx] text-[#232338]">
            {{ item.name }}
          </text>
          <view class="ml-[20rpx]" @click.stop="playAudio(item.audioId, $event)">
            <wd-icon
              :name="playingAudioId === item.audioId ? 'pause-circle' : 'play-circle'"
              size="24px"
              :custom-class="playingAudioId === item.audioId ? 'text-[#336cff]' : 'text-[#9d9ea3]'"
            />
          </view>
        </view>
      </view>
    </view>
  </wd-popup>
</template>

<style lang="scss" scoped>
.voiceprint-container {
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

::v-deep .custom-popup {
  .wd-popup {
    padding: 20rpx !important;
    background: transparent;
  }
}
</style>
