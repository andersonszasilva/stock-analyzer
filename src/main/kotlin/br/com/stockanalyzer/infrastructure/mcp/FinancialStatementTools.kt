package br.com.stockanalyzer.infrastructure.mcp

import br.com.stockanalyzer.application.analysis.AnalysisRequest
import br.com.stockanalyzer.application.analysis.FinancialIndicators
import br.com.stockanalyzer.application.analysis.FinancialStatementUseCase
import br.com.stockanalyzer.application.analysis.IndicatorCalculationEngine
import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.MonetaryUnit
import br.com.stockanalyzer.domain.model.StatementPeriod
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Component
class FinancialStatementTools(
    private val assetUseCase: AssetUseCase,
    private val statementUseCase: FinancialStatementUseCase,
    private val engine: IndicatorCalculationEngine
) {

    @Tool(description = "Lista todos os ativos cadastrados no sistema com código, nome e setor")
    fun findAllAssets(): List<Asset> = assetUseCase.findAll()

    @Tool(description = "Lista todas as DREs de um ativo pelo seu código de bolsa (ex: ITUB4, PETR4). Retorna as demonstrações ordenadas por ano e período.")
    fun findStatementsByAssetCode(assetCode: String): List<FinancialStatement> {
        val asset = assetUseCase.findByCode(assetCode.uppercase())
            ?: return emptyList()
        return statementUseCase.findByAsset(asset.id)
    }

    @Tool(description = "Lista as DREs de um ativo filtradas por ano (ex: assetCode=ITUB4, year=2024). Útil para analisar um exercício específico.")
    fun findStatementsByAssetCodeAndYear(assetCode: String, year: Int): List<FinancialStatement> {
        val asset = assetUseCase.findByCode(assetCode.uppercase())
            ?: return emptyList()
        return statementUseCase.findByAssetAndYear(asset.id, year)
    }

    @Tool(description = """
        Cadastra ou atualiza uma DRE de um ativo a partir dos dados extraídos de um relatório de resultado.
        Todos os valores financeiros devem estar na unidade indicada por monetaryUnit (padrão: MILLIONS = R$ milhões).
        O período deve ser: Q1, Q2, Q3, Q4 ou ANNUAL.
        O monetaryUnit deve ser: UNITS, THOUSANDS, MILLIONS ou BILLIONS.
        Se já existir uma DRE para o mesmo ativo, ano e período, ela será sobrescrita.
        O ativo deve estar previamente cadastrado no sistema (use findAllAssets para verificar).
    """)
    fun saveFinancialStatement(
        assetCode: String,
        year: Int,
        period: String,
        monetaryUnit: String = "MILLIONS",
        netRevenue: Double,
        grossProfit: Double,
        ebitda: Double,
        ebit: Double,
        netIncome: Double,
        operatingCashFlow: Double,
        freeCashFlow: Double,
        totalDebt: Double,
        netDebt: Double,
        equity: Double,
        totalAssets: Double
    ): FinancialStatement {
        val asset = assetUseCase.findByCode(assetCode.uppercase())
            ?: throw IllegalArgumentException("Ativo '$assetCode' não encontrado. Cadastre o ativo primeiro via interface web.")

        val statementPeriod = runCatching { StatementPeriod.valueOf(period.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Período '$period' inválido. Use: Q1, Q2, Q3, Q4 ou ANNUAL.") }

        val unit = runCatching { MonetaryUnit.valueOf(monetaryUnit.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Unidade '$monetaryUnit' inválida. Use: UNITS, THOUSANDS, MILLIONS ou BILLIONS.") }

        val existing = statementUseCase.findByAssetAndYear(asset.id, year)
            .firstOrNull { it.period == statementPeriod }

        val statement = FinancialStatement(
            id = existing?.id ?: UUID.randomUUID(),
            assetId = asset.id,
            year = year,
            period = statementPeriod,
            monetaryUnit = unit,
            netRevenue = BigDecimal(netRevenue.toString()),
            grossProfit = BigDecimal(grossProfit.toString()),
            ebitda = BigDecimal(ebitda.toString()),
            ebit = BigDecimal(ebit.toString()),
            netIncome = BigDecimal(netIncome.toString()),
            operatingCashFlow = BigDecimal(operatingCashFlow.toString()),
            freeCashFlow = BigDecimal(freeCashFlow.toString()),
            totalDebt = BigDecimal(totalDebt.toString()),
            netDebt = BigDecimal(netDebt.toString()),
            equity = BigDecimal(equity.toString()),
            totalAssets = BigDecimal(totalAssets.toString()),
            createdAt = existing?.createdAt ?: LocalDateTime.now()
        )
        return statementUseCase.save(statement)
    }

    @Tool(description = """
        Calcula indicadores fundamentalistas com base nas DREs cadastradas de um ativo.
        Indicadores calculados: margens (bruta, EBITDA, líquida, FCL), ROE, ROIC, ROA,
        dívida/EBITDA, dívida/PL, crescimento YoY de receita e lucro, conversão FCL,
        preço Graham e valuation DCF (perpetuidade).
        Parâmetros opcionais: discountRate (padrão 10%), taxRate (padrão 34%), dcfProjectionYears (padrão 5).
    """)
    fun calculateIndicators(
        assetCode: String,
        discountRate: Double = 0.10,
        taxRate: Double = 0.34,
        dcfProjectionYears: Int = 5
    ): FinancialIndicators {
        val asset = assetUseCase.findByCode(assetCode.uppercase())
            ?: throw IllegalArgumentException("Ativo '$assetCode' não encontrado")
        val statements = statementUseCase.findByAsset(asset.id)
        require(statements.isNotEmpty()) { "Nenhuma DRE encontrada para '$assetCode'" }

        val request = AnalysisRequest(
            statementIds = statements.map { it.id },
            discountRate = BigDecimal(discountRate.toString()),
            taxRate = BigDecimal(taxRate.toString()),
            dcfProjectionYears = dcfProjectionYears,
            sharesOutstanding = asset.sharesOutstanding
        )
        val indicators = engine.calculate(statements, request)

        assetUseCase.save(asset.copy(
            recommendedPrice = indicators.recommendedPrice,
            lastCalculatedAt = LocalDateTime.now()
        ))

        return indicators
    }
}
