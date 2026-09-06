import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // Получение списка голосов
    getVoiceList(params, callback) {
        const queryParams = new URLSearchParams({
            ttsModelId: params.ttsModelId,
            page: params.page || 1,
            limit: params.limit || 10,
            name: params.name || ''
        }).toString();

        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ttsVoice?${queryParams}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res.data || []);
            })
            .networkFail((err) => {
                console.error('Ошибка получения списка голосов:', err);
                RequestService.reAjaxFun(() => {
                    this.getVoiceList(params, callback);
                });
            }).send();
    },
    // Сохранение голоса
    saveVoice(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ttsVoice`)
            .method('POST')
            .data(JSON.stringify({
                languages: params.languageType,
                name: params.voiceName,
                remark: params.remark,
                referenceAudio: params.referenceAudio,
                referenceText: params.referenceText,
                sort: params.sort,
                ttsModelId: params.ttsModelId,
                ttsVoice: params.voiceCode,
                voiceDemo: params.voiceDemo || ''
            }))
            .success((res) => {
                callback(res.data);
            })
            .networkFail((err) => {
                console.error('Ошибка сохранения голоса:', err);
                RequestService.reAjaxFun(() => {
                    this.saveVoice(params, callback);
                });
            }).send();
    },
    // Удаление голоса
    deleteVoice(ids, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ttsVoice/delete`)
            .method('POST')
            .data(ids)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res);
            })
            .networkFail((err) => {
                console.error('Ошибка удаления голоса:', err);
                RequestService.reAjaxFun(() => {
                    this.deleteVoice(ids, callback);
                });
            }).send();
    },
    // Изменение голоса
    updateVoice(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/ttsVoice/${params.id}`)
            .method('PUT')
            .data(JSON.stringify({
                languages: params.languageType,
                name: params.voiceName,
                remark: params.remark,
                referenceAudio: params.referenceAudio,
                referenceText: params.referenceText,
                sort: params.sort,
                ttsModelId: params.ttsModelId,
                ttsVoice: params.voiceCode,
                voiceDemo: params.voiceDemo || ''
            }))
            .success((res) => {
                callback(res.data);
            })
            .networkFail((err) => {
                console.error('Ошибка изменения голоса:', err);
                RequestService.reAjaxFun(() => {
                    this.updateVoice(params, callback);
                });
            }).send();
    }
}
