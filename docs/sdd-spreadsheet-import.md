# SDD — Importação de DRE via PDF pelo Claude Code (MCP)

## Contexto

A extração de dados financeiros de PDFs de resultado exige inteligência para interpretar tabelas em formatos variados por empresa. Em vez de embutir essa lógica no servidor, o modelo já tem essa capacidade nativamente — o Claude Code lê o PDF, extrai os dados e chama um **MCP tool de escrita** para cadastrar o `FinancialStatement` diretamente no banco.

A plataforma suporta os períodos `Q1`, `Q2`, `Q3`, `Q4` e `ANNUAL`. Na prática, o foco é em **relatórios anuais** — os releases trimestrais intermediários geralmente não contêm todos os campos necessários (veja `CLAUDE.md` — Tipos de PDF).

---

## Arquitetura da solução

```
Usuário (terminal Claude Code)
    │
    │  "Leia o PDF docs/ativos/ativo-XX/2025/4T25.pdf e cadastre o resultado anual"
    ▼
Claude Code
    ├── Lê o PDF nativamente (ferramenta Read)
    ├── Identifica: empresa, ano (ex: 2025), period=ANNUAL
    ├── Extrai valores das tabelas do relatório anual
    └── Chama MCP tool: saveFinancialStatement(assetCode, year, period="ANNUAL", ...)
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

**O que muda no servidor:** apenas um `@Tool` em `FinancialStatementTools.kt`.  
**Sem PDFBox. Sem AI client. Sem upload web.**

---

## Dados extraíveis do PDF (release de resultados anual)

Claude Code lê as seguintes tabelas do relatório:

| Tabela | Campo do domínio | Rótulo típico no PDF |
|---|---|---|
| DRE | `netRevenue` | "Receita líquida de vendas" |
| DRE | `grossProfit` | "Lucro bruto" |
| DRE | `ebit` | "Lucro antes do resultado financeiro e tributos" |
| DRE | `netIncome` | "Lucro líquido do período" |
| Reconciliação EBITDA | `ebitda` | "EBITDA ajustado" |
| Fluxo de Caixa | `operatingCashFlow` | "Caixa líquido gerado pelas atividades operacionais" |
| Fluxo de Caixa | `freeCashFlow` | "Fluxo de caixa livre" |
| Endividamento | `totalDebt` | "Dívida bruta" |
| Endividamento | `netDebt` | "Dívida líquida" |
| Balanço | `equity` | "Patrimônio líquido total" |
| Balanço | `totalAssets` | "Total do ativo" |

O período (`Q1`–`Q4` ou `ANNUAL`) e o ano são identificados a partir da capa e das tabelas do PDF.

---

## O que mudar no servidor

### `FinancialStatementTools.kt` — `@Tool`

```kotlin
@Tool(description = """
    Cadastra ou atualiza uma DRE anual de um ativo a partir dos dados extraídos de um release de resultados.
    Todos os valores financeiros devem estar na unidade indicada por monetaryUnit (padrão: MILLIONS).
    O período deve ser: Q1, Q2, Q3, Q4 ou ANNUAL.
    O monetaryUnit deve ser: UNITS, THOUSANDS, MILLIONS ou BILLIONS.
    Se já existir uma DRE para o mesmo ativo e ano, ela será sobrescrita.
""")
fun saveFinancialStatement(
    assetCode: String,
    year: Int,
    period: String,       // Q1, Q2, Q3, Q4 ou ANNUAL
    monetaryUnit: String,
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

### Exemplo 1 — Relatório anual

```
# 1. Garantir que a aplicação está rodando
./gradlew bootRun

# 2. No Claude Code, solicitar a importação
"Leia o PDF docs/ativos/ativo-XX/2025/4T25.pdf,
 extraia os dados financeiros consolidados do exercício anual de 2025
 e cadastre como DRE anual do ativo XX"

# Claude Code vai:
# → Ler o PDF (release de resultados)
# → Identificar: ano=2025, period=ANNUAL, valores em R$ milhões
# → Chamar saveFinancialStatement(assetCode="XX", year=2025, period="ANNUAL", ...)
# → Confirmar o cadastro

# 3. Verificar o resultado
# Claude Code chama findStatementsByAssetCode("XX") para confirmar
```

### Exemplo 2 — Relatório trimestral

```
# 2. No Claude Code, solicitar a importação do trimestre
"Leia o PDF docs/ativos/ativo-XX/2025/3T25.pdf,
 extraia os dados financeiros consolidados do 3º trimestre de 2025
 e cadastre como DRE trimestral do ativo XX"

# Claude Code vai:
# → Ler o PDF (release de resultados trimestral)
# → Identificar: ano=2025, period=Q3, valores em R$ milhões
# → Chamar saveFinancialStatement(assetCode="XX", year=2025, period="Q3", ...)
# → Confirmar o cadastro

# Atenção: releases trimestrais intermediários (1T, 2T, 3T) frequentemente
# são apresentações para investidores com dados resumidos — verifique se o PDF
# contém todos os campos antes de importar (ver CLAUDE.md — Tipos de PDF).
```

---

## Decisões tomadas

| Decisão | Escolha | Motivo |
|---|---|---|
| Onde ocorre a extração do PDF | Claude Code (terminal) | Evita dependência de AI client no servidor; o modelo já lê PDFs |
| Onde ocorre o cadastro | Servidor via MCP tool | Mantém o banco como fonte de verdade |
| Periodicidade suportada | Q1, Q2, Q3, Q4, ANNUAL | Flexibilidade total; na prática o foco é em ANNUAL pois releases intermediários costumam ter dados incompletos |
| Comportamento em duplicatas | Sobrescrever (reutiliza o ID existente) | Reimportações do mesmo ano devem corrigir valores |
| Unidade dos valores | Configurável via `monetaryUnit` | Suporta ativos que reportam em diferentes moedas/escalas |
| Pré-requisito | Ativo deve existir (cadastrado via web UI) | Separação de responsabilidades |

---

## Referências

- PDFs dos ativos: `docs/ativos/<ativo-XX>/<ANO>/`
- Tools existentes: `infrastructure/mcp/FinancialStatementTools.kt`
- Use case: `application/analysis/FinancialStatementUseCase.kt`
- Domínio: `domain/model/FinancialStatement.kt`, `domain/model/StatementPeriod.kt`
