const GLYPHS = {
  KING: "♚",
  QUEEN: "♛",
  ROOK: "♜",
  BISHOP: "♝",
  KNIGHT: "♞",
  PAWN: "♟",
};

const FILES = ["a", "b", "c", "d", "e", "f", "g", "h"];

// board is the 8x8 array from ChessGame JSON: board[row-1][col-1], row 1 = white's back rank
export default function Board({ board, orientation, selected, targets, onSquareClick }) {
  const rows = [];
  const ranks = orientation === "BLACK" ? [1, 2, 3, 4, 5, 6, 7, 8] : [8, 7, 6, 5, 4, 3, 2, 1];
  const cols = orientation === "BLACK" ? [8, 7, 6, 5, 4, 3, 2, 1] : [1, 2, 3, 4, 5, 6, 7, 8];

  for (const row of ranks) {
    const squares = [];
    for (const col of cols) {
      const piece = board?.[row - 1]?.[col - 1];
      const dark = (row + col) % 2 === 0;
      const isSelected = selected && selected.row === row && selected.col === col;
      const isTarget = targets.has(`${row},${col}`);

      squares.push(
        <div
          key={`${row},${col}`}
          className={[
            "square",
            dark ? "dark" : "light",
            isSelected ? "selected" : "",
            isTarget ? (piece ? "capture-target" : "move-target") : "",
          ].join(" ")}
          onClick={() => onSquareClick(row, col)}
        >
          {piece && (
            <span className={piece.pieceColor === "WHITE" ? "piece-white" : "piece-black"}>
              {GLYPHS[piece.type]}
            </span>
          )}
          {col === cols[0] && <span className="coord rank">{row}</span>}
          {row === ranks[ranks.length - 1] && (
            <span className="coord file">{FILES[col - 1]}</span>
          )}
        </div>,
      );
    }
    rows.push(
      <div className="board-row" key={row}>
        {squares}
      </div>,
    );
  }

  return <div className="board">{rows}</div>;
}
