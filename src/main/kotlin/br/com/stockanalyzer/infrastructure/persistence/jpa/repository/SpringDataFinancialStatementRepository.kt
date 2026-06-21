package br.com.stockanalyzer.infrastructure.persistence.jpa.repository

import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.FinancialStatementJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFinancialStatementRepository : JpaRepository<FinancialStatementJpaEntity, UUID> {
    fun findByAssetId(assetId: UUID): List<FinancialStatementJpaEntity>
    fun findByAssetIdAndYear(assetId: UUID, year: Int): List<FinancialStatementJpaEntity>
}
