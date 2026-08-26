package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.PeriodClose;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PeriodCloseMapper {

    @Select("SELECT * FROM t_period_close WHERE user_id = #{userId} AND period = #{period}")
    PeriodClose findByUserIdAndPeriod(Long userId, String period);

    @Insert("INSERT INTO t_period_close (user_id, period, close_voucher_id, created_at) " +
            "VALUES (#{userId}, #{period}, #{closeVoucherId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PeriodClose periodClose);

    @Delete("DELETE FROM t_period_close WHERE user_id = #{userId} AND period = #{period}")
    int deleteByUserIdAndPeriod(Long userId, String period);
}
