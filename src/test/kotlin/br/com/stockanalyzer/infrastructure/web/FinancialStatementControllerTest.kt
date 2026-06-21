package br.com.stockanalyzer.infrastructure.web

import br.com.stockanalyzer.application.analysis.FinancialStatementUseCase
import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.StatementPeriod
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(FinancialStatementController::class)
class FinancialStatementControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var assetUseCase: AssetUseCase
    @MockitoBean lateinit var statementUseCase: FinancialStatementUseCase

    private val assetId: UUID = UUID.randomUUID()
    private val stmtId: UUID = UUID.randomUUID()

    private val asset = Asset(
        id = assetId,
        code = "ITUB4",
        name = "Itaú Unibanco",
        sector = "Financeiro",
        createdAt = LocalDateTime.now()
    )

    private val statement = FinancialStatement(
        id = stmtId,
        assetId = assetId,
        year = 2024,
        period = StatementPeriod.ANNUAL,
        netRevenue = BigDecimal("100000"),
        grossProfit = BigDecimal("40000"),
        ebitda = BigDecimal("25000"),
        ebit = BigDecimal("20000"),
        netIncome = BigDecimal("15000"),
        operatingCashFlow = BigDecimal("22000"),
        freeCashFlow = BigDecimal("18000"),
        totalDebt = BigDecimal("50000"),
        netDebt = BigDecimal("30000"),
        equity = BigDecimal("100000"),
        totalAssets = BigDecimal("200000"),
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `GET statements lista retorna view com DREs`() {
        `when`(assetUseCase.findById(assetId)).thenReturn(asset)
        `when`(statementUseCase.findByAsset(assetId)).thenReturn(listOf(statement))

        mockMvc.perform(get("/assets/$assetId/statements"))
            .andExpect(status().isOk)
            .andExpect(view().name("statements/list"))
            .andExpect(model().attributeExists("asset", "statements"))
    }

    @Test
    fun `GET statements new retorna formulario vazio`() {
        `when`(assetUseCase.findById(assetId)).thenReturn(asset)

        mockMvc.perform(get("/assets/$assetId/statements/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("statements/form"))
    }

    @Test
    fun `POST statements salva DRE e redireciona`() {
        `when`(assetUseCase.findById(assetId)).thenReturn(asset)
        `when`(statementUseCase.save(any())).thenReturn(statement)

        mockMvc.perform(
            post("/assets/$assetId/statements")
                .param("year", "2024")
                .param("period", "ANNUAL")
                .param("netRevenue", "100000")
                .param("grossProfit", "40000")
                .param("ebitda", "25000")
                .param("ebit", "20000")
                .param("netIncome", "15000")
                .param("operatingCashFlow", "22000")
                .param("freeCashFlow", "18000")
                .param("totalDebt", "50000")
                .param("netDebt", "30000")
                .param("equity", "100000")
                .param("totalAssets", "200000")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/assets/$assetId/statements"))
    }

    @Test
    fun `POST statements com ano invalido retorna formulario com erros`() {
        `when`(assetUseCase.findById(assetId)).thenReturn(asset)

        mockMvc.perform(
            post("/assets/$assetId/statements")
                .param("year", "1999")
                .param("period", "ANNUAL")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("statements/form"))
    }

    @Test
    fun `POST statements id delete remove DRE e redireciona`() {
        mockMvc.perform(post("/assets/$assetId/statements/$stmtId/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/assets/$assetId/statements"))

        verify(statementUseCase).delete(stmtId)
    }
}
