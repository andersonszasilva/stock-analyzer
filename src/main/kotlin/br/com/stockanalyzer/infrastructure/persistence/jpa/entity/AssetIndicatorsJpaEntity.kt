package br.com.stockanalyzer.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "asset_indicators")
class AssetIndicatorsJpaEntity(
    @Id
    val id: UUID,

    @Column(name = "asset_id", nullable = false, unique = true)
    val assetId: UUID,

    @Column(name = "gross_margin", nullable = false, precision = 10, scale = 4)
    val grossMargin: BigDecimal,

    @Column(name = "ebitda_margin", nullable = false, precision = 10, scale = 4)
    val ebitdaMargin: BigDecimal,

    @Column(name = "net_margin", nullable = false, precision = 10, scale = 4)
    val netMargin: BigDecimal,

    @Column(name = "fcf_margin", nullable = false, precision = 10, scale = 4)
    val fcfMargin: BigDecimal,

    @Column(name = "roe", nullable = false, precision = 10, scale = 4)
    val roe: BigDecimal,

    @Column(name = "roic", nullable = false, precision = 10, scale = 4)
    val roic: BigDecimal,

    @Column(name = "roa", nullable = false, precision = 10, scale = 4)
    val roa: BigDecimal,

    @Column(name = "debt_to_ebitda", nullable = false, precision = 10, scale = 4)
    val debtToEbitda: BigDecimal,

    @Column(name = "debt_to_equity", nullable = false, precision = 10, scale = 4)
    val debtToEquity: BigDecimal,

    @Column(name = "revenue_growth_yoy", nullable = false, precision = 10, scale = 4)
    val revenueGrowthYoY: BigDecimal,

    @Column(name = "net_income_growth_yoy", nullable = false, precision = 10, scale = 4)
    val netIncomeGrowthYoY: BigDecimal,

    @Column(name = "fcf_conversion", nullable = false, precision = 10, scale = 4)
    val fcfConversion: BigDecimal,

    @Column(name = "graham_price", nullable = false, precision = 12, scale = 4)
    val grahamPrice: BigDecimal,

    @Column(name = "dcf_fair_value", nullable = false, precision = 12, scale = 4)
    val dcfFairValue: BigDecimal,

    @Column(name = "eps", precision = 12, scale = 4)
    val eps: BigDecimal?,

    @Column(name = "bvps", precision = 12, scale = 4)
    val bvps: BigDecimal?,

    @Column(name = "recommended_price", precision = 12, scale = 2)
    val recommendedPrice: BigDecimal?,

    @Column(name = "calculated_at", nullable = false)
    val calculatedAt: LocalDateTime,
)
