package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.repository.FinancialStatementRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FinancialStatementService(
    private val repository: FinancialStatementRepository
) : FinancialStatementUseCase {

    override fun save(statement: FinancialStatement): FinancialStatement = repository.save(statement)

    override fun findById(id: UUID): FinancialStatement? = repository.findById(id)

    override fun findByAsset(assetId: UUID): List<FinancialStatement> =
        repository.findByAssetId(assetId).sortedWith(compareBy({ it.year }, { it.period }))

    override fun findByAssetAndYear(assetId: UUID, year: Int): List<FinancialStatement> =
        repository.findByAssetIdAndYear(assetId, year).sortedBy { it.period }

    override fun delete(id: UUID) = repository.delete(id)
}
