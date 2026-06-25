package br.com.stockanalyzer.infrastructure.web

import br.com.stockanalyzer.application.analysis.FinancialStatementUseCase
import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.FinancialStatement
import br.com.stockanalyzer.domain.model.MonetaryUnit
import br.com.stockanalyzer.domain.model.StatementPeriod
import br.com.stockanalyzer.infrastructure.web.form.FinancialStatementForm
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

@Controller
@RequestMapping("/assets/{assetId}/statements")
class FinancialStatementController(
    private val assetUseCase: AssetUseCase,
    private val statementUseCase: FinancialStatementUseCase
) {

    @GetMapping
    fun list(@PathVariable assetId: UUID, model: Model): String {
        val asset = assetUseCase.findById(assetId) ?: return "redirect:/assets"
        model.addAttribute("asset", asset)
        model.addAttribute("statements", statementUseCase.findByAsset(assetId))
        return "statements/list"
    }

    @GetMapping("/new")
    fun newForm(@PathVariable assetId: UUID, model: Model): String {
        val asset = assetUseCase.findById(assetId) ?: return "redirect:/assets"
        model.addAttribute("asset", asset)
        model.addAttribute("form", FinancialStatementForm())
        model.addAttribute("statementId", null)
        model.addAttribute("periods", StatementPeriod.entries)
        model.addAttribute("monetaryUnits", MonetaryUnit.entries)
        return "statements/form"
    }

    @PostMapping
    fun create(
        @PathVariable assetId: UUID,
        @Valid @ModelAttribute("form") form: FinancialStatementForm,
        result: BindingResult,
        model: Model
    ): String {
        if (result.hasErrors()) {
            model.addAttribute("asset", assetUseCase.findById(assetId))
            model.addAttribute("statementId", null)
            model.addAttribute("periods", StatementPeriod.entries)
            model.addAttribute("monetaryUnits", MonetaryUnit.entries)
            return "statements/form"
        }
        statementUseCase.save(form.toStatement(assetId))
        return "redirect:/assets/$assetId/statements"
    }

    @GetMapping("/{statementId}")
    fun editForm(@PathVariable assetId: UUID, @PathVariable statementId: UUID, model: Model): String {
        val asset = assetUseCase.findById(assetId) ?: return "redirect:/assets"
        val stmt = statementUseCase.findById(statementId) ?: return "redirect:/assets/$assetId/statements"
        model.addAttribute("asset", asset)
        model.addAttribute("form", stmt.toForm())
        model.addAttribute("statementId", statementId)
        model.addAttribute("periods", StatementPeriod.entries)
        model.addAttribute("monetaryUnits", MonetaryUnit.entries)
        return "statements/form"
    }

    @PostMapping("/{statementId}")
    fun update(
        @PathVariable assetId: UUID,
        @PathVariable statementId: UUID,
        @Valid @ModelAttribute("form") form: FinancialStatementForm,
        result: BindingResult,
        model: Model
    ): String {
        if (result.hasErrors()) {
            model.addAttribute("asset", assetUseCase.findById(assetId))
            model.addAttribute("statementId", statementId)
            model.addAttribute("periods", StatementPeriod.entries)
            model.addAttribute("monetaryUnits", MonetaryUnit.entries)
            return "statements/form"
        }
        statementUseCase.save(form.toStatement(assetId, statementId))
        return "redirect:/assets/$assetId/statements"
    }

    @PostMapping("/{statementId}/delete")
    fun delete(@PathVariable assetId: UUID, @PathVariable statementId: UUID): String {
        statementUseCase.delete(statementId)
        return "redirect:/assets/$assetId/statements"
    }

    private fun FinancialStatementForm.toStatement(assetId: UUID, id: UUID = UUID.randomUUID()) =
        FinancialStatement(
            id = id,
            assetId = assetId,
            year = year!!,
            period = period!!,
            monetaryUnit = monetaryUnit,
            netRevenue = netRevenue,
            grossProfit = grossProfit,
            ebitda = ebitda,
            ebit = ebit,
            netIncome = netIncome,
            operatingCashFlow = operatingCashFlow,
            freeCashFlow = freeCashFlow,
            totalDebt = totalDebt,
            netDebt = netDebt,
            equity = equity,
            totalAssets = totalAssets,
            createdAt = LocalDateTime.now()
        )

    private fun FinancialStatement.toForm() = FinancialStatementForm(
        year = year,
        period = period,
        monetaryUnit = monetaryUnit,
        netRevenue = netRevenue,
        grossProfit = grossProfit,
        ebitda = ebitda,
        ebit = ebit,
        netIncome = netIncome,
        operatingCashFlow = operatingCashFlow,
        freeCashFlow = freeCashFlow,
        totalDebt = totalDebt,
        netDebt = netDebt,
        equity = equity,
        totalAssets = totalAssets
    )
}
