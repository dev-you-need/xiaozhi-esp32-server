package xiaozhi.common.service;

import java.io.Serializable;
import java.util.Collection;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

/**
 * Базовый интерфейс сервиса, который должны наследовать все интерфейсы сервисов
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
public interface BaseService<T> {
    Class<T> currentModelClass();

    /**
     * <p>
     * Вставить запись (выбор полей, стратегическая вставка)
     * </p>
     *
     * @param entity объект сущности
     */
    boolean insert(T entity);

    /**
     * <p>
     * Пакетная вставка, данный метод не поддерживает Oracle, SQL Server
     * </p>
     *
     * @param entityList коллекция объектов сущности
     */
    boolean insertBatch(Collection<T> entityList);

    /**
     * <p>
     * Пакетная вставка, данный метод не поддерживает Oracle, SQL Server
     * </p>
     *
     * @param entityList коллекция объектов сущности
     * @param batchSize  размер пакета вставки
     */
    boolean insertBatch(Collection<T> entityList, int batchSize);

    /**
     * <p>
     * Изменение по ID
     * </p>
     *
     * @param entity объект сущности
     */
    boolean updateById(T entity);

    /**
     * <p>
     * Обновление записей по условию whereEntity
     * </p>
     *
     * @param entity        объект сущности
     * @param updateWrapper класс обёртки операций
     *                      {@link com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper}
     */
    boolean update(T entity, Wrapper<T> updateWrapper);

    /**
     * <p>
     * Пакетное обновление по ID
     * </p>
     *
     * @param entityList коллекция объектов сущности
     */
    boolean updateBatchById(Collection<T> entityList);

    /**
     * <p>
     * Пакетное обновление по ID
     * </p>
     *
     * @param entityList коллекция объектов сущности
     * @param batchSize  размер пакета обновления
     */
    boolean updateBatchById(Collection<T> entityList, int batchSize);

    /**
     * <p>
     * Запрос по ID
     * </p>
     *
     * @param id Первичный ключID
     */
    T selectById(Serializable id);

    /**
     * <p>
     * Удаление по ID
     * </p>
     *
     * @param id Первичный ключID
     */
    boolean deleteById(Serializable id);

    /**
     * <p>
     * Удаление (пакетное по ID)
     * </p>
     *
     * @param idList список первичных ключей
     */
    boolean deleteBatchIds(Collection<? extends Serializable> idList);
}