# CustomGroupSystem
---
## 📦 Installation

### 1. Requirements
- Minecraft Server (Ich verwende Paper 1.21)
- Java 17 oder höher
- MySQL 8.0+ oder MariaDB 10.5+

### 2. Installation
1. Plugin-JAR in den `plugins/` Ordner kopieren
2. Server starten (erstellt config.yml)
3. MySQL-Datenbank anlegen
4. Config anpassen (siehe unten)
5. Server neustarten

---

## ⚙️ Konfiguration

### config.yml
```yaml
# MySQL Datenbankverbindung
database:
  host: "localhost"          # MySQL Server Adresse
  port: 3307                 # MySQL Port (Standard: 3306)
  database: "minecraft"      # Datenbankname
  username: "mcuser"         # Datenbank-Benutzer
  password: "mcuser"         # Datenbank-Passwort
  
  # Connection Pool Einstellungen (für Performance)
  pool:
    maximum-pool-size: 10    # Max. gleichzeitige Verbindungen
    minimum-idle: 2          # Min. Verbindungen im Pool
    connection-timeout: 30000 # Timeout in Millisekunden
```

### MySQL Datenbank einrichten
```sql
-- 1. Datenbank erstellen
CREATE DATABASE minecraft;

-- 2. Benutzer erstellen und Rechte geben
CREATE USER 'mcuser'@'localhost' IDENTIFIED BY 'mcuser';
GRANT ALL PRIVILEGES ON minecraft.* TO 'mcuser'@'localhost';
FLUSH PRIVILEGES;

-- Tabellen werden automatisch beim ersten Start erstellt!
```

### messages.yml

Alle Nachrichten können angepasst werden:
```yaml
messages:
  help:
    header: "§e=== Group System Commands ==="
    adduser: "§e/gs adduser <player> <group> [duration]"
  
  adduser:
    success: "§a{player} added to {group}!"
    duration-examples: "§7Duration examples: 4d, 1mo2w, 7d12h, permanent"
  
  # Siehe messages.yml für alle verfügbaren Nachrichten
```

---

## 🎮 Commands

### Übersicht

| Command | Beschreibung | Permission |
|---------|-------------|-----------|
| `/gs creategroup <name> <prefix>` | Gruppe erstellen | `groupsystem.admin.creategroup` |
| `/gs deletegroup <name>` | Gruppe löschen | `groupsystem.admin.deletegroup` |
| `/gs adduser <player> <group> [duration]` | Spieler zu Gruppe hinzufügen | `groupsystem.admin.adduser` |
| `/gs setpermission <group> <permission> <true/false>` | Permission hinzufügen/entfernen | `groupsystem.admin.setpermission` |
| `/gs setperm <group> <permission> <true/false>` | Alias für setpermission | `groupsystem.admin.setpermission` |
| `/gs listpermissions <group>` | Alle Permissions einer Gruppe anzeigen | `groupsystem.admin.listpermissions` |
| `/gs listperms <group>` | Alias für listpermissions | `groupsystem.admin.listpermissions` |
| `/gs playerinfo <player>` | Gruppeninfo anzeigen | `groupsystem.admin.playerinfo` |
| `/gs pinfo <player>` | Alias für playerinfo | `groupsystem.admin.playerinfo` |

---

## 📝 Command Beispiele

### Gruppe erstellen
```bash
# Syntax: /gs creategroup <name> <prefix>
/gs creategroup vip &6[VIP]&r
/gs creategroup admin &c[Admin]&r
/gs creategroup moderator &b[Mod]&r
```
---

### Spieler zu Gruppe hinzufügen

#### Permanent
```bash
# Syntax: /gs adduser <player> <group>
/gs adduser Steve vip
/gs adduser Alice admin

# Spieler hat die Gruppe für immer
```

#### Temporär
```bash
# Syntax: /gs adduser <player> <group> <duration>

# 7 Tage VIP
/gs adduser Steve vip 7d

# 1 Monat + 2 Wochen Admin
/gs adduser Bob admin 1mo2w

# 12 Stunden Moderator
/gs adduser Charlie moderator 12h

# 4 Tage, 7 Minuten, 23 Sekunden (sehr spezifisch)
/gs adduser Dave vip 4d7m23s

# 1 Stunde und 30 Minuten
/gs adduser Eve helper 1h30m
```

**Dauer-Einheiten:**
- `mo` = Monat (30 Tage)
- `w` = Woche
- `d` = Tag
- `h` = Stunde
- `m` = Minute
- `s` = Sekunde

**Kombinierbar:** `1mo2w3d4h5m6s`

---

## 🔐 Berechtigungssystem

### Permission zu Gruppe hinzufügen
```bash
# Syntax: /gs setpermission <group> <permission> true
/gs setpermission vip essentials.fly true
/gs setpermission admin minecraft.command.gamemode true

# Kurze Version:
/gs setperm vip essentials.heal true
```
---

### Permission von Gruppe entfernen
```bash
# Syntax: /gs setpermission <group> <permission> false
/gs setpermission vip essentials.fly false
/gs setperm admin worldedit.* false
```
---

### Alle Permissions einer Gruppe anzeigen
```bash
# Syntax: /gs listpermissions <group>
/gs listpermissions vip

# Ausgabe:
# === Permissions for vip ===
# - essentials.fly
# - essentials.heal
# - essentials.home

```

---

### Wildcard-Permissions

Wildcards geben alle Permissions unter einem bestimmten Präfix:
```bash
# Alle Minecraft Befehle
/gs setperm admin minecraft.command.* true

# Alle Essentials Permissions
/gs setperm vip essentials.* true

# Alle WorldEdit Permissions
/gs setperm builder worldedit.* true

# ALLE Permissions (Vorsicht!)
/gs setperm owner * true

# Alle CustomGroupSystem Admin-Befehle
/gs setperm moderator groupsystem.admin.* true
```
---

### Spieler-Info anzeigen
```bash
# Syntax: /gs playerinfo <player>
/gs playerinfo Steve

# Ausgabe:
# === Player Info: Steve ===
# UUID: 550e8400-e29b-41d4-a716-446655440000
# Group: vip
# Prefix: [VIP] Steve
# Time Remaining: 6 days, 14 hours, 23 minutes
# Expires: 2026-01-17 15:30:00
# Status: Online
```
```bash
# Kurze Version
/gs pinfo Steve
```

---

### Gruppe löschen
```bash
# Syntax: /gs deletegroup <name>
/gs deletegroup vip

# Alle Spieler in dieser Gruppe werden automatisch zu "default"
# Ihre Permissions werden automatisch aktualisiert
```

---

## 🏷️ Schilder mit Platzhaltern

Spieler können auf Schildern andere Spieler mit ihrem Prefix referenzieren:

### Verfügbare Platzhalter

- `%PlayerName%` - Zeigt Prefix + Name (z.B. `[VIP] Steve`)
- `%PlayerName%group%` - Zeigt nur Prefix (z.B. `[VIP] `)

### Beispiele

#### Shop-Schild
```
Zeile 1: Shop by
Zeile 2: %Steve%
Zeile 3: Diamonds
Zeile 4: 50$ each

Ergebnis:
Zeile 1: Shop by
Zeile 2: [VIP] Steve
Zeile 3: Diamonds
Zeile 4: 50$ each
```

#### Team-Schild
```
Zeile 1: Team Alpha
Zeile 2: %Alice%
Zeile 3: %Bob%
Zeile 4: %Charlie%

Ergebnis:
Zeile 1: Team Alpha
Zeile 2: [Admin] Alice
Zeile 3: [Mod] Bob
Zeile 4: [Helper] Charlie
```

#### Prefix-Anzeige
```
Zeile 1: %Steve%group%
Zeile 2: Steve's Shop
Zeile 3: Welcome!
Zeile 4:

Ergebnis:
Zeile 1: [VIP] 
Zeile 2: Steve's Shop
Zeile 3: Welcome!
Zeile 4:
```

**Hinweis:** Schilder werden nicht automatisch aktualisiert wenn sich Gruppen ändern. Sie müssen neu platziert werden.

---

## 🗄️ Datenbank-Struktur

Das Plugin erstellt automatisch 3 Tabellen:

### `group_data`
Speichert alle Gruppen mit ihren Prefixen.

### `player_groups`
Speichert welcher Spieler welche Gruppe hat und wann sie abläuft.

### `group_permissions`
Speichert welche Permissions jede Gruppe hat.

---

### Standard-Gruppen

Beim ersten Start werden automatisch erstellt:
- `default` - Prefix: `&7[Member]`
- `vip` - Prefix: `&6[VIP]`
- `admin` - Prefix: `&c[Admin]`

---

## 🔐 Plugin Permissions

Diese Permissions steuern, wer die Plugin-Befehle verwenden kann:
```yaml
permissions:
  groupsystem.admin.creategroup:
    description: Allows creating new groups
    default: op

  groupsystem.admin.deletegroup:
    description: Allows deleting groups
    default: op

  groupsystem.admin.setpermission:
    description: Allows setting group permissions
    default: op

  groupsystem.admin.listpermissions:
    description: Allows listing group permissions
    default: op

  groupsystem.admin.adduser:
    description: Allows adding users to groups
    default: op

  groupsystem.admin.playerinfo:
    description: Allows checking player information
    default: true  # Jeder kann Spielerinfos sehen
```

---

## ⏱️ Temporäre Gruppen

### Automatisches Ablaufen

- Alle 10 Sekunden prüft das Plugin auf abgelaufene Gruppen
- Spieler werden automatisch zu "default" verschoben
- Permissions werden automatisch aktualisiert
- Keine Benachrichtigung an den Spieler (still)

## 📁 Dateistruktur
```
plugins/
└── CustomGroupSystem/
    ├── config.yml          # Hauptkonfiguration (MySQL)
    └── messages.yml        # Alle Texte (editierbar)
```