package br.com.stockanalyzer.infrastructure.persistence

import br.com.stockanalyzer.infrastructure.persistence.adapter.FinancialStatementRepositoryAdapter
import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetJpaEntity
import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.FinancialStatementJpaEntity
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataAssetRepository
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataFinancialStatementRepository
import br.com.stockanalyzer.infrastructure.persistence.mapper.FinancialStatementMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Import(FinancialStatementRepositoryAdapter::class, FinancialStatementMapper::class)
class FinancialStatementRepositoryAdapterTest {

    @Autowired lateinit var adapter: FinancialStatementRepositoryAdapter
    @Autowired lateinit var assetRepo: SpringDataAssetRepository
    @Autowired lateinit var stmtRepo: SpringDataFinancialStatementRepository

    private lateinit var assetId: UUID

    @BeforeEach
    fun setup() {
        val asset = assetRepo.save(
            AssetJpaEntity(UUID.randomUUID(), "ITUB4", "Itaú Unibanco", "Financeiro", LocalDateTime.now())
        )
        assetId = asset.id
    }

    private fun entity(year: Int = 2024, period: String = "ANNUAL") = FinancialStatementJpaEntity(
        id = UUID.randomUUID(),
        assetId = assetId,
        year = year,
        period = period,
        monetaryUnit = "MILLIONS",
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
    fun `findByAssetId retorna DREs do ativo`() {
        stmtRepo.save(entity(2023))
        stmtRepo.save(entity(2024))

        val result = adapter.findByAssetId(assetId)

        assertEquals(2, result.size)
    }

    @Test
    fun `findByAssetIdAndYear filtra por ano`() {
        stmtRepo.save(entity(2023))
        stmtRepo.save(entity(2024))

        val result = adapter.findByAssetIdAndYear(assetId, 2024)

        assertEquals(1, result.size)
        assertEquals(2024, result[0].year)
    }

    @Test
    fun `delete remove DRE`() {
        val saved = stmtRepo.save(entity())
        adapter.delete(saved.id)

        assertNull(adapter.findById(saved.id))
    }

    @Test
    fun `findById retorna null quando nao existe`() {
        assertNull(adapter.findById(UUID.randomUUID()))
    }
}
