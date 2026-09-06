import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // Получение списка типов словаря
    getDictTypeList(params, callback) {
        const queryParams = new URLSearchParams({
            dictType: params.dictType || '',
            dictName: params.dictName || '',
            page: params.page || 1,
            limit: params.limit || 10
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/type/page?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка типов словаря:', err)
                this.$message.error(err.msg || 'Ошибка получения списка типов словаря')
                RequestService.reAjaxFun(() => {
                    this.getDictTypeList(params, callback)
                })
            }).send()
    },

    // Получение деталей типа словаря
    getDictTypeDetail(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/type/${id}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения деталей типа словаря:', err)
                this.$message.error(err.msg || 'Ошибка получения деталей типа словаря')
                RequestService.reAjaxFun(() => {
                    this.getDictTypeDetail(id, callback)
                })
            }).send()
    },

    // Добавление типа словаря
    addDictType(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/type/save`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка добавления типа словаря:', err)
                this.$message.error(err.msg || 'Ошибка добавления типа словаря')
                RequestService.reAjaxFun(() => {
                    this.addDictType(data, callback)
                })
            }).send()
    },

    // Обновление типа словаря
    updateDictType(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/type/update`)
            .method('PUT')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка обновления типа словаря:', err)
                this.$message.error(err.msg || 'Ошибка обновления типа словаря')
                RequestService.reAjaxFun(() => {
                    this.updateDictType(data, callback)
                })
            }).send()
    },

    // Удаление типа словаря
    deleteDictType(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/type/delete`)
            .method('POST')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка удаления типа словаря:', err)
                this.$message.error(err.msg || 'Ошибка удаления типа словаря')
                RequestService.reAjaxFun(() => {
                    this.deleteDictType(ids, callback)
                })
            }).send()
    },

    // Получение списка данных словаря
    getDictDataList(params, callback) {
        const queryParams = new URLSearchParams({
            dictTypeId: params.dictTypeId,
            dictLabel: params.dictLabel || '',
            dictValue: params.dictValue || '',
            page: params.page || 1,
            limit: params.limit || 10
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/data/page?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка данных словаря:', err)
                this.$message.error(err.msg || 'Ошибка получения списка данных словаря')
                RequestService.reAjaxFun(() => {
                    this.getDictDataList(params, callback)
                })
            }).send()
    },

    // Получение деталей данных словаря
    getDictDataDetail(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/data/${id}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка получения деталей данных словаря:', err)
                this.$message.error(err.msg || 'Ошибка получения деталей данных словаря')
                RequestService.reAjaxFun(() => {
                    this.getDictDataDetail(id, callback)
                })
            }).send()
    },

    // Добавление данных словаря
    addDictData(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/data/save`)
            .method('POST')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка добавления данных словаря:', err)
                this.$message.error(err.msg || 'Ошибка добавления данных словаря')
                RequestService.reAjaxFun(() => {
                    this.addDictData(data, callback)
                })
            }).send()
    },

    // Обновление данных словаря
    updateDictData(data, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/data/update`)
            .method('PUT')
            .data(data)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка обновления данных словаря:', err)
                this.$message.error(err.msg || 'Ошибка обновления данных словаря')
                RequestService.reAjaxFun(() => {
                    this.updateDictData(data, callback)
                })
            }).send()
    },

    // Удаление данных словаря
    deleteDictData(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/admin/dict/data/delete`)
            .method('POST')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .networkFail((err) => {
                console.error('Ошибка удаления данных словаря:', err)
                this.$message.error(err.msg || 'Ошибка удаления данных словаря')
                RequestService.reAjaxFun(() => {
                    this.deleteDictData(ids, callback)
                })
            }).send()
    },

    // Получение списка данных словаря
    getDictDataByType(dictType) {
        return new Promise((resolve, reject) => {
            RequestService.sendRequest()
                .url(`${getServiceUrl()}/admin/dict/data/type/${dictType}`)
                .method('GET')
                .success((res) => {
                    RequestService.clearRequestTime()
                    if (res.data && res.data.code === 0) {
                        resolve(res.data)
                    } else {
                        reject(new Error(res.data?.msg || 'Ошибка получения списка данных словаря'))
                    }
                })
                .networkFail((err) => {
                    console.error('Ошибка получения списка данных словаря:', err)
                    reject(err)
                }).send()
        })
    }

} 
