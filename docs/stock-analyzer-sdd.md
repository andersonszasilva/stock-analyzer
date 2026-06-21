# SDD — Analisador de Ações por DRE com IA (MCP)

## 1. Objetivo

Prover uma aplicação com dois canais de acesso complementares:

- **Interface web (Thymeleaf)** — cadastro de ativos e suas DREs
- **Servidor MCP** — ferramentas que o modelo de IA no terminal invoca para consultar DREs e calcular indicadores fundamentalistas

A análise narrativa e o parecer sobre o ativo acontecem no terminal, com o modelo chamando as ferramentas MCP conforme necessário.

**Exemplos de uso no terminal:**
- _"Calcule o ROE e ROIC de ITUB4 com base nos últimos 4 trimestres"_
- _"A empresa está gerando caixa livre suficiente para sustentar os dividendos?"_
- _"Qual o preço justo pelo método DCF com taxa de desconto de 10%?"_
- _"Compare as margens de 2023 vs 2024 e identifique a tendência"_

---

## 2. Escopo

### Incluído

- Cadastro de ativos (código, nome, setor) via interface web
- Cadastro de DRE por ativo e período (trimestral ou anual) via interface web
- Histórico de DREs por ativo na interface web
- Cálculo automático de indicadores fundamentalistas via ferramenta MCP
- Servidor MCP com transporte HTTP/SSE (Spring AI MCP Server)

### Excluído (v1)

- Análise narrativa na interface web (acontece no terminal)
- Importação automática de DRE via B3 ou CVM
- Upload de PDF de DRE
- Comparação entre ativos distintos (análise cross-asset)
- Salvamento persistente de análises geradas pela IA

---

## 3. Arquitetura

```
Terminal (Claude Code + MCP)          Usuário (browser)
         │                                    │
         │  MCP (HTTP/SSE)                    │  Thymeleaf + Bootstrap
         ▼                                    ▼
FinancialStatementTools          AssetController
IndicatorTools                   FinancialStatementController
         │                                    │
         └──────────────┬─────────────────────┘
                        │  Use case interfaces
                        ▼
              AssetService
              FinancialStatementService
              IndicatorCalculationEngine
                        │
                        │  JPA / Hibernate
                        ▼
                   PostgreSQL
```

### Fluxo — cadastro de dados (web)

```
1. Usuário acessa a interface web e cadastra um ativo (código + nome)
2. Usuário cadastra DREs do ativo por período
```

### Fluxo — análise (terminal)

```
1. Usuário pede ao modelo no terminal para analisar um ativo
2. Modelo chama a ferramenta MCP `find_statements_by_asset`
3. Modelo chama a ferramenta MCP `calculate_indicators`
4. Modelo recebe os indicadores e produz a análise narrativa
5. Usuário lê o parecer diretamente no terminal
```

---

## 4. Modelo de Domínio

### 4.1 Asset

```kotlin
// domain/model/Asset.kt
data class Asset(
    val id: UUID,
    val code: String,          // ex: "ITUB4"
    val name: String,          // ex: "Itaú Unibanco"
    val sector: String?,       // ex: "Financeiro"
    val createdAt: LocalDateTime
)
```

### 4.2 FinancialStatement

```kotlin
// domain/model/FinancialStatement.kt
data class FinancialStatement(
    val id: UUID,
    val assetId: UUID,
    val year: Int,
    val period: StatementPeriod,       // Q1, Q2, Q3, Q4, ANNUAL
    val netRevenue: BigDecimal,        // Receita líquida
    val grossProfit: BigDecimal,       // Lucro bruto
    val ebitda: BigDecimal,
    val ebit: BigDecimal,
    val netIncome: BigDecimal,         // Lucro líquido
    val operatingCashFlow: BigDecimal, // FCO — fluxo de caixa operacional
    val freeCashFlow: BigDecimal,      // FCL — fluxo de caixa livre
    val totalDebt: BigDecimal,         // Dívida bruta
    val netDebt: BigDecimal,           // Dívida líquida
    val equity: BigDecimal,            // Patrimônio líquido
    val totalAssets: BigDecimal,       // Ativo total
    val createdAt: LocalDateTime
)

enum class StatementPeriod { Q1, Q2, Q3, Q4, ANNUAL }
```

### 4.3 FinancialIndicators

```kotlin
// application/analysis/FinancialIndicators.kt
data class FinancialIndicators(
    // Margens
    val grossMargin: BigDecimal,       // Lucro bruto / Receita
    val ebitdaMargin: BigDecimal,
    val netMargin: BigDecimal,
    val fcfMargin: BigDecimal,         // FCL / Receita

    // Rentabilidade
    val roe: BigDecimal,               // Lucro líquido / Patrimônio
    val roic: BigDecimal,              // EBIT*(1-t) / Capital investido
    val roa: BigDecimal,               // Lucro líquido / Ativo total

    // Endividamento
    val debtToEbitda: BigDecimal,      // Dívida líquida / EBITDA
    val debtToEquity: BigDecimal,

    // Crescimento (se histórico disponível)
    val revenueGrowthYoY: BigDecimal,
    val netIncomeGrowthYoY: BigDecimal,

    // Geração de caixa
    val fcfConversion: BigDecimal,     // FCL / Lucro líquido

    // Valuation
    val grahamPrice: BigDecimal,       // √(22,5 × LPA × VPA)
    val dcfFairValue: BigDecimal       // FCL médio × fator perpétuo
)
```

---

## 5. Camada de Aplicação

### 5.1 Use Cases

```kotlin
// application/asset/AssetUseCase.kt
interface AssetUseCase {
    fun save(asset: Asset): Asset
    fun findById(id: UUID): Asset?
    fun findAll(): List<Asset>
    fun delete(id: UUID)
}

// application/analysis/FinancialStatementUseCase.kt
interface FinancialStatementUseCase {
    fun save(statement: FinancialStatement): FinancialStatement
    fun findById(id: UUID): FinancialStatement?
    fun findByAsset(assetId: UUID): List<FinancialStatement>
    fun findByAssetAndYear(assetId: UUID, year: Int): List<FinancialStatement>
    fun delete(id: UUID)
}
```

### 5.2 AnalysisRequest

```kotlin
// application/analysis/AnalysisRequest.kt
data class AnalysisRequest(
    val statementIds: List<UUID>,   // DREs selecionadas para a análise
    val discountRate: BigDecimal,   // taxa de desconto DCF (padrão: 10%)
    val taxRate: BigDecimal,        // alíquota IR p/ ROIC (padrão: 34%)
    val dcfProjectionYears: Int     // anos de projeção DCF (padrão: 5)
)
```

### 5.3 IndicatorCalculationEngine

Componente puro (sem dependências externas) responsável pelos cálculos:

```kotlin
// application/analysis/IndicatorCalculationEngine.kt
@Component
class IndicatorCalculationEngine {
    fun calculate(stmts: List<FinancialStatement>, req: AnalysisRequest): FinancialIndicators { ... }

    private fun roe(s: FinancialStatement): BigDecimal { ... }
    private fun roic(s: FinancialStatement, taxRate: BigDecimal): BigDecimal { ... }
    private fun grahamPrice(eps: BigDecimal, bvps: BigDecimal): BigDecimal { ... }
    private fun dcf(fcfs: List<BigDecimal>, rate: BigDecimal, years: Int): BigDecimal { ... }
}
```

---

## 6. Ferramentas MCP

Expostas via Spring AI MCP Server com `@Tool`. O modelo no terminal descobre e invoca essas ferramentas automaticamente.

```kotlin
// infrastructure/mcp/FinancialStatementTools.kt
@Component
class FinancialStatementTools(
    private val statementUseCase: FinancialStatementUseCase,
    private val assetUseCase: AssetUseCase,
    private val engine: IndicatorCalculationEngine
) {
    @Tool(description = "Lista todos os ativos cadastrados")
    fun findAllAssets(): List<Asset> { ... }

    @Tool(description = "Lista todas as DREs de um ativo pelo código (ex: ITUB4)")
    fun findStatementsByAssetCode(assetCode: String): List<FinancialStatement> { ... }

    @Tool(description = "Lista as DREs de um ativo filtradas por ano")
    fun findStatementsByAssetCodeAndYear(assetCode: String, year: Int): List<FinancialStatement> { ... }

    @Tool(description = "Calcula indicadores fundamentalistas com base nas DREs de um ativo")
    fun calculateIndicators(
        assetCode: String,
        discountRate: Double = 0.10,
        taxRate: Double = 0.34,
        dcfProjectionYears: Int = 5
    ): FinancialIndicators { ... }
}
```

---

## 7. Interface Web (Thymeleaf)

### 7.1 Telas

| Rota                                  | Template                        | Descrição                      |
|---------------------------------------|---------------------------------|--------------------------------|
| `GET /assets`                         | `assets/list.html`              | Lista de ativos cadastrados    |
| `GET /assets/new`                     | `assets/form.html`              | Formulário de novo ativo       |
| `POST /assets`                        | redirect → list                 | Salva ativo                    |
| `GET /assets/{id}/edit`               | `assets/form.html`              | Edição de ativo                |
| `POST /assets/{id}`                   | redirect → list                 | Atualiza ativo                 |
| `DELETE /assets/{id}`                 | redirect → list                 | Remove ativo                   |
| `GET /assets/{id}/statements`         | `statements/list.html`          | Lista de DREs do ativo         |
| `GET /assets/{id}/statements/new`     | `statements/form.html`          | Formulário de nova DRE         |
| `POST /assets/{id}/statements`        | redirect → list                 | Salva DRE                      |
| `GET /assets/{id}/statements/{sid}`   | `statements/form.html`          | Edição de DRE                  |
| `POST /assets/{id}/statements/{sid}`  | redirect → list                 | Atualiza DRE                   |
| `DELETE /assets/{id}/statements/{sid}`| redirect → list                 | Remove DRE                     |

---

## 8. Camada de Infraestrutura

### 8.1 Estrutura de arquivos

```
domain/
  model/
    Asset.kt
    FinancialStatement.kt
  repository/
    AssetRepository.kt                 ← interface (porta)
    FinancialStatementRepository.kt    ← interface (porta)

application/
  asset/
    AssetUseCase.kt
    AssetService.kt
  analysis/
    FinancialStatementUseCase.kt
    FinancialStatementService.kt
    IndicatorCalculationEngine.kt
    FinancialIndicators.kt
    AnalysisRequest.kt

infrastructure/
  web/
    AssetController.kt
    FinancialStatementController.kt
    form/
      AssetForm.kt
      FinancialStatementForm.kt
  mcp/
    FinancialStatementTools.kt         ← ferramentas MCP (@Tool)
  persistence/
    jpa/entity/
      AssetJpaEntity.kt
      FinancialStatementJpaEntity.kt
    jpa/repository/
      SpringDataAssetRepository.kt
      SpringDataFinancialStatementRepository.kt
    adapter/
      AssetRepositoryAdapter.kt
      FinancialStatementRepositoryAdapter.kt
    mapper/
      AssetMapper.kt
      FinancialStatementMapper.kt
```

### 8.2 Schema SQL

```sql
CREATE TABLE assets (
  id          UUID          NOT NULL PRIMARY KEY,
  code        VARCHAR(10)   NOT NULL UNIQUE,   -- ex: ITUB4
  name        VARCHAR(100)  NOT NULL,
  sector      VARCHAR(100),
  created_at  TIMESTAMP     NOT NULL
);

CREATE TABLE financial_statements (
  id              UUID           NOT NULL PRIMARY KEY,
  asset_id        UUID           NOT NULL REFERENCES assets(id),
  year            INTEGER        NOT NULL,
  period          VARCHAR(10)    NOT NULL,   -- Q1/Q2/Q3/Q4/ANNUAL
  net_revenue     NUMERIC(18,2),
  gross_profit    NUMERIC(18,2),
  ebitda          NUMERIC(18,2),
  ebit            NUMERIC(18,2),
  net_income      NUMERIC(18,2),
  op_cash_flow    NUMERIC(18,2),
  free_cash_flow  NUMERIC(18,2),
  total_debt      NUMERIC(18,2),
  net_debt        NUMERIC(18,2),
  equity          NUMERIC(18,2),
  total_assets    NUMERIC(18,2),
  created_at      TIMESTAMP      NOT NULL,
  CONSTRAINT uq_asset_period UNIQUE (asset_id, year, period)
);
```

### 8.3 Dependência Spring AI MCP Server

```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.ai:spring-ai-mcp-server-webmvc-spring-boot-starter'
}
```

---

## 9. Configuração do MCP no Claude Code

Após subir a aplicação (`./gradlew bootRun`), registrar o servidor MCP:

```json
// .claude/settings.json
{
  "mcpServers": {
    "stock-analyzer": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Para verificar as ferramentas disponíveis no terminal: `/mcp`

---

## 10. Testes

| Camada                       | Estratégia                                                     |
|------------------------------|----------------------------------------------------------------|
| `IndicatorCalculationEngine` | Testes unitários com valores conhecidos e assertivas exatas    |
| `FinancialStatementService`  | Testes unitários com mock do repositório                       |
| `AssetService`               | Testes unitários com mock do repositório                       |
| `FinancialStatementTools`    | Testes unitários com mock dos use cases                        |
| Controllers                  | `@WebMvcTest` com mock dos use cases                           |
| Repositórios                 | `@DataJpaTest` com H2 (perfil test)                            |
| End-to-end                   | Manual: cadastrar via web, analisar via terminal               |

---

## 11. Decisões Técnicas

| Decisão                        | Escolha                            | Motivo                                                              |
|--------------------------------|------------------------------------|---------------------------------------------------------------------|
| Cadastro de dados              | Interface web (Thymeleaf)          | Formulário é mais ergonômico para entrada manual de DREs            |
| Integração com IA              | MCP (servidor)                     | Análise acontece no terminal; app é storage + cálculos              |
| Transporte MCP                 | HTTP/SSE                           | Natural para Spring Boot WebMVC; não exige modo stdio               |
| Cálculo de indicadores         | Kotlin puro (sem lib financeira)   | Fórmulas simples e transparentes; facilita auditoria                |
| Persistência de análises da IA | Não persiste                       | Análise é gerada pelo modelo no terminal; regenerar é barato        |
| DCF simplificado               | FCL médio × fator perpétuo         | Evita projeções complexas; usuário pode ajustar a taxa via terminal |

---

## 12. Tarefas de Implementação

### Fase 1 — Domínio e Persistência

- [x] **T1** — Criar `Asset` e `FinancialStatement` (data classes de domínio) e enum `StatementPeriod`
- [x] **T2** — Criar interfaces `AssetRepository` e `FinancialStatementRepository` (domínio)
- [x] **T3** — Criar `AssetJpaEntity` e `FinancialStatementJpaEntity` com anotações JPA
- [x] **T4** — Criar mappers (domain ↔ JPA entity)
- [x] **T5** — Criar repositórios Spring Data e adapters
- [x] **T6** — Criar migration SQL (tabelas `assets` e `financial_statements`)

### Fase 2 — Aplicação (Cálculos)

- [x] **T7** — Criar `FinancialIndicators` e `AnalysisRequest`
- [x] **T8** — Implementar `IndicatorCalculationEngine` com todos os indicadores
- [x] **T9** — Escrever testes unitários de `IndicatorCalculationEngine` com dados reais de balanço
- [x] **T10** — Criar `AssetUseCase` / `AssetService` e `FinancialStatementUseCase` / `FinancialStatementService`

### Fase 3 — Interface Web

- [x] **T11** — Criar `AssetForm`, `AssetController` e templates (`assets/list.html`, `assets/form.html`)
- [x] **T12** — Criar `FinancialStatementForm`, `FinancialStatementController` e templates (`statements/list.html`, `statements/form.html`)

### Fase 4 — Ferramentas MCP

- [x] **T13** — Adicionar dependência `spring-ai-mcp-server-webmvc-spring-boot-starter` no `build.gradle`
- [x] **T14** — Implementar `FinancialStatementTools` com todas as ferramentas `@Tool`
- [x] **T15** — Registrar o servidor MCP no `.claude/settings.json`
- [x] **T16** — Escrever testes unitários de `FinancialStatementTools` com mocks

### Fase 5 — Testes e Ajustes

- [x] **T17** — `@WebMvcTest` dos controllers
- [x] **T18** — `@DataJpaTest` dos repositórios
- [x] **T19** — Validação manual: cadastrar via web, analisar via terminal com DRE real
- [x] **T20** — Refinar descrições das ferramentas `@Tool` com base no uso real

---

## 13. Referências

- [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [Model Context Protocol](https://modelcontextprotocol.io)
- `docs/architecture.md` — camadas e padrões do Stock Analyzer
- Fórmula Graham: `√(22,5 × LPA × VPA)`
- Fórmula ROIC: `EBIT × (1 − t) / (Dívida líquida + PL)`
