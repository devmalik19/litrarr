# Litrarr

One stop solution to all your literature needs.

- Comics
- Manga
- Ebooks
- Audiobooks

> Litrarr is in alpha. Expect breaking changes, including to the database schema.

## Features

- Search for books, comics, manga and audiobooks through your indexers
- Send downloads to your download clients
- Scan and browse your library
- Fetch metadata and covers from online sources

## Integrations

- **Prowlarr**: indexer management and search
- **qBittorrent**: torrent download client
- **SABnzbd**: usenet download client
- **Slskd**: P2P (Soulseek) search and download
- **Hardcover**: book metadata (API token required)
- **ComicVine**: comic metadata
- **Audnexus**: audiobook metadata (Audible data, no key needed)
- **MyAnimeList**: manga metadata (client ID required, manga metadata is skipped without it)

## Supported file types

`CBZ`, `CBR`, `EPUB`, `PDF`, `MOBI`, `AZW3`, `MP3`, `M4B`, `FLAC`, `OGG`

## Library layout

Comics and manga:

```
comics/<series>/#<issue number, zero padded>/<issue title>.<ext>
comics/Sample Title/#001/Origins.cbz
comics/Sample Title/#002/#002.cbz      (no title: the file is named after the issue number)
manga/<series>/#<volume number>/<volume>.<ext>
```

Books and audiobooks:

```
books/<author>/<book>/<file>
audiobooks/<author>/<book>/<file>
```

Downloaded comics and manga are moved into this layout automatically. Issue numbers are whole numbers padded to three digits (`#001`).

Downloaded `CBR` files are converted to `CBZ` with [7-Zip](https://www.7-zip.org/), which is already included in the Docker image. For local development install it and make sure `7zz`, `7z` or `7za` is on your PATH, or set `SEVENZIP_PATH`. Without 7-Zip, real RAR files stay as `CBR` (CBR files that are really ZIP files are still renamed). Files already in your library are converted when you press **Convert** under Settings, General.

## Audiobooks
An audiobook download with several `MP3`, `FLAC` or `OGG` files is merged into one `M4B` named after the book, with one chapter per file. Files are merged in natural order (`2` before `10`) and re-encoded to AAC at 64 kbps. This needs [ffmpeg](https://ffmpeg.org/), which is already included in the Docker image. For local development install it and make sure `ffmpeg` and `ffprobe` are on your PATH, or set `FFMPEG_PATH`. Without ffmpeg, or when `CONVERT_AUDIOBOOKS=false`, the files are moved into the book folder unchanged. Existing multi-file audiobooks are merged when you press **Convert** under Settings, General.
## Docker

Images are published to `ghcr.io/devmalik19/litrarr:latest`.

### Docker command

```bash
docker run -p 3800:3800 \
  -v /path/to/config:/config \
  -v /path/to/library:/library \
  -v /path/to/downloads:/downloads \
  ghcr.io/devmalik19/litrarr:latest
```

### Docker compose

```yaml
services:
    litrarr:
        image: ghcr.io/devmalik19/litrarr:latest
        ports:
            - "3800:3800"
        volumes:
            - /path/to/config:/config
            - /path/to/library:/library
            - /path/to/download-client-downloads:/downloads
        environment:
            # - APP_USER=user
            # - APP_PASSWORD=XXXXXX
        restart: unless-stopped
```

### Volumes

| Path         | Purpose                                                              |
|--------------|----------------------------------------------------------------------|
| `/config`    | Database and cache. Required, mount it to keep your data.           |
| `/library`   | Your books, comics, manga and audiobooks.                            |
| `/downloads` | The folder your download client saves to. Must match its own path.   |

## Configuration

All settings are environment variables. Everything is optional.

| Variable               | Default      | Description                                                                          |
|------------------------|--------------|--------------------------------------------------------------------------------------|
| `PORT`                 | `3800`       | HTTP port.                                                                           |
| `BASE_URL`             | `/`          | Sub-path to run under, for example `/litrarr` behind a reverse proxy.                |
| `APP_USER`             | not set      | Login username. Login is enabled only when both `APP_USER` and `APP_PASSWORD` are set. |
| `APP_PASSWORD`         | not set      | Login password.                                                                      |
| `CONFIG_PATH`          | `/config`    | Database and cache folder.                                                           |
| `LIBRARY_PATH`         | `/library`   | Root of your library.                                                                |
| `DOWNLOAD_PATH`        | `/downloads` | Download client folder.                                                              |
| `HARDCOVER_API_KEY`    | empty        | Hardcover API token for book metadata.                                               |
| `COMIC_VINE_API_KEY`   | empty        | ComicVine API key for comic metadata.                                                |
| `MYANIMELIST_API_KEY`  | empty        | MyAnimeList client ID for manga metadata. Manga search is disabled if not set.      |
| `SEVENZIP_PATH`        | empty        | Path to the 7-Zip program. Empty: `7zz`, `7z` and `7za` are looked up on the PATH.   |
| `FFMPEG_PATH`          | empty        | Path to ffmpeg. Empty: `ffmpeg` and `ffprobe` are looked up on the PATH.             |
| `CONVERT_AUDIOBOOKS`   | `true`       | Merge multi-file audiobook downloads into one M4B. Set `false` to keep the files.    |
| `USER_AGENT`           | `Litrarr/1.0`| User agent sent to external services.                                                |
| `LOGGING_LEVEL`        | `INFO`       | Root log level, for example `DEBUG`.                                                 |

Library folders (comics, manga, books, audiobooks) are set in the app under Settings. Defaults are `/comics`, `/manga`, `/books` and `/audiobooks`, relative to `LIBRARY_PATH`.

## Development

### Requirements

- Java 25 (JDK 25)
- Maven, or the included `mvnw` wrapper

### Running locally

```bash
./mvnw spring-boot:run
```

The app starts on http://localhost:3800.

Local settings live in the `.env` file in the project root. Spring Boot loads it automatically, so it works the same from any IDE and from the command line. Real environment variables override it.

The `.env` file turns on the `dev` profile, which on startup:

- creates the `data/` folder (`config`, `downloads`, `library`) if it does not exist,
- adds a few empty sample files to the library so you have something to scan,
- turns off template caching, so HTML changes show up on refresh.

`data/` is ignored by git. To start fresh, delete it.

### Tests

```bash
./mvnw test
```

### Building

```bash
./mvnw package -DskipTests
```

This produces a runnable jar at `target/litrarr-0.0.1-SNAPSHOT.jar`.

### Database

Litrarr uses SQLite and Flyway. Migrations are in `src/main/resources/db/migration`. While the project is in alpha, the schema is kept in a single `V1__Setup.sql`. After pulling a schema change, delete `data/config/litrarr.db`.

## Tech stack

- Spring Boot 4.0
- Java 25
- Thymeleaf (server-side rendering)
- SQLite
- Flyway (database migrations)
- Caffeine (caching)

## Roadmap

### Alpha release

1. Metadata search and add to library
2. Search by issues in comics and manga

### Good to have

1. File icons and design
2. Finish ComicVine, Hardcover and MyAnimeList metadata integration
