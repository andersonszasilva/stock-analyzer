package br.com.stockanalyzer.infrastructure.persistence.adapter

import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.domain.repository.AssetRepository
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataAssetRepository
import br.com.stockanalyzer.infrastructure.persistence.mapper.AssetMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AssetRepositoryAdapter(
    private val springDataRepo: SpringDataAssetRepository,
    private val mapper: AssetMapper
) : AssetRepository {

    override fun save(asset: Asset): Asset =
        mapper.toDomain(springDataRepo.save(mapper.toEntity(asset)))

    override fun findById(id: UUID): Asset? =
        springDataRepo.findById(id).map(mapper::toDomain).orElse(null)

    override fun findByCode(code: String): Asset? =
        springDataRepo.findByCode(code)?.let(mapper::toDomain)

    override fun findAll(): List<Asset> =
        springDataRepo.findAll().map(mapper::toDomain)

    override fun delete(id: UUID) =
        springDataRepo.deleteById(id)
}
