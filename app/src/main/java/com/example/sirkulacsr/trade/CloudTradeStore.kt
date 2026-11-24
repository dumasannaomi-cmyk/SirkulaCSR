package com.example.sirkulacsr.trade

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URI

internal interface TradeData {
    fun snapshot(): TradeSnapshot
    fun createListing(role: TradeRole, title: String, price: Long, weight: String, location: String,
        category: String, condition: String, description: String, company: String): Long
    fun submit(role: TradeRole, id: Long, price: Long, message: String, buyerName: String): Boolean
    fun counter(role: TradeRole, id: Long, price: Long, message: String)
    fun respond(role: TradeRole, id: Long, accept: Boolean)
    fun send(role: TradeRole, id: Long, message: String)
    fun confirm(role: TradeRole, id: Long)
}

internal data class CloudSession(val account: LocalTradeAccount, val token: String)

internal class CloudTradeApi(private val origin: String) {
    private val gson = Gson()

    fun authenticate(account: LocalTradeAccount, password: String, register: Boolean) =
        gson.fromJson(request(if (register) "/auth/register" else "/auth/login",
            mapOf("email" to account.email, "name" to account.name, "role" to account.role.name,
                "password" to password)), CloudSession::class.java)

    fun snapshot(token: String) = gson.fromJson(request("/trade", token = token), TradeSnapshot::class.java)

    fun signOut(token: String) {
        request("/auth/logout", emptyMap(), token)
    }

    fun change(token: String, action: String, body: Map<String, Any?>) =
        request("/trade/$action", body, token)

    private fun request(path: String, body: Map<String, Any?>? = null, token: String? = null): JsonObject {
        val connection = URI.create(origin + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(gson.toJson(body).toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val message = runCatching { gson.fromJson(error, JsonObject::class.java)?.get("error")?.asString }.getOrNull()
                throw IllegalArgumentException(message ?: "Server SIRKULA tidak dapat diakses.")
            }
            return connection.inputStream.bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
        } finally {
            connection.disconnect()
        }
    }
}

internal class CloudTradeStore(private val api: CloudTradeApi, private val token: String) : TradeData {
    override fun snapshot() = api.snapshot(token)

    override fun createListing(role: TradeRole, title: String, price: Long, weight: String, location: String,
        category: String, condition: String, description: String, company: String): Long {
        return api.change(token, "listings", mapOf("title" to title, "price" to price, "weight" to weight,
            "location" to location, "category" to category, "condition" to condition, "description" to description))
            .get("id").asLong
    }

    override fun submit(role: TradeRole, id: Long, price: Long, message: String, buyerName: String) =
        api.change(token, "submit", mapOf("listingId" to id, "price" to price, "message" to message))
            .get("created").asBoolean

    override fun counter(role: TradeRole, id: Long, price: Long, message: String) {
        api.change(token, "counter", mapOf("listingId" to id, "price" to price, "message" to message))
    }

    override fun respond(role: TradeRole, id: Long, accept: Boolean) {
        api.change(token, "respond", mapOf("listingId" to id, "accept" to accept))
    }

    override fun send(role: TradeRole, id: Long, message: String) {
        api.change(token, "send", mapOf("listingId" to id, "message" to message))
    }

    override fun confirm(role: TradeRole, id: Long) {
        api.change(token, "confirm", mapOf("listingId" to id))
    }
}
