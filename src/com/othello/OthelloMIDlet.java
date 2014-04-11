package com.othello;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This class is the top class of the Othello game MIDlet
 * 
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloMIDlet extends MIDlet {

	/**
	 * The gui used to draw the game
	 */
	private OthelloGame game;

	/**
	 * Creates an instance of the Othello midlet
	 * 
	 */
	public OthelloMIDlet() {
		try {
			game = new OthelloGame(this);
			Display display = Display.getDisplay(this);
			display.setCurrent(game);
		} catch (Exception e) {
			
		}
	}

	/**
	 * Called when the MIDlet is started
	 */
	protected void startApp() throws MIDletStateChangeException {
		game.repaint();
	}

	/**
	 * Called when the MIDlet is paused
	 */
	protected void pauseApp() {
	}

	/**
	 * Called when the MIDlet is destroyed
	 */
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		game.stopGame(true);
	}
}
