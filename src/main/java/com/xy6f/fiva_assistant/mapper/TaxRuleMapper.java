package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.AccountMapping;
import com.xy6f.fiva_assistant.model.TaxRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TaxRuleMapper {

    @Select("SELECT * FROM t_tax_rule")
    List<TaxRule> findAll();

    @Select("SELECT * FROM t_account_mapping ORDER BY priority ASC")
    List<AccountMapping> findAllMappingsOrderByPriority();
}
