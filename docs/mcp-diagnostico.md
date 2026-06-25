# MCP no Stock Analyzer

Documenta como o servidor MCP está configurado e funcionando neste projeto.

---

## Visão geral

O Stock Analyzer expõe um servidor MCP via **Streamable HTTP** na mesma porta da aplicação web (`:4000`). O Claude Code se conecta a `http://localhost:4000/mcp` e invoca as ferramentas para consultar ativos e calcular indicadores financeiros — a análise narrativa acontece no terminal do modelo.

```
Claude Code (terminal)
      │
      │  JSON-RPC over Streamable HTTP
      ▼
http://localhost:4000/mcp
      │
      ├── findAllAssets
      ├── findStatementsByAssetCode
      ├── findStatementsByAssetCodeAndYear
      └── calculateIndicators
```

---

## Configuração atual

**`src/main/resources/application.properties`**
```properties
spring.ai.mcp.server.protocol=STREAMABLE
server.port=4000
```

**Registro no Claude Code** (armazenado em `~/.claude.json`, gerenciado pelo CLI):
```bash
claude mcp add --transport http --scope local stock-analyzer http://localhost:4000/mcp
```

O arquivo `.claude/settings.json` do projeto **não** registra servidores MCP — apenas o comando `claude mcp add` faz isso.

---

## Ferramentas disponíveis

| Ferramenta | Descrição |
|---|---|
| `findAllAssets` | Lista todos os ativos cadastrados |
| `findStatementsByAssetCode` | Retorna todos os DREs de um ativo pelo código (ex: `WEGE3`) |
| `findStatementsByAssetCodeAndYear` | Retorna o DRE de um ativo em um ano específico |
| `calculateIndicators` | Calcula indicadores financeiros (P/L, ROE, margem, etc.) para um ativo |

Para ver as ferramentas ativas no Claude Code: `/mcp`

---

## Arquivos-chave

| Arquivo | Papel |
|---|---|
| `src/main/resources/application.properties` | Define `protocol=STREAMABLE` e porta |
| `infrastructure/mcp/FinancialStatementTools.kt` | Ferramentas anotadas com `@Tool` |
| `infrastructure/mcp/McpToolsConfig.kt` | Registro do `ToolCallbackProvider` no Spring |

---

## Como subir e verificar

```bash
# 1. Sobe a aplicação
./gradlew bootRun

# 2. Verifica que o endpoint MCP responde
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:4000/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
# Esperado: 200

# 3. Verifica conexão no Claude Code
claude mcp list
# stock-analyzer: http://localhost:4000/mcp (HTTP) - ✔ Connected
```

---

## Transportes suportados pelo Spring AI MCP

| Protocolo | `application.properties` | Endpoint |
|---|---|---|
| Streamable HTTP (**atual**) | `protocol=STREAMABLE` | `/mcp` |
| SSE legado | `protocol=SSE` | `/sse` |
| Stateless | `protocol=STATELESS` | `/mcp` |

O projeto usa **Streamable HTTP** porque é o transporte recomendado pela especificação MCP e o único suportado nativamente pelo `claude mcp add --transport http`.
