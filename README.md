# BYU CS 240 Chess

Here's an AI generate summary below. This started as a project for CS 240 but I extended it significantly to be much cleaner and include an adverserial search AI and a pytorch trained model to play against.
This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

- **Client**: Command line program used to play chess over the network. Includes the AI agent.
- **Server**: Listens for network requests, manages users and games.
- **Shared**: Chess rules and game state shared between client and server.

## AI Agent

The chess AI supports two modes, switchable at runtime:

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
