package com.othello;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * This class holds the text box object
 * 
 * @author Bjorn.Husberg@guide.se
 */
public class OthelloTextBox {

	/**
	 * The width of a dot
	 */
	public final static int DOT_WIDTH = 2;

	/**
	 * The height of a dot
	 */
	public final static int DOT_HEIGHT = 3;

	/**
	 * The char pixel width
	 */
	public final static int CHAR_WIDTH = 6;

	/**
	 * The char pixel height
	 */
	public final static int CHAR_HEIGHT = 5;

	/**
	 * The fixed height of the text box
	 */
	public final static int BOX_HEIGHT = CHAR_HEIGHT * DOT_HEIGHT;

	/**
	 * The width of this textbox
	 */
	public int width;

	/**
	 * The actual image buffer
	 */
	private Image image;

	/**
	 * The prerendered image background
	 */
	private Image background;

	/**
	 * The text string that is beeing displayed
	 */
	private String text;

	/**
	 * The timestamp for when the current text was added (used in animations)
	 */
	private long textTime;

	/**
	 * Indicates that there is an overlay text displaying
	 */
	private boolean overlay;

	/**
	 * Alpha characters (a, b, c...)
	 */
	private int[] alpha = { 478931106, 1015793852, 511838238, 1015687356,
			1048807486, 1048807456, 511895708, 579594402, 471892508,
			1040722076, 580094242, 545392702, 584755362, 583707042, 478816412,
			1015793696, 478816538, 1015793826, 511819964, 1042317832,
			579479708, 579478792, 579512980, 575702306, 575701512, 1041269822 };

	/**
	 * Numeric characters (1, 2, 3...)
	 */
	private int[] numeric = { 479898780, 140542492, 1007274046, 1007206588,
			579592322, 1048821948, 511953052, 1040728584, 478791836, 478798012 };

	/**
	 * Special characters
	 */
	private int[][] extra = { { '!', 136347656 }, { '.', 8 }, { '-', 114688 },
			{ '?', 1007206408 }, { '\'', 69206016 }, { '>', 274843152 },
			{ '<', 70370052 } };

	/**
	 * Creates an OthelloText textbox with the given width in pixels
	 * 
	 * @param width
	 *            The wanted pixel width of the text box
	 */
	public OthelloTextBox(int width) {
		this.width = width;
		image = Image.createImage(width, BOX_HEIGHT);
		background = Image.createImage(width, BOX_HEIGHT);
		Graphics g = background.getGraphics();
		g.setColor(0);
		g.fillRect(0, 0, width, BOX_HEIGHT);
		g.setColor(0x424e31);
		for (int x = 0; x < width / DOT_WIDTH; x++)
			for (int y = 0; y < CHAR_HEIGHT; y++)
				g.fillRect(x * DOT_WIDTH, y * DOT_HEIGHT, DOT_WIDTH - 1,
						DOT_HEIGHT - 1);
		renderText("");
		renderText("");
	}

	/**
	 * Renders the text string onto the text box as overlay
	 * 
	 * @param text
	 *            The text message to be rendered
	 */
	public void renderTextOverlay(String text) {
		overlay = true;
		renderTextImage(text);
	}

	/**
	 * Renders the text string onto the text box
	 * 
	 * @param text
	 *            The text message to be rendered
	 */
	public void renderText(String text) {
		this.text = text;
		if (overlay)
			return;
		renderTextImage(text);
	}

	/**
	 * Private method used to render the actual text image
	 * 
	 * @param text
	 */
	private synchronized void renderTextImage(String text) {
		Graphics g = image.getGraphics();
		int length = text.length();
		if (DOT_WIDTH * length * CHAR_WIDTH > image.getWidth())
			length = image.getWidth() / (CHAR_WIDTH * DOT_WIDTH);

		g.drawImage(background, 0, 0, Graphics.TOP | Graphics.LEFT);

		int startX = (image.getWidth() - length * DOT_WIDTH * CHAR_WIDTH) / 2;
		startX = DOT_WIDTH * (startX / DOT_WIDTH);

		for (int i = 0; i < length; i++) {
			char ch = text.charAt(i);
			int layout = 0;
			if (ch >= 'a' && ch <= 'z')
				layout = alpha[ch - 'a'];
			else if (ch >= 'A' && ch <= 'Z')
				layout = alpha[ch - 'A'];
			else if (ch >= '0' && ch <= '9')
				layout = numeric[ch - '0'];
			else
				for (int j = 0; j < extra.length; j++)
					if (ch == extra[j][0])
						layout = extra[j][1];

			int bitp = 1 << 30;
			for (int y = 0; y < CHAR_HEIGHT; y++) {
				for (int x = 0; x < CHAR_WIDTH; x++) {
					bitp = bitp >> 1;
					if ((layout & bitp) != 0) {
						g.setColor(0xf8f500);
						g.fillRect((startX + x * DOT_WIDTH), (y * DOT_HEIGHT),
								DOT_WIDTH - 1, DOT_HEIGHT - 1);
					}
				}
			}
			startX += CHAR_WIDTH * DOT_WIDTH;
		}

		textTime = System.currentTimeMillis();
	}

	/**
	 * Paints the textbox into a graphics object
	 * 
	 * @param g
	 *            The graphics that will be drawn on
	 * @param x
	 *            The horizontal position of the textBox
	 * @param y
	 *            The vertical position of the textBox
	 * @return boolean true if the textBox is still animating and needs to be
	 *         repainted
	 */
	public synchronized boolean paint(Graphics g, int x, int y) {
		int clipX = g.getClipX();
		int clipY = g.getClipX();
		int clipWidth = g.getClipWidth();
		int clipHeight = g.getClipHeight();
		g.setClip(x, y, width, BOX_HEIGHT);
		g.drawImage(background, x, y, Graphics.TOP | Graphics.LEFT);

		long timer = System.currentTimeMillis() - textTime;
		int offset = width - DOT_WIDTH * (int) (timer / 5);
		if (offset < 0)
			offset = 0;
		g.drawImage(image, x + offset, y, Graphics.TOP | Graphics.LEFT);
		g.setClip(clipX, clipY, clipWidth, clipHeight);
		return offset > 0;
	}

	/**
	 * Disables the overlay if available
	 */
	public void removeOverlay() {
		if (overlay) {
			overlay = false;
			renderTextImage(text);
		}
	}

	/**
	 * Returns the text currently beeing displayed
	 * 
	 * @return The text currently beeing displayed
	 */
	public String getText() {
		return text;
	}
}
