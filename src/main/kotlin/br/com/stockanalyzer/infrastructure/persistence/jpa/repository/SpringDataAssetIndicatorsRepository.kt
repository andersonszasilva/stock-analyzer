package br.com.stockanalyzer.infrastructure.persistence.jpa.repository

import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetIndicatorsJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataAssetIndicatorsRepository : JpaRepository<AssetIndicatorsJpaEntity, UUID> {
    fun findByAssetId(assetId: UUID): AssetIndicatorsJpaEntity?
}
