package br.com.stockanalyzer.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class FinancialStatement(
    val id: UUID,
    val assetId: UUID,
    val year: Int,
    val period: StatementPeriod,
    val netRevenue: BigDecimal,
    val grossProfit: BigDecimal,
    val ebitda: BigDecimal,
    val ebit: BigDecimal,
    val netIncome: BigDecimal,
    val operatingCashFlow: BigDecimal,
    val freeCashFlow: BigDecimal,
    val totalDebt: BigDecimal,
    val netDebt: BigDecimal,
    val equity: BigDecimal,
    val totalAssets: BigDecimal,
    val createdAt: LocalDateTime
)
