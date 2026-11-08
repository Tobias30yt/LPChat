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

1. `target/LPChat-1.3.1.jar` in den `plugins`-Ordner des Servers kopieren (alte LPChat-JAR ersetzen).
2. LuckPerms installieren. Fuer Teams auch UltimateTeams und PlaceholderAPI installieren.
3. Server neu starten. Die Einstellungen entstehen unter `plugins/LPChat/config.yml`.
4. Andere Plugins, die den oeffentlichen Chat formatieren, entsprechend deaktivieren,
   damit nur ein Plugin das Chatformat bestimmt. UltimateTeams-Teamchat kann aktiv bleiben.

LPChat veraendert den Paper-Renderer, sendet keine eigene globale Kopie und respektiert
abgebrochene Chatereignisse sowie deren Empfaenger. Team- und Allychat bleiben bei UltimateTeams.

## Automatische GitHub-Updates

Ab Version 1.2.0 prueft LPChat **bei jedem Serverstart im Hintergrund** das neueste
stabile [GitHub-Release](https://github.com/Tobias30yt/LPChat/releases/latest).
Neue Releases werden heruntergeladen und im von Paper konfigurierten Updateordner
(normalerweise `plugins/update`) bereitgestellt. **Paper installiert das Update beim
darauffolgenden Serverstart.** Es gibt keinen automatischen Neustart oder Hot-Reload.

Version 1.2.0 muss einmal manuell installiert werden; aeltere LPChat-Versionen haben
noch keinen Updater. Bestehende Konfigurationen funktionieren weiter, Auto-Updates
sind standardmaessig aktiv. Zum Abschalten in `plugins/LPChat/config.yml` eintragen:

```yaml
auto-update:
  enabled: false
```

Nach einem Neustart erfolgen dann keine Update-Anfragen mehr. Bereits heruntergeladene
Updates im Paper-Updateordner werden dadurch nicht entfernt; Paper verarbeitet sie
vor dem Start des Plugins. Bei Bedarf die bereitgelegte LPChat-JAR dort vor dem Neustart entfernen.

Die Quelle ist fest `Tobias30yt/LPChat`. Ein GitHub-Token oder Git-Zugang ist nicht
erforderlich. Synchronisiert werden veroeffentlichte Release-JARs, keine unveroeffentlichten
Commits oder lokalen Einstellungen. Vorabversionen und Downgrades werden ignoriert.
Downloadgroesse, GitHub-SHA256, Plugin-Identitaet, Release-Version, Mindest-Java und
Minecraft-API werden geprueft. Ein unvollstaendiger Kandidat bleibt ausserhalb der
von Paper gelesenen Update-Dateien. Erst nach erfolgreicher Pruefung wird die JAR
atomar bereitgestellt. Bei Netzwerk-/Prueffehlern laeuft die installierte Version weiter.

Wenn das neueste Release eine neuere Minecraft-/Java-Version voraussetzt, wird es
nicht installiert. Netzwerkzugriff auf GitHub und dessen Release-Downloadserver sowie
ein aktivierter, beschreibbarer [Paper-Updateordner](https://docs.papermc.io/paper/reference/bukkit-configuration/#settings_update-folder)
sind erforderlich. Das Ergebnis steht im Serverlog.

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

## LGBTQIA+-Flaggenfarben

Im `/namecolor`-Menue gibt es jetzt **Pride-Flaggen** mit Vorschau. Alternativ:

```text
/namecolor flag
/namecolor flag trans
/namecolor flag gay_men
/namecolor flag lesbian
```

Auch `/namecolor trans`, `/namecolor gay` und `/namecolor lesbian` funktionieren.
Weitere Paletten: `rainbow`, `bisexual`, `pansexual`, `nonbinary`, `asexual`,
`aromantic`, `agender`, `genderfluid`, `genderqueer` und `intersex`.

Die Streifenfarben laufen von links nach rechts ueber die Buchstaben. `>>` hat die
Farbe des letzten Buchstabens. Bei sehr kurzen Namen koennen nicht alle Streifen
sichtbar sein. Intersex nutzt eine gelb-violett-gelbe Farbpalette; ein Kreis laesst
sich in einer Textzeile nicht nachbilden. Die Paletten orientieren sich an den
[beschriebenen Pride-Flaggen](https://rcsgd.sa.ucsb.edu/resources/lgbtqia-informational-resources/pride-flags-glossary).

`namecolor.flags` ist standardmaessig fuer alle erlaubt, zusaetzlich zu `namecolor.use`.
Es ist unabhaengig von `namecolor.gradient` und `chatcolor.N`. Die Auswahl wird wie
andere Namensfarben gespeichert und mit `/namecolor reset` zurueckgesetzt.

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

## Orbitals (ab 1.3.1)

Nur Tobias30: Eine normale Angel auswerfen und einholen. Normales Auswerfen waehlt
Nuke, Schleichen beim Auswerfen waehlt Stab. Andere Spieler angeln unveraendert.
Die Angel zerbricht beim erfolgreichen Ausloesen, auch im Kreativmodus.
Beim Einholen wird der erste feste Block in Blickrichtung als Ziel festgelegt
(maximal 128 Bloecke); ohne Block kein Schuss und kein Verbrauch.
Stab erscheint nach 60 Serverticks (3 Sekunden bei 20 TPS) als senkrechte TNT-Saeule.
Alle TNTs dieser Saeule explodieren gemeinsam im ersten Tick nach dem Erscheinen.
Nuke faechert seine Ringe ueber dem anvisierten Block auf.

Keine Orbital-Befehle, keine Orbital-Tab-Vorschlaege, kein Cooldown und keine
Orbital-Einstellungen in LPChats config.yml. Alte Orbital-Eintraege werden beim
Start entfernt. Zwei gleichzeitig aktive/vorgemerkte Schuesse bleiben als
Mengenlimit bestehen; dies ist keine zeitliche Abklingzeit.
Die technische Paper-Einstellung max-tnt-per-tick bleibt im Testserver auf -1,
damit echte TNT-Entities gleichzeitig simuliert werden.
Eigene Umsetzung, kein Original-Port von OrbitalStrike+. Bedrock bleibt erhalten.
Ein bildgenauer Vergleich mit cubicmetre im Minecraft-Client steht aus.

## Bauen und pruefen

Ein lokaler Windows-Testserver fuer Paper 26.2 ist unter [`test-server`](test-server/README.md)
vorbereitet. Start ueber `test-server/launch.bat`, Verbindung mit `127.0.0.1:25566`.

Mit JDK 25+ und Maven:

```text
mvn clean verify
```

Optionaler echter GitHub-Downloadtest (nur temporaerer Testordner, keine Installation):

```text
mvn -Dlpchat.liveUpdateTest=true verify
```

Automatische Tests pruefen Farblimits, Rechteverlust, Chatformat, Farbisolation,
unveraenderten Nachrichtentext, Namensfarben/Verlaeufe/Flaggen, GitHub-Updates, UltimateTeams sowie das
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
11. Flaggenmenue, gespeicherte Flaggen und die Berechtigung `namecolor.flags` pruefen.
12. Auf einem Testserver ein Update bereitstellen lassen, vollstaendig neu starten
    und die geladene LPChat-Version kontrollieren. Dieser Neustart-Test ist noch offen.
