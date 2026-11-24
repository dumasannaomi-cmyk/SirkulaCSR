package sirkula

import com.google.gson.JsonObject
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.path.createDirectories

internal data class Account(val id: Long, val email: String, val name: String, val role: String)

internal class Database(path: Path) {
    private val random = SecureRandom()
    private val url = "jdbc:sqlite:" + path.toAbsolutePath()

    init {
        path.toAbsolutePath().parent.createDirectories()
        connect().use { db ->
            db.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode = WAL")
                val schema = checkNotNull(javaClass.getResourceAsStream("/schema.sql"))
                    .bufferedReader().use { it.readText() }
                schema.split(';').filter(String::isNotBlank).forEach(statement::execute)
            }
        }
    }

    fun authenticate(body: JsonObject, register: Boolean): Map<String, Any> {
        val email = body.text("email", 254).lowercase()
        require(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) { "Email tidak valid." }
        val password = body.get("password")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        require(password.length in 6..120) { "Gunakan kata sandi dengan 6–120 karakter." }
        val account = if (register) {
            val role = body.text("role", 12)
            require(role in setOf("MITRA", "PENGHASIL")) { "Peran tidak valid." }
            val name = body.text("name", 100)
            val salt = ByteArray(16).also(random::nextBytes)
            val hash = passwordHash(password, salt)
            write {
                require(update("INSERT OR IGNORE INTO accounts(email,name,role,salt,password_hash) VALUES (?,?,?,?,?)",
                    email, name, role, salt, hash) == 1) { "Email sudah terdaftar." }
                query("SELECT id,email,name,role FROM accounts WHERE email = ?", email, read = ::account).single()
            }
        } else {
            val result = connect().use {
                it.query("SELECT * FROM accounts WHERE email = ?", email) {
                    Triple(account(this), getBytes("salt"), getBytes("password_hash"))
                }.singleOrNull()
            }
            val candidate = passwordHash(password, result?.second ?: ByteArray(16))
            if (result == null || !MessageDigest.isEqual(candidate, result.third)) {
                throw Problem(401, "Email atau kata sandi tidak sesuai.")
            }
            result.first
        }
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
        write {
            val now = System.currentTimeMillis()
            update("DELETE FROM sessions WHERE expires_at <= ?", now)
            update("INSERT INTO sessions VALUES (?,?,?)", tokenHash(token), account.id, now + 30L * 24 * 60 * 60 * 1000)
        }
        return mapOf("account" to account, "token" to token)
    }

    fun session(token: String): Account = connect().use {
        it.query("""
            SELECT a.id,a.email,a.name,a.role FROM sessions s JOIN accounts a ON a.id=s.account_id
            WHERE s.token_hash=? AND s.expires_at>?
        """, tokenHash(token), System.currentTimeMillis(), read = ::account).singleOrNull()
            ?: throw Problem(401, "Sesi berakhir. Silakan masuk lagi.")
    }

    fun signOut(token: String) = write {
        update("DELETE FROM sessions WHERE token_hash=?", tokenHash(token))
    }

    fun snapshot(actor: Account): Map<String, Any> = write {
        val visible = if (actor.role == "PENGHASIL") "WHERE l.seller_id = ?" else "WHERE ? IS NOT NULL"
        val listings = query("""
            SELECT l.id,l.title,l.price,l.weight,l.location,a.name AS company,l.category,l.condition,l.description
            FROM listings l JOIN accounts a ON a.id=l.seller_id $visible ORDER BY l.id
        """, actor.id, read = ::row)
        val offers = query("""
            SELECT o.listing_id AS listingId,o.price,o.counter_price AS counterPrice,o.status,
            o.buyer_confirmed,o.seller_confirmed,o.updated_at AS updatedAt,a.name AS buyerName
            FROM offers o JOIN listings l ON l.id=o.listing_id JOIN accounts a ON a.id=o.buyer_id
            WHERE o.buyer_id=? OR l.seller_id=?
        """, actor.id, actor.id) {
            val fields = row(this).toMutableMap()
            fields["buyerConfirmed"] = getBoolean("buyer_confirmed")
            fields["sellerConfirmed"] = getBoolean("seller_confirmed")
            fields
        }.associateBy { it.getValue("listingId").toString() }
        val messages = query("""
            SELECT m.listing_id AS listingId,a.role AS author,m.text,m.price,m.sent_at AS sentAt
            FROM messages m JOIN offers o ON o.listing_id=m.listing_id
            JOIN listings l ON l.id=o.listing_id JOIN accounts a ON a.id=m.author_id
            WHERE o.buyer_id=? OR l.seller_id=? ORDER BY m.id
        """, actor.id, actor.id, read = ::row)
        mapOf("listings" to listings, "offers" to offers, "messages" to messages)
    }

    fun change(actor: Account, action: String, body: JsonObject): Map<String, Any> = write {
        if (action == "listings") {
            require(actor.role == "PENGHASIL") { "Hanya penghasil yang dapat membuat listing." }
            val weight = body.text("weight", 30)
            require((weight.toBigDecimalOrNull()?.signum() ?: 0) > 0) { "Berat harus lebih dari nol." }
            update("INSERT INTO listings(seller_id,title,price,weight,location,category,condition,description) VALUES (?,?,?,?,?,?,?,?)",
                actor.id, body.text("title", 160), body.price(), weight, body.text("location", 200),
                body.text("category", 100), body.text("condition", 200), body.text("description", 2000, true))
            return@write mapOf("id" to query("SELECT last_insert_rowid()") { getLong(1) }.single())
        }
        val id = body.integer("listingId")
        val now = System.currentTimeMillis()
        if (action == "submit") {
            require(actor.role == "MITRA") { "Hanya mitra yang dapat mengirim penawaran." }
            require(query("SELECT id FROM listings WHERE id=?", id) { getLong(1) }.isNotEmpty()) { "Listing tidak ditemukan." }
            val owner = query("SELECT buyer_id FROM offers WHERE listing_id=?", id) { getLong(1) }.singleOrNull()
            if (owner == actor.id) return@write mapOf("created" to false)
            if (owner != null) throw Problem(409, "Listing ini sudah memiliki penawaran.")
            val price = body.price()
            update("INSERT INTO offers(listing_id,buyer_id,price,status,updated_at) VALUES (?,?,?,'PENDING',?)",
                id, actor.id, price, now)
            message(id, actor, body.text("message", 1000, true), price, now)
            return@write mapOf("created" to true)
        }
        val offer = query("""
            SELECT o.* FROM offers o JOIN listings l ON l.id=o.listing_id
            WHERE o.listing_id=? AND (o.buyer_id=? OR l.seller_id=?)
        """, id, actor.id, actor.id) {
            val counter = getLong("counter_price")
            val current = if (wasNull()) getLong("price") else counter
            Offer(getString("status"), current, getBoolean("buyer_confirmed"), getBoolean("seller_confirmed"))
        }.singleOrNull() ?: throw Problem(404, "Penawaran tidak tersedia.")
        val buyer = actor.role == "MITRA"
        when (action) {
            "counter" -> {
                require(!buyer && offer.status == "PENDING") { "Penawaran ini sudah ditanggapi." }
                val price = body.price()
                update("UPDATE offers SET counter_price=?,status='COUNTERED',updated_at=? WHERE listing_id=?", price, now, id)
                message(id, actor, body.text("message", 1000, true), price, now)
            }
            "respond" -> {
                require(offer.status == if (buyer) "COUNTERED" else "PENDING") { "Penawaran ini sudah ditanggapi." }
                val accept = body.get("accept")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
                require(accept != null) { "Tanggapan harus dipilih." }
                update("UPDATE offers SET status=?,updated_at=? WHERE listing_id=?",
                    if (accept) "AGREED" else "REJECTED", now, id)
                if (accept) message(id, actor, "Penawaran disetujui.", offer.price, now)
            }
            "send" -> {
                require(offer.status != "REJECTED") { "Penawaran ini sudah ditutup." }
                message(id, actor, body.text("message", 1000), null, now)
            }
            "confirm" -> {
                val alreadyConfirmed = if (buyer) offer.buyer else offer.seller
                if (!alreadyConfirmed) {
                    require(offer.status in setOf("AGREED", "CONFIRMING")) { "Transaksi belum dapat dikonfirmasi." }
                    val field = if (buyer) "buyer_confirmed" else "seller_confirmed"
                    val otherConfirmed = if (buyer) offer.seller else offer.buyer
                    update("UPDATE offers SET $field=1,status=?,updated_at=? WHERE listing_id=?",
                        if (otherConfirmed) "COMPLETE" else "CONFIRMING", now, id)
                }
            }
            else -> throw Problem(404, "Operasi tidak tersedia.")
        }
        mapOf("ok" to true)
    }

    private data class Offer(val status: String, val price: Long, val buyer: Boolean, val seller: Boolean)

    private fun Connection.message(id: Long, actor: Account, text: String, price: Long?, now: Long) {
        update("INSERT INTO messages(listing_id,author_id,text,price,sent_at) VALUES (?,?,?,?,?)",
            id, actor.id, text, price, now)
    }

    private fun connect(): Connection = DriverManager.getConnection(url).also { db ->
        db.createStatement().use {
            it.execute("PRAGMA foreign_keys=ON")
            it.execute("PRAGMA busy_timeout=5000")
        }
    }

    @Synchronized
    private fun <T> write(block: Connection.() -> T): T = connect().use { db ->
        db.autoCommit = false
        try {
            db.block().also { db.commit() }
        } catch (error: Exception) {
            db.rollback()
            throw error
        }
    }
}

private fun passwordHash(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

private fun tokenHash(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())

private fun account(row: ResultSet) = Account(
    row.getLong("id"), row.getString("email"), row.getString("name"), row.getString("role")
)

private fun row(result: ResultSet): Map<String, Any?> = result.metaData.let { metadata ->
    (1..metadata.columnCount).associate { metadata.getColumnLabel(it) to result.getObject(it) }
}

private fun PreparedStatement.bind(args: Array<out Any?>) {
    args.forEachIndexed { index, value -> setObject(index + 1, value) }
}

private fun Connection.update(sql: String, vararg args: Any?) = prepareStatement(sql).use {
    it.bind(args)
    it.executeUpdate()
}

private fun <T> Connection.query(sql: String, vararg args: Any?, read: ResultSet.() -> T): List<T> =
    prepareStatement(sql).use {
        it.bind(args)
        it.executeQuery().use { rows -> buildList { while (rows.next()) add(rows.read()) } }
    }

private fun JsonObject.text(key: String, max: Int, optional: Boolean = false): String {
    val value = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.trim().orEmpty()
    require((optional || value.isNotEmpty()) && value.length <= max) { "Kolom $key tidak valid." }
    return value
}

private fun JsonObject.integer(key: String): Long =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString?.toLongOrNull()
        ?: throw IllegalArgumentException("Kolom $key harus berupa bilangan bulat.")

private fun JsonObject.price() = integer("price").also {
    require(it in 1..999_999_999_999L) { "Masukkan harga yang valid." }
}
