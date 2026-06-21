# SDD — Analisador de Ações por DRE com IA

## 1. Objetivo

Permitir que o usuário cadastre demonstrações de resultado (DRE) de ativos por período e solicite análises financeiras geradas por IA. A IA calcula indicadores fundamentalistas, interpreta tendências e emite um parecer sobre a saúde financeira do ativo.

**Exemplos de uso:**
- _"Calcule o ROE e ROIC de ITUB4 com base nos últimos 4 trimestres"_
- _"A empresa está gerando caixa livre suficiente para sustentar os dividendos?"_
- _"Qual o preço justo pelo método DCF com taxa de desconto de 10%?"_
- _"Compare as margens de 2023 vs 2024 e identifique a tendência"_

---

## 2. Escopo

### Incluído

- Cadastro de DRE por ativo e período (trimestral ou anual)
- Cálculo automático de indicadores fundamentalistas
- Análise narrativa gerada pela Claude API
- Dashboard de análise com indicadores e parecer da IA
- Histórico de DREs por ativo

### Excluído (v1)

- Importação automática de DRE via B3 ou CVM
- Upload de PDF de DRE
- Comparação entre ativos distintos (análise cross-asset)
- Salvamento persistente de análises geradas pela IA

---

## 3. Arquitetura

```
Usuário (browser)
      │
      │  Thymeleaf + Bootstrap
      ▼
FinancialStatementController   ← CRUD de DREs
StockAnalysisController        ← solicita análise
      │
      │  Use case interfaces
      ▼
FinancialStatementService      ← persistência de DREs
StockAnalysisService           ← orquestra cálculos + IA
IndicatorCalculationEngine     ← cálculos fundamentalistas
      │                  │
      │                  │  HTTP / Claude API
      │                  ▼
      │           ClaudeAiClient          ← integração com Anthropic
      │
      │  JPA / Hibernate
      ▼
   MySQL (tabela financial_statements)
```

### Fluxo principal

```
1. Usuário cadastra DRE de um ativo (formulário)
2. Usuário clica em "Analisar"
3. StockAnalysisService busca as DREs do ativo
4. IndicatorCalculationEngine calcula os indicadores
5. ClaudeAiClient envia os indicadores + dados brutos à IA
6. IA retorna análise estruturada (JSON + narrativa)
7. Dashboard exibe indicadores e parecer
```

---

## 4. Modelo de Domínio

### 4.1 FinancialStatement

```java
// domain/model/FinancialStatement.java
public record FinancialStatement(
    UUID id,
    UUID assetId,
    int year,
    StatementPeriod period,       // Q1, Q2, Q3, Q4, ANNUAL
    BigDecimal netRevenue,        // Receita líquida
    BigDecimal grossProfit,       // Lucro bruto
    BigDecimal ebitda,
    BigDecimal ebit,
    BigDecimal netIncome,         // Lucro líquido
    BigDecimal operatingCashFlow, // FCO — fluxo de caixa operacional
    BigDecimal freeCashFlow,      // FCL — fluxo de caixa livre
    BigDecimal totalDebt,         // Dívida bruta
    BigDecimal netDebt,           // Dívida líquida
    BigDecimal equity,            // Patrimônio líquido
    BigDecimal totalAssets,       // Ativo total
    LocalDateTime createdAt
) {}

public enum StatementPeriod { Q1, Q2, Q3, Q4, ANNUAL }
```

### 4.2 FinancialIndicators (view object)

```java
// application/analysis/FinancialIndicators.java
public record FinancialIndicators(
    // Margens
    BigDecimal grossMargin,       // Lucro bruto / Receita
    BigDecimal ebitdaMargin,
    BigDecimal netMargin,
    BigDecimal fcfMargin,         // FCL / Receita

    // Rentabilidade
    BigDecimal roe,               // Lucro líquido / Patrimônio
    BigDecimal roic,              // EBIT*(1-t) / Capital investido
    BigDecimal roa,               // Lucro líquido / Ativo total

    // Endividamento
    BigDecimal debtToEbitda,      // Dívida líquida / EBITDA
    BigDecimal debtToEquity,

    // Crescimento (se histórico disponível)
    BigDecimal revenueGrowthYoY,
    BigDecimal netIncomeGrowthYoY,

    // Geração de caixa
    BigDecimal fcfConversion,     // FCL / Lucro líquido

    // Valuation (requer preço atual do ativo)
    BigDecimal grahamPrice,       // √(22,5 × LPA × VPA)
    BigDecimal dcfFairValue       // projetado com taxa de desconto configurável
) {}
```

### 4.3 StockAnalysisResult (view object)

```java
// application/analysis/StockAnalysisResult.java
public record StockAnalysisResult(
    String assetCode,
    String assetName,
    List<FinancialStatement> statements,
    FinancialIndicators indicators,
    String aiNarrative,           // texto gerado pela IA
    String aiVerdict,             // COMPRAR / AGUARDAR / EVITAR
    String aiRiskLevel,           // BAIXO / MÉDIO / ALTO
    LocalDateTime analyzedAt
) {}
```

---

## 5. Camada de Aplicação

### 5.1 Use Cases

```java
// application/analysis/FinancialStatementUseCase.java
public interface FinancialStatementUseCase {
    FinancialStatement save(FinancialStatement statement);
    Optional<FinancialStatement> findById(UUID id);
    List<FinancialStatement> findByAsset(UUID assetId);
    List<FinancialStatement> findByAssetAndYear(UUID assetId, int year);
    void delete(UUID id);
}

// application/analysis/StockAnalysisUseCase.java
public interface StockAnalysisUseCase {
    StockAnalysisResult analyze(UUID assetId, AnalysisRequest request);
}
```

### 5.2 AnalysisRequest

```java
// application/analysis/AnalysisRequest.java
public record AnalysisRequest(
    List<UUID> statementIds,   // DREs selecionadas para a análise
    BigDecimal discountRate,   // taxa de desconto DCF (padrão: 10%)
    BigDecimal taxRate,        // alíquota IR p/ ROIC (padrão: 34%)
    int dcfProjectionYears     // anos de projeção DCF (padrão: 5)
) {}
```

### 5.3 IndicatorCalculationEngine

Componente puro (sem dependências externas) responsável pelos cálculos:

```java
// application/analysis/IndicatorCalculationEngine.java
@Component
public class IndicatorCalculationEngine {
    public FinancialIndicators calculate(List<FinancialStatement> stmts, AnalysisRequest req) { ... }

    private BigDecimal roe(FinancialStatement s) { ... }
    private BigDecimal roic(FinancialStatement s, BigDecimal taxRate) { ... }
    private BigDecimal grahamPrice(BigDecimal eps, BigDecimal bvps) { ... }
    private BigDecimal dcf(List<BigDecimal> fcfs, BigDecimal rate, int years) { ... }
}
```

---

## 6. Integração com Claude AI

### 6.1 ClaudeAiClient

```java
// infrastructure/integration/ClaudeAiClient.java
@Component
public class ClaudeAiClient {
    // POST https://api.anthropic.com/v1/messages
    public ClaudeAnalysisResponse analyze(ClaudeAnalysisRequest request) { ... }
}
```

### 6.2 Prompt enviado à IA

```
Você é um analista financeiro especializado em ações brasileiras.

Analise os dados financeiros abaixo e retorne um JSON com:
- "narrative": texto explicativo em português (3-5 parágrafos)
- "verdict": "COMPRAR" | "AGUARDAR" | "EVITAR"
- "riskLevel": "BAIXO" | "MÉDIO" | "ALTO"
- "highlights": lista de até 5 pontos positivos e negativos

Dados do ativo: {assetCode} — {assetName}
Período analisado: {periods}

Indicadores calculados:
{indicatorsJson}

Dados brutos das DREs:
{statementsJson}
```

### 6.3 Configuração

```yaml
# application.yaml
wealthlix:
  ai:
    enabled: true
    api-key: ${ANTHROPIC_API_KEY}
    model: claude-sonnet-4-6
    max-tokens: 2000
```

Se `ai.enabled=false`, o `StockAnalysisService` retorna indicadores calculados sem narrativa da IA.

---

## 7. Camada de Infraestrutura

### 7.1 Estrutura de arquivos

```
domain/
  model/
    FinancialStatement.java
  repository/
    FinancialStatementRepository.java    ← interface (porta)

application/
  analysis/
    FinancialStatementUseCase.java
    FinancialStatementService.java
    StockAnalysisUseCase.java
    StockAnalysisService.java
    IndicatorCalculationEngine.java
    FinancialIndicators.java
    StockAnalysisResult.java
    AnalysisRequest.java

infrastructure/
  web/
    FinancialStatementController.java    ← CRUD de DREs
    StockAnalysisController.java         ← solicita e exibe análise
    form/
      FinancialStatementForm.java
      AnalysisRequestForm.java
  persistence/
    jpa/entity/
      FinancialStatementJpaEntity.java
    jpa/repository/
      SpringDataFinancialStatementRepository.java
    adapter/
      FinancialStatementRepositoryAdapter.java
    mapper/
      FinancialStatementMapper.java
  integration/
    ClaudeAiClient.java
    ClaudeAnalysisRequest.java
    ClaudeAnalysisResponse.java
```

### 7.2 Entidade JPA

```sql
CREATE TABLE financial_statements (
  id              CHAR(36)       NOT NULL PRIMARY KEY,
  asset_id        CHAR(36)       NOT NULL,
  year            INT            NOT NULL,
  period          VARCHAR(10)    NOT NULL,   -- Q1/Q2/Q3/Q4/ANNUAL
  net_revenue     DECIMAL(18,2),
  gross_profit    DECIMAL(18,2),
  ebitda          DECIMAL(18,2),
  ebit            DECIMAL(18,2),
  net_income      DECIMAL(18,2),
  op_cash_flow    DECIMAL(18,2),
  free_cash_flow  DECIMAL(18,2),
  total_debt      DECIMAL(18,2),
  net_debt        DECIMAL(18,2),
  equity          DECIMAL(18,2),
  total_assets    DECIMAL(18,2),
  created_at      DATETIME       NOT NULL,
  UNIQUE KEY uq_asset_period (asset_id, year, period)
);
```

---

## 8. Interface Web (Thymeleaf)

### 8.1 Telas

| Rota                                  | Template                              | Descrição                        |
|---------------------------------------|---------------------------------------|----------------------------------|
| `GET /assets/{id}/statements`         | `statements/list.html`                | Lista de DREs do ativo           |
| `GET /assets/{id}/statements/new`     | `statements/form.html`                | Formulário de nova DRE           |
| `POST /assets/{id}/statements`        | redirect → list                       | Salva DRE                        |
| `GET /assets/{id}/statements/{sid}`   | `statements/form.html`                | Edição de DRE                    |
| `POST /assets/{id}/statements/{sid}`  | redirect → list                       | Atualiza DRE                     |
| `DELETE /assets/{id}/statements/{sid}`| redirect → list                       | Remove DRE                       |
| `GET /assets/{id}/analysis`           | `statements/analysis-setup.html`      | Seleciona DREs + parâmetros DCF  |
| `POST /assets/{id}/analysis`          | `statements/analysis-result.html`     | Exibe resultado da análise       |

### 8.2 Layout da tela de resultado da análise

```
┌─────────────────────────────────────────────────────────────┐
│  ITUB4 — Itaú Unibanco  │  Veredicto: COMPRAR  │ Risco: BAIXO │
├──────────────┬──────────────────────────────────────────────┤
│ MARGENS      │  RENTABILIDADE  │ ENDIVIDAMENTO  │ VALUATION   │
│ Net: 27%     │  ROE: 18%       │ Dív/EBITDA: 2x │ Graham: R$32│
│ EBITDA: 42%  │  ROIC: 15%      │ Dív/PL: 0,8x   │ DCF: R$38   │
│ FCF: 22%     │  ROA: 5%        │                 │             │
├──────────────┴──────────────────────────────────────────────┤
│ ANÁLISE DA IA                                               │
│ "Itaú demonstra sólida geração de caixa com crescimento    │
│  consistente de receita nos últimos 4 trimestres..."       │
│                                                             │
│ ✅ FCL cobre 1,8x os dividendos pagos                       │
│ ✅ Margem líquida acima da média do setor                   │
│ ⚠️  Dívida líquida cresceu 12% no último trimestre          │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Testes

| Camada                    | Estratégia                                                          |
|---------------------------|---------------------------------------------------------------------|
| `IndicatorCalculationEngine` | Testes unitários com valores conhecidos e assertivas exatas      |
| `StockAnalysisService`    | Testes unitários com mock do `ClaudeAiClient`                       |
| `FinancialStatementService` | Testes unitários com mock do repositório                          |
| Controllers               | `@WebMvcTest` com mock dos use cases                                |
| Repositórios              | `@DataJpaTest` com H2 (perfil test)                                 |
| End-to-end                | Manual via browser após subir a aplicação                           |

---

## 10. Decisões Técnicas

| Decisão                              | Escolha                          | Motivo                                                         |
|--------------------------------------|----------------------------------|----------------------------------------------------------------|
| Modelo de IA                         | `claude-sonnet-4-6`              | Melhor custo-benefício para análise estruturada de dados       |
| Cálculo de indicadores               | Java puro (sem lib financeira)   | Fórmulas simples e transparentes; facilita auditoria           |
| Persistência de análise da IA        | Não persiste (v1)                | Resultado é determinístico dado os dados; regenerar é barato   |
| Prompt format                        | JSON estruturado no retorno      | Facilita parsing e exibição separada de narrativa e veredicto  |
| DCF simplificado                     | FCL médio × fator perpétuo       | Evita projeções complexas; usuário pode ajustar a taxa         |
| ai.enabled flag                      | Sim                              | Permite usar a feature sem chave da API (só indicadores)       |

---

## 11. Tarefas de Implementação

### Fase 1 — Domínio e Persistência

- [ ] **T1** — Criar `FinancialStatement` (record de domínio) e enum `StatementPeriod`
- [ ] **T2** — Criar interface `FinancialStatementRepository` (domínio)
- [ ] **T3** — Criar `FinancialStatementJpaEntity` com anotações JPA
- [ ] **T4** — Criar `FinancialStatementMapper` (domain ↔ JPA entity)
- [ ] **T5** — Criar `SpringDataFinancialStatementRepository` e `FinancialStatementRepositoryAdapter`
- [ ] **T6** — Criar migration SQL / garantir que Hibernate cria a tabela no boot

### Fase 2 — Aplicação (Cálculos)

- [ ] **T7** — Criar `FinancialIndicators` e `StockAnalysisResult` (view objects)
- [ ] **T8** — Criar `AnalysisRequest` (parâmetros de entrada da análise)
- [ ] **T9** — Implementar `IndicatorCalculationEngine` com todos os indicadores
- [ ] **T10** — Escrever testes unitários de `IndicatorCalculationEngine` com dados reais de balanço
- [ ] **T11** — Criar `FinancialStatementUseCase` e `FinancialStatementService`
- [ ] **T12** — Criar `StockAnalysisUseCase` e `StockAnalysisService` (sem IA ainda)

### Fase 3 — Integração com Claude AI

- [ ] **T13** — Adicionar `ANTHROPIC_API_KEY` no `application.yaml` e `.env`
- [ ] **T14** — Implementar `ClaudeAiClient` com chamada HTTP à API Anthropic
- [ ] **T15** — Definir e refinar o prompt de análise
- [ ] **T16** — Integrar `ClaudeAiClient` no `StockAnalysisService`
- [ ] **T17** — Escrever teste unitário de `StockAnalysisService` com mock do `ClaudeAiClient`

### Fase 4 — Interface Web

- [ ] **T18** — Criar `FinancialStatementForm`, `FinancialStatementController` e templates de CRUD (`list.html`, `form.html`)
- [ ] **T19** — Criar `AnalysisRequestForm`, `StockAnalysisController` e template `analysis-setup.html`
- [ ] **T20** — Criar template `analysis-result.html` com cards de indicadores e bloco narrativo da IA
- [ ] **T21** — Adicionar link "DREs / Analisar" na tela de detalhe do ativo

### Fase 5 — Testes e Ajustes

- [ ] **T22** — Testes de integração dos controllers (`@WebMvcTest`)
- [ ] **T23** — Teste `@DataJpaTest` do repositório
- [ ] **T24** — Validação manual com DRE real de um ativo da carteira
- [ ] **T25** — Ajustar prompt com base no resultado real da IA

---

## 12. Referências

- [Anthropic API — Messages](https://docs.anthropic.com/en/api/messages)
- `docs/architecture.md` — camadas e padrões do Wealthlix
- `docs/business-rules.md` — regras dos módulos existentes
- Fórmula Graham: `√(22,5 × LPA × VPA)`
- Fórmula ROIC: `EBIT × (1 − t) / (Dívida líquida + PL)`
