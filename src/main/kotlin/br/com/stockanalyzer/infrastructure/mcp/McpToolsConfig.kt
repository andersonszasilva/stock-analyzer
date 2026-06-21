package br.com.stockanalyzer.infrastructure.mcp

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpToolsConfig {
    @Bean
    fun toolCallbackProvider(tools: FinancialStatementTools): ToolCallbackProvider =
        MethodToolCallbackProvider.builder().toolObjects(tools).build()
}
