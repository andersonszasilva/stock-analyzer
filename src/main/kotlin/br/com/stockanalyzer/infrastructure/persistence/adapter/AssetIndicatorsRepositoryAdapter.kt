package br.com.stockanalyzer.infrastructure.persistence.adapter

import br.com.stockanalyzer.domain.model.AssetIndicators
import br.com.stockanalyzer.domain.repository.AssetIndicatorsRepository
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataAssetIndicatorsRepository
import br.com.stockanalyzer.infrastructure.persistence.mapper.AssetIndicatorsMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AssetIndicatorsRepositoryAdapter(
    private val springDataRepo: SpringDataAssetIndicatorsRepository,
    private val mapper: AssetIndicatorsMapper,
) : AssetIndicatorsRepository {

    override fun save(indicators: AssetIndicators): AssetIndicators =
        mapper.toDomain(springDataRepo.save(mapper.toEntity(indicators)))

    override fun findByAssetId(assetId: UUID): AssetIndicators? =
        springDataRepo.findByAssetId(assetId)?.let(mapper::toDomain)
}
