CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('MITRA', 'PENGHASIL')),
    salt BLOB NOT NULL,
    password_hash BLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    token_hash BLOB PRIMARY KEY NOT NULL,
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    expires_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS listings (
    id INTEGER PRIMARY KEY,
    seller_id INTEGER NOT NULL REFERENCES accounts(id),
    title TEXT NOT NULL,
    price INTEGER NOT NULL CHECK (price BETWEEN 1 AND 999999999999),
    weight TEXT NOT NULL,
    location TEXT NOT NULL,
    category TEXT NOT NULL,
    condition TEXT NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS offers (
    listing_id INTEGER PRIMARY KEY REFERENCES listings(id),
    buyer_id INTEGER NOT NULL REFERENCES accounts(id),
    price INTEGER NOT NULL CHECK (price BETWEEN 1 AND 999999999999),
    counter_price INTEGER CHECK (counter_price BETWEEN 1 AND 999999999999),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'COUNTERED', 'AGREED', 'CONFIRMING', 'COMPLETE', 'REJECTED')),
    buyer_confirmed INTEGER NOT NULL DEFAULT 0 CHECK (buyer_confirmed IN (0, 1)),
    seller_confirmed INTEGER NOT NULL DEFAULT 0 CHECK (seller_confirmed IN (0, 1)),
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY,
    listing_id INTEGER NOT NULL REFERENCES offers(listing_id),
    author_id INTEGER NOT NULL REFERENCES accounts(id),
    text TEXT NOT NULL,
    price INTEGER,
    sent_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS listings_seller ON listings(seller_id);
CREATE INDEX IF NOT EXISTS offers_buyer ON offers(buyer_id);
CREATE INDEX IF NOT EXISTS messages_offer ON messages(listing_id, id);
