package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.MonetaryUnit
import br.com.stockanalyzer.domain.model.StatementPeriod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class IndicatorCalculationEngineTest {

    private val engine = IndicatorCalculationEngine()

    private fun statement(
        year: Int = 2024,
        period: StatementPeriod = StatementPeriod.ANNUAL,
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
        totalAssets: String = "20000",
        monetaryUnit: MonetaryUnit = MonetaryUnit.UNITS,
    ) = FinancialStatement(
        id = UUID.randomUUID(),
        assetId = UUID.randomUUID(),
        year = year,
        period = period,
        monetaryUnit = monetaryUnit,
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
        createdAt = LocalDateTime.now(),
    )

    private val defaultRequest = AnalysisRequest(
        statementIds = emptyList(),
        discountRate = BigDecimal("0.10"),
        taxRate = BigDecimal("0.34"),
        dcfProjectionYears = 5,
    )

    // ── Com um único ANNUAL (fallback LTM = próprio annual) ──────────────

    @Test
    fun `calcula margens corretamente com annual`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)

        assertEquals(BigDecimal("0.4000"), indicators.grossMargin)
        assertEquals(BigDecimal("0.2500"), indicators.ebitdaMargin)
        assertEquals(BigDecimal("0.1500"), indicators.netMargin)
        assertEquals(BigDecimal("0.1800"), indicators.fcfMargin)
    }

    @Test
    fun `calcula ROE corretamente com annual`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // ROE = 1500 / 10000 = 0.1500
        assertEquals(BigDecimal("0.1500"), indicators.roe)
    }

    @Test
    fun `calcula ROIC corretamente com annual`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // NOPAT = 2000 * (1 - 0.34) = 1320
        // invested capital = netDebt + equity = 3000 + 10000 = 13000
        // ROIC = 1320 / 13000 ≈ 0.1015
        assertEquals(BigDecimal("0.1015"), indicators.roic)
    }

    @Test
    fun `calcula ROA corretamente com annual`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // ROA = 1500 / 20000 = 0.0750
        assertEquals(BigDecimal("0.0750"), indicators.roa)
    }

    @Test
    fun `calcula endividamento corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // debtToEbitda = netDebt / ebitda = 3000 / 2500 = 1.2000
        assertEquals(BigDecimal("1.2000"), indicators.debtToEbitda)
        // debtToEquity = totalDebt / equity = 5000 / 10000 = 0.5000
        assertEquals(BigDecimal("0.5000"), indicators.debtToEquity)
    }

    @Test
    fun `calcula FCF conversion corretamente`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        // fcfConversion = 1800 / 1500 = 1.2000
        assertEquals(BigDecimal("1.2000"), indicators.fcfConversion)
    }

    @Test
    fun `crescimento zero quando apenas um annual`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        assertEquals(BigDecimal.ZERO, indicators.revenueGrowthYoY)
        assertEquals(BigDecimal.ZERO, indicators.netIncomeGrowthYoY)
    }

    @Test
    fun `calcula crescimento YoY comparando anuais consecutivos`() {
        val prev = statement(year = 2023, netRevenue = "8000", netIncome = "1000")
        val curr = statement(year = 2024, netRevenue = "10000", netIncome = "1500")

        val indicators = engine.calculate(listOf(prev, curr), defaultRequest)

        // revenueGrowth = (10000 - 8000) / 8000 = 0.2500
        assertEquals(BigDecimal("0.2500"), indicators.revenueGrowthYoY)
        // netIncomeGrowth = (1500 - 1000) / 1000 = 0.5000
        assertEquals(BigDecimal("0.5000"), indicators.netIncomeGrowthYoY)
    }

    @Test
    fun `crescimento YoY ignora trimestres e usa apenas anuais`() {
        val annual2024 = statement(year = 2024, netRevenue = "8000",  netIncome = "1000")
        val annual2025 = statement(year = 2025, netRevenue = "10000", netIncome = "1500")
        // T1/2026 não deve influenciar o crescimento YoY
        val q1_2026    = statement(year = 2026, period = StatementPeriod.Q1, netRevenue = "3000", netIncome = "400")

        val indicators = engine.calculate(listOf(annual2024, annual2025, q1_2026), defaultRequest)

        // Crescimento deve comparar 2024 vs 2025, não 2025 vs T1/2026
        assertEquals(BigDecimal("0.2500"), indicators.revenueGrowthYoY)
        assertEquals(BigDecimal("0.5000"), indicators.netIncomeGrowthYoY)
    }

    @Test
    fun `calcula DCF como perpetuidade sobre FCL LTM`() {
        val indicators = engine.calculate(listOf(statement(freeCashFlow = "1800")), defaultRequest)
        // dcf = ltmFcl / discountRate = 1800 / 0.10 = 18000.0000
        assertEquals(BigDecimal("18000.0000"), indicators.dcfFairValue)
    }

    @Test
    fun `graham zero sem sharesOutstanding`() {
        val indicators = engine.calculate(listOf(statement()), defaultRequest)
        assertEquals(BigDecimal.ZERO, indicators.grahamPrice)
        assertNull(indicators.eps)
        assertNull(indicators.bvps)
    }

    @Test
    fun `calcula preco Graham por acao com annual`() {
        // monetaryUnit = UNITS, sharesOutstanding = 100
        // EPS  = 1500 / 100 = 15.00
        // BVPS = 10000 / 100 = 100.00
        // Graham = sqrt(22.5 * 15 * 100) = sqrt(33750) ≈ 183.71
        val req = defaultRequest.copy(sharesOutstanding = 100L)
        val indicators = engine.calculate(listOf(statement()), req)

        assertEquals(BigDecimal("15.0000"), indicators.eps)
        assertEquals(BigDecimal("100.0000"), indicators.bvps)
        assertTrue(indicators.grahamPrice > BigDecimal("183") && indicators.grahamPrice < BigDecimal("184"))
    }

    @Test
    fun `retorna zero para margens quando receita e zero`() {
        val indicators = engine.calculate(listOf(statement(netRevenue = "0")), defaultRequest)

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

    // ── Com 4 trimestres (LTM real) ───────────────────────────────────────

    private fun quarter(year: Int, period: StatementPeriod, income: String, revenue: String,
                        fcl: String = "400", ebitda: String = "600", ebit: String = "500",
                        gross: String = "1000",
                        equity: String = "10000", netDebt: String = "3000",
                        totalDebt: String = "5000", totalAssets: String = "20000") =
        statement(year = year, period = period, netRevenue = revenue, grossProfit = gross,
                  ebitda = ebitda, ebit = ebit, netIncome = income, freeCashFlow = fcl,
                  equity = equity, netDebt = netDebt, totalDebt = totalDebt, totalAssets = totalAssets)

    @Test
    fun `usa LTM de 4 trimestres em vez do ultimo trimestre isolado`() {
        // Cada trimestre tem lucro = 400; LTM = 1600
        // Equity do ultimo trimestre = 10000
        // ROE esperado = 1600 / 10000 = 0.1600
        val stmts = listOf(
            quarter(2025, StatementPeriod.Q1, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q2, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q3, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q4, income = "400", revenue = "2500"),
        )
        val indicators = engine.calculate(stmts, defaultRequest)

        // ROE deve usar soma dos 4 trimestres (1600), não apenas o último (400)
        assertEquals(BigDecimal("0.1600"), indicators.roe)
    }

    @Test
    fun `margens usam soma LTM quando ha 4 trimestres`() {
        // Cada trimestre: receita=2500, grossProfit=1000 → margem bruta LTM = 4000/10000 = 0.4000
        val stmts = listOf(
            quarter(2025, StatementPeriod.Q1, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q2, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q3, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q4, income = "400", revenue = "2500"),
        )
        val indicators = engine.calculate(stmts, defaultRequest)

        assertEquals(BigDecimal("0.4000"), indicators.grossMargin)
    }

    @Test
    fun `LTM usa apenas ultimos 4 trimestres quando ha mais de 4`() {
        // T1/2024 tem lucro=100; os outros 4 têm lucro=400
        // LTM deve ser T2/2024+T3/2024+T4/2024+T1/2025 = 1600, não incluir T1/2024
        val stmts = listOf(
            quarter(2024, StatementPeriod.Q1, income = "100", revenue = "2500"),
            quarter(2024, StatementPeriod.Q2, income = "400", revenue = "2500"),
            quarter(2024, StatementPeriod.Q3, income = "400", revenue = "2500"),
            quarter(2024, StatementPeriod.Q4, income = "400", revenue = "2500"),
            quarter(2025, StatementPeriod.Q1, income = "400", revenue = "2500"),
        )
        val indicators = engine.calculate(stmts, defaultRequest)

        // ROE = 1600 / 10000 = 0.1600 (T1/2024 excluído)
        assertEquals(BigDecimal("0.1600"), indicators.roe)
    }

    @Test
    fun `EPS usa lucro LTM nao apenas o ultimo trimestre`() {
        // 4 trimestres, cada um com netIncome=375 (UNITS, 100 ações)
        // EPS LTM = (375*4) / 100 = 15.00
        val stmts = listOf(
            quarter(2025, StatementPeriod.Q1, income = "375", revenue = "2500"),
            quarter(2025, StatementPeriod.Q2, income = "375", revenue = "2500"),
            quarter(2025, StatementPeriod.Q3, income = "375", revenue = "2500"),
            quarter(2025, StatementPeriod.Q4, income = "375", revenue = "2500"),
        )
        val req = defaultRequest.copy(sharesOutstanding = 100L)
        val indicators = engine.calculate(stmts, req)

        assertEquals(BigDecimal("15.0000"), indicators.eps)
    }

    @Test
    fun `debtToEbitda usa divida do balanco mais recente e ebitda LTM`() {
        // EBITDA por trimestre = 600 → LTM = 2400
        // netDebt do último trimestre = 3000
        // debtToEbitda = 3000 / 2400 = 1.2500
        val stmts = listOf(
            quarter(2025, StatementPeriod.Q1, income = "400", revenue = "2500", netDebt = "3000"),
            quarter(2025, StatementPeriod.Q2, income = "400", revenue = "2500", netDebt = "3000"),
            quarter(2025, StatementPeriod.Q3, income = "400", revenue = "2500", netDebt = "3000"),
            quarter(2025, StatementPeriod.Q4, income = "400", revenue = "2500", netDebt = "3000"),
        )
        val indicators = engine.calculate(stmts, defaultRequest)

        assertEquals(BigDecimal("1.2500"), indicators.debtToEbitda)
    }
}
