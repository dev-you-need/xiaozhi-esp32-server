// Утилита конфигурации функций
import Api from "@/apis/api";
import store from "@/store";

class FeatureManager {
    constructor() {
        this.defaultFeatures = {
            voiceprintRecognition: {
                name: 'feature.voiceprintRecognition.name',
                enabled: false,
                description: 'feature.voiceprintRecognition.description'
            },
            voiceClone: {
                name: 'feature.voiceClone.name',
                enabled: false,
                description: 'feature.voiceClone.description'
            },
            knowledgeBase: {
                name: 'feature.knowledgeBase.name',
                enabled: false,
                description: 'feature.knowledgeBase.description'
            },
            mcpAccessPoint: {
                name: 'feature.mcpAccessPoint.name',
                enabled: false,
                description: 'feature.mcpAccessPoint.description'
            },
            vad: {
                name: 'feature.vad.name',
                enabled: false,
                description: 'feature.vad.description'
            },
            asr: {
                name: 'feature.asr.name',
                enabled: false,
                description: 'feature.asr.description'
            },
            addressBook: {
                name: 'feature.addressBook.name',
                enabled: false,
                description: 'feature.addressBook.description'
            }
        };
        this.currentFeatures = { ...this.defaultFeatures }; // Текущая конфигурация в памяти
        this.initialized = false;
        this.initPromise = null;
    }

    /**
     * Ожидание завершения инициализации
     */
    async waitForInitialization() {
        if (!this.initPromise) {
            this.initPromise = this.init();
        }
        await this.initPromise;
        return this.initialized;
    }

    /**
     * Инициализация конфигурации функций
     */
    async init() {
        try {
            // Получение конфигурации из интерфейса pub-config
            const config = await this.getConfigFromPubConfig();
            if (config) {
                this.currentFeatures = { ...config }; // Сохранение в память
                this.initialized = true;
                return;
            }
        } catch (error) {
            console.warn('Ошибка получения конфигурации из интерфейса pub-config:', error);
        }

        // Интерфейс pub-config не удался, использование конфигурации по умолчанию
        this.currentFeatures = { ...this.defaultFeatures }; // Сохранение конфигурации по умолчанию в память
        this.initialized = true;
    }

    /**
     * Обновление кэша конфигурации
     */
    updateConfigCache(config) {
        store.commit('setPubConfig', config);
        localStorage.setItem('pubConfig', JSON.stringify(config));
    }

    /**
     * Получение конфигурации из интерфейса pub-config
     */
    async getConfigFromPubConfig() {
        return new Promise((resolve) => {
            // Прямой вызов интерфейса pub-config для получения конфигурации
            Api.user.getPubConfig((result) => {
                // Проверка структуры возвращаемого результата
                if (result && result.status === 200) {
                    // Проверка наличия поля data
                    if (result.data) {
                        const configCache = result.data.data || {};
                        // Проверка наличия поля code, если есть, то 판단 по code
                        if (result.data.code !== undefined) {
                            if (result.data.code === 0 && result.data.data && result.data.data.systemWebMenu) {
                                try {
                                    let config;
                                    if (typeof result.data.data.systemWebMenu === 'string') {
                                        // Если это строка, необходимо разобрать JSON
                                        config = JSON.parse(result.data.data.systemWebMenu);
                                    } else {
                                        // Если уже объект, использовать напрямую
                                        config = result.data.data.systemWebMenu;
                                    }

                                    // Проверка наличия объекта features в конфигурации
                                    if (config && config.features) {
                                        // Гарантия существования функции knowledgeBase и правильной конфигурации
                                        if (!config.features.knowledgeBase) {
                                            console.warn('В конфигурации отсутствует функция knowledgeBase, объединение с конфигурацией по умолчанию');
                                            config.features = { ...this.defaultFeatures, ...config.features };
                                        }
                                        resolve(config.features);
                                    } else {
                                        console.warn('В конфигурации отсутствует объект features, использование конфигурации по умолчанию');
                                        resolve(this.defaultFeatures);
                                    }
                                    configCache.systemWebMenu = config;
                                } catch (error) {
                                    console.warn('Ошибка обработки конфигурации systemWebMenu:', error);
                                    resolve(null);
                                }
                            } else {
                                console.warn('Интерфейс вернул code не равный 0 или отсутствуют обязательные данные, использование конфигурации по умолчанию');
                                resolve(null);
                            }
                        } else {
                            // Если поля code нет, проверка systemWebMenu напрямую
                            if (result.data && result.data.systemWebMenu) {
                                try {
                                    let config;
                                    if (typeof result.data.systemWebMenu === 'string') {
                                        // Если это строка, необходимо разобрать JSON
                                        config = JSON.parse(result.data.systemWebMenu);
                                    } else {
                                        // Если уже объект, использовать напрямую
                                        config = result.data.systemWebMenu;
                                    }

                                    // Проверка наличия объекта features в конфигурации
                                    if (config && config.features) {
                                        // Гарантия существования функции knowledgeBase и правильной конфигурации
                                        if (!config.features.knowledgeBase) {
                                            console.warn('В конфигурации отсутствует функция knowledgeBase, объединение с конфигурацией по умолчанию');
                                            config.features = { ...this.defaultFeatures, ...config.features };
                                        }
                                        resolve(config.features);
                                    } else {
                                        console.warn('В конфигурации отсутствует объект features, использование конфигурации по умолчанию');
                                        resolve(this.defaultFeatures);
                                    }
                                    configCache.systemWebMenu = config;
                                } catch (error) {
                                    console.warn('Ошибка обработки конфигурации systemWebMenu:', error);
                                    resolve(null);
                                }
                            } else {
                                console.warn('Интерфейс вернул данные без systemWebMenu, использование конфигурации по умолчанию');
                                resolve(null);
                            }
                        }
                        this.updateConfigCache(configCache)
                    } else {
                        console.warn('В данных ответа интерфейса отсутствует поле data, использование конфигурации по умолчанию');
                        resolve(null);
                    }
                } else {
                    console.warn('Ошибка вызова интерфейса pub-config, использование конфигурации по умолчанию');
                    resolve(null);
                }
            });
        });
    }

    /**
     * Получение текущей конфигурации
     */
    getCurrentConfig() {
        // Возврат текущей конфигурации в памяти
        return this.currentFeatures;
    }

    /**
     * Сохранение конфигурации в API бэкенда
     */
    async saveConfig(config) {
        try {
            // Обновление конфигурации в памяти
            this.currentFeatures = { ...config };

            // Асинхронное сохранение в API бэкенда
            this.saveConfigToAPI(config).catch(error => {
                console.warn('Ошибка сохранения конфигурации в API:', error);
            }).finally(() => {
                this.init()
            });

            // Генерация события об изменении конфигурации
            window.dispatchEvent(new CustomEvent('featureConfigChanged', {
                detail: config
            }));
        } catch (error) {
            console.error('Ошибка сохранения конфигурации функций:', error);
        }
    }

    /**
     * Сохранение конфигурации в API бэкенда
     */
    async saveConfigToAPI(config) {
        return new Promise((resolve) => {
            // Использование известного ID (600) для обновления параметра
            Api.admin.updateParam(
                {
                    id: 600,
                    paramCode: 'system-web.menu',
                    paramValue: JSON.stringify({
                        features: config,
                        groups: {
                            featureManagement: ["voiceprintRecognition", "voiceClone", "knowledgeBase", "mcpAccessPoint", "addressBook"],
                            voiceManagement: ["vad", "asr"]
                        }
                    }),
                    valueType: 'json',
                    remark: 'Конфигурация системного функционального меню'
                },
                (updateResult) => {
                    if (updateResult.code === 0) {
                        resolve();
                    } else {
                        // Если обновление не удалось, возможно параметр не существует или другая ошибка, запись без блокировки сохранения в localStorage
                        console.warn('Ошибка обновления параметра:', updateResult.msg);
                        resolve(); // Без блокировки сохранения в localStorage
                    }
                },
                (error) => {
                    console.warn('Ошибка обновления параметра:', error);
                    resolve(); // Без блокировки сохранения в localStorage
                }
            );
        });
    }



    /**
     * Получение всех конфигураций функций
     */
    getAllFeatures() {
        return this.getCurrentConfig();
    }

    /**
     * Получение упрощенного объекта конфигурации (для компонента главной страницы)
     */
    getConfig() {
        const features = this.getAllFeatures();
        return {
            voiceprintRecognition: features.voiceprintRecognition?.enabled || false,
            voiceClone: features.voiceClone?.enabled || false,
            knowledgeBase: features.knowledgeBase?.enabled || false,
            mcpAccessPoint: features.mcpAccessPoint?.enabled || false,
            vad: features.vad?.enabled || false,
            asr: features.asr?.enabled || false,
            addressBook: features.addressBook?.enabled || false
        };
    }

    /**
     * Получение статуса указанной функции
     */
    getFeatureStatus(featureKey) {
        const features = this.getAllFeatures();
        return features[featureKey]?.enabled || false;
    }

    /**
     * Установка статуса функции
     */
    setFeatureStatus(featureKey, enabled) {
        const features = this.getAllFeatures();
        if (features[featureKey]) {
            features[featureKey].enabled = enabled;
            this.saveConfig(features);
            return true;
        }
        return false;
    }

    /**
     * Включение функции
     */
    enableFeature(featureKey) {
        return this.setFeatureStatus(featureKey, true);
    }

    /**
     * Отключение функции
     */
    disableFeature(featureKey) {
        return this.setFeatureStatus(featureKey, false);
    }

    /**
     * Переключение статуса функции
     */
    toggleFeature(featureKey) {
        const currentStatus = this.getFeatureStatus(featureKey);
        return this.setFeatureStatus(featureKey, !currentStatus);
    }

    /**
     * Сброс всех функций до состояния по умолчанию
     */
    resetToDefault() {
        this.saveConfig(this.defaultFeatures);
    }

    /**
     * Пакетное обновление статусов функций
     */
    updateFeatures(featureUpdates) {
        const features = this.getAllFeatures();
        Object.keys(featureUpdates).forEach(featureKey => {
            if (features[featureKey]) {
                features[featureKey].enabled = featureUpdates[featureKey];
            } else if (this.defaultFeatures[featureKey]) {
                features[featureKey] = { ...this.defaultFeatures[featureKey] };
                features[featureKey].enabled = featureUpdates[featureKey];
            }
        });
        this.saveConfig(features);
    }

    /**
     * Получение списка включенных функций
     */
    getEnabledFeatures() {
        const features = this.getAllFeatures();
        return Object.keys(features).filter(key => features[key].enabled);
    }

    /**
     * Проверка включена ли функция
     */
    isFeatureEnabled(featureKey) {
        return this.getFeatureStatus(featureKey);
    }
}

// Создание экземпляра синглтона
const featureManager = new FeatureManager();

export default featureManager;
