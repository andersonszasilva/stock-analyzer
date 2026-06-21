package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.StatementPeriod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class IndicatorCalculationEngineTest {

    private val engine = IndicatorCalculationEngine()

    private fun statement(
        netRevenue: String = "10000",
        grossProfit: String = "4000",
        ebitda: String = "2500",
        ebit: String = "2000",
        netIncome: String = "1500",
        operatingCashFlow: String = "2200",
        freeCashFlow: String = "1800",
        totalDebt: String = "5000",
        netDebt: String = "3000",
        equity: String = "10000",
        totalAssets: String = "20000"
    ) = FinancialStatement(
        id = UUID.randomUUID(),
        assetId = UUID.randomUUID(),
        year = 2024,
        period = StatementPeriod.ANNUAL,
        netRevenue = BigDecimal(netRevenue),
        grossProfit = BigDecimal(grossProfit),
        ebitda = BigDecimal(ebitda),
        ebit = BigDecimal(ebit),
        netIncome = BigDecimal(netIncome),
        operatingCashFlow = BigDecimal(operatingCashFlow),
        freeCashFlow = BigDecimal(freeCashFlow),
        totalDebt = BigDecimal(totalDebt),
        netDebt = BigDecimal(netDebt),
        equity = BigDecimal(equity),
        totalAssets = BigDecimal(totalAssets),
        createdAt = LocalDateTime.now()
    )

    private val defaultRequest = AnalysisRequest(
        statementIds = emptyList(),
        discountRate = BigDecimal("0.10"),
        taxRate = BigDecimal("0.34"),
        dcfProjectionYears = 5
    )

    @Test
    fun `calcula margens corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)

        // grossMargin = 4000 / 10000 = 0.4000
        assertEquals(BigDecimal("0.4000"), indicators.grossMargin)
        // ebitdaMargin = 2500 / 10000 = 0.2500
        assertEquals(BigDecimal("0.2500"), indicators.ebitdaMargin)
        // netMargin = 1500 / 10000 = 0.1500
        assertEquals(BigDecimal("0.1500"), indicators.netMargin)
        // fcfMargin = 1800 / 10000 = 0.1800
        assertEquals(BigDecimal("0.1800"), indicators.fcfMargin)
    }

    @Test
    fun `calcula ROE corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // ROE = 1500 / 10000 = 0.1500
        assertEquals(BigDecimal("0.1500"), indicators.roe)
    }

    @Test
    fun `calcula ROIC corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // NOPAT = 2000 * (1 - 0.34) = 1320
        // invested capital = netDebt + equity = 3000 + 10000 = 13000
        // ROIC = 1320 / 13000 ≈ 0.1015
        assertEquals(BigDecimal("0.1015"), indicators.roic)
    }

    @Test
    fun `calcula ROA corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // ROA = 1500 / 20000 = 0.0750
        assertEquals(BigDecimal("0.0750"), indicators.roa)
    }

    @Test
    fun `calcula endividamento corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // debtToEbitda = 3000 / 2500 = 1.2000
        assertEquals(BigDecimal("1.2000"), indicators.debtToEbitda)
        // debtToEquity = 5000 / 10000 = 0.5000
        assertEquals(BigDecimal("0.5000"), indicators.debtToEquity)
    }

    @Test
    fun `calcula FCF conversion corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // fcfConversion = 1800 / 1500 = 1.2000
        assertEquals(BigDecimal("1.2000"), indicators.fcfConversion)
    }

    @Test
    fun `calcula crescimento YoY com dois periodos`() {
        val prev = statement(netRevenue = "8000", netIncome = "1000")
        val curr = statement(netRevenue = "10000", netIncome = "1500")

        val indicators = engine.calculate(listOf(prev, curr), defaultRequest)

        // revenueGrowth = (10000 - 8000) / 8000 = 0.2500
        assertEquals(BigDecimal("0.2500"), indicators.revenueGrowthYoY)
        // netIncomeGrowth = (1500 - 1000) / 1000 = 0.5000
        assertEquals(BigDecimal("0.5000"), indicators.netIncomeGrowthYoY)
    }

    @Test
    fun `crescimento zero quando apenas um periodo`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)

        assertEquals(BigDecimal.ZERO, indicators.revenueGrowthYoY)
        assertEquals(BigDecimal.ZERO, indicators.netIncomeGrowthYoY)
    }

    @Test
    fun `calcula DCF como perpetuidade`() {
        val indicators = engine.calculate(listOf(statement(freeCashFlow = "1800")), defaultRequest)
        // dcf = 1800 / 0.10 = 18000.0000
        assertEquals(BigDecimal("18000.0000"), indicators.dcfFairValue)
    }

    @Test
    fun `calcula preco Graham`() {
        // Graham = sqrt(22.5 * LPA * VPA)
        // LPA = netIncome / equity = 1500 / 10000 = 0.15
        // VPA = equity = 10000
        // Graham = sqrt(22.5 * 0.15 * 10000) = sqrt(33750) ≈ 183.7117
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        assertTrue(indicators.grahamPrice > BigDecimal("183") && indicators.grahamPrice < BigDecimal("184"))
    }

    @Test
    fun `retorna zero para margens quando receita e zero`() {
        val stmt = statement(netRevenue = "0")
        val indicators = engine.calculate(listOf(stmt), defaultRequest)

        assertEquals(BigDecimal.ZERO, indicators.grossMargin)
        assertEquals(BigDecimal.ZERO, indicators.ebitdaMargin)
        assertEquals(BigDecimal.ZERO, indicators.netMargin)
    }

    @Test
    fun `lanca excecao quando lista de DREs vazia`() {
        assertThrows(IllegalArgumentException::class.java) {
            engine.calculate(emptyList(), defaultRequest)
        }
    }
}
