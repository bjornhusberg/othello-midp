package com.othello;

/**
 * This class holds the Othello game table
 * 
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloTable {

	/**
	 * Invalid movement
	 */
	public static final byte INVALID_MOVE = -1;

	/**
	 * Empty square (no player)
	 */
	public static final byte EMPTY_SQUARE = 0;

	/**
	 * Black player
	 */
	public static final byte BLACK_PLAYER = 1;

	/**
	 * White player
	 */
	public static final byte WHITE_PLAYER = 2;

	/**
	 * OthelloTable width
	 */
	public static final byte TABLE_WIDTH = 8;

	/**
	 * OthelloTable height
	 */
	public static final byte TABLE_HEIGHT = 8;

	/**
	 * OthelloTable history depth
	 */
	private static final byte HISTORY_DEPTH = TABLE_WIDTH * TABLE_HEIGHT;

	/**
	 * The table history
	 */
	private byte[][][] table;

	/**
	 * The score history
	 */
	private byte[][] score;

	/**
	 * Pointer to current table in history
	 */
	private byte currentTable;

	/**
	 * A switch for showing the title pattern
	 */
	private boolean title;

	/**
	 * Constructor for a Othello game table
	 */
	public OthelloTable() {
		score = new byte[HISTORY_DEPTH][Math.max(BLACK_PLAYER, WHITE_PLAYER) + 1];
		table = new byte[HISTORY_DEPTH][TABLE_WIDTH][TABLE_HEIGHT];

		// Insert a title pattern at position 0
		int layout[] = { 12985669, 633733120 };
		for (byte y = 0; y < 8; y++)
			for (byte x = 0; x < 8; x++)
				table[0][x][y] = (layout[y / 4] & Integer.MIN_VALUE >>> x + y
						% 4 * 8) == 0 ? BLACK_PLAYER : WHITE_PLAYER;
		
		currentTable = 1;
	}

	/**
	 * Resets the table for a new game
	 */
	public void startNewGame() {
		currentTable = 1;
		score[currentTable][BLACK_PLAYER] = 2;
		score[currentTable][WHITE_PLAYER] = 2;

		for (byte x = 0; x < TABLE_WIDTH; x++)
			for (byte y = 0; y < TABLE_HEIGHT; y++)
				table[currentTable][x][y] = EMPTY_SQUARE;

		table[currentTable][3][3] = BLACK_PLAYER;
		table[currentTable][3][4] = WHITE_PLAYER;
		table[currentTable][4][3] = WHITE_PLAYER;
		table[currentTable][4][4] = BLACK_PLAYER;

		title = false;
	}
	
	/**
	 * Sets the title display
	 * 
	 * @param title
	 *            Switch for enabling / disabling the title display
	 */
	public void displayTitle(boolean title) {
		this.title = title;
	}

	/**
	 * Returns the player (OthelloTable.BLACK_PLAYER, OthelloTable.WHITE_PLAYER)
	 * or empty square (OthelloTable.EMPTY) given a pair of table coordinates.
	 * 
	 * @param x
	 *            The x-coordinate
	 * @param y
	 *            The y-coordinate
	 * @return The player color of the selected table coordinate
	 */
	public byte getPiece(byte x, byte y) {

		if (x < 0 || x >= TABLE_WIDTH || y < 0 || y >= TABLE_HEIGHT)
			return EMPTY_SQUARE;

		return table[title ? 0 : currentTable][x][y];
	}

	/**
	 * Get the current score for black player
	 * 
	 * @return The score the the black player
	 */
	public byte getBlackScore() {
		return score[currentTable][BLACK_PLAYER];
	}

	/**
	 * Get the current score for white player
	 * 
	 * @return The score the the white player
	 */
	public byte getWhiteScore() {
		return score[currentTable][WHITE_PLAYER];
	}

	/**
	 * Performs a move given a table coordinate and a player
	 * 
	 * @param x
	 *            The x-coordinate of the move
	 * @param y
	 *            The y-coordinate of the move
	 * @param player
	 *            The player color
	 * @return The score or Table.INVALID
	 */
	public byte putPiece(byte x, byte y, byte player) {

		// Check coordinates
		if (x < 0 || x >= TABLE_WIDTH || y < 0 || y >= TABLE_HEIGHT)
			return INVALID_MOVE;

		// Check player color
		if (player != BLACK_PLAYER && player != WHITE_PLAYER)
			return INVALID_MOVE;

		// Check emptiness
		if (table[currentTable][x][y] != EMPTY_SQUARE)
			return INVALID_MOVE;

		// Check history overflow
		if (currentTable == HISTORY_DEPTH - 1)
			return INVALID_MOVE;

		// Create a new move in the history
		for (byte i = 0; i < TABLE_HEIGHT; i++)
			System.arraycopy(table[currentTable][i], 0,
					table[currentTable + 1][i], 0, TABLE_WIDTH);

		// Update the current table pointer
		currentTable++;

		// Set the new player
		table[currentTable][x][y] = player;

		// Verify all eight directions
		byte rx = (byte) (TABLE_WIDTH - x - 1);
		byte ry = (byte) (TABLE_HEIGHT - y - 1);
		byte sum = innerTurn(x, y, player, (byte) 1, (byte) 0, rx);
		sum += innerTurn(x, y, player, (byte) -1, (byte) 0, x);
		sum += innerTurn(x, y, player, (byte) 0, (byte) 1, ry);
		sum += innerTurn(x, y, player, (byte) 0, (byte) -1, y);
		sum += innerTurn(x, y, player, (byte) 1, (byte) 1, (byte) Math.min(rx,
				ry));
		sum += innerTurn(x, y, player, (byte) 1, (byte) -1, (byte) Math.min(rx,
				y));
		sum += innerTurn(x, y, player, (byte) -1, (byte) 1, (byte) Math.min(x,
				ry));
		sum += innerTurn(x, y, player, (byte) -1, (byte) -1, (byte) Math.min(x,
				y));

		// If no pieces was turned the move is invalid
		if (sum == 0) {
			rewind();
			return INVALID_MOVE;
		}

		// Update the score
		int altPlayer = alternatePlayer(player);
		score[currentTable][player] = (byte) (score[currentTable - 1][player]
				+ sum + 1);
		score[currentTable][altPlayer] = (byte) (score[currentTable - 1][altPlayer] - sum);

		// Return the score
		return sum;
	}

	/**
	 * Rewinds this table one move back in history
	 * 
	 * @return True if rewind was successful
	 */
	public boolean rewind() {
		if (currentTable == 1)
			return false;

		// Simply subtract the current table pointer
		currentTable--;
		return true;
	}

	/**
	 * Returns the alternating player, given a player
	 * 
	 * @param player
	 *            The original player
	 * @return The alternating player
	 */
	public static byte alternatePlayer(byte player) {
		switch (player) {
		case BLACK_PLAYER:
			return WHITE_PLAYER;
		case WHITE_PLAYER:
			return BLACK_PLAYER;
		default:
			return EMPTY_SQUARE;
		}
	}

	/**
	 * Checks if a move is possible for the given player
	 * 
	 * @param player
	 *            The player color
	 * @return True if a move is possible
	 */
	public boolean canMove(byte player) {

		// Iterate over the entire table
		for (byte y = 0; y < TABLE_HEIGHT; y++)
			for (byte x = 0; x < TABLE_WIDTH; x++)
				if ((putPiece(x, y, player)) != INVALID_MOVE) {

					// Rewind the move
					rewind();

					// Return true since a possible move was found
					return true;
				}

		// No move found
		return false;
	}

	/**
	 * Private method for simplifying the score check
	 * 
	 * @param x
	 *            The x-coordinate of the move
	 * @param y
	 *            The y-coordinate of the move
	 * @param player
	 *            The player color
	 * @param xd
	 *            The horisontal direction step (1, 0 or -1)
	 * @param yd
	 *            The vertical direction step (1, 0 or -1)
	 * @param steps
	 *            The maximum step counter (due to table edge)
	 * @return The score from turning this row
	 */
	private byte innerTurn(byte x, byte y, byte player, byte xd, byte yd,
			byte steps) {

		// Only allow max steps
		for (byte i = 1; i <= steps; i++) {
			x += xd;
			y += yd;

			// Return score 0 if an empty position was found
			if (table[currentTable][x][y] == EMPTY_SQUARE)
				return 0;

			// Turn all pieces up to this one if own player color was found
			if (table[currentTable][x][y] == player) {
				byte turns = (byte) (i - 1);
				for (i = turns; i > 0; i--) {
					x -= xd;
					y -= yd;
					table[currentTable][x][y] = player;
				}
				return turns;
			}
		}

		// Own player color was never found so no turns could be performed
		return 0;
	}

	/**
	 * Loads a saved table from a byte buffer
	 * 
	 * @param buffer
	 */
	public int load(byte[] buffer, int offset) {

		if (buffer.length - offset < TABLE_WIDTH * TABLE_HEIGHT) {
			startNewGame();
		} else {
			currentTable = 1;
			score[currentTable][WHITE_PLAYER] = 0;
			score[currentTable][BLACK_PLAYER] = 0;
			for (byte x = 0; x < TABLE_WIDTH; x++)
				for (byte y = 0; y < TABLE_HEIGHT; y++) {
					byte piece = buffer[offset++];
					if (piece == WHITE_PLAYER)
						score[currentTable][WHITE_PLAYER]++;
					else if (piece == BLACK_PLAYER)
						score[currentTable][BLACK_PLAYER]++;
					table[currentTable][x][y] = piece;
				}
		}
		title = false;
		return offset;
	}

	/**
	 * Saves a saved table from a byte buffer
	 * 
	 * @return The saved table buffer
	 */
	public int save(byte[] buffer, int offset) {
		if (buffer.length >= offset + TABLE_WIDTH * TABLE_HEIGHT) {
			for (byte x = 0; x < TABLE_WIDTH; x++)
				for (byte y = 0; y < TABLE_HEIGHT; y++)
					buffer[offset++] = table[currentTable][x][y];
		}
		return offset;
	}
}
