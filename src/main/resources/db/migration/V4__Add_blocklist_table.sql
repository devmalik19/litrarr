CREATE TABLE blocklist (
    "id" INTEGER PRIMARY KEY,
    "search_id" INTEGER NOT NULL,
    "identifier" TEXT NOT NULL,
    "service" VARCHAR(255),
    "blocked_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("search_id") REFERENCES search("id") ON DELETE CASCADE
);
