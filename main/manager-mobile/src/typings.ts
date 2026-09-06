// Определения перечислений

export enum TestEnum {
  A = '1',
  B = '2',
}

// Параметры загрузки файла uni.uploadFile
export interface IUniUploadFileOptions {
  file?: File
  files?: UniApp.UploadFileOptionFiles[]
  filePath?: string
  name?: string
  formData?: any
}
