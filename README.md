# LPChat

Autor: **tobias30**

Paper **26.2**, Java **25 oder neuer**, LuckPerms. Team-Anzeige mit
[UltimateTeams](https://modrinth.com/plugin/ultimate-teams) (4.7.5+) und PlaceholderAPI.

```text
(Team) Supporter Tobias30 >>  Yo was geht
```

Das Praefix kommt aus den aktiven LuckPerms-Metadaten, der Spielername ist standardmaessig grau.
`>>` hat die Namensfarbe (bei einem Verlauf die Endfarbe). Danach stehen genau zwei
Leerzeichen vor der Nachricht. Ohne Team entfaellt `(Team) `,
ohne Praefix entfaellt auch dessen Zwischenraum. Die Nachrichtenfarbe ist standardmaessig weiss.

## Installation

1. `target/LPChat-1.1.0.jar` in den `plugins`-Ordner des Servers kopieren (alte LPChat-JAR ersetzen).
2. LuckPerms installieren. Fuer Teams auch UltimateTeams und PlaceholderAPI installieren.
3. Server neu starten. Die Einstellungen entstehen unter `plugins/LPChat/config.yml`.
4. Andere Plugins, die den oeffentlichen Chat formatieren, entsprechend deaktivieren,
   damit nur ein Plugin das Chatformat bestimmt. UltimateTeams-Teamchat kann aktiv bleiben.

LPChat veraendert den Paper-Renderer, sendet keine eigene globale Kopie und respektiert
abgebrochene Chatereignisse sowie deren Empfaenger. Team- und Allychat bleiben bei UltimateTeams.

## Farben und Berechtigungen

- `/chatcolor` (auch `/chatfarbe`): Inventar mit freigeschalteten und gesperrten Farben.
- `/chatcolor yellow`: Farbe direkt auswaehlen; Tab zeigt erlaubte Farben.
- `/chatcolor reset`: Standardfarbe wiederherstellen.
- `chatcolor.use`: Menue/Befehl verwenden; standardmaessig fuer alle.
- `chatcolor.1` bis `chatcolor.16`: Die ersten N Farben aus `color-order` freischalten.
  Die hoechste wirksame Berechtigung gewinnt; Zahlen werden nicht addiert.
- `chatcolor.*`: Alle Farben; standardmaessig fuer Operatoren.

Die ersten fuenf Farben sind weiss, grau, gruen, aqua und gelb. Die Reihenfolge ist
in `config.yml` aenderbar. Ohne Anzahl-Berechtigung bleibt die Standardfarbe verfuegbar.
Ausgewaehlte Farben werden in den persistenten Spielerdaten gespeichert und ueberleben
regulaere Neustarts. Entfaellt eine Berechtigung, gilt sofort die Standardfarbe;
die gespeicherte Auswahl wird erst wieder aktiv, wenn sie erneut erlaubt ist.

Beispiel (auf dem Server ausfuehren):

```text
/lp group supporter meta setprefix 100 "&4Supporter"
/lp group supporter permission set chatcolor.5 true
/lp user Tobias30 parent add supporter
```

Praefixe unterstuetzen standardmaessig `&`, Abschnittszeichen und `&#RRGGBB`.
Fuer MiniMessage-Praefixe `prefix-format: minimessage` einstellen.
Nach Konfigurationsaenderungen den Server neu starten. Spielernachrichten werden
nicht als Farbcodes oder MiniMessage interpretiert.

## Namensfarben und Farbverlaeufe

- `/namecolor`: Menue fuer eine einfarbige Namensfarbe; mit Schaltflaeche fuer Verlaeufe.
- `/namecolor red`: Eine Farbe direkt auswaehlen.
- `/namecolor gradient`: Zuerst Startfarbe, dann Endfarbe im Menue auswaehlen.
- `/namecolor gradient aqua blue`: Verlauf direkt setzen.
- `/namecolor gradient #ff5500 #55aaff`: Eigene Hex-Farben verwenden.
- `/namecolor reset`: Zum grauen Standardnamen zurueckkehren.

`namecolor.use` und `namecolor.gradient` sind standardmaessig fuer alle Spieler erlaubt.
Sie koennen ueber LuckPerms eingeschraenkt werden. Namensfarben sind unabhaengig von
den `chatcolor.N`-Freischaltungen. Name und `>>` verwenden die Namensfarbe,
die Nachricht verwendet weiterhin die mit `/chatcolor` gewaehlte Farbe.
Die Namensfarbe wird in den persistenten Spielerdaten gespeichert. Bei Entzug der
benoetigten Berechtigung erscheint der Name wieder grau.

## UltimateTeams

Die [offiziellen Platzhalter](https://ultimateteams.gitbook.io/ultimateteams-docs/hooks)
werden durch PlaceholderAPI aufgeloest: `%ultimateteams_is_in_team%` und
`%ultimateteams_team_name%`. UltimateTeams liefert die Expansion selbst mit.
Unaufgeloeste Platzhalter werden nicht angezeigt. Die Platzhalter sind konfigurierbar.
Falls eine aeltere Expansion den Mitgliedschafts-Platzhalter nicht kennt, den
Kein-Team-Text aus UltimateTeams unter `teams.empty-values` eintragen. Bei bestaetigter
Teammitgliedschaft werden echte Teams mit Namen wie `None` oder `Kein Team` angezeigt.
Fehlt die Expansion trotz aktivem PlaceholderAPI, wird ein nicht aufgeloester Teamname
ausgeblendet und einmalig im Serverlog gemeldet.

Die Integrationstests verwenden die unveraenderte, auf Modrinth als **5.1** veroeffentlichte
UltimateTeams-JAR (Versions-ID `bWbGjJKk`, Dateiname `UltimateTeams-5.0.4.jar`).
Dabei laufen die echte PlaceholderAPI-Ersetzung, die echte UltimateTeams-Expansion und
der echte Team-/Allychat-Listener. Server, Spieler und Teamspeicher sind simuliert;
das ist kein vollstaendiger Live-Servertest. UltimateTeams wird nur fuer Tests geladen
und nicht in die LPChat-JAR eingebaut.

## Attribute-Swapping

Ab Version 1.0.1 aktiviert LPChat Attribute-Swapping beim Start serverweit. Das gilt
auch beim Update mit einer bestehenden LPChat-Konfiguration ohne den neuen Eintrag.
Zum Abschalten der LPChat-Anpassung in `plugins/LPChat/config.yml` eintragen:

```yaml
attribute-swapping:
  enabled: false
```

Danach den Server vollstaendig neu starten. `false` laesst Papers eigene Einstellung gelten.
Standardmaessig ist die Funktion mit `enabled: true` aktiv.

LPChat setzt die [Paper-Einstellung `unsupported-settings.update-equipment-on-player-actions`](https://docs.papermc.io/paper/reference/global-configuration/#unsupported-settings_update-equipment-on-player-actions)
im Speicher auf `false`. Die Paper-Datei wird nicht veraendert. Beim Deaktivieren von
LPChat wird eine von LPChat vorgenommene Aenderung am bisherigen Konfigurationsobjekt
zurueckgenommen. Nach einem Paper-Konfigurationsreload kann die Einstellung wieder
Papers Dateiwert entsprechen; zum erneuten Aktivieren den Server neu starten.

Der Zugriff erfolgt ueber Papers interne Konfigurationsklasse und ist fuer Paper 26.2
ausgelegt. Falls die interne Einstellung nicht verfuegbar ist, meldet LPChat das im Log;
die Chatfunktionen laufen weiter. Es werden keine zusaetzlichen Schadenswerte oder
Cooldowns simuliert. Andere Kampf- oder Anti-Cheat-Plugins koennen Swaps weiterhin beeinflussen.

## Bauen und pruefen

Mit JDK 25+ und Maven:

```text
mvn clean verify
```

Automatische Tests pruefen Farblimits, Rechteverlust, Chatformat, Farbisolation,
unveraenderten Nachrichtentext, Namensfarben/Verlaeufe, UltimateTeams sowie das
Setzen/Zuruecknehmen der Paper-Einstellung an Test-Konfigurationsobjekten.
Fuer den abschliessenden Servertest:

1. Mit und ohne Team schreiben; Praefix, Farben und `>>` mit zwei Leerzeichen kontrollieren.
2. `chatcolor.5` geben: Die ersten fuenf Farben sind waehlbar, `gold` ist gesperrt.
3. Farbe waehlen, aus-/einloggen und Server neustarten; Auswahl bleibt erhalten.
4. Berechtigung entfernen: Folgende Nachrichten nutzen die Standardfarbe.
5. Im Menue Shift-Klick, Zahlentasten, Doppelklick und Ziehen pruefen: keine Gegenstaende entnehmbar.
6. UltimateTeams-Team-/Allychat pruefen: keine Nachrichten bei unbeteiligten Spielern.
7. Startmeldung `Attribute-Swapping aktiviert` pruefen und Waffen-Swaps im Spiel testen.
8. `attribute-swapping.enabled: false` setzen und neu starten: Papers eigene Einstellung gilt.
9. `/namecolor` und `/namecolor gradient` durchklicken; Farbe nach Aus-/Einloggen und Neustart pruefen.
10. Namensfarbe/Verlauf, `>>` und unabhaengige Nachrichtenfarbe gemeinsam pruefen.
