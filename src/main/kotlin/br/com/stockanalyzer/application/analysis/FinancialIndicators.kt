package br.com.stockanalyzer.application.analysis

import java.math.BigDecimal

data class FinancialIndicators(
    // Margens
    val grossMargin: BigDecimal,
    val ebitdaMargin: BigDecimal,
    val netMargin: BigDecimal,
    val fcfMargin: BigDecimal,

    // Rentabilidade
    val roe: BigDecimal,
    val roic: BigDecimal,
    val roa: BigDecimal,

    // Endividamento
    val debtToEbitda: BigDecimal,
    val debtToEquity: BigDecimal,

    // Crescimento (BigDecimal.ZERO quando histórico insuficiente)
    val revenueGrowthYoY: BigDecimal,
    val netIncomeGrowthYoY: BigDecimal,

    // Geração de caixa
    val fcfConversion: BigDecimal,

    // Valuation
    val grahamPrice: BigDecimal,
    val dcfFairValue: BigDecimal,
    val eps: BigDecimal? = null,
    val bvps: BigDecimal? = null,
    val recommendedPrice: BigDecimal? = null
)
