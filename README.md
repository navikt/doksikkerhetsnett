# Doksikkerhetsnett
Sikkerhetsnettet er en skedulert jobb som henter alle ubehandlede journalposter i Joark 
som er eldre enn fem dager og oppretter oppgaver for disse om de ikke allerede har.

Tjenesten er satt til å kjøre klokken 7:00 man-fre.

Man kan spesifisere hvilke temaer man ønsker å fange ved å oppdatere miljøvariabelen "doksikkerhetsnett.lesTemaer" og "doksikkerhetsnett.skrivTemaer".
Temaene er separert med komma. Om en vil ha alle tema skriv "ALLE"

### NB!
Om man gjør endringer på hvilke temaer som man enabler, eller når tjenesten kjøres må denne oppdateres:
https://confluence.adeo.no/display/BOA/doksikkerhetsnett

### Henvendelser
Spørsmål om koden eller prosjektet kan rettes til [Slack-kanalen for \#Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)
