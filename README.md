# 🧊 Smart Fridge Manager (SmortFridge)

> A JavaFX desktop application for managing a smart fridge/pantry — inventory tracking, live sensor readouts, freshness urgency, and an AI-powered recipe generator, all backed by a REST API.
>
> Built as the UI + data layer for a Smart Pantry IoT project (KU Leuven coursework).

<!-- TODO: Add a screenshot of the main "All Food" grid view, and one of the Lazy Susan pie chart — this app has a real UI, show it off -->
<!-- ![SmortFridge main view](./docs/screenshot-main.png) -->

[![Java](https://img.shields.io/badge/Java-JavaFX-blue)]()
[![Maven](https://img.shields.io/badge/build-Maven-red)]()

---

## 📖 Overview

SmortFridge is a JavaFX desktop app that turns a fridge/pantry into something queryable and manageable: what's in it, where it's stored, what's about to expire, what the live sensor readings are, and — via a Gemini-powered recipe generator — what you could actually cook with it right now.

> ⚠️ **Note on the backend:** all data (inventory, categories, storage locations, sensor readings, access-time schedules) is fetched from a REST API hosted at `studev.groept.be`, a KU Leuven course-infrastructure server. **This backend is not publicly accessible**, so cloning and running this repo outside that environment will not connect to real data — the UI and request logic are still fully readable and functional in isolation, but you'd need to point `Connector`'s URLs at your own backend with a matching API shape to actually run it live.

> 🔑 **Note on API keys:** the AI recipe feature calls the Gemini API and expects a key supplied via environment variable rather than hardcoded in source — see [Getting Started](#-getting-started).

> **Setting up your own Gemini key:**
> 1. Get a key from [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
> 2. Set it as an environment variable named `GEMINI_API_KEY` (Windows: `setx GEMINI_API_KEY "your_key_here"`, then reopen your terminal — `setx` doesn't apply to the window it was run in)
> 3. Rebuild/run — if the variable isn't set, the app prints a clear error instead of failing silently

## ✨ Features

- 📋 **Multi-view inventory** — switch between viewing all food by storage location, by category, or by urgency (freshness), each pulled live from the backend
- 🚦 **Color-coded freshness/urgency** — items are visually flagged (ok / warning / expired) based on days remaining, computed per item
- 🌡️ **Live fridge sensor dashboard** — real-time temperature, humidity, and weight readings pulled from hardware sensors via the API
- ⏰ **Diet access-time scheduling** — define named time windows (e.g. "Midnight Snack") controlling when the fridge/pantry should be accessible
- 🎡 **"Lazy Susan" rotating storage control** — an interactive pie-chart UI for a physical rotating storage mechanism, click to set a section to front, double-click to rename
- 🛒 **Shopping list** — add/remove items, synced to the backend
- 🤖 **AI recipe generation** — calls Google's Gemini API to suggest recipes, presumably based on current inventory
- 🗂️ **Category & storage management** — add/edit/delete custom categories (with freshness duration) and storage locations directly from the UI

## 🛠️ Tech Stack

| Layer | Tech |
|---|---|
| Language | Java |
| UI Framework | JavaFX (Controls, FXML, Web, Swing, Media) + ControlsFX, FormsFX, Ikonli, TilesFX |
| Build | Maven (with Maven Wrapper — no local Maven install needed) |
| Backend Communication | Java 11+ `HttpClient`, REST (GET-based), JSON via `org.json` |
| AI Integration | Google GenAI SDK (Gemini) |
| Testing | JUnit 5 (Jupiter) |

## 🏗️ Architecture

The app is deliberately thin on the client side — almost all state lives server-side, and the Java app is a JavaFX front end plus a small HTTP layer:

- **`UI.java`** — the main JavaFX `Application`. Builds every view (inventory grids, category/storage managers, sensor dashboard, Lazy Susan, shopping list, recipe generator) and wires up all REST calls directly. This is the biggest file in the project and the main candidate for future refactoring — splitting it into one class per view would be the natural next step if this evolves further.
- **`Connector.java`** — a minimal wrapper around `HttpClient` for making GET requests to the backend. Deliberately simple; all the interesting logic (URL construction, JSON parsing, UI updates) currently lives in `UI.java` rather than here.
- **`CallingGeminiFromJava.java`** — isolated wrapper around the Gemini API call for the recipe generator, kept separate from the UI/data logic.
- **`HelloController.java` / `hello-view.fxml`** — leftover from the initial JavaFX project template; not part of the active app.

**Honesty note:** this was built solo, fast, under coursework time pressure — the architecture prioritizes "get every feature working" over clean separation of concerns. `UI.java` doing double duty as both view-builder and API-caller for every feature is the clearest example. Fully aware of it, and it's on the list if this project gets revisited.

## 🚀 Getting Started

**Requirements:** JDK 17+ (JavaFX-compatible), no local Maven install needed (Maven Wrapper included)

```bash
# Clone the repo
git clone https://github.com/[your-username]/smortfridge-ui.git
cd smortfridge-ui

# Set your Gemini API key as an environment variable before running
# (never commit a real key directly into the source)
export GEMINI_API_KEY=your_key_here      # macOS/Linux
setx GEMINI_API_KEY "your_key_here"      # Windows

# Run via Maven Wrapper
./mvnw javafx:run       # macOS/Linux
mvnw.cmd javafx:run     # Windows
```

> Note: since the backend is a private university server, most data-fetching features won't return real data outside that network/environment. The AI recipe feature will work independently as long as a valid Gemini key is set.

## 📁 Project Structure

```
smortfridge-ui/
├── src/main/java/ee2/smortfridge/
│   ├── UI.java                    # Main JavaFX application — all views + REST calls
│   ├── Connector.java              # Thin HTTP GET wrapper
│   ├── CallingGeminiFromJava.java  # Gemini API integration for recipe generation
│   └── HelloController.java        # Unused JavaFX template leftover
├── src/main/resources/ee2/smortfridge/
│   ├── StylishFridge.css           # App styling
│   └── hello-view.fxml             # Unused JavaFX template leftover
└── pom.xml                         # Maven build config
```

## 🔗 Related Project

This is the UI + database layer intended to pair with the [Smart Pantry IoT Device](../smart-pantry-iot) hardware project — built as a separate repo since the two can be developed and evaluated independently.

## 🧠 What I Learned

[ 2-4 sentences — e.g. consuming a REST API you don't control end-to-end, integrating a third-party AI API safely (env vars vs. hardcoded keys), building a multi-view JavaFX app, working directly with live sensor data and handling its edge cases (like the negative-weight guard in the code). ]

## 📄 License

MIT — see [`LICENSE`](./LICENSE)

---

<!--
TODO before publishing:
- [ ] Confirm the Gemini key has been fully removed from git history (not just the latest commit — check with `git log -p` or a tool like git-filter-repo if it was ever committed)
- [ ] Add screenshots — this app has a real, colorful UI and currently has zero visuals in the README
- [ ] Fill in "What I Learned"
- [ ] Clean up unused HelloController.java/hello-view.fxml, or note intentionally, before publishing
- [ ] Add a .gitignore for target/ (Maven build output) if not already present
-->
