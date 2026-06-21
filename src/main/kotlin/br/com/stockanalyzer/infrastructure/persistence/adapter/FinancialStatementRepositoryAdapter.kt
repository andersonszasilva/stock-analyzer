package br.com.stockanalyzer.infrastructure.persistence.adapter

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.repository.FinancialStatementRepository
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataFinancialStatementRepository
import br.com.stockanalyzer.infrastructure.persistence.mapper.FinancialStatementMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class FinancialStatementRepositoryAdapter(
    private val springDataRepo: SpringDataFinancialStatementRepository,
    private val mapper: FinancialStatementMapper
) : FinancialStatementRepository {

    override fun save(statement: FinancialStatement): FinancialStatement =
        mapper.toDomain(springDataRepo.save(mapper.toEntity(statement)))

    override fun findById(id: UUID): FinancialStatement? =
        springDataRepo.findById(id).map(mapper::toDomain).orElse(null)

    override fun findByAssetId(assetId: UUID): List<FinancialStatement> =
        springDataRepo.findByAssetId(assetId).map(mapper::toDomain)

    override fun findByAssetIdAndYear(assetId: UUID, year: Int): List<FinancialStatement> =
        springDataRepo.findByAssetIdAndYear(assetId, year).map(mapper::toDomain)

    override fun delete(id: UUID) =
        springDataRepo.deleteById(id)
}
