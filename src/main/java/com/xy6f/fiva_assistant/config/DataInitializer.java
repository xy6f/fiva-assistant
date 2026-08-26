package com.xy6f.fiva_assistant.config;

import com.xy6f.fiva_assistant.mapper.SubjectTemplateMapper;
import com.xy6f.fiva_assistant.model.Company;
import com.xy6f.fiva_assistant.model.SubjectTemplate;
import com.xy6f.fiva_assistant.model.User;
import com.xy6f.fiva_assistant.mapper.CompanyMapper;
import com.xy6f.fiva_assistant.mapper.UserMapper;
import com.xy6f.fiva_assistant.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final SubjectTemplateMapper subjectTemplateMapper;

    @Override
    public void run(String... args) {
        userService.initDefaultAdmin();
        initSubjectTemplates();

        // 为admin用户创建默认公司
        User admin = userMapper.findByUsername("admin");
        if (admin != null && admin.getCompanyId() == null) {
            Company company = Company.builder()
                    .userId(admin.getId())
                    .name("默认公司")
                    .build();
            companyMapper.insert(company);
            admin.setCompanyId(company.getId());
            userMapper.update(admin);
            log.info("Default company created for admin user");
        }
    }

    /**
     * 初始化会计科目模板（行业标准科目表），仅当模板表为空时播种
     */
    private void initSubjectTemplates() {
        if (subjectTemplateMapper.countAll() > 0) {
            return;
        }

        // 资产类 (ASSET)
        List<SubjectTemplate> templates = List.of(
                template("1001", "库存现金", "ASSET", "DEBIT"),
                template("1002", "银行存款", "ASSET", "DEBIT"),
                template("1122", "应收账款", "ASSET", "DEBIT"),
                template("1221", "其他应收款", "ASSET", "DEBIT"),
                template("1401", "库存商品", "ASSET", "DEBIT"),
                template("1601", "固定资产", "ASSET", "DEBIT"),
                template("1602", "累计折旧", "ASSET", "CREDIT"),

                // 负债类 (LIABILITY)
                template("2202", "应付账款", "LIABILITY", "CREDIT"),
                template("2211", "应付职工薪酬", "LIABILITY", "CREDIT"),
                template("2221", "应交税费", "LIABILITY", "CREDIT"),
                template("2241", "其他应付款", "LIABILITY", "CREDIT"),

                // 权益类 (EQUITY)
                template("3001", "实收资本", "EQUITY", "CREDIT"),
                template("3101", "未分配利润", "EQUITY", "CREDIT"),

                // 收入类 (REVENUE)
                template("5001", "主营业务收入", "REVENUE", "CREDIT"),
                template("5051", "其他业务收入", "REVENUE", "CREDIT"),
                template("5301", "营业外收入", "REVENUE", "CREDIT"),

                // 费用类 (EXPENSE)
                template("6001", "主营业务成本", "EXPENSE", "DEBIT"),
                template("6401", "税金及附加", "EXPENSE", "DEBIT"),
                template("6601", "销售费用", "EXPENSE", "DEBIT"),
                template("6602", "管理费用", "EXPENSE", "DEBIT"),
                template("6603", "财务费用", "EXPENSE", "DEBIT"),
                template("6701", "资产减值损失", "EXPENSE", "DEBIT"),
                template("6711", "营业外支出", "EXPENSE", "DEBIT")
        );

        templates.forEach(subjectTemplateMapper::insert);
        log.info("Initialized {} subject templates", templates.size());
    }

    private SubjectTemplate template(String code, String name, String type, String direction) {
        return SubjectTemplate.builder()
                .code(code)
                .name(name)
                .type(type)
                .balanceDirection(direction)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
