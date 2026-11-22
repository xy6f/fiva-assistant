"""AI 只读账务查询工具集：8 个工具与 Java FivaDataTools 逐一对齐。

工具名、@Tool 描述、底层 SQL、输出文案格式均复刻自 E 盘 Java 实现，
保证模型看到的行为与迁移前一致。每个工具都带 user_id 数据隔离。
"""
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
from typing import Annotated

from langchain_core.tools import tool
from pydantic import Field

from app import db

ZERO = Decimal("0")

# ---- 与 Java Mapper 相同的 SQL ----
SQL_SUM_BETWEEN = (
    "SELECT COALESCE(SUM(amount), 0) AS total FROM t_voucher "
    "WHERE type = :type AND voucher_date BETWEEN :start AND :end AND user_id = :user_id"
)
SQL_SUM_SINCE = (
    "SELECT COALESCE(SUM(amount), 0) AS total FROM t_voucher "
    "WHERE type = :type AND voucher_date >= :start AND user_id = :user_id"
)
SQL_MONTHLY_INCOME_EXPENSE = (
    "SELECT m, income, expense FROM ("
    "SELECT YEAR(voucher_date) * 100 + MONTH(voucher_date) AS m, "
    "COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE 0 END), 0) AS income, "
    "COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END), 0) AS expense "
    "FROM t_voucher WHERE voucher_date BETWEEN :start AND :end AND user_id = :user_id "
    "GROUP BY YEAR(voucher_date) * 100 + MONTH(voucher_date)) t ORDER BY m"
)
SQL_ACCOUNTS_ENABLED = (
    "SELECT * FROM t_account WHERE user_id = :user_id AND enabled = 1 ORDER BY code ASC"
)
SQL_SUM_GROUP_BY_ACCOUNT = (
    "SELECT e.account_id AS accountId, COALESCE(SUM(e.debit_amount),0) AS debit, "
    "COALESCE(SUM(e.credit_amount),0) AS credit "
    "FROM t_voucher_entry e INNER JOIN t_voucher v ON e.voucher_id = v.id "
    "WHERE v.user_id = :user_id AND v.voucher_date <= :end GROUP BY e.account_id"
)
SQL_ALL_VOUCHERS = (
    "SELECT * FROM t_voucher WHERE user_id = :user_id ORDER BY voucher_date DESC, created_at DESC"
)
SQL_INVOICE_COUNT = "SELECT COUNT(*) AS cnt FROM t_invoice WHERE user_id = :user_id"
SQL_INVOICE_CATEGORY_STATS = (
    "SELECT category, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amt "
    "FROM t_invoice WHERE user_id = :user_id GROUP BY category"
)
SQL_BUDGETS_BY_YEAR = (
    "SELECT * FROM t_budget WHERE user_id = :user_id AND year = :year ORDER BY created_at DESC"
)


def _bd(value) -> Decimal:
    """等价 Java nvl + toBd：None -> 0。"""
    if value is None:
        return ZERO
    if isinstance(value, Decimal):
        return value
    return Decimal(str(value))


def _parse_date(text: str, field: str) -> date:
    try:
        return date.fromisoformat(text.strip())
    except (ValueError, AttributeError):
        raise ValueError(f"{field}格式错误，请使用 yyyy-MM-dd 格式")


def build_fiva_tools(user_id: int) -> list:
    """构造携带当前用户 ID 的工具列表（每次请求构造，复刻 Java 行为）。"""

    @tool("monthlyIncomeExpense", description="查询当前用户本月的收入合计、支出合计和净额（基于凭证数据）")
    def monthly_income_expense() -> str:
        now = date.today()
        month_start = now.replace(day=1)
        income = _bd(db.fetch_one(
            SQL_SUM_BETWEEN, {"type": "INCOME", "start": month_start, "end": now, "user_id": user_id}
        )["total"])
        expense = _bd(db.fetch_one(
            SQL_SUM_BETWEEN, {"type": "EXPENSE", "start": month_start, "end": now, "user_id": user_id}
        )["total"])
        return (f"本月({now.year}年{now.month}月)收入合计: {income} 元, "
                f"支出合计: {expense} 元, 净额: {income - expense} 元")

    @tool("yearlyIncomeExpense", description="查询当前用户今年的收入合计、支出合计和净利润（基于凭证数据）")
    def yearly_income_expense() -> str:
        now = date.today()
        year_start = now.replace(month=1, day=1)
        income = _bd(db.fetch_one(
            SQL_SUM_SINCE, {"type": "INCOME", "start": year_start, "user_id": user_id}
        )["total"])
        expense = _bd(db.fetch_one(
            SQL_SUM_SINCE, {"type": "EXPENSE", "start": year_start, "user_id": user_id}
        )["total"])
        return (f"今年({now.year}年)累计收入: {income} 元, "
                f"累计支出: {expense} 元, 净利润: {income - expense} 元")

    @tool("incomeExpenseBetween", description="查询指定日期区间的收支汇总，日期格式 yyyy-MM-dd")
    def income_expense_between(
        start: Annotated[str, Field(description="开始日期 yyyy-MM-dd")],
        end: Annotated[str, Field(description="结束日期 yyyy-MM-dd")],
    ) -> str:
        s = _parse_date(start, "开始日期")
        e = _parse_date(end, "结束日期")
        if e < s:
            return "结束日期不能早于开始日期"
        income = _bd(db.fetch_one(
            SQL_SUM_BETWEEN, {"type": "INCOME", "start": s, "end": e, "user_id": user_id}
        )["total"])
        expense = _bd(db.fetch_one(
            SQL_SUM_BETWEEN, {"type": "EXPENSE", "start": s, "end": e, "user_id": user_id}
        )["total"])
        return (f"{start} 至 {end} 收入合计: {income} 元, "
                f"支出合计: {expense} 元, 净额: {income - expense} 元")

    @tool("accountBalances", description="查询当前用户各会计科目的期末余额（借方为正、贷方为负），用于分析资产负债结构")
    def account_balances() -> str:
        accounts = db.fetch_all(SQL_ACCOUNTS_ENABLED, {"user_id": user_id})
        account_map = {a["id"]: a for a in accounts}
        sums = db.fetch_all(SQL_SUM_GROUP_BY_ACCOUNT, {"user_id": user_id, "end": date.today()})

        lines: list[str] = []
        for row in sums:
            acc = account_map.get(row.get("accountId"))
            if acc is None:
                continue
            balance = _bd(row.get("debit")) - _bd(row.get("credit"))
            if balance == ZERO:
                continue
            lines.append(f"{acc['code']} {acc['name']} [{acc['type']}]: {balance}")
        if not lines:
            return "暂无科目余额数据"
        return "科目余额(借-贷,单位元):\n" + "\n".join(lines)

    @tool("recentVouchers", description="查询当前用户最近的凭证列表（默认10条，最多50条）")
    def recent_vouchers(limit: Annotated[int, Field(description="返回条数")]) -> str:
        n = min(max(limit, 1), 50)
        vouchers = db.fetch_all(SQL_ALL_VOUCHERS, {"user_id": user_id})
        shown = min(n, len(vouchers))
        lines = [f"最近凭证(共{len(vouchers)}张,显示{shown}张):"]
        for v in vouchers[:n]:
            type_cn = "收入" if v.get("type") == "INCOME" else "支出"
            description = v.get("description") or ""
            lines.append(
                f"{v.get('voucher_no')} | {v.get('voucher_date')} | {type_cn} "
                f"| 金额{v.get('amount')}元 | {description} | 状态:{v.get('status')}"
            )
        return "\n".join(lines).strip()

    @tool("invoiceStats", description="查询当前用户的发票统计：总数、各类别数量与金额、价税合计")
    def invoice_stats() -> str:
        count = (db.fetch_one(SQL_INVOICE_COUNT, {"user_id": user_id}) or {"cnt": 0})["cnt"]
        cats = db.fetch_all(SQL_INVOICE_CATEGORY_STATS, {"user_id": user_id})
        parts = [f"发票总数: {count or 0} 张"]
        total = ZERO
        for row in cats:
            cat = row.get("category")
            amt = _bd(row.get("amt"))
            cnt = int(row.get("cnt") or 0)
            total += amt
            parts.append(f"类别[{cat if cat is not None else '未分类'}]: {cnt} 张, 价税合计 {amt} 元")
        parts.append(f"全部发票价税合计: {total} 元")
        return "\n".join(parts)

    @tool("monthlyTrend", description="查询当前用户今年各月份的收支趋势")
    def monthly_trend() -> str:
        now = date.today()
        year_start = now.replace(month=1, day=1)
        rows = db.fetch_all(
            SQL_MONTHLY_INCOME_EXPENSE, {"start": year_start, "end": now, "user_id": user_id}
        )
        if not rows:
            return "今年暂无收支数据"
        lines = ["今年各月收支(元):"]
        for row in rows:
            income = _bd(row.get("income"))
            expense = _bd(row.get("expense"))
            lines.append(f"{int(row['m'])}月: 收入{income}, 支出{expense}, 净额{income - expense}")
        return "\n".join(lines)

    @tool("budgetUsage", description="查询当前用户本年度预算执行情况")
    def budget_usage() -> str:
        budgets = db.fetch_all(SQL_BUDGETS_BY_YEAR, {"user_id": user_id, "year": date.today().year})
        if not budgets:
            return "今年暂无预算数据"
        lines = ["预算执行情况:"]
        for b in budgets[:30]:
            used = _bd(b.get("used_amount"))
            budget_amount = _bd(b.get("budget_amount"))
            if budget_amount > ZERO:
                pct_value = (used * 100 / budget_amount).quantize(Decimal("0.1"), rounding=ROUND_HALF_UP)
                pct = f"{pct_value}%"
            else:
                pct = "N/A"
            category = b.get("category") if b.get("category") is not None else ""
            lines.append(
                f"{b.get('year')}年{b.get('month')}月 {b.get('department')}[{category}]: "
                f"预算{budget_amount}元, 已用{used}元({pct})"
            )
        return "\n".join(lines)

    return [
        monthly_income_expense,
        yearly_income_expense,
        income_expense_between,
        account_balances,
        recent_vouchers,
        invoice_stats,
        monthly_trend,
        budget_usage,
    ]
