package br.com.stockanalyzer.infrastructure.web.form

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AssetForm(
    @field:NotBlank
    @field:Size(max = 10)
    val code: String = "",

    @field:NotBlank
    @field:Size(max = 100)
    val name: String = "",

    @field:Size(max = 100)
    val sector: String? = null
)
