import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

/**
 * Получение токена авторизации
 */
function getAuthToken() {
  return localStorage.getItem('token') || '';
}

/**
 * Универсальная обертка API запроса
 * @param {Object} config - Конфигурация запроса
 * @param {string} config.url - URL запроса
 * @param {string} config.method - Метод запроса
 * @param {Object} [config.data] - Данные запроса
 * @param {Object} [config.headers] - Дополнительные заголовки
 * @param {Function} config.callback - Обратный вызов при успехе
 * @param {Function} [config.errorCallback] - Обратный вызов при ошибке
 * @param {string} [config.errorMessage] - Сообщение об ошибке
 * @param {Function} [config.retryFunction] - Функция повтора
 */
function makeApiRequest(config) {
  const token = getAuthToken();
  const { url, method, data, headers, callback, errorCallback, errorMessage, retryFunction } = config;

  const requestBuilder = RequestService.sendRequest()
    .url(url)
    .method(method)
    .header({
      'Authorization': `Bearer ${token}`,
      ...headers
    });

  if (data) {
    requestBuilder.data(data);
  }

  requestBuilder
    .success((res) => {
      RequestService.clearRequestTime();
      callback(res);
    })
    .fail((err) => {
      console.error(errorMessage || 'Операция не удалась', err);
      if (errorCallback) {
        errorCallback(err);
      }
    })
    .networkFail(() => {
      if (retryFunction) {
        RequestService.reAjaxFun(() => {
          retryFunction();
        });
      }
    }).send();
}

/**
 * API управления базой знаний
 */
export default {
  /**
   * Получение списка баз знаний
   * @param {Object} params - Параметры запроса
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  getKnowledgeBaseList(params, callback, errorCallback) {
    const queryParams = new URLSearchParams({
      page: params.page,
      page_size: params.page_size,
      name: params.name || ''
    }).toString();

    makeApiRequest({
      url: `${getServiceUrl()}/datasets?${queryParams}`,
      method: 'GET',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка получения списка баз знаний',
      retryFunction: () => this.getKnowledgeBaseList(params, callback, errorCallback)
    });
  },

  /**
   * Создание базы знаний
   * @param {Object} data - Данные базы знаний
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  createKnowledgeBase(data, callback, errorCallback) {
    console.log('createKnowledgeBase вызван с данными:', data);
    console.log('API URL:', `${getServiceUrl()}/datasets`);

    makeApiRequest({
      url: `${getServiceUrl()}/datasets`,
      method: 'POST',
      data: data,
      headers: { 'Content-Type': 'application/json' },
      callback: (res) => {
        console.log('createKnowledgeBase успешный ответ:', res);
        callback(res);
      },
      errorCallback: (err) => {
        console.error('Ошибка создания базы знаний:', err);
        if (err.response) {
          console.error('Данные ответа ошибки:', err.response.data);
          console.error('Статус ответа ошибки:', err.response.status);
        }
        if (errorCallback) {
          errorCallback(err);
        }
      },
      errorMessage: 'Ошибка создания базы знаний',
      retryFunction: () => this.createKnowledgeBase(data, callback, errorCallback)
    });
  },

  /**
   * Обновление базы знаний
   * @param {string} datasetId - ID базы знаний
   * @param {Object} data - Данные для обновления
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  updateKnowledgeBase(datasetId, data, callback, errorCallback) {
    console.log('updateKnowledgeBase вызван с datasetId:', datasetId, 'data:', data);
    console.log('API URL:', `${getServiceUrl()}/datasets/${datasetId}`);

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}`,
      method: 'PUT',
      data: data,
      headers: { 'Content-Type': 'application/json' },
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка обновления базы знаний',
      retryFunction: () => this.updateKnowledgeBase(datasetId, data, callback, errorCallback)
    });
  },

  /**
   * Удаление отдельной базы знаний
   * @param {string} datasetId - ID базы знаний
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  deleteKnowledgeBase(datasetId, callback, errorCallback) {
    console.log('deleteKnowledgeBase вызван с datasetId:', datasetId);
    console.log('API URL:', `${getServiceUrl()}/datasets/${datasetId}`);

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}`,
      method: 'DELETE',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка удаления базы знаний',
      retryFunction: () => this.deleteKnowledgeBase(datasetId, callback, errorCallback)
    });
  },

  /**
   * Пакетное удаление баз знаний
   * @param {string|Array} ids - Строка или массив ID баз знаний
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  deleteKnowledgeBases(ids, callback, errorCallback) {
    // Гарантия правильного формата строки ids
    const idsStr = Array.isArray(ids) ? ids.join(',') : ids;

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/batch?ids=${idsStr}`,
      method: 'DELETE',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка пакетного удаления баз знаний',
      retryFunction: () => this.deleteKnowledgeBases(ids, callback, errorCallback)
    });
  },

  /**
   * Получение списка документов
   * @param {string} datasetId - ID базы знаний
   * @param {Object} params - Параметры запроса
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  getDocumentList(datasetId, params, callback, errorCallback) {
    const queryParams = new URLSearchParams({
      page: params.page,
      page_size: params.page_size,
      name: params.name || ''
    }).toString();

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/documents?${queryParams}`,
      method: 'GET',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка получения списка документов',
      retryFunction: () => this.getDocumentList(datasetId, params, callback, errorCallback)
    });
  },

  /**
   * Загрузка документа
   * @param {string} datasetId - ID базы знаний
   * @param {Object} formData - Данные формы
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  uploadDocument(datasetId, formData, callback, errorCallback) {
    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/documents`,
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка загрузки документа',
      retryFunction: () => this.uploadDocument(datasetId, formData, callback, errorCallback)
    });
  },

  /**
   * Парсинг документа
   * @param {string} datasetId - ID базы знаний
   * @param {string} documentId - ID документа
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  parseDocument(datasetId, documentId, callback, errorCallback) {
    const requestBody = {
      document_ids: [documentId]
    };

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/chunks`,
      method: 'POST',
      data: requestBody,
      headers: { 'Content-Type': 'application/json' },
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка парсинга документа',
      retryFunction: () => this.parseDocument(datasetId, documentId, callback, errorCallback)
    });
  },

  /**
   * Удаление документа
   * @param {string} datasetId - ID базы знаний
   * @param {string} documentId - ID документа
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  deleteDocument(datasetId, documentId, callback, errorCallback) {
    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/documents/${documentId}`,
      method: 'DELETE',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка удаления документа',
      retryFunction: () => this.deleteDocument(datasetId, documentId, callback, errorCallback)
    });
  },

  /**
   * Получение списка фрагментов документа
   * @param {string} datasetId - ID базы знаний
   * @param {string} documentId - ID документа
   * @param {Object} params - Параметры запроса
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  listChunks(datasetId, documentId, params, callback, errorCallback) {
    let queryParams = new URLSearchParams({
      page: params.page || 1,
      page_size: params.page_size || 10
    }).toString();

    // Добавление параметров поиска по ключевым словам
    if (params.keywords) {
      queryParams += `&keywords=${encodeURIComponent(params.keywords)}`;
    }

    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/documents/${documentId}/chunks?${queryParams}`,
      method: 'GET',
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка получения списка фрагментов',
      retryFunction: () => this.listChunks(datasetId, documentId, params, callback, errorCallback)
    });
  },

  /**
   * Тестовый запрос
   * @param {string} datasetId - ID базы знаний
   * @param {Object} data - Параметры тестового запроса
   * @param {Function} callback - Функция обратного вызова
   * @param {Function} errorCallback - Обратный вызов при ошибке
   */
  retrievalTest(datasetId, data, callback, errorCallback) {
    makeApiRequest({
      url: `${getServiceUrl()}/datasets/${datasetId}/retrieval-test`,
      method: 'POST',
      data: data,
      headers: { 'Content-Type': 'application/json' },
      callback: callback,
      errorCallback: errorCallback,
      errorMessage: 'Ошибка тестового запроса',
      retryFunction: () => this.retrievalTest(datasetId, data, callback, errorCallback)
    });
  }

};
