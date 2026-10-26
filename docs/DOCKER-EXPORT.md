# Testing on Another Host (LAN & Docker Export)

Two ways to test JCasheesh! Bank outside your dev machine:

1. **LAN access** — run the stack on your Mac and open it from other devices on the same network.
2. **Export** — package the Docker images and run them on another machine (e.g. a Windows 11 PC) without the source code.

---

## 1. LAN access from other devices

Find your Mac's LAN IP:

```bash
ipconfig getifaddr en0        # e.g. 192.168.1.37
```

Start the stack with `APP_BASE_URL` pointing at that IP, so emailed links (e.g. forgot-PIN reset) open correctly on other devices:

```bash
APP_BASE_URL=http://192.168.1.37:8081 docker compose up -d --build
```

The shell variable overrides the value in `.env` for that run only; `.env` stays untouched. Running plain `docker compose up -d` later reverts to `localhost`.

Open `http://<mac-ip>:8081` from any device on the same Wi-Fi/LAN. If it can't connect, allow Docker through the macOS firewall (System Settings → Network → Firewall).

---

## 2. Exporting to another machine (Windows 11 / amd64)

### Check the architecture first

Apple Silicon Macs build **arm64** images. Most Windows PCs and Linux servers are **amd64 (x86_64)** and cannot run arm64 images. Check the target:

| Target | Command | Result |
|---|---|---|
| Windows | `echo $env:PROCESSOR_ARCHITECTURE` (PowerShell) | `AMD64` → build amd64 |
| Linux | `uname -m` | `x86_64` → build amd64; `aarch64` → arm64 works as-is |

The steps below use separate `:amd64` tags so your local arm64 images are not overwritten.

### On the Mac: build the bundle

```bash
cd jcash-bank
mkdir -p ~/jcash-export

# Build the app image for amd64 (slower — runs under emulation)
docker buildx build --platform linux/amd64 -t jcash-bank-app:amd64 --load .

# Fetch amd64 Postgres under its own tag, then restore the local arm64 tag
docker pull --platform linux/amd64 postgres:15-alpine
docker tag postgres:15-alpine jcash-postgres:15-alpine-amd64
docker pull --platform linux/arm64 postgres:15-alpine

# Save both images into one file (~260 MB)
docker save jcash-bank-app:amd64 jcash-postgres:15-alpine-amd64 | gzip > ~/jcash-export/jcash-images.tar.gz

# Credentials (hidden file — press ⌘⇧. in Finder to see it)
cp .env ~/jcash-export/
```

Create `~/jcash-export/docker-compose.yml`. It is the same as the project's compose file but uses `image:` instead of `build: .`:

```yaml
services:
  postgres:
    image: jcash-postgres:15-alpine-amd64
    container_name: jcasheesh-db
    environment:
      TZ: Asia/Manila
      PGTZ: Asia/Manila
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5434:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - jcasheesh-net

  app:
    image: jcash-bank-app:amd64
    container_name: jcasheesh-app
    ports:
      - "8081:8081"
    environment:
      TZ: Asia/Manila
      JAVA_TOOL_OPTIONS: "-Duser.timezone=Asia/Manila"
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      APP_BASE_URL: ${APP_BASE_URL:-http://localhost:8081}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
    depends_on:
      - postgres
    networks:
      - jcasheesh-net

volumes:
  pgdata:

networks:
  jcasheesh-net:
    driver: bridge
```

#### Optional: include existing data

Without this, the target starts with an empty database (tables are created automatically via `ddl-auto=update`).

```bash
set -a; . ./.env; set +a
docker exec jcasheesh-db pg_dump -U "$POSTGRES_USER" --clean --if-exists "$POSTGRES_DB" > ~/jcash-export/db.sql
```

And create `~/jcash-export/import-db.bat` for the Windows side:

```bat
@echo off
REM Imports db.sql into the jcasheesh-db container. Run AFTER `docker compose up -d`.
cd /d "%~dp0"
for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"
docker exec -i jcasheesh-db psql -U %POSTGRES_USER% %POSTGRES_DB% < db.sql
pause
```

A `.bat` is used instead of PowerShell because it can be double-clicked, always runs from its own folder, and `cmd`'s `<` redirect passes the file through byte-for-byte (Windows PowerShell 5.1 can garble non-ASCII characters like `₱` or `ñ` when piping to native commands).

### On Windows 11: load and run

1. Install **Docker Desktop** (WSL 2 backend) and start it.
2. Copy the whole `jcash-export` folder over (USB, shared folder, cloud). Make sure the hidden `.env` comes along.
3. In **PowerShell**, inside that folder:
   ```powershell
   docker load -i jcash-images.tar.gz
   docker compose up -d
   ```
4. *(Optional)* Import data once the app has started (~30 s): double-click `import-db.bat`, or run `.\import-db.bat`.
   If you get *"file does not exist"*, run `dir` — you're likely in the wrong folder, or Windows renamed the file (e.g. `import-db.bat.txt`; extensions are hidden by default).
5. Open http://localhost:8081.

#### Reaching it from other devices on the Windows PC's network

```powershell
ipconfig                                         # note the IPv4 Address
$env:APP_BASE_URL="http://<pc-ip>:8081"; docker compose up -d
```

If Windows Firewall blocks it, open the port from an **admin** PowerShell:

```powershell
New-NetFirewallRule -DisplayName "JCash 8081" -Direction Inbound -Protocol TCP -LocalPort 8081 -Action Allow
```

---

## Security notes

- `.env` holds the database password and Gmail app password; `db.sql` holds account data. Only copy them to machines you trust, and wipe them from USB drives or shared locations afterwards.
- Never commit `.env`, `db.sql`, or the image tarball.
