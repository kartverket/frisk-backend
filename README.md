# FRiSK Backend

FunksjonsRegister i Statens Kartverk

## Kom i gang

Du trenger Docker Desktop eller tilsvarende (Podman) for å kjøre dette prosjektet.

1. Start db-en: Kjør `docker compose up --build` i rotmappen til prosjektet.
2. Start Application.kt
2. Gå til `http://0.0.0.0:8080/health` i nettleseren din.

Voila! Du kjører nå backend lokalt.

Kjør `docker compose down --volumes --remove-orphans` for å stoppe Docker Compose.

## FAQ

### Colima klarer ikke starte db-en med docker-compose.
Hvis du får permission denied på startup:
``sh
colima delete
colima start --vm-type=vz
``
https://github.com/abiosoft/colima/issues/1067
