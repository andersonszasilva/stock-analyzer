package br.com.stockanalyzer.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class AssetIndicators(
    val id: UUID,
    val assetId: UUID,
    val grossMargin: BigDecimal,
    val ebitdaMargin: BigDecimal,
    val netMargin: BigDecimal,
    val fcfMargin: BigDecimal,
    val roe: BigDecimal,
    val roic: BigDecimal,
    val roa: BigDecimal,
    val debtToEbitda: BigDecimal,
    val debtToEquity: BigDecimal,
    val revenueGrowthYoY: BigDecimal,
    val netIncomeGrowthYoY: BigDecimal,
    val fcfConversion: BigDecimal,
    val grahamPrice: BigDecimal,
    val dcfFairValue: BigDecimal,
    val eps: BigDecimal?,
    val bvps: BigDecimal?,
    val recommendedPrice: BigDecimal?,
    val calculatedAt: LocalDateTime,
)
