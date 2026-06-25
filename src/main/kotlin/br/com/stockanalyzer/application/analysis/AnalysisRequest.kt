package br.com.stockanalyzer.application.analysis

import java.math.BigDecimal
import java.util.UUID

data class AnalysisRequest(
    val statementIds: List<UUID>,
    val discountRate: BigDecimal = BigDecimal("0.10"),
    val taxRate: BigDecimal = BigDecimal("0.34"),
    val dcfProjectionYears: Int = 5,
    val sharesOutstanding: Long? = null
)
