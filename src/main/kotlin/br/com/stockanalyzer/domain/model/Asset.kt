package br.com.stockanalyzer.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Asset(
    val id: UUID,
    val code: String,
    val name: String,
    val sector: String?,
    val createdAt: LocalDateTime
)
