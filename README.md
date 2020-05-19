Doksikkerhetsnett
================

Sikkerhetsnett for Joark 

Sikkerhetsnettet er en skedulert jobb som henter alle ubehandlede journalposter 
som er eldre enn en uke og oppretter oppgaver for disse om de ikke allerede har.

Tjenesten er nå satt til å kjøre klokken 7:00 mandag, onsdag og fredag. Dette kan endres i no.nav.doksikkerhetsnett.scheduler.DoksikkerhetsnettScheduled

Man kan spesifisere hvilke temaer man ønsker å fange ved å oppdatere miljøvariabelen "doksikkerhetsnett.lesTemaer" og "doksikkerhetsnett.skrivTemaer".
Temaene er separert med komma. Om en vil ha alle tema skriv "ALLE"

# NB!
Om man gjør endringer på hvilke temaer som man enabler, eller når tjenesten kjøres må denne oppdateres:
https://confluence.adeo.no/display/BOA/doksikkerhetsnett

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* Applikasjonsansvarlig Ida Stenerud
* Hovedutviklere Marton Skjæveland & Joakim Borgersen

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team_dok_komponenter.
