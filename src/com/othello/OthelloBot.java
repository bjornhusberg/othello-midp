package com.othello;

import java.util.Random;

/**
 * This class plays Othello
 * 
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloBot {

	/**
	 * The minimum time to make move
	 */
	private static final long MINIMUM_MOVE_TIME = 500;

	/**
	 * The fixed maximum level
	 */
	public static final byte MAX_LEVEL = 5;

	/**
	 * The fixed minimum level
	 */
	public static final byte MIN_LEVEL = 1;

	/**
	 * The table which is beeing played
	 */
	private OthelloTable table;

	/**
	 * The game receiving moves etc.
	 */
	private OthelloGame game;

	/**
	 * The difficulty of this player
	 */
	private byte recursionDepth;

	/**
	 * The color of this player
	 */
	private byte player;

	/**
	 * Indicates that the bot is dying
	 */
	private boolean dying;

	/**
	 * The randomizer object
	 */
	private Random random;

	/**
	 * Creates an instance of an othello robot
	 * 
	 * @param game
	 *            The Othello game that will receive moves when calculated
	 * @param table
	 *            The table that is beeing played
	 * @param player
	 *            The color of this player
	 * @param level
	 *            The difficulty level of this player (MIN_LEVEL <= level <=
	 *            MAX_LEVEL)
	 */
	public OthelloBot(OthelloGame game, OthelloTable table, byte player,
			byte level) {
		this.game = game;
		this.table = table;
		this.player = player;
		if (level < MIN_LEVEL)
			level = MIN_LEVEL;
		if (level > MAX_LEVEL)
			level = MAX_LEVEL;
		this.recursionDepth = (byte) (level - 1);
		random = new Random();
	}

	/**
	 * Starts a separate thread that calculates the best move and returns it to
	 * the GUI when ready
	 */
	public synchronized void play() {

		dying = false;
		Thread t = new Thread(new Runnable() {
			public void run() {

				// Reset timer
				long timer = System.currentTimeMillis() + MINIMUM_MOVE_TIME;

				// Find the best move
				byte[] move = findBestMove(player, recursionDepth);

				if (move != null && !dying) {
					// Make sure we are not too fast
					try {
						if ((timer -= System.currentTimeMillis()) > 0)
							Thread.sleep(timer);
					} catch (InterruptedException e) {
					}

					// Put the piece
					if (!dying)
						game.putPiece(move[0], move[1]);
				}
			}
		});

		t.start();
	}

	/**
	 * Kills the bot so that its thread dies
	 */
	public synchronized void kill() {
		dying = true;
	}

	/**
	 * Finds the best move given a table, a color and a recursion depth.
	 * 
	 * @param table
	 *            The game table
	 * @param player
	 *            The color of the player
	 * @param depth
	 *            The maximum allowed recursion level
	 * @return An integer array of {x-coordinate, y-coordinate, score}
	 */
	private byte[] findBestMove(byte player, byte depth) {

		if (dying)
			return null;

		// Initialize all counters
		byte max = Byte.MIN_VALUE;
		byte maxCount = 0;
		byte xmax = 0;
		byte ymax = 0;
		byte result = 0;
		byte altColor = OthelloTable.alternatePlayer(player);

		// Subtract the depth counter
		depth--;

		// Iterate over the entire table
		for (byte y = 0; y < OthelloTable.TABLE_HEIGHT; y++)
			for (byte x = 0; x < OthelloTable.TABLE_WIDTH; x++)

				// Try to put a piece on this coordinate
				if ((result = table.putPiece(x, y, player)) != OthelloTable.INVALID_MOVE) {

					// Recurse counterattacks if allowed
					if (depth > 0) {
						byte[] counterMove = findBestMove(altColor, depth);
						if (counterMove != null)
							result -= counterMove[2];
					}

					// Strategy weights
					if ((x == 0 || x == OthelloTable.TABLE_WIDTH - 1)
							&& (y == 0 || y == OthelloTable.TABLE_HEIGHT - 1))
						result += 10;

					// Check if a new maximum was found
					if (result > max) {
						max = result;
						maxCount = 0;
						xmax = x;
						ymax = y;
					} else if (result == max
							&& Math.abs(random.nextInt()) < Integer.MAX_VALUE
									/ ++maxCount) {
						xmax = x;
						ymax = y;
					}

					// Rewind the move
					table.rewind();
				}

		// Return null if no maximum was found
		if (max == Integer.MIN_VALUE)
			return null;

		// Return the found maximum
		return new byte[] { xmax, ymax, max };
	}
}
