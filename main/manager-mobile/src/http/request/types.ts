// Универсальный формат ответа
export interface IResponse<T = any> {
  code: number | string
  data: T
  msg: string
  status: string | number
}

// Параметры постраничного запроса
export interface PageParams {
  page: number
  pageSize: number
  [key: string]: any
}

// Данные постраничного ответа
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}
