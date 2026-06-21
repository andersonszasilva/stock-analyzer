package br.com.stockanalyzer.infrastructure.mcp

import br.com.stockanalyzer.application.analysis.AnalysisRequest
import br.com.stockanalyzer.application.analysis.FinancialIndicators
import br.com.stockanalyzer.application.analysis.FinancialStatementUseCase
import br.com.stockanalyzer.application.analysis.IndicatorCalculationEngine
import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.FinancialStatement
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component
import java.math.BigDecimal

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
            dcfProjectionYears = dcfProjectionYears
        )
        return engine.calculate(statements, request)
    }
}
