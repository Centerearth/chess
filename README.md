# BYU CS 240 Chess

This started as a project for CS 240 but I extended it significantly to be much cleaner and include an adverserial search AI and a pytorch trained model to play against.

Here's an AI generated summary below. 
This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

- **Client**: Command line program used to play chess over the network.
- **Server**: Listens for network requests, manages users and games, and plays the AI's moves.
- **Shared**: Chess rules, game state, and the AI agent — shared between client and server.
- **Web**: React + Vite web client for playing in the browser.

## Web Client

The website lets you register/login, create games, join as white or black, observe, and play
against another player or either AI — all in the browser.

Features:
- Click-to-move or drag-and-drop, with server-computed legal-move highlights and a
  last-move highlight
- Move history in algebraic notation and PGN export (`GET /pgn?gameID=…`)
- Optional chess clocks (pick a time control at game creation; running out of time loses)
- AI difficulty levels (easy/medium/hard) and AI-vs-AI spectator games — the bots only
  play while someone is watching
- Automatic draw detection: stalemate, threefold repetition, the fifty-move rule, and
  insufficient material
- Auto-reconnecting websocket, finished-games filter in the lobby, mobile-friendly layout

**Play (production build served by the server):**

```sh
mvn install -DskipTests        # build the Java modules
mvn -pl server exec:java       # start the server (needs MySQL running)
# then open http://localhost:8080
```

**Develop the web client (hot reload):**

```sh
cd web
npm install
npm run dev                    # Vite dev server on :5173, proxies API + websocket to :8080
```

**Ship a new web build into the server:**

```sh
cd web && npm run deploy       # builds and copies into server/src/main/resources/web
```

The old HTTP endpoint test page is still available at `/api-test.html`.

### How AI games work now

The AI runs on the server. Joining a game with the reserved username slots `ai`
(alpha-beta) or `ml` (neural net) — via the web UI's "New game → Opponent" picker, or
`PUT /game` with `{"gameID": …, "playerColor": "BLACK", "ai": "ai", "aiDifficulty": 3}` —
makes the server compute and play that color's moves automatically after each of your
moves. Difficulty 1–3 maps to search depth (and a larger random-move chance on lower
difficulties). The server also exposes `GET /moves?gameID=…&row=…&col=…` for legal-move
highlighting. The neural-net agent evaluates quiescence positions in batched ONNX
inference calls.

The ONNX model is loaded from `python-agent/chess_eval.onnx` (override with the
`CHESS_ONNX_PATH` environment variable).

## AI Agent

The chess AI supports two modes, selectable per game (web) or switchable at runtime (CLI):

### Alpha-Beta (adversarial search)
- Depth-4 minimax with alpha-beta pruning
- Move ordering (captures first) for better pruning efficiency
- Quiescence search (an additional depth of 5) to resolve tactical sequences at leaf nodes
- Piece-square tables (PSTs) for midgame and endgame positional scoring
- Separate piece values for midgame and endgame phases

### Neural Network (ML mode)
- Convolutional neural network trained on Lichess games with Stockfish evaluations
- Board encoded as a (12, 8, 8) float tensor — 6 piece types × 2 colors
- Depth-3 alpha-beta search using the neural net as the leaf evaluator
- Move ordering (captures first) in the ML search tree
- Quiescence search at leaf nodes using the neural net for static evaluation
- ONNX model exported from PyTorch and loaded via ONNX Runtime in Java

### Training pipeline (`python-agent/`)
- `converter.py` — streams a compressed Lichess PGN (.zst), extracts positions with Stockfish eval annotations, saves as `.npy` tensors
- `model.py` — trains a CNN on the extracted positions using PyTorch (MPS-accelerated on Apple Silicon), exports to `chess_eval.onnx`

**Training commands:**
```sh
# Extract 2M positions from a Lichess PGN
python-agent/venv/bin/python python-agent/converter.py ~/Downloads/lichess_db_standard_rated_2026-05.pgn.zst python-agent/output 2000000

# Train and export the model
python-agent/venv/bin/python python-agent/model.py
```

## Maven Commands

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client                        |
| `mvn -pl server exec:java` | Build and run the server                        |

## Running

```sh
java -jar client/target/client-jar-with-dependencies.jar
```
