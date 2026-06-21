package br.com.stockanalyzer.application.analysis

import br.com.stockanalyzer.domain.model.FinancialStatement
import java.util.UUID

interface FinancialStatementUseCase {
    fun save(statement: FinancialStatement): FinancialStatement
    fun findById(id: UUID): FinancialStatement?
    fun findByAsset(assetId: UUID): List<FinancialStatement>
    fun findByAssetAndYear(assetId: UUID, year: Int): List<FinancialStatement>
    fun delete(id: UUID)
}
