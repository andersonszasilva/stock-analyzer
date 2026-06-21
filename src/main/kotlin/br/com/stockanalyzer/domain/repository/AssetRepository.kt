package br.com.stockanalyzer.domain.repository

import br.com.stockanalyzer.domain.model.Asset
import java.util.UUID

interface AssetRepository {
    fun save(asset: Asset): Asset
    fun findById(id: UUID): Asset?
    fun findByCode(code: String): Asset?
    fun findAll(): List<Asset>
    fun delete(id: UUID)
}
