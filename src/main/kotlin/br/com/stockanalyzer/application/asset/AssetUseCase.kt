package br.com.stockanalyzer.application.asset

import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.AssetIndicators
import java.util.UUID

interface AssetUseCase {
    fun save(asset: Asset): Asset
    fun findById(id: UUID): Asset?
    fun findByCode(code: String): Asset?
    fun findAll(): List<Asset>
    fun delete(id: UUID)
    fun saveIndicators(indicators: AssetIndicators): AssetIndicators
    fun findIndicatorsByAssetId(assetId: UUID): AssetIndicators?
}
