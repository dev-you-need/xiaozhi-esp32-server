export enum ResultEnum {
  Success = 0, // Успех
  Error = 400, // Ошибка
  Unauthorized = 401, // Не авторизован
  Forbidden = 403, // Доступ запрещен (было forbidden)
  NotFound = 404, // Не найдено (было notFound)
  MethodNotAllowed = 405, // Метод не разрешен (было methodNotAllowed)
  RequestTimeout = 408, // Тайм-аут запроса (было requestTimeout)
  InternalServerError = 500, // Ошибка сервера (было internalServerError)
  NotImplemented = 501, // Не реализовано (было notImplemented)
  BadGateway = 502, // Ошибка шлюза (было badGateway)
  ServiceUnavailable = 503, // Сервис недоступен (было serviceUnavailable)
  GatewayTimeout = 504, // Тайм-аут шлюза (было gatewayTimeout)
  HttpVersionNotSupported = 505, // Версия HTTP не поддерживается (было httpVersionNotSupported)
  MixedContent = 600, // Ошибка смешанного контента (страница HTTPS запрашивает интерфейс HTTP)
}
export enum ContentTypeEnum {
  JSON = 'application/json;charset=UTF-8',
  FORM_URLENCODED = 'application/x-www-form-urlencoded;charset=UTF-8',
  FORM_DATA = 'multipart/form-data;charset=UTF-8',
}
/**
 * Генерация соответствующего сообщения об ошибке по коду состояния
 * @param {number|string} status Код состояния
 * @returns {string} Сообщение об ошибке
 */
export function ShowMessage(status: number | string): string {
  let message: string
  switch (status) {
    case 400:
      message = 'Ошибка запроса(400)'
      break
    case 401:
      message = 'Не авторизован, войдите снова(401)'
      break
    case 403:
      message = 'Доступ запрещен(403)'
      break
    case 404:
      message = 'Ошибка запроса(404)'
      break
    case 408:
      message = 'Тайм-аут запроса(408)'
      break
    case 500:
      message = 'Ошибка сервера(500)'
      break
    case 501:
      message = 'Сервис не реализован(501)'
      break
    case 502:
      message = 'Ошибка сети(502)'
      break
    case 503:
      message = 'Сервис недоступен(503)'
      break
    case 504:
      message = 'Тайм-аут сети(504)'
      break
    case 505:
      message = 'Версия HTTP не поддерживается(505)'
      break
    case 600:
      message = 'Ошибка смешанного контента(600)'
      break
    default:
      message = `Ошибка подключения(${status})!`
  }
  return `${message}, проверьте сеть или обратитесь к администратору!`
}
