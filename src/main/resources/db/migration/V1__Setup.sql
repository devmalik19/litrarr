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
    "id" INTEGER PRIMARY KEY,
    "title" TEXT NOT NULL,
    "author" TEXT,
    "issue" TEXT,
    "year" VARCHAR(255) NULL,
   	"category" VARCHAR(255) NULL,
    "status" VARCHAR(255) NULL,
    "library" INTEGER NULL,
    "data" TEXT,
    CONSTRAINT FK_SEARCH_ON_LIBRARY  FOREIGN KEY (library) REFERENCES library (id)
);

CREATE TABLE library (
	"id" INTEGER PRIMARY KEY,
	"guid" VARCHAR(255) NULL,
	"name" VARCHAR(255) NULL,
	"type" VARCHAR(255) NULL,
	"category" VARCHAR(255) NULL,
	"path" VARCHAR(255) NOT NULL UNIQUE,
	"image" VARCHAR(255) NULL,
	"creator" VARCHAR(255) NULL,
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
	"parent" INTEGER NULL,
	CONSTRAINT FK_ITEM_ON_LIBRARY  FOREIGN KEY (parent) REFERENCES library (id)
);

CREATE TABLE library_filter (
	"path" VARCHAR(255) PRIMARY KEY,
	"type" VARCHAR(255) NULL
);

INSERT INTO settings VALUES('priority','{"TORRENT":1,"USENET":2,"slskd":3}');
INSERT INTO settings VALUES('library_paths','{"COMICS":"/Comics","BOOKS":"/Books","MANGA":"/Manga","AUDIOBOOKS":"/Audiobooks"}');