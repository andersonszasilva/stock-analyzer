package br.com.stockanalyzer.infrastructure.persistence.mapper

import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetJpaEntity
import org.springframework.stereotype.Component

@Component
class AssetMapper {
    fun toDomain(entity: AssetJpaEntity): Asset = Asset(
        id = entity.id,
        code = entity.code,
        name = entity.name,
        sector = entity.sector,
        createdAt = entity.createdAt,
        sharesOutstanding = entity.sharesOutstanding,
        recommendedPrice = entity.recommendedPrice,
        lastCalculatedAt = entity.lastCalculatedAt
    )

    fun toEntity(domain: Asset): AssetJpaEntity = AssetJpaEntity(
        id = domain.id,
        code = domain.code,
        name = domain.name,
        sector = domain.sector,
        createdAt = domain.createdAt,
        sharesOutstanding = domain.sharesOutstanding,
        recommendedPrice = domain.recommendedPrice,
        lastCalculatedAt = domain.lastCalculatedAt
    )
}
