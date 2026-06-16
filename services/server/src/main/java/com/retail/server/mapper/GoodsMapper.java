package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Goods;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    @Select("SELECT * FROM sys_goods WHERE id = #{id} FOR UPDATE")
    Goods selectByIdForUpdate(@Param("id") Long id);

    @Update("""
        UPDATE sys_goods
        SET stock = stock - #{quantity}
        WHERE id = #{id}
          AND stock >= #{quantity}
        """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE sys_goods SET shelf_id = #{shelfId} WHERE id = #{goodsId}")
    int updateShelfId(@Param("goodsId") Long goodsId, @Param("shelfId") String shelfId);

    @Select("SELECT COUNT(*) FROM sys_goods WHERE status = 1 AND deleted = 0")
    long countActiveGoods();

    @Select("SELECT id FROM sys_goods WHERE status = 1 AND deleted = 0 ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<Long> selectGoodsIdsByOffset(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT g.id, g.name, g.barcode, g.category_id AS categoryId, c.name AS categoryName,
                   g.price, g.stock, g.shelf_id AS shelfId, g.image_url AS imageUrl
            FROM sys_goods g
            LEFT JOIN sys_goods_category c ON c.id = g.category_id
            WHERE g.status = 1 AND g.deleted = 0
            <if test='categoryId != null and categoryId &gt; 0'>
              AND g.category_id = #{categoryId}
            </if>
            ORDER BY g.update_time DESC, g.id ASC
            </script>
            """)
    List<GoodsSearchDocument> selectActiveSearchDocuments(@Param("categoryId") Long categoryId);

    @Select("""
            <script>
            SELECT g.id, g.name, g.barcode, g.category_id AS categoryId, c.name AS categoryName,
                   g.price, g.stock, g.shelf_id AS shelfId, g.image_url AS imageUrl
            FROM sys_goods g
            LEFT JOIN sys_goods_category c ON c.id = g.category_id
            WHERE g.status = 1 AND g.deleted = 0
              AND g.id IN
              <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<GoodsSearchDocument> selectSearchDocumentsByIds(@Param("ids") List<Long> ids);

    @Update("""
        <script>
        UPDATE sys_goods SET shelf_id = #{shelfId}
        WHERE id IN
        <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>
        </script>
        """)
    int batchUpdateShelfId(@Param("ids") List<Long> goodsIds, @Param("shelfId") String shelfId);
}
