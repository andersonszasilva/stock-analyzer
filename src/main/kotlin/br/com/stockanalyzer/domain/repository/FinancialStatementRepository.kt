package br.com.stockanalyzer.domain.repository

import br.com.stockanalyzer.domain.model.FinancialStatement
import java.util.UUID

interface FinancialStatementRepository {
    fun save(statement: FinancialStatement): FinancialStatement
    fun findById(id: UUID): FinancialStatement?
    fun findByAssetId(assetId: UUID): List<FinancialStatement>
    fun findByAssetIdAndYear(assetId: UUID, year: Int): List<FinancialStatement>
    fun delete(id: UUID)
}
