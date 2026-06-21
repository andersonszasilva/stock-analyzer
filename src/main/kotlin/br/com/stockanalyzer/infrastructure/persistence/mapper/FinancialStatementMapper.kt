package br.com.stockanalyzer.infrastructure.persistence.mapper

import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.StatementPeriod
import br.com.stockanalyzer.infrastructure.persistence.jpa.entity.FinancialStatementJpaEntity
import org.springframework.stereotype.Component

@Component
class FinancialStatementMapper {
    fun toDomain(entity: FinancialStatementJpaEntity): FinancialStatement = FinancialStatement(
        id = entity.id,
        assetId = entity.assetId,
        year = entity.year,
        period = StatementPeriod.valueOf(entity.period),
        netRevenue = entity.netRevenue,
        grossProfit = entity.grossProfit,
        ebitda = entity.ebitda,
        ebit = entity.ebit,
        netIncome = entity.netIncome,
        operatingCashFlow = entity.operatingCashFlow,
        freeCashFlow = entity.freeCashFlow,
        totalDebt = entity.totalDebt,
        netDebt = entity.netDebt,
        equity = entity.equity,
        totalAssets = entity.totalAssets,
        createdAt = entity.createdAt
    )

    fun toEntity(domain: FinancialStatement): FinancialStatementJpaEntity = FinancialStatementJpaEntity(
        id = domain.id,
        assetId = domain.assetId,
        year = domain.year,
        period = domain.period.name,
        netRevenue = domain.netRevenue,
        grossProfit = domain.grossProfit,
        ebitda = domain.ebitda,
        ebit = domain.ebit,
        netIncome = domain.netIncome,
        operatingCashFlow = domain.operatingCashFlow,
        freeCashFlow = domain.freeCashFlow,
        totalDebt = domain.totalDebt,
        netDebt = domain.netDebt,
        equity = domain.equity,
        totalAssets = domain.totalAssets,
        createdAt = domain.createdAt
    )
}
