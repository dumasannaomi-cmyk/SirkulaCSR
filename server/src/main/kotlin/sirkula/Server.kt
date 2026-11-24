package sirkula

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.concurrent.Executors

internal class Problem(val status: Int, message: String) : RuntimeException(message)

private val gson = Gson()

fun main() {
    val db = Database(Path.of(System.getenv("SIRKULA_DB") ?: "data/sirkula.db"))
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 32)
    server.executor = Executors.newFixedThreadPool(8)
    server.createContext("/") { exchange ->
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            val payload = when {
                method == "GET" && path == "/health" -> mapOf("ok" to true)
                method == "POST" && path == "/auth/register" -> db.authenticate(exchange.json(), register = true)
                method == "POST" && path == "/auth/login" -> db.authenticate(exchange.json(), register = false)
                else -> {
                    val token = exchange.requestHeaders.getFirst("Authorization")
                        ?.takeIf { it.startsWith("Bearer ") }?.substringAfter("Bearer ")
                        ?: throw Problem(401, "Silakan masuk dulu.")
                    val account = db.session(token)
                    when {
                        method == "GET" && path == "/trade" -> db.snapshot(account)
                        method == "POST" && path == "/auth/logout" -> mapOf("ok" to (db.signOut(token) >= 0))
                        method == "POST" && path.startsWith("/trade/") ->
                            db.change(account, path.removePrefix("/trade/"), exchange.json())
                        else -> throw Problem(404, "Halaman tidak ditemukan.")
                    }
                }
            }
            exchange.reply(200, payload)
        } catch (error: Problem) {
            exchange.reply(error.status, mapOf("error" to error.message))
        } catch (error: IllegalArgumentException) {
            exchange.reply(400, mapOf("error" to error.message))
        } catch (_: JsonParseException) {
            exchange.reply(400, mapOf("error" to "Format data tidak valid."))
        } catch (error: Exception) {
            System.err.println("Request failed: ${error.javaClass.simpleName}")
            exchange.reply(500, mapOf("error" to "Server belum bisa memproses permintaan."))
        } finally {
            exchange.close()
        }
    }
    server.start()
    println("Sirkula listening on port $port")
}

private fun HttpExchange.json(): JsonObject {
    val bytes = requestBody.readNBytes(16_385)
    if (bytes.size > 16_384) throw Problem(413, "Data terlalu besar.")
    return gson.fromJson(bytes.toString(Charsets.UTF_8), JsonObject::class.java)
        ?: throw IllegalArgumentException("Data wajib diisi.")
}

private fun HttpExchange.reply(status: Int, payload: Any) {
    val bytes = gson.toJson(payload).toByteArray(Charsets.UTF_8)
    responseHeaders.set("Content-Type", "application/json; charset=utf-8")
    responseHeaders.set("Cache-Control", "no-store")
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.write(bytes)
}
