import { http } from '@/http/request/alova'

// Тип данных интерфейса авторизации
export interface LoginData {
  username: string
  password: string
  captchaId: string
  areaCode?: string
  mobile?: string
}

// Тип данных ответа авторизации
export interface LoginResponse {
  token: string
  expire: number
  clientHash: string
}

// Тип данных ответа капчи
export interface CaptchaResponse {
  captchaId: string
  captchaImage: string
}

// Получение капчи
export function getCaptcha(uuid: string) {
  return http.Get<string>('/user/captcha', {
    params: { uuid },
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// Авторизация пользователя
export function login(data: LoginData) {
  return http.Post<LoginResponse>('/user/login', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}

// Тип данных информации о пользователе
export interface UserInfo {
  id: number
  username: string
  realName: string
  email: string
  mobile: string
  status: number
  superAdmin: number
}

// Тип данных публичной конфигурации
export interface PublicConfig {
  enableMobileRegister: boolean
  version: string
  year: string
  allowUserRegister: boolean
  mobileAreaList: Array<{
    name: string
    key: string
  }>
  beianIcpNum: string
  beianGaNum: string
  name: string
  sm2PublicKey: string
}

// Получение информации о пользователе
export function getUserInfo() {
  return http.Get<UserInfo>('/user/info', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

// Получение публичной конфигурации
export function getPublicConfig() {
  return http.Get<PublicConfig>('/user/pub-config', {
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// Тип данных регистрации
export interface RegisterData {
  username: string
  password: string
  captchaId: string
  areaCode: string
  mobile: string
  mobileCaptcha: string
}

// Отправка SMS кода подтверждения
export function sendSmsCode(data: {
  phone: string
  captcha: string
  captchaId: string
}) {
  return http.Post('/user/smsVerification', data, {
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// Регистрация пользователя
export function register(data: RegisterData) {
  return http.Post('/user/register', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}

// Тип данных забытого пароля
export interface ForgotPasswordData {
  phone: string
  code: string
  password: string
  captchaId: string
}

// Забытый пароль (восстановление пароля)
export function retrievePassword(data: ForgotPasswordData) {
  return http.Put('/user/retrieve-password', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}
