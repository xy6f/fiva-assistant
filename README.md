# FIVA Assistant — 智能财税 AI 助手

> 面向中小企业的 AI 驱动智能财税管理 SaaS 平台。深度集成大语言模型（DeepSeek）与视觉语言模型（通义千问），实现从发票 AI 识别 → 智能查验 → 自动凭证 → 账簿登记 → 报表出具 → 税务测算 → AI 经营分析的全链路智能化闭环。

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.10%2B-3776ab.svg)](https://www.python.org/)
[![LangChain](https://img.shields.io/badge/LangChain-LangGraph-1c3c3c.svg)](https://github.com/langchain-ai/langchain)
[![Vue](https://img.shields.io/badge/Vue-3.4-42b883.svg)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479a1.svg)](https://www.mysql.com/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.13.0-blue.svg)](https://github.com/langchain4j/langchain4j)

让没有专业财务团队的中小企业，也能享受 AI 级别的财税智能服务——从「手工记账」到「AI 驱动」，一站式覆盖记账、发票、报表、税务、预算、工资、资产全链路。

## ✨ 核心特性

- 🤖 **AI 智能助手**：对话 Agent 与税负 AI 由独立的 Python LangChain 服务（`ai-agent/`，LangGraph ReAct 工具循环）接入 DeepSeek 大模型，支持 SSE 流式对话、自然语言查账（AI 可自主查询真实账目数据）与税务筹划建议。
- 🧾 **发票 AI 识别（OCR）**：接入通义千问视觉模型，上传发票图片自动识别票面信息、智能查验、一键生成凭证。
- 📒 **智能记账**：会计科目模板、凭证模板、凭证自动生成、凭证编号防冲突（序列表）。
- 📊 **财务报告**：总账、明细账、试算平衡表、资产负债表、利润表自动出具，支持 Excel / PDF 导出。
- 🧮 **税务测算**：内置增值税、附加税、企业所得税策略引擎，税率规则可配置入库，随政策灵活调整。
- 📈 **经营驾驶舱**：财务仪表盘，收入/支出/利润走势、发票结构、分类排行、预算预警、风险提醒。
- 💰 **全模块覆盖**：预算管理、工资管理、固定资产、银行流水、往来联系人、费用报销、期末结账。
- 🔐 **安全与权限**：Spring Security + JWT 认证，四种角色（ADMIN / MANAGER / ACCOUNTANT / VIEWER），操作审计日志。
- 📡 **可观测性**：AI 调用日志与 Token 统计、审计日志切面（AOP）。

## 🛠 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 4.1 · Spring Security · Spring MVC · MyBatis 4 · JWT (jjwt) |
| AI（对话/税负） | Python 3.10+ · FastAPI · LangChain（langchain-openai）· LangGraph（ReAct） · DeepSeek |
| AI（发票 OCR） | LangChain4j 1.13 · 通义千问视觉模型 |
| 数据 | MySQL 8 · 阿里云 OSS（文件存储） |
| 导出 | Apache POI（Excel）· OpenPDF（PDF） |
| 前端 | Vue 3 · Vite 6 · Element Plus 2.8 · ECharts 5 · Vue Router 4 · Axios · marked · DOMPurify |
| 工程 | Maven Wrapper · Lombok · GitHub Actions CI |

## 📁 项目结构

```
fiva-assistant/
├── .github/workflows/ci.yml   # CI：后端编译 + 单元测试 + 前端构建
├── ai-agent/                  # Python LangChain AI 服务（对话 Agent + 税负 AI，端口 8001）
│   ├── app/agent/             #   LangGraph ReAct Agent、8 个只读账务工具、会话记忆、系统提示词
│   ├── app/routers/           #   /api/chat/* 与 /api/tax AI 端点（契约与原 Java 版一致）
│   ├── app/services/          #   编排：落库、AI 调用埋点、错误脱敏
│   ├── tests/                 #   纯逻辑单元测试（pytest）
│   └── .env                   #   本地配置（不入库，参考 .env.example）
├── docs/                      # 文档（商业计划书、演示脚本、优化计划等）
├── frontend/                  # Vue 3 + Vite 前端
│   ├── src/api/               #   接口封装
│   ├── src/router/            #   路由
│   └── src/views/             #   页面（29 个功能页面）
├── sql/                       # 数据库脚本
│   ├── init_data.sql          #   完整初始化：建库 + 建表 + 演示数据
│   ├── fiva_assistant_full.sql#   全量导出（含数据）
│   └── V2__optimization.sql   #   优化新增表（科目模板/税率规则/AI 日志/审计日志/序列）
├── src/                       # Spring Boot 后端（业务模块 + 发票 OCR 视觉模型）
│   ├── main/java/com/xy6f/fiva_assistant/
│   │   ├── controller/        #   REST API 控制器
│   │   ├── service/           #   业务服务模块
│   │   ├── mapper/            #   MyBatis 数据访问层
│   │   ├── model/             #   实体模型
│   │   ├── config/            #   安全 / OSS / CORS / AI（视觉模型）配置
│   │   ├── aspect/            #   审计日志切面
│   │   └── util/              #   Excel / PDF / JWT 工具
│   └── resources/             #   application.properties
├── tests/                     # 端到端（E2E）测试脚本
├── pom.xml                    # Maven 配置
├── mvnw / mvnw.cmd            # Maven Wrapper
└── ...
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Python 3.10+（AI 对话/税负服务）
- Maven 3.9+（或直接使用项目自带的 `mvnw`）
- Node.js 20+、npm
- MySQL 8+

### 1. 初始化数据库

使用 `sql/init_data.sql` 一键完成建库、建表并写入演示数据（默认数据库名 `fiva_assistant`）：

```bash
mysql -u root -p < sql/init_data.sql
```

然后执行优化脚本（新增科目模板、税率规则、AI 调用日志、审计日志、凭证序列等表）：

```bash
mysql -u root -p fiva_assistant < sql/V2__optimization.sql
```

> 也可以直接导入 `sql/fiva_assistant_full.sql`（完整导出，含数据）。

### 2. 配置后端

编辑 `src/main/resources/application.properties`，将 `changeme` 替换为真实配置：

| 配置项 | 说明 |
|---|---|
| `spring.datasource.url / username / password` | MySQL 连接（默认 `localhost:3306/fiva_assistant`，账号 `root`） |
| `ai.vision.api-key` | 通义千问 DashScope API Key（发票 OCR） |
| `aliyun.oss.access-key-id / secret` | 阿里云 OSS 凭证（发票/文件存储） |
| `jwt.secret` | JWT 签名密钥，请设置为足够长的随机字符串 |

> 敏感配置约定存放在 `application-dev.properties`（已被 `.gitignore` 忽略，不会误提交）。

### 3. 配置并启动 AI 服务（Python LangChain）

对话 Agent（`/api/chat/*`）与税负 AI（`/api/tax/suggestion*`、`/api/tax/analysis`）由 `ai-agent/` 提供：

```bash
cd ai-agent
py -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt   # Windows；Linux/macOS 为 .venv/bin/python
copy .env.example .env                                     # 填入 DB / DeepSeek / jwt.secret（与后端保持一致）
.venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8001
```

AI 服务默认运行在 `http://localhost:8001`，健康检查 `/health`。详细说明见 [ai-agent/README.md](ai-agent/README.md)。

### 4. 启动 Java 后端

```bash
./mvnw spring-boot:run        # Linux / macOS
mvnw.cmd spring-boot:run      # Windows
```

后端默认运行在 `http://localhost:8080`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开浏览器访问 `http://localhost:5173`。

### 默认演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员（ADMIN） |

> 演示环境默认账号，正式使用请立即修改密码。

## 🧪 测试

- **后端单元测试**（不依赖数据库）：

```bash
./mvnw -B test "-Dtest=OcrResultParserTest"
```

- **AI 服务单元测试**（不依赖数据库/大模型）：

```bash
cd ai-agent
.venv\Scripts\python -m pytest tests/
```

- **端到端（E2E）测试脚本**：位于 `tests/`，用于验证登录、仪表盘、发票、税务、AI 聊天等完整流程。

- **CI**：`.github/workflows/ci.yml` 在每次 push / PR 时自动执行后端编译 + 单元测试 + 前端构建。

## 📄 相关文档

- [商业计划书](docs/商业计划书.txt) — 项目定位、市场分析、商业模式与融资计划
- [演示脚本](docs/演示脚本.txt) — 约 4-5 分钟完整演示脚本
- [竞赛录屏脚本](docs/竞赛录屏脚本.md) — 竞赛录屏讲解脚本
- [优化计划](docs/OPTIMIZATION_PLAN.md) — 已知问题与后续优化路线图

## 📄 许可证

本项目目前未指定开源许可证，保留所有权利。如需商用或二次开发，请联系作者。