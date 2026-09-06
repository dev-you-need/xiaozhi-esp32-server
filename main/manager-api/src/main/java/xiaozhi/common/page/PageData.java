package xiaozhi.common.page;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Утилита постраничного разбиения
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
@Data
@Schema(description = "分页数据")
public class PageData<T> implements Serializable {
    @Schema(description = "总记录数")
    private int total;

    @Schema(description = "列表数据")
    private List<T> list;

    /**
     * Постраничное разбиение
     *
     * @param list  данные списка
     * @param total общее количество записей
     */
    public PageData(List<T> list, long total) {
        this.list = list;
        this.total = (int) total;
    }
}