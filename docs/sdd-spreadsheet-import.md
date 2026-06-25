# SDD — Importação de DRE via PDF pelo Claude Code (MCP)

## Contexto

A extração de dados financeiros de PDFs de resultado exige inteligência para interpretar tabelas em formatos variados por empresa. Em vez de embutir essa lógica no servidor, o modelo já tem essa capacidade nativamente — o Claude Code lê o PDF, extrai os dados e chama um **novo MCP tool de escrita** para cadastrar o `FinancialStatement` diretamente no banco.

---

## Arquitetura da solução

```
Usuário (terminal Claude Code)
    │
    │  "Leia o PDF docs/009512000101011.pdf e cadastre o resultado da PETR4"
    ▼
Claude Code
    ├── Lê o PDF nativamente (ferramenta Read)
    ├── Identifica: empresa=PETR4, período=1T26 → Q1/2026
    ├── Extrai valores das tabelas do relatório
    └── Chama MCP tool: saveFinancialStatement(assetCode, year, period, ...)
                                │
                                ▼
                    http://localhost:4000/mcp
                                │
                    FinancialStatementTools.saveFinancialStatement()
                                │
                    FinancialStatementUseCase.save()
                                │
                    PostgreSQL ✓
```

**O que muda no servidor:** apenas um novo `@Tool` em `FinancialStatementTools.kt`.  
**Sem PDFBox. Sem AI client. Sem upload web.**

---

## Dados extraíveis do PDF (Petrobras 1T26)

Claude Code lê as seguintes tabelas do relatório:

| Tabela | Página | Campo do domínio | Rótulo no PDF |
|---|---|---|---|
| Tabela 11 — DRE | 19 | `netRevenue` | "Receita de vendas" |
| Tabela 11 — DRE | 19 | `grossProfit` | "Lucro bruto" |
| Tabela 11 — DRE | 19 | `ebit` | "Lucro antes do resultado financeiro, participações e tributos" |
| Tabela 11 — DRE | 19 | `netIncome` | "Lucro líquido do período" |
| Tabela 10 — EBITDA | 18 | `ebitda` | "EBITDA" (linha base, antes dos ajustes) |
| Tabela 5 — Liquidez | 11 | `operatingCashFlow` | "Recursos gerados pelas atividades operacionais" |
| Tabela 5 — Liquidez | 11 | `freeCashFlow` | "Fluxo de caixa livre" |
| Tabela 6 — Endividamento | 13 | `totalDebt` | "Dívida Financeira" + "Arrendamentos" em R$ |
| Tabela 6 — Endividamento | 13 | `netDebt` | "Dívida Líquida" em R$ |
| Tabela 12 — Balanço | 20 | `equity` | "Patrimônio Líquido" |
| Tabela 12 — Balanço | 20 | `totalAssets` | "Total do Ativo" |

Período identificado pela capa: `1T26` → `Q1`, ano `2026`.

---

## O que mudar no servidor

### `FinancialStatementTools.kt` — novo `@Tool`

```kotlin
@Tool(description = """
    Cadastra ou atualiza uma DRE de um ativo a partir dos dados extraídos de um relatório de resultado.
    Todos os valores financeiros devem estar em R$ milhões.
    O período deve ser: Q1, Q2, Q3, Q4 ou ANNUAL.
    Se já existir uma DRE para o mesmo ativo, ano e período, ela será sobrescrita.
""")
fun saveFinancialStatement(
    assetCode: String,
    year: Int,
    period: String,
    netRevenue: Double,
    grossProfit: Double,
    ebitda: Double,
    ebit: Double,
    netIncome: Double,
    operatingCashFlow: Double,
    freeCashFlow: Double,
    totalDebt: Double,
    netDebt: Double,
    equity: Double,
    totalAssets: Double
): FinancialStatement {
    val asset = assetUseCase.findByCode(assetCode.uppercase())
        ?: throw IllegalArgumentException("Ativo '$assetCode' não encontrado. Cadastre o ativo primeiro.")

    val statementPeriod = StatementPeriod.valueOf(period.uppercase())

    // Verifica se já existe — reutiliza o ID para sobrescrever
    val existing = statementUseCase.findByAssetAndYear(asset.id, year)
        .firstOrNull { it.period == statementPeriod }

    val statement = FinancialStatement(
        id = existing?.id ?: UUID.randomUUID(),
        assetId = asset.id,
        year = year,
        period = statementPeriod,
        netRevenue = BigDecimal(netRevenue.toString()),
        grossProfit = BigDecimal(grossProfit.toString()),
        ebitda = BigDecimal(ebitda.toString()),
        ebit = BigDecimal(ebit.toString()),
        netIncome = BigDecimal(netIncome.toString()),
        operatingCashFlow = BigDecimal(operatingCashFlow.toString()),
        freeCashFlow = BigDecimal(freeCashFlow.toString()),
        totalDebt = BigDecimal(totalDebt.toString()),
        netDebt = BigDecimal(netDebt.toString()),
        equity = BigDecimal(equity.toString()),
        totalAssets = BigDecimal(totalAssets.toString()),
        createdAt = existing?.createdAt ?: LocalDateTime.now()
    )
    return statementUseCase.save(statement)
}
```

### Nenhuma outra alteração no servidor

Não há mudança em controllers, templates, build.gradle ou application.properties.

---

## Fluxo de uso no terminal

```
# 1. Garantir que a aplicação está rodando
./gradlew bootRun

# 2. No Claude Code, solicitar a importação
"Leia o arquivo docs/009512000101011.pdf,
 extraia os dados financeiros consolidados do período corrente
 e cadastre como DRE do ativo PETR4"

# Claude Code vai:
# → Ler o PDF
# → Identificar: 1T26 = Q1/2026, valores em R$ milhões
# → Chamar saveFinancialStatement(assetCode="PETR4", year=2026, period="Q1", ...)
# → Confirmar o cadastro

# 3. Verificar o resultado
# Claude Code chama findStatementsByAssetCode("PETR4") para confirmar
```

---

## Decisões tomadas

| Decisão | Escolha | Motivo |
|---|---|---|
| Onde ocorre a extração do PDF | Claude Code (terminal) | Evita dependência de AI client no servidor; o modelo já lê PDFs |
| Onde ocorre o cadastro | Servidor via MCP tool | Mantém o banco como fonte de verdade |
| Comportamento em duplicatas | Sobrescrever (reutiliza o ID existente) | Importações repetidas do mesmo trimestre devem corrigir valores |
| Unidade dos valores | R$ milhões (explícito na description do tool) | Padrão dos relatórios brasileiros |
| Pré-requisito | Ativo deve existir (cadastrado via web UI ou outro tool) | Separação de responsabilidades |

---

## Ordem de implementação

1. Adicionar `saveFinancialStatement` em `FinancialStatementTools.kt`
2. Adicionar imports necessários (`StatementPeriod`, `LocalDateTime`, `UUID`)
3. Testar unitariamente em `FinancialStatementToolsTest.kt`
4. Reiniciar a aplicação e verificar com `/mcp` que o novo tool aparece (total: 5 ferramentas)
5. Validar com o PDF de exemplo: pedir ao Claude Code para importar o 1T26 da PETR4

---

## Referências

- PDF de exemplo: `docs/009512000101011.pdf` (Petrobras 1T26)
- Tools existentes: `infrastructure/mcp/FinancialStatementTools.kt`
- Use case: `application/analysis/FinancialStatementUseCase.kt`
- Domínio: `domain/model/FinancialStatement.kt`, `domain/model/StatementPeriod.kt`
