package com.hermes.android.tools

import com.hermes.android.api.models.ToolDefinition
import com.hermes.android.api.models.ToolFunctionDef
import com.hermes.android.api.models.JsonSchema
import com.hermes.android.api.models.JsonSchemaProperty

interface Tool {
    val name: String
    val description: String
    val parameters: JsonSchema

    suspend fun execute(args: Map<String, String>): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val output: String,
    val data: Map<String, Any>? = null
)

object ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun get(name: String): Tool? = tools[name]

    fun all(): List<Tool> = tools.values.toList()

    fun enabled(enabledNames: Set<String>): List<Tool> =
        tools.filterKeys { it in enabledNames }.values.toList()

    fun toDefinitions(enabledNames: Set<String>): List<ToolDefinition> =
        enabled(enabledNames).map { t ->
            ToolDefinition(
                function = ToolFunctionDef(
                    name = t.name,
                    description = t.description,
                    parameters = t.parameters
                )
            )
        }
}

// Helper to build JsonSchema easily
fun schema(
    type: String = "object",
    properties: Map<String, JsonSchemaProperty>? = null,
    required: List<String>? = null
) = JsonSchema(type = type, properties = properties, required = required)

fun prop(type: String, description: String, enum: List<String>? = null) =
    JsonSchemaProperty(type = type, description = description, enum = enum)
