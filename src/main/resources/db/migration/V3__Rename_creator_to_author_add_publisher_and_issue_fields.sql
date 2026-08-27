-- Rename creator to author and add publisher in library table
ALTER TABLE library RENAME COLUMN "creator" TO "author";
ALTER TABLE library ADD COLUMN "publisher" VARCHAR(255) NULL;
ALTER TABLE library ADD COLUMN "release_on" DATE NULL;

-- Recreate item table: rename creator to author, add publisher/release_on/missing, make path nullable
CREATE TABLE item_new (
	"id" INTEGER PRIMARY KEY,
	"guid" VARCHAR(255) NULL,
	"name" VARCHAR(255) NULL,
	"type" VARCHAR(255) NULL,
	"path" VARCHAR(255) NULL UNIQUE,
	"image" VARCHAR(255) NULL,
	"author" VARCHAR(255) NULL,
	"publisher" VARCHAR(255) NULL,
	"release_on" DATE NULL,
	"missing" BOOLEAN NOT NULL DEFAULT 0,
	"parent" INTEGER NULL,
	CONSTRAINT FK_ITEM_ON_LIBRARY FOREIGN KEY (parent) REFERENCES library (id)
);

INSERT INTO item_new (id, guid, name, type, path, image, author, parent)
SELECT id, guid, name, type, path, image, creator, parent FROM item;

DROP TABLE item;
ALTER TABLE item_new RENAME TO item;
