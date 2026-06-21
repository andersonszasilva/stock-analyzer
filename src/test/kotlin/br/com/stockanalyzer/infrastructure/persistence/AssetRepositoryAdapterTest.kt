package br.com.stockanalyzer.infrastructure.persistence

import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetJpaEntity
import br.com.stockanalyzer.infrastructure.persistence.jpa.repository.SpringDataAssetRepository
import br.com.stockanalyzer.infrastructure.persistence.mapper.AssetMapper
import br.com.stockanalyzer.infrastructure.persistence.adapter.AssetRepositoryAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Import(AssetRepositoryAdapter::class, AssetMapper::class)
class AssetRepositoryAdapterTest {

    @Autowired lateinit var adapter: AssetRepositoryAdapter
    @Autowired lateinit var springRepo: SpringDataAssetRepository

    private fun entity(code: String = "ITUB4") = AssetJpaEntity(
        id = UUID.randomUUID(),
        code = code,
        name = "Itaú Unibanco",
        sector = "Financeiro",
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `save persiste e retorna ativo de dominio`() {
        val saved = springRepo.save(entity())
        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals("ITUB4", found!!.code)
    }

    @Test
    fun `findByCode retorna ativo quando existe`() {
        springRepo.save(entity("PETR4"))

        val found = adapter.findByCode("PETR4")

        assertNotNull(found)
        assertEquals("PETR4", found!!.code)
    }

    @Test
    fun `findByCode retorna null quando nao existe`() {
        assertNull(adapter.findByCode("XPTO"))
    }

    @Test
    fun `findAll retorna todos os ativos`() {
        springRepo.save(entity("ITUB4"))
        springRepo.save(entity("PETR4"))

        val all = adapter.findAll()

        assertEquals(2, all.size)
    }

    @Test
    fun `delete remove ativo`() {
        val saved = springRepo.save(entity())
        adapter.delete(saved.id)

        assertNull(adapter.findById(saved.id))
    }
}
