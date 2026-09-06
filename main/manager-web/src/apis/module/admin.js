import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';


export default {
    // Список пользователей
    getUserList(params, callback) {
        const queryParams = new URLSearchParams({
            page: params.page,
            limit: params.limit,
            mobile: params.mobile
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/users?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка запроса:', err)
                RequestService.reAjaxFun(() => {
                    this.getUserList(callback)
                })
            }).send()
    },
    // Удаление пользователя
    deleteUser(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/users/${id}`)
            .method('DELETE')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка удаления:', err)
                RequestService.reAjaxFun(() => {
                    this.deleteUser(id, callback)
                })
            }).send()
    },
    // Сброс пароля пользователя
    resetUserPassword(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/users/${id}`)
            .method('PUT')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка сброса пароля:', err)
                RequestService.reAjaxFun(() => {
                    this.resetUserPassword(id, callback)
                })
            }).send()
    },
    // Получение списка параметров
    getParamsList(params, callback) {
        const queryParams = new URLSearchParams({
            page: params.page,
            limit: params.limit,
            paramCode: params.paramCode || ''
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/params/page?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка параметров:', err)
                RequestService.reAjaxFun(() => {
                    this.getParamsList(params, callback)
                })
            }).send()
    },
    // Сохранение
    addParam(data, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/params`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
                RequestService.clearRequestTime()
                failCallback(err)
            })
            .networkFail((err) => {
                console.error('Ошибка добавления параметра:', err)
                RequestService.reAjaxFun(() => {
                    this.addParam(data, callback)
                })
            }).send()
    },
    // Изменение
    updateParam(data, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/params`)
            .method('PUT')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
                RequestService.clearRequestTime()
                failCallback(err)
            })
            .networkFail((err) => {
                console.error('Ошибка обновления параметра:', err)
                RequestService.reAjaxFun(() => {
                    this.updateParam(data, callback)
                })
            }).send()
    },
    // Удаление
    deleteParam(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/params/delete`)
            .method('POST')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res);
            })
            .networkFail((err) => {
                console.error('Ошибка удаления параметра:', err)
                RequestService.reAjaxFun(() => {
                    this.deleteParam(ids, callback)
                })
            }).send()
    },
    // Получение списка серверов ws
    getWsServerList(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/server/server-list`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка серверов ws:', err)
                RequestService.reAjaxFun(() => {
                    this.getWsServerList(params, callback)
                })
            }).send();
    },
    // Отправка команды действия сервера ws
    sendWsServerAction(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/server/emit-action`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                RequestService.reAjaxFun(() => {
                    this.sendWsServerAction(data, callback)
                })
            }).send();
    }

}
