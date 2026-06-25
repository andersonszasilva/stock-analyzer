package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.MonetaryUnit
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Component
class IndicatorCalculationEngine {

    private val mc = MathContext(10, RoundingMode.HALF_UP)
    private val scale = 4
    private val zero = BigDecimal.ZERO

    fun calculate(stmts: List<FinancialStatement>, req: AnalysisRequest): FinancialIndicators {
        require(stmts.isNotEmpty()) { "Ao menos uma DRE é necessária para calcular indicadores" }

        val latest = stmts.last()
        val avgFcf = stmts.map { it.freeCashFlow }.average()

        val revenueGrowth = if (stmts.size >= 2) growth(stmts[stmts.size - 2].netRevenue, latest.netRevenue) else zero
        val netIncomeGrowth = if (stmts.size >= 2) growth(stmts[stmts.size - 2].netIncome, latest.netIncome) else zero

        val shares = req.sharesOutstanding
        val unitMultiplier = when (latest.monetaryUnit) {
            MonetaryUnit.UNITS -> BigDecimal.ONE
            MonetaryUnit.THOUSANDS -> BigDecimal("1000")
            MonetaryUnit.MILLIONS -> BigDecimal("1000000")
            MonetaryUnit.BILLIONS -> BigDecimal("1000000000")
        }

        val (eps, bvps, dcfVal) = if (shares != null && shares > 0) {
            val sharesBd = BigDecimal(shares)
            val e = latest.netIncome.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            val b = latest.equity.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            val avgFcfPerShare = avgFcf.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            Triple(e, b, dcf(avgFcfPerShare, req.discountRate, req.dcfProjectionYears))
        } else {
            Triple(null as BigDecimal?, null as BigDecimal?, dcf(avgFcf, req.discountRate, req.dcfProjectionYears))
        }

        return FinancialIndicators(
            grossMargin = margin(latest.grossProfit, latest.netRevenue),
            ebitdaMargin = margin(latest.ebitda, latest.netRevenue),
            netMargin = margin(latest.netIncome, latest.netRevenue),
            fcfMargin = margin(latest.freeCashFlow, latest.netRevenue),
            roe = roe(latest),
            roic = roic(latest, req.taxRate),
            roa = roa(latest),
            debtToEbitda = debtToEbitda(latest),
            debtToEquity = debtToEquity(latest),
            revenueGrowthYoY = revenueGrowth,
            netIncomeGrowthYoY = netIncomeGrowth,
            fcfConversion = fcfConversion(latest),
            grahamPrice = if (eps != null && bvps != null) grahamPrice(eps, bvps) else zero,
            dcfFairValue = dcfVal,
            eps = eps,
            bvps = bvps
        )
    }

    private fun margin(numerator: BigDecimal, revenue: BigDecimal): BigDecimal =
        if (revenue == zero) zero
        else numerator.divide(revenue, scale, RoundingMode.HALF_UP)

    private fun roe(s: FinancialStatement): BigDecimal =
        if (s.equity == zero) zero
        else s.netIncome.divide(s.equity, scale, RoundingMode.HALF_UP)

    private fun roic(s: FinancialStatement, taxRate: BigDecimal): BigDecimal {
        val investedCapital = s.netDebt + s.equity
        if (investedCapital == zero) return zero
        val nopat = s.ebit.multiply(BigDecimal.ONE.subtract(taxRate), mc)
        return nopat.divide(investedCapital, scale, RoundingMode.HALF_UP)
    }

    private fun roa(s: FinancialStatement): BigDecimal =
        if (s.totalAssets == zero) zero
        else s.netIncome.divide(s.totalAssets, scale, RoundingMode.HALF_UP)

    private fun debtToEbitda(s: FinancialStatement): BigDecimal =
        if (s.ebitda == zero) zero
        else s.netDebt.divide(s.ebitda, scale, RoundingMode.HALF_UP)

    private fun debtToEquity(s: FinancialStatement): BigDecimal =
        if (s.equity == zero) zero
        else s.totalDebt.divide(s.equity, scale, RoundingMode.HALF_UP)

    private fun fcfConversion(s: FinancialStatement): BigDecimal =
        if (s.netIncome == zero) zero
        else s.freeCashFlow.divide(s.netIncome, scale, RoundingMode.HALF_UP)

    private fun growth(previous: BigDecimal, current: BigDecimal): BigDecimal =
        if (previous == zero) zero
        else current.subtract(previous).divide(previous.abs(), scale, RoundingMode.HALF_UP)

    private fun grahamPrice(eps: BigDecimal, bvps: BigDecimal): BigDecimal {
        val product = BigDecimal("22.5").multiply(eps, mc).multiply(bvps, mc)
        if (product <= zero) return zero
        return product.sqrt(mc).setScale(scale, RoundingMode.HALF_UP)
    }

    private fun dcf(avgFcfPerShare: BigDecimal, discountRate: BigDecimal, years: Int): BigDecimal {
        if (avgFcfPerShare <= zero || discountRate <= zero) return zero
        return avgFcfPerShare.divide(discountRate, scale, RoundingMode.HALF_UP)
    }

    private fun List<BigDecimal>.average(): BigDecimal {
        if (isEmpty()) return zero
        val sum = fold(zero) { acc, v -> acc.add(v) }
        return sum.divide(BigDecimal(size), scale, RoundingMode.HALF_UP)
    }
}
