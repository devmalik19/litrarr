One stop solution to all your literature needs.

Comics
Anime
Ebooks
Audiobooks


### Docker command

````
docker run -p 8021:8021 ghcr.io/devmalik19/litrarr:latest
````

### Docker compose

````
version: "3.8"
services:
    litrarr:
        image: ghcr.io/devmalik19/litrarr:latest
        ports:
            - "8021:8021"
        volumes:
            - /path/to/config:/config 
            - /path/to/library:/library #optional
            - /path/to/download-client-downloads:/downloads #optional
        environment:
            # - PORT=8021  # Optional, if you want to change the port.
            # - BASE_URL=/litrarr  # Optional, if you want to run the app under subfolder like domain.com/litrarr (useful for reverse proxy)
            # - USER=user # Optional, if you want to change the default username.
            # - PASSWORD=XXXXXX  # Optional, if you want to change the default password.
            # - SPRING_PROFILES_ACTIVE=mariadb # Optional, if you want to use your own MariaDB database.
            # - DB_URL="jdbc:mariadb://localhost:3306/litrarr" # Optional, this is the default value.
            # - DB_USER=mariadb # Optional, this is the default value.
            # - DB_PASSWORD=mariadb # Optional, this is the default value.
            # - ENCRYPTION_KEY=12345678901234567890123456789012 # Optional, please replace this with a 32 byte random string to enable encryption and decryption of credentials in DB
            # - LOGGING_LEVEL=DEBUG  # Optional
        restart: unless-stopped
````

# TODO
## alpha release :
1. Metadata Search and add to library
2. Search by issues in comics and manga

## Good to have (Not needed) :
1. Files icons and design
2. How to run on local