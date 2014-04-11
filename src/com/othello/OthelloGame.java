package com.othello;

import java.io.IOException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

/**
 * This class holds most of the gui-related tasks along with a state-machine
 * 
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloGame extends Canvas implements CommandListener {

	/**
	 * The enclosing midlet
	 */
	private MIDlet midlet;

	/**
	 * The Othello table
	 */
	private OthelloTable table;

	/**
	 * The table layers
	 */
	private Image[] tableLayers;

	/**
	 * Cell bounds for the background images
	 */
	private static final byte[][] CELL_BOUNDS_X = new byte[][] {
			{ 0, 32, 43, 54, 65, 75, 86, 97, 127 },
			{ 0, 31, 42, 53, 64, 75, 86, 98, 127 },
			{ 0, 29, 41, 53, 64, 76, 87, 99, 127 },
			{ 0, 28, 40, 52, 64, 76, 88, 100, 127 },
			{ 0, 26, 39, 51, 64, 77, 89, 102, 127 },
			{ 0, 25, 38, 51, 64, 77, 91, 104, 127 },
			{ 0, 23, 36, 51, 64, 78, 92, 106, 127 },
			{ 0, 21, 35, 50, 65, 79, 93, 108, 127 } };

	/**
	 * Cell bounds for the background images
	 */
	private static final byte[] CELL_BOUNDS_Y = new byte[] { 0, 11, 19, 28, 38,
			49, 61, 74, 91 };

	/**
	 * The width of the table
	 */
	private static final byte TABLE_WIDTH = 127;

	/**
	 * The height of the table
	 */
	private static final byte TABLE_HEIGHT = 91;

	/**
	 * The textbox width
	 */
	private static final byte TEXT_WIDTH = 126;

	/**
	 * The initialization state
	 */
	private static final byte INITIALIZATION_STATE = 0;

	/**
	 * The title state
	 */
	private static final byte TITLE_STATE = 1;

	/**
	 * The load state
	 */
	private static final byte LOAD_STATE = 2;

	/**
	 * The game mode selection state
	 */
	private static final byte GAME_MODE_SELECTION_STATE = 3;

	/**
	 * The level selection state
	 */
	private static final byte LEVEL_SELECTION_STATE = 4;

	/**
	 * The game state
	 */
	private static final byte GAME_STATE = 5;

	/**
	 * The game over state
	 */
	private static final byte GAME_OVER_STATE = 6;

	/**
	 * The rendered table
	 */
	private Image renderedTable;

	/**
	 * The offscreen buffer used for manual double buffering
	 */
	private Image offScreen;

	/**
	 * The textbox
	 */
	private OthelloTextBox textBox;

	/**
	 * The settings object
	 */
	private OthelloSettings settings;

	/**
	 * The current state
	 */
	private byte state;

	/**
	 * The othelloBots if available
	 */
	private OthelloBot[] othelloBots;

	/**
	 * The current player color
	 */
	private byte currentPlayer;

	/**
	 * The horizontal cursor position
	 */
	private byte cursorX;

	/**
	 * The vertical cursor position
	 */
	private byte cursorY;

	/**
	 * True if the exit question is beeing displayed
	 */
	private boolean exitSelected;

	/**
	 * True if the load option is selected
	 */
	private boolean loadSelected;

	/**
	 * The current level
	 */
	private byte level;

	/**
	 * The current game mode number of players (0, 1 or 2)
	 */
	private byte players;

	/**
	 * Creates an instance of the OthelloGame
	 */
	public OthelloGame(MIDlet midlet) throws IOException {

		this.midlet = midlet;

		if (!isDoubleBuffered())
			offScreen = Image.createImage(getWidth(), getHeight());

		othelloBots = new OthelloBot[Math.max(OthelloTable.BLACK_PLAYER,
				OthelloTable.WHITE_PLAYER) + 1];

		table = new OthelloTable();
		textBox = new OthelloTextBox(TEXT_WIDTH);

		addCommand(new Command("Cancel", Command.CANCEL, 0));
		addCommand(new Command("Ok", Command.OK, 1));
		setCommandListener(this);

		// Read the settings object
		settings = new OthelloSettings();
		players = settings.getPlayers();
		level = settings.getLevel();
		state = INITIALIZATION_STATE;
		textBox.renderText("LOADING");
		repaint();

		new Thread() {
			public void run() {
				try {
					Image[] images = new Image[6];
					images[OthelloTable.BLACK_PLAYER << 1] = Image
							.createImage("/images/black.png");
					images[(OthelloTable.BLACK_PLAYER << 1) | 1] = Image
							.createImage("/images/blacksel.png");
					images[OthelloTable.WHITE_PLAYER << 1] = Image
							.createImage("/images/white.png");
					images[(OthelloTable.WHITE_PLAYER << 1) | 1] = Image
							.createImage("/images/whitesel.png");
					images[OthelloTable.EMPTY_SQUARE << 1] = Image
							.createImage("/images/empty.png");
					images[(OthelloTable.EMPTY_SQUARE << 1) | 1] = Image
							.createImage("/images/emptysel.png");
					tableLayers = images;
				} catch (Exception e) {
					textBox.renderText("FAILED");
					repaint();
					return;
				}

				displayTitle();
			}
		}.start();
	}

	/**
	 * Stops and optionally saves the current game
	 */
	public void stopGame(boolean save) {
		if (state == GAME_STATE) {
			for (int i = 0; i < othelloBots.length; i++)
				if (othelloBots[i] != null) {
					othelloBots[i].kill();
					othelloBots[i] = null;
				}
			if (save) {
				settings.saveGame(players, level, table, currentPlayer,
						cursorX, cursorY);
			}
		}
	}

	/**
	 * Restore a saved game
	 */
	private void loadSavedGame() {

		// Set the old cursor position
		cursorX = settings.getCursorX();
		cursorY = settings.getCursorY();

		players = settings.getPlayers();
		level = settings.getLevel();
		currentPlayer = settings.getCurrentPlayer();

		// Initialize the bots
		initializeBots();

		// Load previously saved table
		settings.loadSavedTable(table);

		// Clean the settings
		settings.saveSettings(players, level);

		// Render the table
		renderTable();

		// Start with alternated player since switchPlayer will switch back
		currentPlayer = OthelloTable.alternatePlayer(currentPlayer);
		state = GAME_STATE;
		switchPlayer();
	}

	/**
	 * Starts a new game
	 */
	private void startNewGame() {

		cursorX = 0;
		cursorY = 0;

		// Remember the settings
		settings.saveSettings(players, level);

		// Initialize the bots
		initializeBots();

		// Reset and re-render the table
		table.startNewGame();
		renderTable();

		// Set to black player - will switch back to white in switchPlayer()
		currentPlayer = OthelloTable.BLACK_PLAYER;
		state = GAME_STATE;
		switchPlayer();
	}

	/**
	 * Private method for initializing the bots depending on game mode (player
	 * count)
	 */
	private void initializeBots() {
		// Create the first bot if less than 1 players
		if (players < 1)
			othelloBots[OthelloTable.WHITE_PLAYER] = new OthelloBot(this,
					table, OthelloTable.WHITE_PLAYER, level);
		else
			othelloBots[OthelloTable.WHITE_PLAYER] = null;

		// Create a second bot if no players
		if (players < 2)
			othelloBots[OthelloTable.BLACK_PLAYER] = new OthelloBot(this,
					table, OthelloTable.BLACK_PLAYER, level);
		else
			othelloBots[OthelloTable.BLACK_PLAYER] = null;
	}

	/**
	 * Handles commands
	 * 
	 * @param command
	 *            The command code
	 * @param display
	 *            The current display
	 */
	public void commandAction(Command command, Displayable display) {

		if (command.getCommandType() == Command.OK) {
			// The ok command is always redirected to a FIRE game action
			gameAction(FIRE);
			return;
		} else if (command.getCommandType() == Command.CANCEL) {
			// The cancel command is state dependent
			switch (state) {
			case LOAD_STATE:
				displayTitle();
				return;
			case GAME_MODE_SELECTION_STATE:
				displayTitle();
				return;
			case LEVEL_SELECTION_STATE:
				displayGameModeSelection();
				return;
			case GAME_OVER_STATE:
				displayTitle();
				return;
			case TITLE_STATE:
			case GAME_STATE:
				if (!exitSelected) {
					exitSelected = true;
					textBox
							.renderTextOverlay((state == GAME_STATE) ? "SAVE GAME?"
									: "EXIT?");
					repaint();
					return;
				} else {
					gameAction(~FIRE);
					return;
				}
			}
		}
	}

	/**
	 * Handles key presses
	 * 
	 * @param key
	 *            The keycode of the pressed key
	 */
	public void keyPressed(int key) {
		try {
			gameAction(getGameAction(key));
		} catch (Exception e) {
		}
	}

	/**
	 * Handles game actions
	 * 
	 * @param action
	 *            The action code
	 */
	private void gameAction(int action) {

		// Handle the special exit mode
		if (exitSelected) {

			// The exit mode is exited no matter what
			exitSelected = false;
			textBox.removeOverlay();

			// The action is state dependant
			if (state == TITLE_STATE) {
				if (action == FIRE)
					midlet.notifyDestroyed();
				else
					repaint();
				return;
			} else {
				stopGame(action == FIRE);
				displayTitle();
				return;
			}
		}

		// The behaviour is state dependent
		switch (state) {
		case TITLE_STATE:
			if (settings.containsSavedGame()) {
				loadSelected = true;
				displayLoadSelection();
				return;
			} else {
				displayGameModeSelection();
				return;
			}
		case LOAD_STATE:
			if (action == FIRE) {
				if (loadSelected) {
					loadSavedGame();
					return;
				} else {
					displayGameModeSelection();
					return;
				}
			} else {
				loadSelected = !loadSelected;
				displayLoadSelection();
				return;
			}
		case GAME_MODE_SELECTION_STATE:
			gameModeSelectionAction(action);
			return;
		case LEVEL_SELECTION_STATE:
			levelSelectionAction(action);
			return;
		case GAME_OVER_STATE:
			displayTitle();
			return;
		case GAME_STATE:
			if (othelloBots[currentPlayer] == null) {
				if (action == Canvas.FIRE) {
					putPiece(cursorX, cursorY);
					repaint();
					return;
				} else {
					int dx = action == Canvas.LEFT ? -1
							: 0 + action == Canvas.RIGHT ? 1 : 0;
					int dy = action == Canvas.UP ? -1
							: 0 + action == Canvas.DOWN ? 1 : 0;
					cursorX = (byte) ((cursorX + OthelloTable.TABLE_WIDTH + dx) % OthelloTable.TABLE_WIDTH);
					cursorY = (byte) ((cursorY + OthelloTable.TABLE_HEIGHT + dy) % OthelloTable.TABLE_HEIGHT);
					repaint();
					return;
				}
			}
		}
	}

	/**
	 * Called to update the level selection
	 * 
	 * @param key
	 *            The Canvas Action key
	 */
	private void levelSelectionAction(int key) {
		switch (key) {
		case Canvas.RIGHT:
		case Canvas.UP:
			level = (byte) (level < OthelloBot.MAX_LEVEL ? level + 1
					: OthelloBot.MIN_LEVEL);
			displayLevelSelection();
			return;
		case Canvas.LEFT:
		case Canvas.DOWN:
			level = (byte) (level > OthelloBot.MIN_LEVEL ? level - 1
					: OthelloBot.MAX_LEVEL);
			displayLevelSelection();
			return;
		case Canvas.FIRE:
			startNewGame();
			return;
		}
	}

	/**
	 * Changes the player count
	 * 
	 * @param key
	 *            The Canvas action key
	 */
	private void gameModeSelectionAction(int key) {
		switch (key) {
		case Canvas.RIGHT:
		case Canvas.UP:
			players = (byte) (players < 2 ? players + 1 : 0);
			displayGameModeSelection();
			return;
		case Canvas.LEFT:
		case Canvas.DOWN:
			players = (byte) (players > 0 ? players - 1 : 2);
			displayGameModeSelection();
			return;
		case Canvas.FIRE:
			if (players == 2)
				startNewGame();
			else
				displayLevelSelection();
			return;
		}
	}

	/**
	 * Displays the load screen
	 */
	private void displayLoadSelection() {
		if (loadSelected) {
			textBox.renderText("LOAD GAME");
		} else {
			textBox.renderText("NEW GAME");
		}
		state = LOAD_STATE;
		repaint();
	}

	/**
	 * Displays the title screen
	 */
	private void displayTitle() {
		textBox.renderText("OTHELLO");
		table.displayTitle(true);
		renderTable();
		state = TITLE_STATE;
		repaint();
	}

	/**
	 * Displays the game type selection screen
	 */
	private void displayGameModeSelection() {
		textBox.renderText(players + " PLAYER" + ((players != 1) ? "S" : ""));
		state = GAME_MODE_SELECTION_STATE;
		repaint();
	}

	/**
	 * Displays the level selection
	 */
	private void displayLevelSelection() {
		textBox.renderText("LEVEL " + level);
		state = LEVEL_SELECTION_STATE;
		repaint();
	}

	/**
	 * Prints the game over results
	 */
	private void displayGameOver() {
		int score = table.getWhiteScore() - table.getBlackScore();
		if (score == 0)
			textBox.renderText("ITS A DRAW");
		else if (score > 0)
			textBox.renderText(players == 1 ? "YOU WIN" : "WHITE WINS");
		else if (score < 0)
			textBox.renderText(players == 1 ? "I WIN" : "BLACK WINS");
		state = GAME_OVER_STATE;
		repaint();
	}

	/**
	 * Changes the player and starts the bot if available
	 */
	private void switchPlayer() {

		if (state == GAME_STATE) {

			// Change the player
			currentPlayer = OthelloTable.alternatePlayer(currentPlayer);
			if (!table.canMove(currentPlayer)) {

				// First player can't move
				currentPlayer = OthelloTable.alternatePlayer(currentPlayer);
				if (!table.canMove(currentPlayer)) {
					displayGameOver();
					return;
				}
			}

			// Print move info
			if (players == 1) {
				if (othelloBots[currentPlayer] == null)
					textBox.renderText("YOUR MOVE");
				else
					textBox.renderText("WAIT");
			} else {
				if (currentPlayer == OthelloTable.WHITE_PLAYER)
					textBox.renderText("WHITE MOVE");
				else
					textBox.renderText("BLACK MOVE");
			}
			repaint();

			// Start the bot if available
			if (othelloBots[currentPlayer] != null)
				othelloBots[currentPlayer].play();
		}
	}

	/**
	 * Puts a new piece on the requested coordinate
	 * 
	 * @param x
	 *            The horisontal position
	 * @param y
	 *            The vertical position
	 */
	public void putPiece(byte x, byte y) {
		if (state == GAME_STATE
				&& table.putPiece(x, y, currentPlayer) != OthelloTable.INVALID_MOVE) {
			renderTable();
			switchPlayer();
		}
	}

	/**
	 * Renders the table
	 */
	private void renderTable() {

		// Create the buffer if not available
		if (renderedTable == null)
			renderedTable = Image.createImage(TABLE_WIDTH, TABLE_HEIGHT);

		Graphics g = renderedTable.getGraphics();
		for (byte x = 0; x < OthelloTable.TABLE_WIDTH; x++)
			for (byte y = 0; y < OthelloTable.TABLE_HEIGHT; y++) {

				// Set the clip to contain only the cell
				g.setClip(CELL_BOUNDS_X[y][x], CELL_BOUNDS_Y[y],
						CELL_BOUNDS_X[y][x + 1] - CELL_BOUNDS_X[y][x],
						CELL_BOUNDS_Y[y + 1] - CELL_BOUNDS_Y[y]);

				// Draw from the appropriate layer
				g.drawImage(tableLayers[table.getPiece(x, y) << 1], 0, 0,
						Graphics.TOP | Graphics.LEFT);
			}
	}

	/**
	 * Renders the cursor directly on a graphics object given the position of
	 * the table
	 * 
	 * @param g
	 *            The graphics object that is used to draw on
	 * @param tableX
	 *            The horisontal position of the table
	 * @param tableY
	 *            The vertical position of the table
	 */
	private void renderCursor(Graphics g, int tableX, int tableY) {

		// Collect the clip
		int clipX = tableX + CELL_BOUNDS_X[cursorY][cursorX];
		int clipY = tableY + CELL_BOUNDS_Y[cursorY];
		int clipWidth = CELL_BOUNDS_X[cursorY][cursorX + 1]
				- CELL_BOUNDS_X[cursorY][cursorX];
		int clipHeight = CELL_BOUNDS_Y[cursorY + 1] - CELL_BOUNDS_Y[cursorY];
		g.setClip(clipX, clipY, clipWidth, clipHeight);

		// Draw the appropriate cell
		byte piece = table.getPiece(cursorX, cursorY);
		if (piece == OthelloTable.EMPTY_SQUARE) {
			g.drawImage(tableLayers[currentPlayer << 1 | 1], tableX, tableY,
					Graphics.TOP | Graphics.LEFT);
		} else {
			g.drawImage(tableLayers[piece << 1 | 1], tableX, tableY,
					Graphics.TOP | Graphics.LEFT);
		}
	}

	/**
	 * Paints the Canvas
	 * 
	 * @param graphics
	 *            The graphics object used to draw on
	 */
	public void paint(Graphics graphics) {

		// Create double buffering if neeeded
		Graphics g = isDoubleBuffered() ? graphics : offScreen.getGraphics();

		// Get the positioning points
		int tableX = (getWidth() - TABLE_WIDTH) / 2;
		int tableY = (getHeight() - TABLE_HEIGHT - OthelloTextBox.BOX_HEIGHT + 5) / 2;
		int textX = (getWidth() - TEXT_WIDTH) / 2;
		int textY = tableY + TABLE_HEIGHT + 5;

		// Clear background
		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Draw the table
		if (renderedTable != null)
			g.drawImage(renderedTable, tableX, tableY,
					(Graphics.TOP | Graphics.LEFT));

		// Draw the text bar
		if (textBox != null)
			if (textBox.paint(g, textX, textY))
				repaint();

		// Draw cursor when in move state with human player
		if (state == GAME_STATE && othelloBots[currentPlayer] == null)
			renderCursor(g, tableX, tableY);

		// Draw the offScreen onto the screen if manually double buffering
		if (!isDoubleBuffered())
			graphics.drawImage(offScreen, 0, 0, (Graphics.TOP | Graphics.LEFT));
	}
}
