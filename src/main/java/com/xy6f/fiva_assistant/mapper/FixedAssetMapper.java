package com.xy6f.fiva_assistant.mapper;

import com.xy6f.fiva_assistant.model.FixedAsset;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FixedAssetMapper {

    @Select("SELECT * FROM t_fixed_asset WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<FixedAsset> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM t_fixed_asset WHERE user_id = #{userId} AND status = #{status} ORDER BY created_at DESC")
    List<FixedAsset> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    @Select("SELECT * FROM t_fixed_asset WHERE id = #{id}")
    FixedAsset findById(Long id);

    @Insert("INSERT INTO t_fixed_asset (user_id, asset_code, asset_name, category, purchase_date, original_value, salvage_value, " +
            "depreciation_method, useful_life_months, monthly_depreciation, accumulated_depreciation, status, " +
            "account_id, depreciation_account_id, expense_account_id, depreciated_months, created_at) " +
            "VALUES (#{userId}, #{assetCode}, #{assetName}, #{category}, #{purchaseDate}, #{originalValue}, #{salvageValue}, " +
            "#{depreciationMethod}, #{usefulLifeMonths}, #{monthlyDepreciation}, #{accumulatedDepreciation}, #{status}, " +
            "#{accountId}, #{depreciationAccountId}, #{expenseAccountId}, #{depreciatedMonths}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FixedAsset fixedAsset);

    @Update("UPDATE t_fixed_asset SET asset_code=#{assetCode}, asset_name=#{assetName}, category=#{category}, " +
            "purchase_date=#{purchaseDate}, original_value=#{originalValue}, salvage_value=#{salvageValue}, " +
            "depreciation_method=#{depreciationMethod}, useful_life_months=#{usefulLifeMonths}, " +
            "monthly_depreciation=#{monthlyDepreciation}, accumulated_depreciation=#{accumulatedDepreciation}, " +
            "status=#{status}, account_id=#{accountId}, depreciation_account_id=#{depreciationAccountId}, " +
            "expense_account_id=#{expenseAccountId}, depreciated_months=#{depreciatedMonths} WHERE id=#{id}")
    int update(FixedAsset fixedAsset);

    @Delete("DELETE FROM t_fixed_asset WHERE id = #{id}")
    int deleteById(Long id);
}
