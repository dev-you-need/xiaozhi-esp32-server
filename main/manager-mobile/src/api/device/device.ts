import type { Device, FirmwareType } from './types'
import { http } from '@/http/request/alova'

/**
 * Получение списка типов устройств
 */
export function getFirmwareTypes() {
  return http.Get<FirmwareType[]>('/admin/dict/data/type/FIRMWARE_TYPE')
}

/**
 * Получение списка привязанных устройств
 * @param agentId ID агента
 */
export function getBindDevices(agentId: string) {
  return http.Get<Device[]>(`/device/bind/${agentId}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

/**
 * Добавление устройства
 * @param agentId ID агента
 * @param code Код подтверждения
 */
export function bindDevice(agentId: string, code: string) {
  return http.Post(`/device/bind/${agentId}/${code}`, null)
}

/**
 * Ручное добавление устройства
 * @param agentId ID агента
 * @param board Тип устройства
 * @param appVersion Версия прошивки
 * @param macAddress MAC адрес
 */
export function bindDeviceManual(data: {
  agentId: string
  board: string
  appVersion: string
  macAddress: string
}) {
  return http.Post('/device/manual-add', data)
}

/**
 * Установка переключателя OTA обновления устройства
 * @param deviceId ID устройства (MAC адрес)
 * @param autoUpdate Автоматическое обновление 0|1
 */
export function updateDeviceAutoUpdate(deviceId: string, autoUpdate: number) {
  return http.Put(`/device/update/${deviceId}`, {
    autoUpdate,
  })
}

/**
 * Отвязка устройства
 * @param deviceId ID устройства (MAC адрес)
 */
export function unbindDevice(deviceId: string) {
  return http.Post('/device/unbind', {
    deviceId,
  })
}
