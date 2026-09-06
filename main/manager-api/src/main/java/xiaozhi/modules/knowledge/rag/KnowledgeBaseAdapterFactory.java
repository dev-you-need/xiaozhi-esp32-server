package xiaozhi.modules.knowledge.rag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;

/**
 * Заводской класс адаптера базы знаний
 * Отвечает за создание и управление различными типами API-адаптеров базы знаний
 */
@Slf4j
public class KnowledgeBaseAdapterFactory {

    //Сопоставление типов зарегистрированных адаптеров
    private static final Map<String, Class<? extends KnowledgeBaseAdapter>> adapterRegistry = new HashMap<>();

    //Кэш экземпляра адаптера
    private static final Map<String, KnowledgeBaseAdapter> adapterCache = new ConcurrentHashMap<>();

    //Максимальное количество кэшированных экземпляров для предотвращения утечек памяти (Выпуск 9)
    private static final int MAX_CACHE_SIZE = 50;

    static {
        //Зарегистрировать тип встроенного адаптера
        registerAdapter("ragflow", xiaozhi.modules.knowledge.rag.impl.RAGFlowAdapter.class);
        //Вы можете зарегистрировать больше типов адаптеров здесь
    }

    /**
     * Зарегистрировать новый тип адаптера
     * 
     * @ param adapterType идентификация типа адаптера
     * @ param adapterClass класс адаптера
     */
    public static void registerAdapter(String adapterType, Class<? extends KnowledgeBaseAdapter> adapterClass) {
        if (adapterRegistry.containsKey(adapterType)) {
            log.warn("适配器类型 '{}' 已存在，将被覆盖", adapterType);
        }
        adapterRegistry.put(adapterType, adapterClass);
        log.info("注册适配器类型: {} -> {}", adapterType, adapterClass.getSimpleName());
    }

    /**
     * Получить экземпляр адаптера
     * 
     * @ param adapterType тип адаптера
     * @ param конфигурационные параметры конфигурации
     * @ return adapter instance
     */
    public static KnowledgeBaseAdapter getAdapter(String adapterType, Map<String, Object> config) {
        String cacheKey = buildCacheKey(adapterType, config);

        //Проверьте, существует ли экземпляр в кэше
        if (adapterCache.containsKey(cacheKey)) {
            log.debug("从缓存获取适配器实例: {}", cacheKey);
            return adapterCache.get(cacheKey);
        }

        //Создание нового экземпляра адаптера
        KnowledgeBaseAdapter adapter = createAdapter(adapterType, config);

        //Экземпляр адаптера кэша (с проверкой предела емкости)
        if (adapterCache.size() >= MAX_CACHE_SIZE) {
            log.warn("适配器缓存已达上限 ({})，执行内存保护性清除", MAX_CACHE_SIZE);
            //Простая обработка: прямое опорожнение, LRU рекомендуется для производственных сред
            adapterCache.clear();
        }

        adapterCache.put(cacheKey, adapter);
        log.info("创建并缓存适配器实例: {}", cacheKey);

        return adapter;
    }

    /**
     * Получить экземпляр адаптера (без конфигурации)
     * 
     * @ param adapterType тип адаптера
     * @ return adapter instance
     */
    public static KnowledgeBaseAdapter getAdapter(String adapterType) {
        return getAdapter(adapterType, null);
    }

    /**
     * Получить все зарегистрированные типы адаптеров
     * 
     * @ return сборник типов адаптеров
     */
    public static Set<String> getRegisteredAdapterTypes() {
        return adapterRegistry.keySet();
    }

    /**
     * Проверьте, зарегистрирован ли тип адаптера
     * 
     * @ param adapterType тип адаптера
     * @ return уже зарегистрирован
     */
    public static boolean isAdapterTypeRegistered(String adapterType) {
        return adapterRegistry.containsKey(adapterType);
    }

    /**
     * Очистить кэш адаптера
     */
    public static void clearCache() {
        int cacheSize = adapterCache.size();
        adapterCache.clear();
        log.info("清除适配器缓存，共清除 {} 个实例", cacheSize);
    }

    /**
     * Удалить кэш для определенных типов адаптеров
     * 
     * @ param adapterType тип адаптера
     */
    public static void removeCacheByType(String adapterType) {
        int removedCount = 0;
        for (String cacheKey : adapterCache.keySet()) {
            if (cacheKey.startsWith(adapterType + "@")) {
                adapterCache.remove(cacheKey);
                removedCount++;
            }
        }
        log.info("移除适配器类型 '{}' 的缓存，共移除 {} 个实例", adapterType, removedCount);
    }

    /**
     * Получить информацию о состоянии адаптера на заводе-изготовителе
     * 
     * @ информация О статусе возврата
     */
    public static Map<String, Object> getFactoryStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("registeredAdapterTypes", adapterRegistry.keySet());
        status.put("cachedAdapterCount", adapterCache.size());
        status.put("cacheKeys", adapterCache.keySet());
        return status;
    }

    /**
     * Создать экземпляр адаптера
     * 
     * @ param adapterType тип адаптера
     * @ param конфигурационные параметры конфигурации
     * @ return adapter instance
     */
    private static KnowledgeBaseAdapter createAdapter(String adapterType, Map<String, Object> config) {
        if (!adapterRegistry.containsKey(adapterType)) {
            throw new RenException(ErrorCode.RAG_ADAPTER_TYPE_NOT_SUPPORTED,
                    "不支持的适配器类型: " + adapterType);
        }

        try {
            Class<? extends KnowledgeBaseAdapter> adapterClass = adapterRegistry.get(adapterType);
            KnowledgeBaseAdapter adapter = adapterClass.getDeclaredConstructor().newInstance();

            //Инициализация адаптера
            if (config != null) {
                adapter.initialize(config);

                //Проверка конфигурации
                if (!adapter.validateConfig(config)) {
                    throw new RenException(ErrorCode.RAG_CONFIG_VALIDATION_FAILED,
                            "适配器配置验证失败: " + adapterType);
                }
            }

            log.info("成功创建适配器实例: {}", adapterType);
            return adapter;

        } catch (Exception e) {
            log.error("创建适配器实例失败: {}", adapterType, e);
            throw new RenException(ErrorCode.RAG_ADAPTER_CREATION_FAILED,
                    "创建适配器失败: " + adapterType + ", 错误: " + e.getMessage());
        }
    }

    /**
     * Создать ключ кэша
     * 
     * @ param adapterType тип адаптера
     * @ param конфигурационные параметры конфигурации
     * @ return cache key
     */
    private static String buildCacheKey(String adapterType, Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return adapterType + "@default";
        }

        //Генерируем ключ кэша на основе параметров конфигурации
        StringBuilder keyBuilder = new StringBuilder(adapterType + "@");

        //Использовать настроенное значение хэша как часть ключа кэша
        int configHash = config.hashCode();
        keyBuilder.append(configHash);

        return keyBuilder.toString();
    }
}