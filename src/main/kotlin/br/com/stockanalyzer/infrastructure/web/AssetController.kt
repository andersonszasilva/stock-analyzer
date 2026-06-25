package br.com.stockanalyzer.infrastructure.web

import br.com.stockanalyzer.application.asset.AssetUseCase
import br.com.stockanalyzer.domain.model.Asset
import br.com.stockanalyzer.infrastructure.web.form.AssetForm
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

@Controller
@RequestMapping("/assets")
class AssetController(private val assetUseCase: AssetUseCase) {

    @GetMapping
    fun list(model: Model): String {
        model.addAttribute("assets", assetUseCase.findAll())
        return "assets/list"
    }

    @GetMapping("/new")
    fun newForm(model: Model): String {
        model.addAttribute("form", AssetForm())
        model.addAttribute("assetId", null)
        return "assets/form"
    }

    @PostMapping
    fun create(@Valid @ModelAttribute("form") form: AssetForm, result: BindingResult, model: Model): String {
        if (result.hasErrors()) {
            model.addAttribute("assetId", null)
            return "assets/form"
        }
        assetUseCase.save(form.toAsset())
        return "redirect:/assets"
    }

    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: UUID, model: Model): String {
        val asset = assetUseCase.findById(id) ?: return "redirect:/assets"
        model.addAttribute("form", AssetForm(code = asset.code, name = asset.name, sector = asset.sector, sharesOutstanding = asset.sharesOutstanding))
        model.addAttribute("assetId", id)
        return "assets/form"
    }

    @PostMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @ModelAttribute("form") form: AssetForm,
        result: BindingResult,
        model: Model
    ): String {
        if (result.hasErrors()) {
            model.addAttribute("assetId", id)
            return "assets/form"
        }
        assetUseCase.save(form.toAsset(id))
        return "redirect:/assets"
    }

    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: UUID): String {
        assetUseCase.delete(id)
        return "redirect:/assets"
    }

    private fun AssetForm.toAsset(id: UUID = UUID.randomUUID()) = Asset(
        id = id,
        code = code.trim().uppercase(),
        name = name.trim(),
        sector = sector?.trim()?.ifBlank { null },
        createdAt = LocalDateTime.now(),
        sharesOutstanding = sharesOutstanding
    )
}
