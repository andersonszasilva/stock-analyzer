package br.com.stockanalyzer.application.asset

import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.model.AssetIndicators
import br.com.stockanalyzer.domain.repository.AssetIndicatorsRepository
import br.com.stockanalyzer.domain.repository.AssetRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssetService(
    private val repository: AssetRepository,
    private val indicatorsRepository: AssetIndicatorsRepository,
) : AssetUseCase {

    override fun save(asset: Asset): Asset = repository.save(asset)

    override fun findById(id: UUID): Asset? = repository.findById(id)

    override fun findByCode(code: String): Asset? = repository.findByCode(code)

    override fun findAll(): List<Asset> = repository.findAll()

    override fun delete(id: UUID) = repository.delete(id)

    override fun saveIndicators(indicators: AssetIndicators): AssetIndicators =
        indicatorsRepository.save(indicators)

    override fun findIndicatorsByAssetId(assetId: UUID): AssetIndicators? =
        indicatorsRepository.findByAssetId(assetId)
}
