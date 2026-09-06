import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

const CALLBACK_RETRY_LIMIT = 10;
const CALLBACK_RETRY_DELAY_MS = 2000;
const CALLBACK_RETRY_WINDOW_MS = CALLBACK_RETRY_LIMIT * CALLBACK_RETRY_DELAY_MS;

function retryCallbackRequest(retry, retryCount, onTerminalFailure, error, retryStartedAt) {
    if (!onTerminalFailure) {
        RequestService.reAjaxFun(() => retry(retryCount + 1))
        return
    }
    const startedAt = retryStartedAt || Date.now()
    if (retryCount >= CALLBACK_RETRY_LIMIT || Date.now() - startedAt >= CALLBACK_RETRY_WINDOW_MS) {
        RequestService.clearRequestTime()
        if (onTerminalFailure) {
            onTerminalFailure(error)
        }
        return
    }
    setTimeout(() => retry(retryCount + 1, startedAt), CALLBACK_RETRY_DELAY_MS)
}


export default {
    // Получение списка файлов замены слов
    getFileList(params, callback) {
        const queryParams = new URLSearchParams({
            page: params.page,
            limit: params.pageSize
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/list?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка файлов замены слов:', err)
                RequestService.reAjaxFun(() => {
                    this.getFileList(params, callback)
                })
            }).send()
    },

    // Получение всех файлов замены слов (без постраничности)
    selectAll(callback, onTerminalFailure, retryCount = 0, retryStartedAt = 0) {
        const retryWindowStartedAt = retryStartedAt || Date.now()
        const request = RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/select`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения всех файлов замены слов:', err)
                retryCallbackRequest(
                    (nextRetryCount, nextRetryStartedAt) => this.selectAll(
                        callback,
                        onTerminalFailure,
                        nextRetryCount,
                        nextRetryStartedAt
                    ),
                    retryCount,
                    onTerminalFailure,
                    err,
                    retryWindowStartedAt
                )
            })
        if (onTerminalFailure) {
            request.fail((error) => {
                RequestService.clearRequestTime()
                onTerminalFailure(error)
            })
        }
        request.send()
    },

    // Скачивание файла замены слов
    downloadFile(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/download/${id}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
              RequestService.clearRequestTime()
              callback(err)
            }).send()
    },

    // Добавление файла замены слов
    addFile(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
              RequestService.clearRequestTime()
              callback(err)
            }).send()
    },

    // Обновление файла замены слов
    updateFile(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/${data.id}`)
            .method('PUT')
            .data({
                fileName: data.fileName,
                content: data.content
            })
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
              RequestService.clearRequestTime()
              callback(err)
            }).send()
    },

    // Удаление файла замены слов
    deleteFile(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/${id}`)
            .method('DELETE')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка удаления файла замены слов:', err)
                RequestService.reAjaxFun(() => {
                    this.deleteFile(id, callback)
                })
            }).send()
    },

    // Пакетное удаление файлов замены слов
    batchDeleteFile(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/correct-word/file/batch-delete`)
            .method('POST')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка пакетного удаления файлов замены слов:', err)
                RequestService.reAjaxFun(() => {
                    this.batchDeleteFile(ids, callback)
                })
            }).send()
    }
}
