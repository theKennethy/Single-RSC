<img width="1200" height="800" alt="rsmap" src="https://github.com/user-attachments/assets/6a91d2ac-2c7c-4ce1-a59c-4e7167c85e19" />

# RSC Single Player

RSC Single Player is a standalone RuneScape Classic reproduction and sandbox: a fully self-contained client that runs locally with no separate server process or database required. It is aimed at preservation, experimentation, and nostalgic single‑player exploration of (nearly) the full original game content.

---

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Gameplay Notes](#gameplay-notes)
- [Commands & Utilities](#commands--utilities)
- [Hardcore Mode](#hardcore-mode)
- [Experience Rates](#experience-rates)
- [Skill Batching System](#skill-batching-system)
- [Development / Building](#development--building)
- [Saving & Data](#saving--data)
- [Administrator Account](#administrator-account)
- [Planned / Possible Enhancements](#planned--possible-enhancements)
- [Media](#media)
- [FAQ](#faq)
- [Disclaimer](#disclaimer)

---

## Features

Core:
- 100% single-process design (no external DB/server setup)
- All core content including all 50 quests
- Batched skill actions (configurable logic per skill/tool) similar in spirit to OpenRSC’s cabbage batching
- Fully fixed UI with window resizing support (choose preferred dimensions)
- Dynamic login screen
- Multi-account capable (see notes below)
- If you want to be not hardcore make sure to actually click on new account and make sure to not check hardcore.
- If you bypass the create account and just login it may not do what you want. 

Quality of Life:
- Bank accessible anywhere via `::bank`
- Teleport utility via `::tele <area>`
- Item swapping in bank via right-click
- Updated save handling for cross‑platform Java 8+ environments
- Flexible experience modifier (1x–50x)
- Hardcore mode (save deletion on death; see below)

Recent Shop Update:
- Bob's Axes now stocks ONLY woodcutting hatchets: Bronze, Iron, Steel, Rune (Mithril/Adamant deliberately omitted)
- Bob’s also stocks Bronze through Rune pickaxes
- Battle axes intentionally excluded from Bob’s inventory

---

## Requirements
- Java 8 or newer (recommended: latest LTS JDK)
- Disk write permission in the working directory (for saves)

---

## Quick Start
1. Download or build the project (see [Development / Building](#development--building) if compiling).
2. Launch:
   - Double-click `rsc.jar`, or
   - Windows: `run.bat`
   - Unix/Linux/macOS: `./run.sh`
3. Click `New User` and create an account.
4. Log in and play.

Tip: To test administrator shortcuts, create a user named exactly `root` (case sensitive).

---

## Gameplay Notes
- You may create multiple accounts, but avoid actively playing more than one simultaneously in the same session. (Multiple windows are allowed; only use one actively to avoid state anomalies.)
- Woodcutting uses the hatchet items; battle axes are purely combat weapons (and not sold in Bob’s shop).
- Hardcore mode can be toggled; death in Hardcore will delete the save and close the client.

---

## Commands & Utilities
| Command | Description |
|---------|-------------|
| `::bank` | Opens the bank interface anywhere. |
| `::tele <area>` | Teleports to a predefined area alias (list depends on internal config). |
| (Admin/root) Mini-map right-click | Teleport utility for quick navigation. |

(If you would like a published list of `::tele` area codes, we can add one—just ask.)

---

## Hardcore Mode
- Enable/toggle via in-game option (once enabled, death enforcement applies).
- On death: character save file is removed; client shuts down.
- Logging back in starts a fresh profile.

---

## Experience Rates
- Global modifier currently: 1x–50x (configured internally).
- If you want a per-skill or dynamic scaling system, that can be added later.

---

## Skill Batching System
The batching system centralizes repeated actions (e.g., woodcutting swings, mining strikes, cooking batches) using a `BatchEvent` scheduler.

Pattern (simplified):
```java
player.setBatchEvent(new BatchEvent(player, delayMs, repeatCount) {
    @Override
    public void action() {
        // Perform one attempt
        // Stop early with interrupt() if resource depleted, inv full, fatigue maxed, etc.
    }
});
```

Guidelines:
- Use `Formulae.getRepeatTimes(player, <skill>)` when level-scaling is desired.
- Do NOT recursively call actions; let timers pace attempts.
- Place validation (fatigue, inventory space, tool checks) inside each `action()` iteration.

Current advantages:
- Consistent pacing across skills
- Clear interrupt semantics
- Prevents runaway loops or CPU spikes from rapid re-queuing

---

## Development / Building
If you want to modify or extend the code:

1. Clone:
   ```bash
   git clone https://github.com/kenyyhy/Single-RSC.git
   cd Single-RSC
   ```
2. Ensure Java 8+ is on PATH:
   ```bash
   java -version
   ```
3. (If using a build tool) Import into your IDE (IntelliJ recommended).  
4. Build (example using `javac`, adjust for your structure if a build script is present):
   ```bash
   javac -d out $(find src -name "*.java")
   ```
5. Package into a jar (if not already):
   ```bash
   jar --create --file rsc.jar -C out .
   ```

(If you’d like a Gradle or Maven build file, that can be introduced.)

---

## Saving & Data
- Saves are written locally (platform-agnostic handling improved).
- If you migrate machines, copy the save directory (to be documented if needed).
- Hardcore deletion events are irreversible unless you manually back up the save file beforehand.

---

## Administrator Account
- Create user: `root`
- Grants admin privileges:
  - Mini-map right-click teleport
  - Additional internal debug or management hooks (if enabled in code)

---

## Planned / Possible Enhancements
(Feel free to open issues or request items be added here.)
- Formal `::tele` alias list documentation
- Optional inclusion of Mithril/Adamant hatchets in Bob’s shop via config flag
- In‑game help panel / command list
- Build automation (Gradle/Maven)
- Cross-save migration utility
- Optional battle axe shop / merchant

---

## Media

![RSCSP1](https://nemotech.org/rsc/rsc-1.png "RSCSP1")
![RSCSP2](https://nemotech.org/rsc/rsc-2.png "RSCSP2")
![RSCSP3](https://nemotech.org/rsc/rsc-3.png "RSCSP3")

---

## FAQ

**Q: Does this connect to any external server?**  
A: No. Everything runs locally.

**Q: Are battle axes usable for Woodcutting?**  
A: No, only hatchets (Bronze/Iron/Steel/Rune in current Bob’s inventory).

**Q: Can I safely resize the window mid-session?**  
A: Yes. UI is dynamic.

**Q: How do I restore a Hardcore character after death?**  
A: Intended behavior is permanent loss; only manual file backups would bypass it.

**Q: Can I change XP rates?**  
A: Not exposed via UI yet—requires code/config modification.

---

## Disclaimer
This project is a preservation / educational single-player reimplementation. All original game assets, names, and concepts belong to their respective owners. Use responsibly and in accordance with applicable laws and terms.

---

## Contributing
If you’d like to propose improvements (e.g., configuration toggles, new batching patterns, bug fixes), feel free to open a pull request or issue. (A CONTRIBUTING.md can be added on request.)

---

## License
GPL v3.0 (see LICENSE or https://www.gnu.org/licenses/gpl-3.0.txt)

---
