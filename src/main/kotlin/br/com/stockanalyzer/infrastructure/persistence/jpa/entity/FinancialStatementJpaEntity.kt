package br.com.stockanalyzer.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "financial_statements",
    uniqueConstraints = [UniqueConstraint(name = "uq_asset_period", columnNames = ["asset_id", "year", "period"])]
)
class FinancialStatementJpaEntity(
    @Id
    val id: UUID,

    @Column(name = "asset_id", nullable = false)
    val assetId: UUID,

    @Column(nullable = false)
    val year: Int,

    @Column(nullable = false, length = 10)
    val period: String,

    @Column(name = "net_revenue", precision = 18, scale = 2)
    val netRevenue: BigDecimal,

    @Column(name = "gross_profit", precision = 18, scale = 2)
    val grossProfit: BigDecimal,

    @Column(precision = 18, scale = 2)
    val ebitda: BigDecimal,

    @Column(precision = 18, scale = 2)
    val ebit: BigDecimal,

    @Column(name = "net_income", precision = 18, scale = 2)
    val netIncome: BigDecimal,

    @Column(name = "op_cash_flow", precision = 18, scale = 2)
    val operatingCashFlow: BigDecimal,

    @Column(name = "free_cash_flow", precision = 18, scale = 2)
    val freeCashFlow: BigDecimal,

    @Column(name = "total_debt", precision = 18, scale = 2)
    val totalDebt: BigDecimal,

    @Column(name = "net_debt", precision = 18, scale = 2)
    val netDebt: BigDecimal,

    @Column(precision = 18, scale = 2)
    val equity: BigDecimal,

    @Column(name = "total_assets", precision = 18, scale = 2)
    val totalAssets: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime
)
