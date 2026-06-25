package br.com.stockanalyzer.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "assets")
class AssetJpaEntity(
    @Id
    val id: UUID,

    @Column(nullable = false, unique = true, length = 10)
    val code: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(length = 100)
    val sector: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "shares_outstanding")
    val sharesOutstanding: Long? = null,

    @Column(name = "recommended_price", precision = 12, scale = 2)
    val recommendedPrice: BigDecimal? = null,

    @Column(name = "last_calculated_at")
    val lastCalculatedAt: LocalDateTime? = null
)
