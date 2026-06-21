package br.com.stockanalyzer.infrastructure.web

import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(AssetController::class)
class AssetControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockitoBean lateinit var assetUseCase: AssetUseCase

    private val assetId: UUID = UUID.randomUUID()
    private val asset = Asset(
        id = assetId,
        code = "PETR4",
        name = "Petrobras",
        sector = "Petróleo",
        createdAt = LocalDateTime.now()
    )

    @Test
    fun `GET assets retorna lista com status 200`() {
        `when`(assetUseCase.findAll()).thenReturn(listOf(asset))

        mockMvc.perform(get("/assets"))
            .andExpect(status().isOk)
            .andExpect(view().name("assets/list"))
            .andExpect(model().attributeExists("assets"))
    }

    @Test
    fun `GET assets new retorna formulario vazio`() {
        mockMvc.perform(get("/assets/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("assets/form"))
            .andExpect(model().attributeExists("form"))
    }

    @Test
    fun `POST assets salva ativo e redireciona`() {
        `when`(assetUseCase.save(any())).thenReturn(asset)

        mockMvc.perform(
            post("/assets")
                .param("code", "PETR4")
                .param("name", "Petrobras")
                .param("sector", "Petróleo")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/assets"))
    }

    @Test
    fun `POST assets com campos invalidos retorna formulario com erros`() {
        mockMvc.perform(
            post("/assets")
                .param("code", "")
                .param("name", "")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("assets/form"))
    }

    @Test
    fun `GET assets id edit retorna formulario preenchido`() {
        `when`(assetUseCase.findById(assetId)).thenReturn(asset)

        mockMvc.perform(get("/assets/$assetId/edit"))
            .andExpect(status().isOk)
            .andExpect(view().name("assets/form"))
    }

    @Test
    fun `POST assets id delete remove ativo e redireciona`() {
        mockMvc.perform(post("/assets/$assetId/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/assets"))

        verify(assetUseCase).delete(assetId)
    }
}
