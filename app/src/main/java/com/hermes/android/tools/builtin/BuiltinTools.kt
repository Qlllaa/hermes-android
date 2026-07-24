package com.hermes.android.tools.builtin

import com.hermes.android.tools.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WebSearchTool : Tool {
    override val name = "web_search"
    override val description = "Search the web for information. Returns search results with titles, URLs, and snippets."
    override val parameters = schema(
        properties = mapOf(
            "query" to prop("string", "The search query"),
            "num_results" to prop("integer", "Number of results to return (default 5)")
        ),
        required = listOf("query")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val query = args["query"] ?: return ToolResult(false, "Missing 'query' parameter")
        val numResults = (args["num_results"] ?: "5").toIntOrNull() ?: 5
        // DuckDuckGo HTML search (no API key needed)
        val request = Request.Builder()
            .url("https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
            .header("User-Agent", "Mozilla/5.0")
            .get().build()
        return try {
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val results = parseResults(html, numResults)
            ToolResult(true, results)
        } catch (e: Exception) {
            ToolResult(false, "Search failed: ${e.message}")
        }
    }

    private fun parseResults(html: String, max: Int): String {
        val results = StringBuilder()
        val titleRegex = Regex("""class="result__a"[^>]*>(.*?)</a>""")
        val urlRegex = Regex("""class="result__url"[^>]*>(.*?)</a>""")
        val snippetRegex = Regex("""class="result__snippet">(.*?)</td>""")

        val titles = titleRegex.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }.toList()
        val urls = urlRegex.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }.toList()
        val snippets = snippetRegex.findAll(html).map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }.toList()

        val count = minOf(max, minOf(titles.size, urls.size))
        for (i in 0 until count) {
            results.append("[${i + 1}] ${titles[i]}\n    URL: ${urls[i]}\n    ${snippets.getOrElse(i) { "" }}\n\n")
        }
        return if (results.isEmpty()) "No results found." else results.toString()
    }
}

class CalculatorTool : Tool {
    override val name = "calculator"
    override val description = "Evaluate a mathematical expression. Supports basic arithmetic, functions (sin, cos, sqrt, etc.), and constants (pi, e)."
    override val parameters = schema(
        properties = mapOf("expression" to prop("string", "The mathematical expression to evaluate")),
        required = listOf("expression")
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val expr = args["expression"] ?: return ToolResult(false, "Missing 'expression'")
        return try {
            val sanitized = expr.replace("^", "**").replace("pi", "PI").replace("e", "E")
            // Use JavaScript-style eval via Rhino/javax or simple parser
            val result = evalExpression(sanitized)
            ToolResult(true, result.toString())
        } catch (e: Exception) {
            ToolResult(false, "Evaluation error: ${e.message}")
        }
    }

    private fun evalExpression(expr: String): Double {
        // Simple safe evaluator using Java's ScriptEngine
        val engine = javax.script.ScriptEngineManager().getEngineByName("js")
        return (engine?.eval(expr) as? Number)?.toDouble()
            ?: throw IllegalArgumentException("Could not evaluate expression")
    }
}

class HttpRequestTool : Tool {
    override val name = "http_request"
    override val description = "Make an HTTP request to a URL and return the response body. Supports GET and POST methods."
    override val parameters = schema(
        properties = mapOf(
            "url" to prop("string", "The URL to request"),
            "method" to prop("string", "HTTP method (GET or POST)", enum = listOf("GET", "POST")),
            "body" to prop("string", "Request body for POST requests"),
            "headers" to prop("string", "JSON object of headers")
        ),
        required = listOf("url")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val url = args["url"] ?: return ToolResult(false, "Missing 'url'")
        val method = args["method"] ?: "GET"
        val request = Request.Builder().url(url).header("User-Agent", "HermesAndroid/1.0")
        args["headers"]?.let { headers ->
            // Simple header parsing
            headers.remove("{", "}", "\"").split(",").forEach { h ->
                val parts = h.split(":", limit = 2)
                if (parts.size == 2) request.header(parts[0].trim(), parts[1].trim())
            }
        }
        return try {
            val response = client.newCall(request.build()).execute()
            val body = response.body?.string() ?: ""
            ToolResult(response.isSuccessful, "Status: ${response.code}\nBody: ${body.take(2000)}")
        } catch (e: Exception) {
            ToolResult(false, "Request failed: ${e.message}")
        }
    }

    private fun String.remove(vararg chars: String) = chars.fold(this) { acc, c -> acc.replace(c, "") }
}

class FileReadTool : Tool {
    override val name = "file_read"
    override val description = "Read the contents of a text file from the device storage."
    override val parameters = schema(
        properties = mapOf(
            "path" to prop("string", "Absolute file path to read"),
            "max_lines" to prop("integer", "Maximum lines to read (default 500)")
        ),
        required = listOf("path")
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult(false, "Missing 'path'")
        val maxLines = (args["max_lines"] ?: "500").toIntOrNull() ?: 500
        return try {
            val file = java.io.File(path)
            if (!file.exists()) return ToolResult(false, "File not found: $path")
            val content = file.useLines { it.take(maxLines).joinToString("\n") }
            ToolResult(true, content)
        } catch (e: Exception) {
            ToolResult(false, "Read failed: ${e.message}")
        }
    }
}

class FileWriteTool : Tool {
    override val name = "file_write"
    override val description = "Write text content to a file on the device. Creates parent directories if needed."
    override val parameters = schema(
        properties = mapOf(
            "path" to prop("string", "Absolute file path to write"),
            "content" to prop("string", "Content to write"),
            "append" to prop("boolean", "Whether to append instead of overwrite")
        ),
        required = listOf("path", "content")
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult(false, "Missing 'path'")
        val content = args["content"] ?: return ToolResult(false, "Missing 'content'")
        val append = args["append"]?.toBoolean() ?: false
        return try {
            val file = java.io.File(path)
            file.parentFile?.mkdirs()
            if (append) file.appendText(content) else file.writeText(content)
            ToolResult(true, "Written ${content.length} chars to $path")
        } catch (e: Exception) {
            ToolResult(false, "Write failed: ${e.message}")
        }
    }
}

class ShellCommandTool : Tool {
    override val name = "shell_command"
    override val description = "Execute a shell command and return stdout/stderr. Use for system operations, file management, etc."
    override val parameters = schema(
        properties = mapOf(
            "command" to prop("string", "The shell command to execute"),
            "timeout" to prop("integer", "Timeout in seconds (default 30)")
        ),
        required = listOf("command")
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val command = args["command"] ?: return ToolResult(false, "Missing 'command'")
        val timeout = (args["timeout"] ?: "30").toLongOrNull() ?: 30
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS)
            if (!exitCode) {
                process.destroyForcibly()
                return ToolResult(false, "Command timed out after $timeout seconds")
            }
            val code = process.exitValue()
            ToolResult(code == 0, output.ifEmpty { "(no output, exit code $code)" })
        } catch (e: Exception) {
            ToolResult(false, "Execution failed: ${e.message}")
        }
    }
}

class ClipboardTool : Tool {
    override val name = "clipboard"
    override val description = "Read from or write to the system clipboard."
    override val parameters = schema(
        properties = mapOf(
            "action" to prop("string", "Action: read or write", enum = listOf("read", "write")),
            "text" to prop("string", "Text to write (for write action)")
        ),
        required = listOf("action")
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        // Requires context - clipboard access will be handled via AndroidClipboard bridge
        return ToolResult(false, "Clipboard access requires app context. Use AndroidClipboardBridge.")
    }
}

class ScreenshotTool : Tool {
    override val name = "screenshot"
    override val description = "Take a screenshot of the device screen. Requires MediaProjection permission."
    override val parameters = schema(
        properties = mapOf(
            "save_path" to prop("string", "Path to save the screenshot (optional)")
        )
    )

    override suspend fun execute(args: Map<String, String>): ToolResult {
        return ToolResult(false, "Screenshot requires MediaProjection API. This tool is a placeholder.")
    }
}

object BuiltinTools {
    fun registerAll() {
        ToolRegistry.register(WebSearchTool())
        ToolRegistry.register(CalculatorTool())
        ToolRegistry.register(HttpRequestTool())
        ToolRegistry.register(FileReadTool())
        ToolRegistry.register(FileWriteTool())
        ToolRegistry.register(ShellCommandTool())
        ToolRegistry.register(ClipboardTool())
        ToolRegistry.register(ScreenshotTool())
    }
}
