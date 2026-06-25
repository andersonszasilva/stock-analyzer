package br.com.stockanalyzer.infrastructure.web.form

import br.com.stockanalyzer.domain.model.MonetaryUnit
import br.com.stockanalyzer.domain.model.StatementPeriod
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class FinancialStatementForm(
    @field:NotNull
    @field:Min(2000) @field:Max(2100)
    val year: Int? = null,

    @field:NotNull
    val period: StatementPeriod? = null,

    val monetaryUnit: MonetaryUnit = MonetaryUnit.MILLIONS,

    val netRevenue: BigDecimal = BigDecimal.ZERO,
    val grossProfit: BigDecimal = BigDecimal.ZERO,
    val ebitda: BigDecimal = BigDecimal.ZERO,
    val ebit: BigDecimal = BigDecimal.ZERO,
    val netIncome: BigDecimal = BigDecimal.ZERO,
    val operatingCashFlow: BigDecimal = BigDecimal.ZERO,
    val freeCashFlow: BigDecimal = BigDecimal.ZERO,
    val totalDebt: BigDecimal = BigDecimal.ZERO,
    val netDebt: BigDecimal = BigDecimal.ZERO,
    val equity: BigDecimal = BigDecimal.ZERO,
    val totalAssets: BigDecimal = BigDecimal.ZERO
)
