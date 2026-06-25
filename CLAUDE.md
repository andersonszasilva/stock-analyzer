# Stock Analyzer — Contexto do Projeto

Aplicação híbrida: interface web para cadastro de ativos e DREs + servidor MCP para o modelo de IA no terminal consultar dados e calcular indicadores. A análise narrativa acontece no terminal — o servidor é storage + cálculos.

## Stack

- **Linguagem:** Kotlin
- **Framework:** Spring Boot 4.1.0
- **Build:** Gradle (Groovy DSL)
- **Banco:** PostgreSQL
- **Template engine:** Thymeleaf + Bootstrap (cadastro de dados)
- **MCP:** Spring AI MCP Server (transporte Streamable HTTP)

## Comandos

```bash
./gradlew bootRun          # sobe a aplicação (web em :4000, MCP em :4000/mcp)
./gradlew test             # roda todos os testes
./gradlew build            # compila e testa
./gradlew bootJar          # gera o JAR executável
```

## Estrutura de pacotes

```
br.com.stockanalyzer
├── domain
│   ├── model          # Asset, FinancialStatement, StatementPeriod
│   └── repository     # AssetRepository, FinancialStatementRepository (portas)
├── application
│   ├── asset          # AssetUseCase, AssetService
│   └── analysis       # FinancialStatementUseCase, FinancialStatementService,
│                      # IndicatorCalculationEngine, FinancialIndicators, AnalysisRequest
└── infrastructure
    ├── web            # AssetController, FinancialStatementController, forms
    ├── mcp            # FinancialStatementTools (@Tool)
    └── persistence    # entidades JPA, Spring Data, adapters, mappers
```

## Arquitetura

Hexagonal (ports & adapters):
- `domain/` não depende de nada externo — sem anotações JPA ou Spring
- `application/` depende apenas de `domain/` — use cases e lógica de negócio
- `infrastructure/` implementa as interfaces de `domain/` e expõe via web e MCP

Nunca vaze entidades JPA (`*JpaEntity`) para fora de `infrastructure/persistence/`. Controllers recebem forms e retornam view objects; ferramentas MCP recebem tipos primitivos e retornam objetos de domínio.

## Convenções

- Modelos de domínio como `data class`; entidades JPA como `@Entity class` separadas
- Tipos anuláveis Kotlin (`T?`) em vez de `Optional<T>`
- Injeção de dependência via construtor (não `@Autowired` em campos)
- Ferramentas MCP anotadas com `@Tool(description = "...")` — descrições claras são essenciais para o modelo entender quando chamar cada ferramenta
- Testes unitários com mocks para services e tools; `@WebMvcTest` para controllers; `@DataJpaTest` com H2 para repositórios

## Variáveis de ambiente

| Variável        | Descrição                  |
|-----------------|----------------------------|
| `DB_URL`        | JDBC URL do PostgreSQL     |
| `DB_USERNAME`   | Usuário do banco           |
| `DB_PASSWORD`   | Senha do banco             |

## Configuração do MCP no Claude Code

Após subir a aplicação, registrar via CLI (armazena em `~/.claude.json`):

```bash
claude mcp add --transport http --scope local stock-analyzer http://localhost:4000/mcp
```

Verificar conexão:

```bash
claude mcp list
# stock-analyzer: http://localhost:4000/mcp (HTTP) - ✔ Connected
```

> **Nota:** o `settings.json` do projeto (`.claude/settings.json`) **não** registra servidores MCP — use `claude mcp add`. O escopo `local` vincula o servidor ao projeto atual.

Para verificar as ferramentas disponíveis no terminal: `/mcp`

## Referências

- Design detalhado: `docs/stock-analyzer-sdd.md`
- Spring AI MCP Server: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
