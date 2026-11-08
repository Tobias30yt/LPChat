# Lokaler Paper-26.2-Testserver

`launch.bat` doppelklicken. Das Skript findet Java 25+ (hier ist JDK 27 vorhanden),
prueft alle JARs auf Vorhandensein und fragt beim ersten Start nach der
[Minecraft-EULA](https://www.minecraft.net/eula). Erst nach Eingabe von `JA` wird
`eula=true` gespeichert und der Server gestartet.

In Minecraft Java **26.2** verbinden mit **127.0.0.1:25566**. Der Server ist absichtlich
nur auf diesem Computer erreichbar. Anmeldung mit einem Minecraft-Konto bleibt aktiv.
Serverkonsole mit `stop` beenden.

Enthalten: Paper 26.2 Build 135, LPChat 1.3.0, LuckPerms 5.5.71,
PlaceholderAPI 2.12.3 und UltimateTeams (Modrinth 5.1, Dateiname 5.0.4).
Downloads werden gegen die Hersteller-Pruefsummen aus `downloads.json` geprueft.
Beim ersten Start laedt Paper weitere Minecraft-/Plugin-Bibliotheken nach.

## Erneut einrichten

Aus einem frischen Git-Checkout im Projekt `mvn verify` ausfuehren, dann:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File .\test-server\setup.ps1
```

`setup.ps1` installiert die festgelegten Testversionen und kopiert die gebaute
LPChat-JAR. Nur explizit ausfuehren, wenn diese Versionen gewuenscht sind.
Das Setup setzt `max-tnt-per-tick: -1` in der Testserver-`spigot.yml`, damit
Orbital-Ringe gleichzeitig simuliert werden. Diese Einstellung gilt fuer alle TNTs;
LPChat begrenzt seine eigenen Schuesse separat. Setup bei gestopptem Server ausfuehren.
`launch.bat` kopiert keine alte JAR ueber ein automatisch heruntergeladenes Update.
Welten, EULA, Logs und heruntergeladene JARs werden nicht in Git gespeichert.

## Startparameter

Standard: 512 MB Start-RAM, maximal 2 GB. Optional vor dem Start Umgebungsvariablen
`LPCHAT_MEMORY=4G` und `LPCHAT_JAVA=C:\Pfad\zu\java.exe` setzen.
`launch.bat -ValidateOnly` prueft die Startvoraussetzungen ohne EULA-Aenderung und Serverstart.

## Testbefehle

In der Serverkonsole (ohne fuehrenden Slash):

```text
op Tobias30
lp user Tobias30 meta setprefix 100 "&4Supporter"
lp user Tobias30 permission set chatcolor.5 true
```

Im Spiel:

```text
/team create TestTeam
/chatcolor
/namecolor gradient aqua blue
/namecolor flag trans
/namecolor flag gay_men
/namecolor flag lesbian
/ob preview nuke 5
/ob give nuke 5 true
/ob give stab 1 true
```

Danach Chat pruefen, das Team verlassen und erneut schreiben. Team-/Allychat mit
einem zweiten Spieler pruefen. Bei Operatoren ist `chatcolor.*` standardmaessig aktiv;
zum Testen des Fuenf-Farben-Limits den Spieler vorher mit `deop Tobias30` zurueckstufen.

Auto-Updates: LPChat prueft beim Start GitHub. Eine neuere passende Release-JAR wird
bereitgelegt und erst beim darauffolgenden vollstaendigen Serverstart von Paper installiert.

## Orbital-Test

Nur der eingeloggte Spieler **Tobias30** kann die Orbitals bedienen, auch andere
Operatoren nicht. Die Angel wirft einen normalen Haken; Einholen feuert auf dessen
Position. Mit `/ob cancel` alle noch aktiven Orbital-TNTs entfernen.
In einem freien Testgebiet ausprobieren: echte Explosionen veraendern die Welt.
Die Vorschau zeigt nur Partikel und verursacht keinen Schaden.
Bei einem normalen Nuke-Schuss fangen TNTs gebuendelt an und faechern waehrend
des Fallens zu Ringen auf. Stab zeigt eine senkrechte, von oben nach unten
explodierende TNT-Saeule. Vanilla-Bedrock bleibt erhalten.

Zum Zugriffstest die Angel einem zweiten Spieler geben und dort Einholen,
`/ob nuke` sowie `/lpchat:orbital nuke` versuchen: alle muessen gesperrt bleiben.
Cooldown (10 Sekunden), Chunkgrenze und `/ob cancel` pruefen.
Ein visueller Vergleich mit cubicmetres Aufnahme muss im Minecraft-Client erfolgen.
