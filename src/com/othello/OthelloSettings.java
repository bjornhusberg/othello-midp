package com.othello;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;

/**
 * This class holds the permanent settings for the Othello game
 *
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloSettings {

	/**
	 * The othello RecordStore
	 */
	private static final String othelloRS = "othello";

	/**
	 * The identifier for settings only object
	 */
	private static final byte IDENTIFIER_SETTINGS_ONLY = -123;

	/**
	 * The identifier for a full save
	 */
	private static final byte IDENTIFIER_FULL_SAVE = -122;

	/**
	 * Default number of players
	 */
	private static final byte DEFAULT_PLAYERS = 1;

	/**
	 * Default level
	 */
	private static final byte DEFAULT_LEVEL = 1;

	/**
	 * Array position holder for the identifier
	 */
	private static final byte IDENTIFIER_POS = 0;

	/**
	 * Array position holder for the players count
	 */
	private static final byte PLAYERS_POS = 1;

	/**
	 * Array position holder for the level
	 */
	private static final byte LEVEL_POS = 2;

	/**
	 * Array position holder for the current player
	 */
	private static final byte CURRENT_PLAYER_POS = 3;

	/**
	 * Array position holder for the horizontal cursor position
	 */
	private static final byte CURSOR_X_POS = 4;

	/**
	 * Array position holder for the vertical cursor position
	 */
	private static final byte CURSOR_Y_POS = 5;
	
	/**
	 * Array position holder for the table position
	 */
	private static final byte TABLE_POS = 6;

	/**
	 * The ID for the single record
	 */
	private int recordId;

	/**
	 * The actual record store object
	 */
	private RecordStore recordStore;

	/**
	 * The buffer used to save options
	 */
	private byte[] saveBuffer;

	/**
	 * Creates an instance of Othello settings
	 */
	public OthelloSettings() {

		saveBuffer = new byte[6 + OthelloTable.TABLE_HEIGHT
				* OthelloTable.TABLE_WIDTH];

		saveBuffer[PLAYERS_POS] = DEFAULT_PLAYERS;
		saveBuffer[LEVEL_POS] = DEFAULT_LEVEL;

		try {
			recordStore = RecordStore.openRecordStore(othelloRS, true);

			RecordEnumeration records = recordStore.enumerateRecords(null,
					null, false);
			if (records.hasNextElement()) {
				recordId = records.nextRecordId();
			} else {
				recordId = recordStore.addRecord(new byte[] {
						IDENTIFIER_SETTINGS_ONLY, DEFAULT_PLAYERS,
						DEFAULT_LEVEL }, 0, 3);
			}

			recordStore.getRecord(recordId, saveBuffer, 0);

			if (saveBuffer[IDENTIFIER_POS] != IDENTIFIER_SETTINGS_ONLY
					&& saveBuffer[IDENTIFIER_POS] != IDENTIFIER_FULL_SAVE) {
				saveBuffer[PLAYERS_POS] = DEFAULT_PLAYERS;
				saveBuffer[LEVEL_POS] = DEFAULT_LEVEL;
			}

		} catch (RecordStoreException e) {
			recordStore = null;
		}
	}

	/**
	 * Save the permanent players count (game mode) and level settings
	 */
	public void saveSettings(byte players, byte level) {
		if (recordStore != null) {
			saveBuffer[IDENTIFIER_POS] = IDENTIFIER_SETTINGS_ONLY;
			saveBuffer[PLAYERS_POS] = players;
			saveBuffer[LEVEL_POS] = level;
			try {
				recordStore.setRecord(recordId, saveBuffer, 0, 3);
			} catch (InvalidRecordIDException e1) {
			} catch (RecordStoreFullException e2) {
			} catch (RecordStoreException e3) {
			}
		}
	}

	/**
	 * Save both permanent settings (players and level) and a full game save
	 */
	public void saveGame(byte players, byte level, OthelloTable table, byte currentPlayer, byte cursorX,
			byte cursorY) {
		if (recordStore != null) {
			saveBuffer[IDENTIFIER_POS] = IDENTIFIER_FULL_SAVE;
			saveBuffer[PLAYERS_POS] = players;
			saveBuffer[LEVEL_POS] = level;
			saveBuffer[CURRENT_PLAYER_POS] = currentPlayer;
			saveBuffer[CURSOR_X_POS] = cursorX;
			saveBuffer[CURSOR_Y_POS] = cursorY;
			int length = table.save(saveBuffer, TABLE_POS);
			try {
				recordStore.setRecord(recordId, saveBuffer, 0, length);
			} catch (InvalidRecordIDException e1) {
			} catch (RecordStoreFullException e2) {
			} catch (RecordStoreException e3) {
			}
		}
	}

	/**
	 * Get the number of players in the selected game mode (0, 1 or 2)
	 */
	public byte getPlayers() {
		return saveBuffer[PLAYERS_POS];
	}

	/**
	 * Get the level in the selected game mode
	 */
	public byte getLevel() {
		return saveBuffer[LEVEL_POS];
	}

	/**
	 * Checks if this settings object contains a saved game data
	 */
	public boolean containsSavedGame() {
		return saveBuffer[IDENTIFIER_POS] == IDENTIFIER_FULL_SAVE;
	}

	/**
	 * Load a saved game into the table
	 */
	public void loadSavedTable(OthelloTable table) {
		table.load(saveBuffer, TABLE_POS);
	}

	/**
	 * Get the current player
	 */
	public byte getCurrentPlayer() {
		return saveBuffer[CURRENT_PLAYER_POS];
	}

	/**
	 * Get the horizontal cursor position
	 */
	public byte getCursorX() {
		return saveBuffer[CURSOR_X_POS];
	}

	/**
	 * Get the vertical cursor position
	 */
	public byte getCursorY() {
		return saveBuffer[CURSOR_Y_POS];
	}
}