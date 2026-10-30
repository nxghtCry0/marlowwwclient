# AGENTS.md

## Roster & Lanes

| Agent | Role | Lane / Owned Paths |
| --- | --- | --- |
| `orchestrator` | Orchestrator (Gemini) | `AGENTS.md`, architecture, task decomposition, cross-lane coordination. |
| `frontend` | Frontend / GUI (Claude) | `src/client/java/com/eclipseware/imnotcheatingyouare/client/clickgui/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/gui/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/ui/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/render/`, `src/client/java/xyz/breadloaf/imguimc/`, and GUI tests in `src/test/`. |
| `backend` | Backend / Core & Build (Gemini) | `build.gradle`, `src/client/resources/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/ImnotcheatingyouareClient.java`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/module/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/setting/`, `src/client/java/com/eclipseware/imnotcheatingyouare/client/utils/`, `src/client/java/com/eclipseware/imnotcheatingyouare/mixin/`, and backend/native tests in `src/test/`. |

## Multi-Agent Rules

1. **Stay in lane**: Do not edit files in another agent's lane unless the user explicitly assigns a cross-lane task.
2. **Handoff format**: Put each message to another agent in its own fenced code block preceded by `Written for: <agent>`:

Written for: <agent>
```text
From: <agent>
Repo/Branch: im not cheating you are / mc-1.21.11
Context: <1-2 lines>
Changed / Task: <files and exact requirements>
Untouched: <other lanes left untouched>
Tests: <test command and results>
```

3. **Frail**:
   - Call `frail_recall` before non-trivial work.
   - Post milestone status (`started`, `blocked`, `handed off`, `tests run`, `done`) under `["Status", "<agent>", "<task>"]`.
   - Keep `["Status", "<agent>", "Session"]` updated with session name, session ID, and transcript path.
4. **Code & Tests**:
   - Never include code comments when writing or editing code.
   - Chain expressions cannot be longer than two (`a.b.c` max).
   - Generate unit tests under `src/test/` for every code change.
5. **Commits**:
   - Commit only when the user asks. Never push unless the user asks. Never include AI attribution or `Co-Authored-By:` trailers.

