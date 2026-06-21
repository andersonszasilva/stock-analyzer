package br.com.stockanalyzer.infrastructure.mcp

import br.com.stockanalyzer.application.analysis.FinancialStatementUseCase
import br.com.stockanalyzer.application.analysis.IndicatorCalculationEngine
import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.StatementPeriod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FinancialStatementToolsTest {

    @Mock lateinit var assetUseCase: AssetUseCase
    @Mock lateinit var statementUseCase: FinancialStatementUseCase
    @InjectMocks lateinit var engine: IndicatorCalculationEngine

    private fun tools() = FinancialStatementTools(assetUseCase, statementUseCase, IndicatorCalculationEngine())

    private val assetId: UUID = UUID.randomUUID()

    private val asset = Asset(
        id = assetId,
        code = "ITUB4",
        name = "Itaú Unibanco",
        sector = "Financeiro",
        createdAt = LocalDateTime.now()
    )

    private fun statement(year: Int = 2024) = FinancialStatement(
        id = UUID.randomUUID(),
        assetId = assetId,
        year = year,
        period = StatementPeriod.ANNUAL,
        netRevenue = BigDecimal("10000"),
        grossProfit = BigDecimal("4000"),
        ebitda = BigDecimal("2500"),
        ebit = BigDecimal("2000"),
        netIncome = BigDecimal("1500"),
        operatingCashFlow = BigDecimal("2200"),
        freeCashFlow = BigDecimal("1800"),
        totalDebt = BigDecimal("5000"),
        netDebt = BigDecimal("3000"),
        equity = BigDecimal("10000"),
        totalAssets = BigDecimal("20000"),
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `findAllAssets retorna lista do use case`() {
        `when`(assetUseCase.findAll()).thenReturn(listOf(asset))

        val result = tools().findAllAssets()

        assertEquals(1, result.size)
        assertEquals("ITUB4", result[0].code)
    }

    @Test
    fun `findStatementsByAssetCode retorna statements quando ativo existe`() {
        val stmts = listOf(statement())
        `when`(assetUseCase.findByCode("ITUB4")).thenReturn(asset)
        `when`(statementUseCase.findByAsset(assetId)).thenReturn(stmts)

        val result = tools().findStatementsByAssetCode("ITUB4")

        assertEquals(1, result.size)
    }

    @Test
    fun `findStatementsByAssetCode retorna lista vazia quando ativo nao existe`() {
        `when`(assetUseCase.findByCode("XXXX")).thenReturn(null)

        val result = tools().findStatementsByAssetCode("XXXX")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findStatementsByAssetCode normaliza codigo para maiusculas`() {
        `when`(assetUseCase.findByCode("ITUB4")).thenReturn(asset)
        `when`(statementUseCase.findByAsset(assetId)).thenReturn(emptyList())

        tools().findStatementsByAssetCode("itub4")

        verify(assetUseCase).findByCode("ITUB4")
    }

    @Test
    fun `findStatementsByAssetCodeAndYear filtra por ano`() {
        val stmts = listOf(statement(year = 2024))
        `when`(assetUseCase.findByCode("ITUB4")).thenReturn(asset)
        `when`(statementUseCase.findByAssetAndYear(assetId, 2024)).thenReturn(stmts)

        val result = tools().findStatementsByAssetCodeAndYear("ITUB4", 2024)

        assertEquals(1, result.size)
        assertEquals(2024, result[0].year)
    }

    @Test
    fun `calculateIndicators retorna indicadores quando ativo e DREs existem`() {
        `when`(assetUseCase.findByCode("ITUB4")).thenReturn(asset)
        `when`(statementUseCase.findByAsset(assetId)).thenReturn(listOf(statement()))

        val indicators = tools().calculateIndicators("ITUB4")

        assertEquals(BigDecimal("0.1500"), indicators.roe)
        assertEquals(BigDecimal("0.1500"), indicators.netMargin)
    }

    @Test
    fun `calculateIndicators lanca excecao quando ativo nao existe`() {
        `when`(assetUseCase.findByCode("XPTO")).thenReturn(null)

        assertThrows(IllegalArgumentException::class.java) {
            tools().calculateIndicators("XPTO")
        }
    }

    @Test
    fun `calculateIndicators lanca excecao quando sem DREs`() {
        `when`(assetUseCase.findByCode("ITUB4")).thenReturn(asset)
        `when`(statementUseCase.findByAsset(assetId)).thenReturn(emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            tools().calculateIndicators("ITUB4")
        }
    }
}
