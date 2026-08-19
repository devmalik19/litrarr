# Litrarr

One stop solution to all your literature needs.

- Comics
- Manga
- Ebooks
- Audiobooks

## Requirements

- Java 25 (JDK 25)
- Maven (or use the included `mvnw` wrapper)

## Running locally

```bash
./mvnw spring-boot:run
```

The app starts on port `3500` by default: http://localhost:3500

## Building

```bash
./mvnw package -DskipTests
```

This produces a runnable jar at `target/litrarr-0.0.1-SNAPSHOT.jar`.

## Docker

### Docker command

```
docker run -p 3500:3500 ghcr.io/devmalik19/litrarr:latest
```

### Docker compose

```yaml
version: "3.8"
services:
    litrarr:
        image: ghcr.io/devmalik19/litrarr:latest
        ports:
            - "3500:3500"
        volumes:
            - /path/to/config:/config 
            - /path/to/library:/library #optional
            - /path/to/download-client-downloads:/downloads #optional
        environment:
            # - PORT=3500  # Optional, if you want to change the port.
            # - BASE_URL=/litrarr  # Optional, if you want to run the app under subfolder like domain.com/litrarr (useful for reverse proxy)
            # - USER=user # Optional, if you want to change the default username.
            # - PASSWORD=XXXXXX  # Optional, if you want to change the default password.
            # - GOOGLE_BOOKS_API_KEY=your-key # Optional, Google Books API key for metadata search
            # - COMIC_VINE_API_KEY=your-key # Optional, ComicVine API key for comic metadata search
            # - LOGGING_LEVEL=DEBUG  # Optional
        restart: unless-stopped
```

## Tech Stack

- Spring Boot 4.0
- Java 25
- Thymeleaf (server-side rendering)
- SQLite (default)
- Flyway (database migrations)
- Caffeine (caching)

## Integrations

- **Prowlarr** — indexer management and search
- **qBittorrent** — torrent download client
- **SABnzbd** — usenet download client
- **Slskd** — P2P (Soulseek) search and download
- **Google Books API** — book metadata

## Supported File Types

`CBZ`, `CBR`, `EPUB`, `PDF`, `MOBI`, `AZW3`, `MP3`, `M4B`, `FLAC`, `OGG`

## TODO

### Alpha release
1. Metadata search and add to library
2. Search by issues in comics and manga

### Good to have
1. File icons and design
2. ComicVine metadata integration
3. MyAnimeList metadata integration
