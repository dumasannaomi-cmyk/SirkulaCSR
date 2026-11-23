package com.example.sirkulacsr.trade

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Persistent on-device data for the two prototype roles. Each write is atomic. */
internal class TradeStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "sirkula_trade.db", null, 1) {

    companion object {
        private var shared: TradeStore? = null

        @Synchronized
        fun instance(context: Context): TradeStore =
            shared ?: TradeStore(context).also { shared = it }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE listing (
                id INTEGER PRIMARY KEY, title TEXT NOT NULL, price INTEGER NOT NULL,
                weight TEXT NOT NULL, location TEXT NOT NULL, company TEXT NOT NULL,
                category TEXT NOT NULL, condition TEXT NOT NULL, description TEXT NOT NULL,
                photo TEXT
            )"""
        )
        db.execSQL(
            """CREATE TABLE offer (
                listing_id INTEGER PRIMARY KEY REFERENCES listing(id),
                price INTEGER NOT NULL, counter_price INTEGER, status TEXT NOT NULL,
                buyer_confirmed INTEGER NOT NULL DEFAULT 0,
                seller_confirmed INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE message (
                id INTEGER PRIMARY KEY, listing_id INTEGER NOT NULL REFERENCES listing(id),
                author TEXT NOT NULL, body TEXT NOT NULL, price INTEGER, sent_at INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX message_listing ON message(listing_id)")
        insertListing(db, "Kardus Bekas", 150_000, "100", "Jakarta Timur", "Kardus",
            "Baik, kering", "Kardus bekas kegiatan kantor, sudah dipilah, dan siap disalurkan.", "kardus")
        insertListing(db, "Plastik PET", 135_000, "80", "Jakarta Utara", "Plastik",
            "Bersih, kering", "Botol PET bekas yang telah dipilah dan siap diambil.", "plastik")
        insertListing(db, "Minyak Jelantah", 100_000, "30", "Jakarta Selatan", "Minyak",
            "Tersaring", "Minyak jelantah dikemas dalam jeriken tertutup.", "minyak")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun snapshot(): TradeSnapshot = transaction { db ->
        val listings = db.read("SELECT * FROM listing ORDER BY id") {
            WasteListing(number("id"), string("title"), number("price"), string("weight"),
                string("location"), string("company"), string("category"), string("condition"),
                string("description"), optionalString("photo"))
        }
        val offers = db.read("SELECT * FROM offer", decode = { decodeOffer() })
        val messages = db.read("SELECT * FROM message ORDER BY id") {
            TradeMessage(number("listing_id"), TradeRole.valueOf(string("author")),
                string("body"), optionalNumber("price"), number("sent_at"))
        }
        TradeSnapshot(listings, offers.associateBy(TradeOffer::listingId), messages)
    }

    fun createListing(
        role: TradeRole, title: String, price: Long, weight: String, location: String,
        category: String, condition: String, description: String
    ): Long = transaction { db ->
        validate(role == TradeRole.PENGHASIL, "Hanya penghasil yang dapat membuat listing.")
        validate(title.isNotBlank() && location.isNotBlank() && category.isNotBlank() &&
            condition.isNotBlank() && (weight.toBigDecimalOrNull()?.signum() ?: 0) > 0,
            "Lengkapi informasi listing dan masukkan berat yang valid.")
        validatePrice(price)
        insertListing(db, title.trim(), price, weight.trim(), location.trim(),
            category.trim(), condition.trim(), description.trim())
    }

    fun submit(role: TradeRole, id: Long, price: Long, message: String): Boolean = transaction { db ->
        validate(role == TradeRole.MITRA, "Hanya mitra yang dapat mengirim penawaran.")
        validatePrice(price)
        validate(db.read("SELECT id FROM listing WHERE id = ?", arrayOf(id.toString())) {
            number("id")
        }.isNotEmpty(), "Listing tidak ditemukan.")
        // A previous tap may have committed before the offer form refreshed.
        if (findOffer(db, id) != null) return@transaction false
        val now = System.currentTimeMillis()
        db.insertOrThrow("offer", null, ContentValues().apply {
            put("listing_id", id)
            put("price", price)
            put("status", OfferStatus.PENDING.name)
            put("updated_at", now)
        })
        insertMessage(db, role, id, message, price, now)
        true
    }

    fun counter(role: TradeRole, id: Long, price: Long, message: String) = transaction { db ->
        val offer = requireOffer(db, id)
        validate(role == TradeRole.PENGHASIL && offer.status == OfferStatus.PENDING,
            "Penawaran ini sudah ditanggapi.")
        validatePrice(price)
        val now = System.currentTimeMillis()
        updateOffer(db, id, OfferStatus.COUNTERED, now) { put("counter_price", price) }
        insertMessage(db, role, id, message, price, now)
    }

    fun respond(role: TradeRole, id: Long, accept: Boolean) = transaction { db ->
        val offer = requireOffer(db, id)
        validate(offer.canRespond(role), "Penawaran ini sudah ditanggapi.")
        val now = System.currentTimeMillis()
        val status = if (accept) OfferStatus.AGREED else OfferStatus.REJECTED
        updateOffer(db, id, status, now)
        if (accept) insertMessage(db, role, id, "Baik, kami setuju di ${rupiah(offer.currentPrice)}.", null, now)
    }

    fun send(role: TradeRole, id: Long, message: String) = transaction { db ->
        validate(message.trim().isNotEmpty(), "Masukkan pesan.")
        validate(findOffer(db, id)?.status != OfferStatus.REJECTED, "Penawaran ini sudah ditutup.")
        insertMessage(db, role, id, message, null, System.currentTimeMillis())
    }

    fun confirm(role: TradeRole, id: Long) = transaction { db ->
        val offer = requireOffer(db, id)
        if (offer.confirmed(role)) return@transaction
        val column = if (role == TradeRole.MITRA) "buyer_confirmed" else "seller_confirmed"
        updateOffer(db, id, offer.afterConfirmation(role), System.currentTimeMillis()) { put(column, 1) }
    }

    private fun insertListing(
        db: SQLiteDatabase, title: String, price: Long, weight: String, location: String,
        category: String, condition: String, description: String, photo: String? = null
    ) = db.insertOrThrow("listing", null, ContentValues().apply {
        put("title", title)
        put("price", price)
        put("weight", weight)
        put("location", location)
        put("company", PENGHASIL_NAME)
        put("category", category)
        put("condition", condition)
        put("description", description)
        put("photo", photo)
    })

    private fun updateOffer(
        db: SQLiteDatabase, id: Long, status: OfferStatus, now: Long,
        extra: ContentValues.() -> Unit = {}
    ) {
        db.update("offer", ContentValues().apply {
            put("status", status.name)
            put("updated_at", now)
            extra()
        }, "listing_id = ?", arrayOf(id.toString()))
    }

    private fun insertMessage(
        db: SQLiteDatabase, role: TradeRole, id: Long, text: String, price: Long?, now: Long
    ) {
        validate(text.trim().length <= 1_000, "Pesan maksimal 1.000 karakter.")
        db.insertOrThrow("message", null, ContentValues().apply {
            put("listing_id", id)
            put("author", role.name)
            put("body", text.trim())
            put("price", price)
            put("sent_at", now)
        })
    }

    private fun findOffer(db: SQLiteDatabase, id: Long) =
        db.read("SELECT * FROM offer WHERE listing_id = ?", arrayOf(id.toString())) { decodeOffer() }
            .singleOrNull()

    private fun requireOffer(db: SQLiteDatabase, id: Long) =
        findOffer(db, id) ?: throw IllegalArgumentException("Penawaran tidak ditemukan.")

    private fun Cursor.decodeOffer() = TradeOffer(
        number("listing_id"), number("price"), optionalNumber("counter_price"),
        OfferStatus.valueOf(string("status")), number("buyer_confirmed") != 0L,
        number("seller_confirmed") != 0L, number("updated_at")
    )

    private fun validatePrice(price: Long) =
        validate(price in 1..MAX_TRADE_PRICE, "Masukkan harga yang valid.")

    private fun validate(condition: Boolean, message: String) {
        require(condition) { message }
    }

    private inline fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block(db)
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    private fun <T> SQLiteDatabase.read(
        query: String, args: Array<String>? = null, decode: Cursor.() -> T
    ): List<T> = rawQuery(query, args).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.decode()) }
    }

    private fun Cursor.number(name: String) = getLong(getColumnIndexOrThrow(name))
    private fun Cursor.string(name: String) = getString(getColumnIndexOrThrow(name))
    private fun Cursor.optionalNumber(name: String) =
        getColumnIndexOrThrow(name).let { if (isNull(it)) null else getLong(it) }
    private fun Cursor.optionalString(name: String) =
        getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }
}
