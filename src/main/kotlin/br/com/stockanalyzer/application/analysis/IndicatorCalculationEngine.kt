package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.MonetaryUnit
import br.com.stockanalyzer.domain.model.StatementPeriod
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

        val sorted = stmts.sortedWith(compareBy({ it.year }, { it.period.ordinal }))

        val quarters = sorted.filter { it.period != StatementPeriod.ANNUAL }
        val annuals  = sorted.filter { it.period == StatementPeriod.ANNUAL }

        // LTM: últimos 4 trimestres quando disponíveis; fallback para o annual mais recente
        val ltmBase: List<FinancialStatement>
        val latestBalance: FinancialStatement
        if (quarters.size >= 4) {
            ltmBase       = quarters.takeLast(4)
            latestBalance = quarters.last()
        } else {
            ltmBase       = listOfNotNull(annuals.lastOrNull() ?: sorted.last())
            latestBalance = sorted.last()
        }

        fun sumLtm(f: (FinancialStatement) -> BigDecimal) = ltmBase.fold(zero) { acc, s -> acc.add(f(s)) }

        val ltmRevenue    = sumLtm { it.netRevenue }
        val ltmGross      = sumLtm { it.grossProfit }
        val ltmEbitda     = sumLtm { it.ebitda }
        val ltmEbit       = sumLtm { it.ebit }
        val ltmNetIncome  = sumLtm { it.netIncome }
        val ltmFcl        = sumLtm { it.freeCashFlow }

        // Margens (fluxos LTM / receita LTM)
        val grossMargin  = div(ltmGross,     ltmRevenue)
        val ebitdaMargin = div(ltmEbitda,    ltmRevenue)
        val netMargin    = div(ltmNetIncome, ltmRevenue)
        val fcfMargin    = div(ltmFcl,       ltmRevenue)

        // Rentabilidade (fluxo LTM / estoque do balanço mais recente)
        val investedCapital = latestBalance.netDebt.add(latestBalance.equity)
        val nopat           = ltmEbit.multiply(BigDecimal.ONE.subtract(req.taxRate), mc)

        val roe  = div(ltmNetIncome, latestBalance.equity)
        val roic = if (investedCapital == zero) zero
                   else nopat.divide(investedCapital, scale, RoundingMode.HALF_UP)
        val roa  = div(ltmNetIncome, latestBalance.totalAssets)

        // Endividamento (dívida do balanço / EBITDA LTM)
        val debtToEbitda = div(latestBalance.netDebt,   ltmEbitda)
        val debtToEquity = div(latestBalance.totalDebt, latestBalance.equity)

        // Conversão FCL (LTM)
        val fcfConversion = div(ltmFcl, ltmNetIncome)

        // Crescimento YoY — compara apenas demonstrações anuais consecutivas
        val revenueGrowth: BigDecimal
        val netIncomeGrowth: BigDecimal
        if (annuals.size >= 2) {
            val prev = annuals[annuals.size - 2]
            val curr = annuals.last()
            revenueGrowth   = growth(prev.netRevenue, curr.netRevenue)
            netIncomeGrowth = growth(prev.netIncome,  curr.netIncome)
        } else {
            revenueGrowth   = zero
            netIncomeGrowth = zero
        }

        val shares = req.sharesOutstanding
        val unitMultiplier = when (latestBalance.monetaryUnit) {
            MonetaryUnit.UNITS     -> BigDecimal.ONE
            MonetaryUnit.THOUSANDS -> BigDecimal("1000")
            MonetaryUnit.MILLIONS  -> BigDecimal("1000000")
            MonetaryUnit.BILLIONS  -> BigDecimal("1000000000")
        }

        val (eps, bvps, dcfVal) = if (shares != null && shares > 0) {
            val sharesBd       = BigDecimal(shares)
            val e              = ltmNetIncome.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            val b              = latestBalance.equity.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            val ltmFclPerShare = ltmFcl.multiply(unitMultiplier, mc).divide(sharesBd, scale, RoundingMode.HALF_UP)
            Triple(e, b, dcf(ltmFclPerShare, req.discountRate))
        } else {
            Triple(null as BigDecimal?, null as BigDecimal?, dcf(ltmFcl, req.discountRate))
        }

        val grahamPriceVal = if (eps != null && bvps != null) grahamPrice(eps, bvps) else zero

        val recommendedPrice: BigDecimal? = when {
            grahamPriceVal > zero && dcfVal > zero ->
                grahamPriceVal.add(dcfVal)
                    .divide(BigDecimal(2), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal("0.70"))
                    .setScale(2, RoundingMode.HALF_UP)
            grahamPriceVal > zero ->
                grahamPriceVal.multiply(BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP)
            dcfVal > zero ->
                dcfVal.multiply(BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP)
            else -> null
        }

        return FinancialIndicators(
            grossMargin         = grossMargin,
            ebitdaMargin        = ebitdaMargin,
            netMargin           = netMargin,
            fcfMargin           = fcfMargin,
            roe                 = roe,
            roic                = roic,
            roa                 = roa,
            debtToEbitda        = debtToEbitda,
            debtToEquity        = debtToEquity,
            revenueGrowthYoY    = revenueGrowth,
            netIncomeGrowthYoY  = netIncomeGrowth,
            fcfConversion       = fcfConversion,
            grahamPrice         = grahamPriceVal,
            dcfFairValue        = dcfVal,
            eps                 = eps,
            bvps                = bvps,
            recommendedPrice    = recommendedPrice,
        )
    }

    private fun div(numerator: BigDecimal, denominator: BigDecimal): BigDecimal =
        if (denominator == zero) zero
        else numerator.divide(denominator, scale, RoundingMode.HALF_UP)

    private fun growth(previous: BigDecimal, current: BigDecimal): BigDecimal =
        if (previous == zero) zero
        else current.subtract(previous).divide(previous.abs(), scale, RoundingMode.HALF_UP)

    private fun grahamPrice(eps: BigDecimal, bvps: BigDecimal): BigDecimal {
        val product = BigDecimal("22.5").multiply(eps, mc).multiply(bvps, mc)
        if (product <= zero) return zero
        return product.sqrt(mc).setScale(scale, RoundingMode.HALF_UP)
    }

    private fun dcf(ltmFclPerShare: BigDecimal, discountRate: BigDecimal): BigDecimal {
        if (ltmFclPerShare <= zero || discountRate <= zero) return zero
        return ltmFclPerShare.divide(discountRate, scale, RoundingMode.HALF_UP)
    }
}
