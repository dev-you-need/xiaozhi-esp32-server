<template>
  <CustomDialog
    :title="title"
    :visible.sync="visible"
    width="800px"
    @confirm="submit"
    @close="cancel"
    @open="handleOpen"
    :confirmLoading="saving"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="auto" label-position="left" class="firmware-form">
      <el-form-item :label="$t('firmwareDialog.firmwareName')" prop="firmwareName">
        <el-input v-model="form.firmwareName" :placeholder="$t('firmwareDialog.firmwareNamePlaceholder')" />
      </el-form-item>
      <el-form-item :label="$t('firmwareDialog.firmwareType')" prop="type">
        <el-select v-model="form.type" :placeholder="$t('firmwareDialog.firmwareTypePlaceholder')"
          class="custom-select" filterable :disabled="isTypeDisabled">
          <el-option v-for="item in firmwareTypes" :key="item.key" :label="item.name" :value="item.key"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('firmwareDialog.version')" prop="version">
        <el-input v-model="form.version" :placeholder="$t('firmwareDialog.versionPlaceholder')" />
      </el-form-item>
      <el-form-item :label="$t('firmwareDialog.firmwareFile')" prop="firmwarePath">
        <el-upload ref="upload" class="upload-demo" action="#" :http-request="handleUpload"
          :before-upload="beforeUpload" :accept="'.bin,.apk,.wav'" :limit="1" :multiple="false" :auto-upload="true"
          :on-remove="handleRemove">
          <el-button size="small" type="primary">{{ $t('firmwareDialog.clickUpload') }}</el-button>
          <div slot="tip" class="el-upload__tip">{{ $t('firmwareDialog.uploadTip') }}</div>
        </el-upload>
        <el-progress v-if="isUploading || uploadStatus === 'success'" :percentage="uploadProgress"
          :status="uploadStatus"></el-progress>
        <div class="hint-text">
          <span>{{ $t('firmwareDialog.uploadHint') }}</span>
        </div>
      </el-form-item>
      <el-form-item :label="$t('firmwareDialog.remark')" prop="remark">
        <el-input type="textarea" v-model="form.remark"
          :placeholder="$t('firmwareDialog.remarkPlaceholder')" />
      </el-form-item>
    </el-form>
  </CustomDialog>
</template>

<script>
import Api from '@/apis/api';
import CustomDialog from './CustomDialog.vue';

export default {
  name: 'FirmwareDialog',
  components: {
    CustomDialog
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    form: {
      type: Object,
      default: () => ({})
    },
    firmwareTypes: {
      type: Array,
      default: () => []
    }
  },

  data() {
    return {
      uploadProgress: 0,
      uploadStatus: '',
      isUploading: false,
      saving: false,
      rules: {
        firmwareName: [
          { required: true, message: this.$t('firmwareDialog.requiredFirmwareName'), trigger: 'blur' }
        ],
        type: [
          { required: true, message: this.$t('firmwareDialog.requiredFirmwareType'), trigger: 'change' }
        ],
        version: [
          { required: true, message: this.$t('firmwareDialog.requiredVersion'), trigger: 'blur' },
          { pattern: /^\d+\.\d+\.\d+$/, message: this.$t('firmwareDialog.versionFormatError'), trigger: 'blur' }
        ],
        firmwarePath: [
          { required: false, message: this.$t('firmwareDialog.requiredFirmwareFile'), trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    isTypeDisabled() {
      // Если да:id，Описание находится в режиме редактирования，Отключить выбор типа
      return !!this.form.id
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          // Если режим добавления и файл не загружен，затем выдает запрос на ошибку
          if (!this.form.id && !this.form.firmwarePath) {
            this.$message.error(this.$t('firmwareDialog.requiredFirmwareFile'))
            return
          }
          this.saving = true
          // После успешной отправки передать логику закрытия диалога родительскому компоненту
          this.$emit('submit', this.form)
        }
      })
    },
    cancel() {
      this.saving = false
      this.$emit('cancel')
    },
    // Предоставляется вызову родительского компонента для сбросаsavingСтатус
    resetSaving() {
      this.saving = false
    },
    beforeUpload(file) {
      const isValidSize = file.size / 1024 / 1024 < 100
      const isValidType = ['.bin', '.apk'].some(ext => file.name.toLowerCase().endsWith(ext))

      if (!isValidType) {
        this.$message.error(this.$t('firmwareDialog.invalidFileType'))
        return false
      }
      if (!isValidSize) {
        this.$message.error(this.$t('firmwareDialog.invalidFileSize'))
        return false
      }
      return true
    },
    handleUpload(options) {
      const { file } = options
      this.uploadProgress = 0
      this.uploadStatus = ''
      this.isUploading = true

      // Эксплуатация setTimeoutРеализация Simple0-50%Переход от оказания чрезвычайной помощи к развитию
      const timer = setTimeout(() => {
        if (this.uploadProgress < 50) {  // Только если прогресс меньше50%Устанавливать только тогда, когда
          this.uploadProgress = 50
        }
      }, 1000)

      Api.ota.uploadFirmware(file, (res) => {
        clearTimeout(timer)  // Очистить таймер
        res = res.data
        if (res.code === 0) {
          this.form.firmwarePath = res.data
          this.form.size = file.size
          this.uploadProgress = 100
          this.uploadStatus = 'success'
          this.$message.success(this.$t('firmwareDialog.uploadSuccess'))
          // задержкой2Скрыть через секундыПолоса прогресса
          setTimeout(() => {
            this.isUploading = false
          }, 2000)
        } else {
          this.uploadStatus = 'exception'
          this.$message.error(res.msg || this.$t('firmwareDialog.uploadFailed'))
          this.isUploading = false
        }
      }, (progressEvent) => {
        if (progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          // Только если прогресс больше50%Обновлено в
          if (progress > 50) {
            this.uploadProgress = progress
          }
          // Если загрузка завершена, но успешный ответ ещё не получен，Удержание в одном положенииПолоса прогрессаТип светового сигнала
          if (progress === 100) {
            this.uploadStatus = ''
          }
        }
      })
    },
    handleRemove() {
      this.form.firmwarePath = ''
      this.form.size = 0
      this.uploadProgress = 0
      this.uploadStatus = ''
      this.isUploading = false
    },
    handleOpen() {
      // Сбросить статус, связанный с загрузкой
      this.uploadProgress = 0
      this.uploadStatus = ''
      this.isUploading = false
      this.saving = false
      // Сброс файловых полей формы
      if (!this.form.id) {  // Сбросить только при добавлении
        this.form.firmwarePath = ''
        this.form.size = 0
      }
      // Редактировать режим или нет，оба сбросить загрузчика
      this.$nextTick(() => {
        if (this.$refs.upload) {
          this.$refs.upload.clearFiles()
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.firmware-form {
  .custom-select {
    width: 100%;
  }
  .upload-demo {
    text-align: left;
  }
  .el-upload__tip {
    line-height: 1.2;
    padding-top: 2%;
    color: #909399;
  }
  .hint-text {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
  }
}
</style>
