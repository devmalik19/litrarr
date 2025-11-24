CREATE TABLE indexes (
    "id" INTEGER PRIMARY KEY,
    "name" VARCHAR(255),
    "enable" BOOLEAN NOT NULL,
    "protocol" VARCHAR(255) NOT NULL,
    "tags" TEXT
);

CREATE TABLE settings (
    "key" VARCHAR(255) NOT NULL,
    "value" VARCHAR(255),
    PRIMARY KEY ("key")
);

CREATE TABLE search (
    "query" TEXT NOT NULL,
    "data" TEXT,
    PRIMARY KEY ("query")
);

CREATE TABLE library (
	"id" INTEGER PRIMARY KEY,
	"guid" VARCHAR(255) NULL,
	"name" VARCHAR(255) NULL,
	"type" VARCHAR(255) NULL,
	"path" VARCHAR(255) NOT NULL UNIQUE,
	"image" VARCHAR(255) NULL,
	"creator" VARCHAR(255) NULL,
	"data" TEXT,
	"parent" INTEGER NULL,
	CONSTRAINT FK_LIBRARY_ON_PARENT  FOREIGN KEY (parent) REFERENCES library (id)
);

CREATE TABLE item (
	"id" INTEGER PRIMARY KEY,
	"guid" VARCHAR(255) NULL,
	"name" VARCHAR(255) NULL,
	"type" VARCHAR(255) NULL,
	"path" VARCHAR(255) NOT NULL UNIQUE,
	"image" VARCHAR(255) NULL,
	"creator" VARCHAR(255) NULL,
	"data" TEXT,
	"parent" INTEGER NULL,
	CONSTRAINT FK_ITEM_ON_LIBRARY  FOREIGN KEY (parent) REFERENCES library (id)
);

CREATE TABLE library_filter (
	"path" VARCHAR(255) PRIMARY KEY,
	"type" VARCHAR(255) NULL
);

INSERT INTO settings VALUES('priority','{"TORRENT":1,"USENET":2,"slskd":3}')