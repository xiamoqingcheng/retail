package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Goods;
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

    @Select("SELECT COUNT(*) FROM sys_goods WHERE status = 1")
    long countActiveGoods();

    @Select("SELECT id FROM sys_goods WHERE status = 1 ORDER BY id LIMIT #{limit} OFFSET #{offset}")
    List<Long> selectGoodsIdsByOffset(@Param("limit") int limit, @Param("offset") int offset);

    @Update("""
        <script>
        UPDATE sys_goods SET shelf_id = #{shelfId}
        WHERE id IN
        <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>
        </script>
        """)
    int batchUpdateShelfId(@Param("ids") List<Long> goodsIds, @Param("shelfId") String shelfId);
}
