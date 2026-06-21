package br.com.stockanalyzer.infrastructure.persistence.jpa.repository

import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataAssetRepository : JpaRepository<AssetJpaEntity, UUID> {
    fun findByCode(code: String): AssetJpaEntity?
}
