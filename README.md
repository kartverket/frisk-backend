# FRISK Backend

Velkommen til FRISK Backend!

Dette repoet inneholder backend-koden til FRISK, som står for FunksjonsRegistreret i Statens Kartverk.
FRISK er en applikasjon utviklet for å gi brukerne mulighet til å opprette et fleksibelt hierarki av vilkårlige funksjoner 
på en oversiktlig og brukervennlig måte. Hver funksjon er lenket til en forelder-funksjon og består av et navn og 
egendefinerte metadata som kan bli konfiguert til å passe et spesifikt bruksområde.  

Følg stegene nedenfor for å komme i gang, og bruk de tilgjengelige skriptene for å administrere prosjektet effektivt.

## Sette opp database lokalt

### Steg 1
Start med å klone repoet fra GitHub.

Med SSH: `git clone git@github.com:kartverket/frisk-backend.git`

Med HTTPS: `git clone https://github.com/kartverket/frisk-backend.git`

### Steg 2
For å sette opp databasen må man ha installert Docker eller tilsvarende (Podman). Dette kan du gjøre ved å kjøre denne kommandoen:

`brew cask install docker`

### Steg 3
Du trenger også et verktøy for håndtering av containere eller et container-runtime miljø som lar deg kjøre containere på din lokale maskin.
Du kan bruker docker desktop dersom du har det. Hvis ikke kan du bruke Colima. Last ned Colima ved å kjøre denne kommandoen:

`brew install colima`.

### Steg 4
Etter å ha installert Colima, kan du starte det opp ved å kjøre denne kommandoen:

`colima start --network-address`

### Steg 5
Når du har Colima eller Docker Desktop kjørende, kjør denne kommandoen i rotmappen til prosjektet:

`docker compose up --build`

Nå skal databasen være oppe og kjøre!


## Kjøre backend lokalt

### Steg 1
Slik kan du sette opp på IntelliJ:
- Gå inn på `Run -> Edit configurations`
- Trykk på + for å legge til ny konfigurasjon og velg KTOR
- Trykk på `modify options` og huk av `use classpath of module`
- Sett `com.kartverket.funksjonsregister.main` som module
- Sett `com.kartverket.ApplicationKt` som main class

### Steg 2
Du trenger å sette følgende miljøvariabler:
```
ALLOWED_CORS_HOSTS
baseUrl
CLIENT_SECRET
clientId
DATABASE_PASSWORD
DATABASE_USERNAME
JDBC_URL
SUPER_USER_GROUP_ID
tenantId
```
For å få tilgang til hemmelighetene, spør noen på teamet om å gi deg tilgang til 1Password vaulten.

Du kan sette miljøvariablene i IntelliJ ved å gå inn på `Run -> Edit configurations`.

### Steg 3
Voila! Du skal nå kunne kjøre backend, gå inn på http://localhost:8080

### Til info
- Kjør `docker compose down --volumes --remove-orphans` for å stoppe Docker Compose.

## Kjøre testene

For å kunne kjøre flere av testene lokalt, så må du ha en fungerende dockerinstallasjon.
I tillegg, avhengig av oppsettet ditt, så er det noen spesifikke miljøvariabler som må settes.
Hvis du bruker colima, sett følgende i .bashrc/.zshrc eller andre tilsvarende konfigurasjonsfiler for ditt shell;
```shell
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=$(colima ls -j | jq -r '.address')
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
```

Merk at det er viktig at colima startes med `--network-address` flagget, da det er trengs for å hente ut adressen til `TESTCONTAINERS_HOST_OVERRIDE`.

Hvis du bruker noe annet, eksempelvis Podman eller Rancher, se dokumentasjonen til testcontainers; 
https://java.testcontainers.org/supported_docker_environment/
