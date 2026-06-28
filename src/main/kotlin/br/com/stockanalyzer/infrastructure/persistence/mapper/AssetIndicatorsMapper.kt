package br.com.stockanalyzer.infrastructure.persistence.mapper

import br.com.stockanalyzer.domain.model.AssetIndicators
import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.AssetIndicatorsJpaEntity
import org.springframework.stereotype.Component

@Component
class AssetIndicatorsMapper {

    fun toDomain(e: AssetIndicatorsJpaEntity) = AssetIndicators(
        id                  = e.id,
        assetId             = e.assetId,
        grossMargin         = e.grossMargin,
        ebitdaMargin        = e.ebitdaMargin,
        netMargin           = e.netMargin,
        fcfMargin           = e.fcfMargin,
        roe                 = e.roe,
        roic                = e.roic,
        roa                 = e.roa,
        debtToEbitda        = e.debtToEbitda,
        debtToEquity        = e.debtToEquity,
        revenueGrowthYoY    = e.revenueGrowthYoY,
        netIncomeGrowthYoY  = e.netIncomeGrowthYoY,
        fcfConversion       = e.fcfConversion,
        grahamPrice         = e.grahamPrice,
        dcfFairValue        = e.dcfFairValue,
        eps                 = e.eps,
        bvps                = e.bvps,
        recommendedPrice    = e.recommendedPrice,
        calculatedAt        = e.calculatedAt,
    )

    fun toEntity(d: AssetIndicators) = AssetIndicatorsJpaEntity(
        id                  = d.id,
        assetId             = d.assetId,
        grossMargin         = d.grossMargin,
        ebitdaMargin        = d.ebitdaMargin,
        netMargin           = d.netMargin,
        fcfMargin           = d.fcfMargin,
        roe                 = d.roe,
        roic                = d.roic,
        roa                 = d.roa,
        debtToEbitda        = d.debtToEbitda,
        debtToEquity        = d.debtToEquity,
        revenueGrowthYoY    = d.revenueGrowthYoY,
        netIncomeGrowthYoY  = d.netIncomeGrowthYoY,
        fcfConversion       = d.fcfConversion,
        grahamPrice         = d.grahamPrice,
        dcfFairValue        = d.dcfFairValue,
        eps                 = d.eps,
        bvps                = d.bvps,
        recommendedPrice    = d.recommendedPrice,
        calculatedAt        = d.calculatedAt,
    )
}
