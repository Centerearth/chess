import { useRef, useState } from "react";

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
export default function Board({ board, orientation, selected, targets, lastMove, myColor, onSquareClick }) {
  const boardRef = useRef(null);
  const ghostRef = useRef(null);
  // { row, col, glyph, colorClass } while a piece is being dragged
  const [dragging, setDragging] = useState(null);

  const ranks = orientation === "BLACK" ? [1, 2, 3, 4, 5, 6, 7, 8] : [8, 7, 6, 5, 4, 3, 2, 1];
  const cols = orientation === "BLACK" ? [8, 7, 6, 5, 4, 3, 2, 1] : [1, 2, 3, 4, 5, 6, 7, 8];

  const isLastMoveSquare = (row, col) =>
    lastMove &&
    ((lastMove.startPosition?.row === row && lastMove.startPosition?.col === col) ||
      (lastMove.endPosition?.row === row && lastMove.endPosition?.col === col));

  const moveGhost = (event) => {
    const ghost = ghostRef.current;
    if (ghost) {
      ghost.style.left = `${event.clientX}px`;
      ghost.style.top = `${event.clientY}px`;
    }
  };

  const handlePointerDown = (event, row, col, piece) => {
    onSquareClick(row, col);
    if (!piece || piece.pieceColor !== myColor) return;

    event.preventDefault();
    boardRef.current?.setPointerCapture(event.pointerId);
    setDragging({
      row,
      col,
      glyph: GLYPHS[piece.type],
      colorClass: piece.pieceColor === "WHITE" ? "piece-white" : "piece-black",
    });
    requestAnimationFrame(() => moveGhost(event));
  };

  const handlePointerMove = (event) => {
    if (dragging) moveGhost(event);
  };

  const handlePointerUp = (event) => {
    if (!dragging) return;
    const from = dragging;
    setDragging(null);
    const element = document.elementFromPoint(event.clientX, event.clientY);
    const square = element?.closest?.("[data-row]");
    if (!square) return;
    const row = Number(square.dataset.row);
    const col = Number(square.dataset.col);
    // dropping back on the origin square is just the click that already selected it
    if (row === from.row && col === from.col) return;
    onSquareClick(row, col);
  };

  const rows = [];
  for (const row of ranks) {
    const squares = [];
    for (const col of cols) {
      const piece = board?.[row - 1]?.[col - 1];
      const dark = (row + col) % 2 === 0;
      const isSelected = selected && selected.row === row && selected.col === col;
      const isTarget = targets.has(`${row},${col}`);
      const hideForDrag = dragging && dragging.row === row && dragging.col === col;

      squares.push(
        <div
          key={`${row},${col}`}
          data-row={row}
          data-col={col}
          className={[
            "square",
            dark ? "dark" : "light",
            isSelected ? "selected" : "",
            isLastMoveSquare(row, col) ? "last-move" : "",
            isTarget ? (piece ? "capture-target" : "move-target") : "",
          ].join(" ")}
          onPointerDown={(event) => handlePointerDown(event, row, col, piece)}
        >
          {piece && !hideForDrag && (
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

  return (
    <div
      className="board"
      ref={boardRef}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={() => setDragging(null)}
    >
      {rows}
      {dragging && (
        <span ref={ghostRef} className={`drag-ghost ${dragging.colorClass}`}>
          {dragging.glyph}
        </span>
      )}
    </div>
  );
}
