package br.com.stockanalyzer.domain.repository

import br.com.stockanalyzer.domain.model.AssetIndicators
import java.util.UUID

interface AssetIndicatorsRepository {
    fun save(indicators: AssetIndicators): AssetIndicators
    fun findByAssetId(assetId: UUID): AssetIndicators?
}
