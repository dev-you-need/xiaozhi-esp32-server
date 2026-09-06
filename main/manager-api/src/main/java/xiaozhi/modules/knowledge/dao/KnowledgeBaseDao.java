package xiaozhi.modules.knowledge.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.knowledge.entity.KnowledgeBaseEntity;

/**
 * База знаний База знаний
 */
@Mapper
public interface KnowledgeBaseDao extends BaseDao<KnowledgeBaseEntity> {

    /**
     * Удалить соответствующие записи сопоставления плагинов на основе идентификатора базы знаний
     * 
     * @ param knowledgeBaseId Идентификатор базы знаний
     */
    void deletePluginMappingByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);

    /**
     * Статистика обновления базы знаний Universal Dimension Atom
     * 
     * @ param datasetId Идентификатор набора данных
     * @ param docDelta приращение количества документов
     * @ param chunkDelta chunk delta
     * @ param tokenDelta Увеличение количества токенов
     */
    void updateStatsAfterChange(@Param("datasetId") String datasetId,
            @Param("docDelta") Integer docDelta,
            @Param("chunkDelta") Long chunkDelta,
            @Param("tokenDelta") Long tokenDelta);

}