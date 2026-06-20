import zstandard as zstd
import chess
import chess.pgn
import numpy as np
import io
import sys

PIECE_IDX = {
    chess.PAWN: 0,
    chess.KNIGHT: 1,
    chess.BISHOP: 2,
    chess.ROOK: 3,
    chess.QUEEN: 4,
    chess.KING: 5,
}

def board_to_tensor(board):
    planes = np.zeros((12, 8, 8), dtype=np.float32)
    for sq, piece in board.piece_map().items():
        row, col = sq // 8, sq % 8
        offset = 0 if piece.color == chess.WHITE else 6
        planes[PIECE_IDX[piece.piece_type] + offset][row][col] = 1.0
    return planes

def convert(pgn_zst_path, output_prefix, max_positions=None):
    positions = []
    labels = []
    games = 0
    count = 0

    with open(pgn_zst_path, "rb") as compressed:
        dctx = zstd.ZstdDecompressor()
        with dctx.stream_reader(compressed) as reader:
            text_reader = io.TextIOWrapper(reader, encoding="utf-8")
            while True:
                game = chess.pgn.read_game(text_reader)
                if game is None:
                    break

                games += 1
                board = game.board()

                for node in game.mainline():
                    ev = node.eval()
                    if ev is not None:
                        score = ev.white().score(mate_score=10000)
                        positions.append(board_to_tensor(board))
                        labels.append(np.tanh(score / 400.0))
                        count += 1
                    board.push(node.move)

                if games % 1000 == 0:
                    print(f"Games: {games}, Positions: {count}", flush=True)

                if max_positions is not None and count >= max_positions:
                    break

    print(f"Done. {games} games, {count} positions.")
    np.save(output_prefix + "_positions.npy", np.array(positions, dtype=np.float32))
    np.save(output_prefix + "_labels.npy", np.array(labels, dtype=np.float32))
    print(f"Saved to {output_prefix}_positions.npy and {output_prefix}_labels.npy")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python converter.py <input.pgn.zst> <output_prefix> [max_positions]")
        sys.exit(1)

    pgn_zst_path = sys.argv[1]
    output_prefix = sys.argv[2]
    max_positions = int(sys.argv[3]) if len(sys.argv) > 3 else None

    convert(pgn_zst_path, output_prefix, max_positions)
