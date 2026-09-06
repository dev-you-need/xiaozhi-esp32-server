package xiaozhi.common.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Узел дерева — все, кому нужна реализация узла дерева, должны наследовать этот класс
 * Copyright (c) 人人开源 All rights reserved.
 * Website: https://www.renren.io
 */
@Data
public class TreeNode<T> implements Serializable {

    /**
     * Первичный ключ
     */
    private Long id;
    /**
     * ID родителя
     */
    private Long pid;
    /**
     * Список дочерних узлов
     */
    private List<T> children = new ArrayList<>();

}