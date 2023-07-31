import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Board extends JPanel
		implements
			Runnable,
			KeyListener,
			MouseListener,
			MouseWheelListener,
			ComponentListener {
	// Final variables, related to formatting, spacing, && such
	final float DAMPENER = 0.9f;
	final int SIZE = 64;
	final int THICKNESS = 3;
	final int MARGIN = 10;
	final int TEXTSIZE = 24;
	final int TEXTSIZESIM = 16;
	final int BOXSIZE = (int) (TEXTSIZE - MARGIN / 4f);
	public final float LINEHEIGHT = TEXTSIZE + MARGIN / 4f;
	final int TEXTOFFSET = (int) (TEXTSIZE - MARGIN / 2f);
	final float darkenCoeff = 0.3f;

	// Variables related to screen && UI size
	static int WIDTH = 1000;
	static int HEIGHT = 800;
	public final int SIDEBARWIDTH = 420;
	public int UIPARTITION = 0;
	int LONGESTNAME = 0;
	BufferedImage UI1;
	BufferedImage UI2;
	BufferedImage UI1Part;
	BufferedImage UI2Part;
	int XBOX = 0;

	// Variables related to running the window
	private final int FPS = 60;
	private boolean running = false;
	private Graphics2D g;
	private BufferedImage image;
	Thread thread;

	// Other variables
	int SPACING = 350;
	int LOOSE = 50;
	int dragging = -1;
	int hovering = -1;
	Point offset = new Point(0, 0);
	final Font font = new Font("Trebuchet MS", Font.PLAIN, TEXTSIZE);
	final Font fontSim = new Font("Trebuchet MS", Font.PLAIN, TEXTSIZESIM);
	final FontMetrics fm = getFontMetrics(font);
	final FontMetrics fsm = getFontMetrics(fontSim);
	float scrollPercent = 0.0f;
	int mouseScroll = 0;
	boolean outOn = true;
	boolean inOn = true;
	Functions func = new Functions();
	Random rand = new Random();
	float linePos = 0f;
	Point mousePos = new Point(0, 0);
	boolean activeChange = true;
	boolean scrolled = true;
	boolean resized = true;
	RescaleOp op = new RescaleOp(darkenCoeff, 0, null);

	// Control inputs
	ArrayList<Integer> keysHeld = new ArrayList<Integer>();
	ArrayList<Integer> mouseHeld = new ArrayList<Integer>();
	ArrayList<Integer> mouseClicked = new ArrayList<Integer>();

	// Variables for data
	Map<String, Color> typeColor;
	Map<String, Color> typeColorLight = new HashMap<String, Color>();
	Map<String, Color> typeColorDark = new HashMap<String, Color>();
	Color[] grey = {new Color(30, 30, 30), new Color(60, 60, 60),
			new Color(70, 70, 70), new Color(150, 150, 150)};
	TreeMap<String, Trigger> triggerData = new TreeMap<String, Trigger>();
	ArrayList<Valkyrie> valks = new ArrayList<Valkyrie>();
	ArrayList<Connection> connections = new ArrayList<Connection>();

	// Render priority
	ArrayList<Valkyrie> valksUpper = new ArrayList<Valkyrie>();
	ArrayList<Valkyrie> valksLower = new ArrayList<Valkyrie>();
	ArrayList<Connection> connectUpper = new ArrayList<Connection>();
	ArrayList<Connection> connectLower = new ArrayList<Connection>();
	
	public static void main(String[] args) throws IOException {
		JFrame window = new JFrame("Honkai QTE Visualizer");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Board board = new Board();

		window.setContentPane(board);
		window.setAlwaysOnTop(false);
		window.setMinimumSize(new Dimension(board.SIDEBARWIDTH + 100, (int)(board.UIPARTITION + 100)));
		window.setResizable(true);
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
	
	public Board() {
		// Creates the window
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		requestFocus();

		// Sets up type colors
		typeColor = Map.ofEntries(Map.entry("MECH", new Color(43, 225, 254)),
				Map.entry("BIO", new Color(253, 178, 43)),
				Map.entry("PSY", new Color(255, 71, 215)),
				Map.entry("QUA", new Color(141, 110, 255)),
				Map.entry("IMG", new Color(241, 212, 143)));

		// Creates light && dark variants of type colors
		float scalingValue = 0.5f;
		for (String key : typeColor.keySet()) {
			Color color = typeColor.get(key);
			typeColorLight.put(key, func.colorMultiply(color, scalingValue, false));
			typeColorDark.put(key, func.colorMultiply(color, scalingValue, true));
		} ;

		// Sets up trigger data
		scalingValue = 0.4f;
		try {
			Scanner sc = new Scanner(new FileReader("triggerColors.txt")).useDelimiter("\\n");
			Trigger map = new Trigger();
			String line;

			for (int i = 0; sc.hasNext(); i++) {
				line = sc.next();
				switch (i % 3) {
					case 0 : // Trigger
						map = new Trigger();
						triggerData.put(line, map);
						break;
					case 1 : // Color
						String[] colorArrStr = line.split(", ");
						int[] colorArr = new int[colorArrStr.length];
						for (int j = 0; j < colorArrStr.length; j++) {
							colorArr[j] = Integer.parseInt(colorArrStr[j]);
						}
						map.color = new Color(colorArr[0], colorArr[1], colorArr[2]);
						map.colorLight = func.colorMultiply(map.color, scalingValue, false);
						map.colorDark = func.colorMultiply(map.color, scalingValue, true);
						break;
					// Case 2 is empty space
				};
			} ;
		} catch (IOException e) {System.out.println("Cannot open triggerColors.txt");}
		int iterator = 0;
		for (Trigger trigger : triggerData.values()) { // Used for the offset of the arrays on connections
			trigger.coefficient = 0.25f / triggerData.size() * iterator + 0.25f;
			iterator++;
		} ;

		// Loads valkyrie data
		try {
			Scanner sc = new Scanner(new FileReader("valkyries.txt")).useDelimiter("\\n");
			Valkyrie valk = new Valkyrie();
			String line;
			String[] s = {"Hit", "QTE", "Passive", "Weapon"};
			List<String> commonTriggers = Arrays.asList(s);

			for (int i = 0; sc.hasNext(); i++) {
				line = sc.next();
				switch (i % 6) {
					case 0 : // Name
						valk = new Valkyrie();
						valk.name = line.substring("Name :".length());// Removes the last character, \n, && the header
						break;
					case 1 : // Type
						valk.type = line.substring("Type: ".length());
						break;
					case 2 : // ConflictID
						valk.conflictID = line.substring("ConflictID: ".length());
						break;
					case 3 : // Effects
						line = line.substring("Effects: ".length());
						ArrayList<String> effects = new ArrayList<String>(Arrays.asList(line.split(", ")));
						effects.addAll(commonTriggers);
						valk.effects = effects;
						break;
					case 4 : // Triggers
						line = line.substring("Triggers: ".length());
						valk.triggers = new ArrayList<String>(Arrays.asList(line.split(", ")));
						
						valk.x = rand.nextFloat() * (WIDTH - SIZE / 2 - SIDEBARWIDTH) + SIDEBARWIDTH + SIZE / 2;
						valk.y = rand.nextFloat() * (HEIGHT - SIZE / 2) + SIZE / 2;
						valks.add(valk);
						break;
					// Case 5 is empty space
				};
			} ;
		} catch (IOException e) {System.out.println("Cannot open valkyries.txt");}
		valks.sort(Comparator.comparing(valk -> valk.name)); // Sorts valkyries by name

		// Creating connections
		for (int i = 0; i < valks.size(); i++) {
			Valkyrie valk1 = valks.get(i);

			for (int j = i + 1; j < valks.size(); j++) {
				Valkyrie valk2 = valks.get(j);
				Connection connection = new Connection(i, j);

				if (valk1.conflictID.equals("") || !(valk1.conflictID.equals(valk2.conflictID))) { // Check if the valkyries they can be used in the same team
					for (int k = 0; k < valk2.triggers.size(); k++) { // Iterates through each Trigger
						String triggerType = valk2.triggers.get(k);
						if (valk1.effects.contains(triggerType)) { // Checks if Trigger matches any Effects
							connection.directTriggers.add(triggerType);
							connection.directActive.add(triggerType);
						} ;
					} ;
					for (int k = 0; k < valk1.triggers.size(); k++) {
						String triggerType = valk1.triggers.get(k);
						if (valk2.effects.contains(triggerType)) {
							connection.reverseTriggers.add(triggerType);
							connection.reverseActive.add(triggerType);
						} ;
					} ;
				} ;
				connections.add(connection);
			} ;
		} ;

		// Image loading && rendering
		// Rendering valkyrie portraits
		for (Valkyrie valk : valks) {
			String name = valk.name;
			BufferedImage texture = loadImage(name.replaceAll("[: ]*", "") + ".png");

			// Creates final output image based on size of name
			int stringW = fsm.stringWidth(name);
			int outputW = Math.max(SIZE, stringW + 2);
			if (stringW > LONGESTNAME) {
				LONGESTNAME = stringW;
			} ;

			BufferedImage output = new BufferedImage(outputW, (int) (SIZE + MARGIN / 2f + TEXTSIZESIM * 1.5 + 1), BufferedImage.TYPE_INT_ARGB); // *1.5 accounts for letters like g && y that are offset below
			g = output.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Adds in background color
			g.setColor(typeColor.get(valk.type));
			g.fillOval((outputW - SIZE) / 2, 0, SIZE, SIZE);

			// Masks texture
			BufferedImage mask = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = mask.createGraphics();

			g2.setComposite(AlphaComposite.Clear); // sets alpha of entire surface to 0
			g2.fillRect(0, 0, SIZE, SIZE);

			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			g2.fillOval(THICKNESS, THICKNESS, SIZE - THICKNESS * 2 - 1, SIZE - THICKNESS * 2 - 1); // creates a circle mask of alpha=1

			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN)); // draws texture to masked area
			g2.drawImage(texture, 0, 0, SIZE, SIZE, null);
			g2.dispose();

			// Draws masked texture
			g.drawImage(mask, (outputW - SIZE) / 2, 0, null);

			// Adds in text
			g.setFont(fontSim);
			g.setColor(Color.BLACK); // bootleg text outline
			g.drawString(name, 2, SIZE + MARGIN / 2f + TEXTSIZESIM + 1);
			g.drawString(name, 2, SIZE + MARGIN / 2f + TEXTSIZESIM);
			g.drawString(name, 1, SIZE + MARGIN / 2f + TEXTSIZESIM);
			g.drawString(name, 0, SIZE + MARGIN / 2f + TEXTSIZESIM);
			g.drawString(name, 0, SIZE + MARGIN / 2f + TEXTSIZESIM + 1);
			g.drawString(name, 0, SIZE + MARGIN / 2f + TEXTSIZESIM + 2);
			g.drawString(name, 1, SIZE + MARGIN / 2f + TEXTSIZESIM + 2);
			g.drawString(name, 2, SIZE + MARGIN / 2f + TEXTSIZESIM + 2);
			g.setColor(typeColor.get(valk.type));
			g.drawString(name, 1, SIZE + MARGIN / 2f + TEXTSIZESIM + 1);

			// Finalize
			BufferedImage outputDark = op.filter(output, null);
			valk.sprite = output;
			valk.spriteDark = outputDark;
			g.dispose();
		} ;
		UIPARTITION = (int) (MARGIN * 4 + LINEHEIGHT * (3 + Math.ceil(triggerData.size() / 2)));
		LONGESTNAME *= (float) TEXTSIZE / (float) TEXTSIZESIM;
		LONGESTNAME += MARGIN * 2;

		// UI rendering
		// UI 1
		UI1 = new BufferedImage(SIDEBARWIDTH, UIPARTITION, BufferedImage.TYPE_INT_RGB);
		g = UI1.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(font);
		linePos = MARGIN * 0.875f;
		XBOX = LONGESTNAME + MARGIN;

		g.setColor(grey[0]); // fill bg color
		g.fillRect(0, 0, UI1.getWidth(), UI1.getHeight());

		g.setColor(Color.white); // title text
		g.drawString("TRIGGERS", MARGIN, linePos + TEXTOFFSET);
		linePos += LINEHEIGHT + MARGIN;

		g.setStroke(new BasicStroke(2)); // box outlines and trigger labels
		iterator = 0;
		for (Entry<String, Trigger> entry : triggerData.entrySet()) {
			int x = (iterator % 2 == 0 ? MARGIN : MARGIN + SIDEBARWIDTH / 2);

			g.setColor(entry.getValue().color);
			g.drawString(entry.getKey(), x + MARGIN + BOXSIZE, linePos + TEXTOFFSET);
			g.setColor(Color.white);

			// dont AA the outlines because they get shifted by a subpixel for whatever fucking reason
			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.drawRect(x, (int) (linePos + MARGIN / 8f), BOXSIZE, BOXSIZE);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (iterator++ % 2 == 1) {
				linePos += LINEHEIGHT;
			} ;
		} ;
		linePos += MARGIN * 1.625f;

		g.setColor(Color.white); // title text
		g.drawString("OUT", XBOX - fm.stringWidth("OUT") / 2 + BOXSIZE / 2, linePos + TEXTOFFSET);
		g.drawString("IN", (int) (XBOX - fm.stringWidth("IN") / 2 + BOXSIZE * 2.5), linePos + TEXTOFFSET);
		linePos += LINEHEIGHT;

		g.drawString("VALKYRIE TOGGLES", MARGIN, linePos + TEXTOFFSET); // title text
		linePos += MARGIN / 8f;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.drawRect(XBOX, (int) linePos, BOXSIZE, BOXSIZE); // box outlines
		g.drawRect(XBOX + BOXSIZE * 2, (int) (linePos), BOXSIZE, BOXSIZE);
		g.dispose();

		// UI 2
		UI2 = new BufferedImage(SIDEBARWIDTH, (int) (LINEHEIGHT * valks.size()), BufferedImage.TYPE_INT_RGB);
		g = UI2.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(font);
		linePos = MARGIN / 8f;

		g.setColor(grey[0]);
		g.fillRect(0, 0, UI2.getWidth(), UI2.getHeight());
		g.setColor(grey[1]);
		g.fillRect(0, 0, SIDEBARWIDTH - MARGIN * 2, UI2.getHeight());

		g.setColor(grey[2]);
		for (int i = 0; i < valks.size(); i += 2) { // Renders the alternating light-dark pattern first,
			g.fillRect(0, (int) (linePos), SIDEBARWIDTH - MARGIN * 2, (int) LINEHEIGHT);
			linePos += LINEHEIGHT * 2;
		} ;

		linePos = MARGIN * 0.25f;
		g.setStroke(new BasicStroke(2));
		for (Valkyrie valk : valks) { // Then renders names && boxes on top of that
			g.setColor(typeColorLight.get(valk.type));
			g.drawString(valk.name, MARGIN, linePos + TEXTOFFSET);

			g.setColor(Color.white);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.drawRect(XBOX, (int) (linePos + MARGIN / 8f), BOXSIZE, BOXSIZE);
			g.drawRect(XBOX + BOXSIZE * 2, (int) (linePos + MARGIN / 8f), BOXSIZE, BOXSIZE);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			linePos += LINEHEIGHT;
		} ;
		g.dispose();
	}

	public void addNotify() {
		super.addNotify();
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		} ;
		addKeyListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addComponentListener(this);
	};

	public void run() {
		running = true;

		long startTime;
		long totalTime = 0;
		long takenTime = 0;
		int frameCount = 0;
		long totalProcessTime = 0;
		long targetTime = 1000000000 / FPS;
		long waitDiff = 0;

		while (running) {
			startTime = System.nanoTime();

			gameUpdate();
			gameRender();
			gameDraw();

			// Calculating how long system needs to wait for
        	long processTime = System.nanoTime() - startTime;
        	long waitTime = targetTime - processTime + waitDiff;

			try {
				Thread.sleep(waitTime / 1000000);
			} catch(Exception e) {};

			takenTime = System.nanoTime() - startTime;
        	waitDiff = (long) (waitDiff*0.75 + (targetTime - takenTime)*0.25);

        	frameCount++;
        	totalTime += takenTime;
        	totalProcessTime += processTime;
        	if(totalTime >= 1000000000) {
        		System.out.print(frameCount + " ");
        		System.out.println(1 - totalProcessTime / 1000000000f);
        		frameCount = 0;
        		totalTime = 0;
        		totalProcessTime = 0;
        	};
		};
		System.exit(0);
	};

	public void gameUpdate() {
		// Exit Program
		if (keysHeld.contains(KeyEvent.VK_ESCAPE)) {
			running = false;
			return;
		} ;

		// Mouse hold and click interactions
		int hovPrev = updateMouse();

		// Updates valk and connection activity
		updateObjects();

		// Creates forces
		createForces();

		// Applies forces
		applyForces();

		// Updates darken effect
		updateDarken(hovPrev);
	}

	public void gameRender() {
		boolean updateUI = false;
		if (resized) {
			Dimension d = this.getSize();
			WIDTH = d.width;
			HEIGHT = d.height;

			image = new BufferedImage(WIDTH, HEIGHT,
					BufferedImage.TYPE_INT_RGB);
			g = image.createGraphics();
			g.setStroke(new BasicStroke(THICKNESS));
			updateUI = true;
			resized = false;
		} ;

		// draw background color
		g.setColor(Color.black);
		g.fillRect(SIDEBARWIDTH, 0, WIDTH - SIDEBARWIDTH, HEIGHT);

		// Drawing graph
		if(hovering == -1) {
			drawObjects(connections, valks);
		} else {
			drawObjects(connectLower, valksLower);
			drawObjects(connectUpper, valksUpper);
		};
			
		// UI // Works by drawing pre-rendered static images and overlaying with the interactive buttons
		drawUI(updateUI);
	};

	public void gameDraw() {
		Graphics2D g2 = (Graphics2D) this.getGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
	};

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (mousePos.x < SIDEBARWIDTH) {
			if (mousePos.y > UIPARTITION) {
				int notches = e.getWheelRotation();
				scrollPercent = func.clamp(scrollPercent + notches / 20f, 0, 1);
				scrolled = true;
			} ;
		} else {
			int notches = e.getWheelRotation();
			float coeff = 1 + notches / 20f;
			SPACING = (int) func.clamp(SPACING * coeff, 200, 1000);
			LOOSE = (int) func.clamp(LOOSE * coeff, 200 / 7, 1000 / 7);
		} ;
	};

	public void componentResized(ComponentEvent componentEvent) { // updates HEIGHT && width variables when window is resized
		resized = true;
	};

	public void keyPressed(KeyEvent e) { // stores which keys are being held when pressed
		int key = e.getKeyCode();

		if (!keysHeld.contains(key)) {
			keysHeld.add(key);
		};
	};

	public void keyReleased(KeyEvent e) { // removes which keys are being held when released
		int key = e.getKeyCode();

		keysHeld.remove(Integer.valueOf(key));
	};

	public void mousePressed(MouseEvent e) { // same thing for mouse buttons
		int key = e.getButton();

		if (!mouseHeld.contains(key)) {
			mouseHeld.add(key);
		};
		mouseClicked.add(key);
	}

	public void mouseReleased(MouseEvent e) {
		int key = e.getButton();

		mouseHeld.remove(Integer.valueOf(key));
	}

	public void componentHidden(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}

	BufferedImage loadImage(String input) {
		try {
			return ImageIO.read(new File("images/" + input));
		} catch (IOException exc) {
			System.out.println("Error opening image file: " + exc.getMessage());
		} ;
		return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	};

	private int updateMouse() {
		mousePos = MouseInfo.getPointerInfo().getLocation();
		Point componentPos = new Point(0, 0);
		if (this.isShowing()) {
			componentPos = this.getLocationOnScreen();
		} ;
		mousePos = func.pointSub(mousePos, componentPos);

		int hovPrev = hovering;
		if (dragging == -1) {
			hovering = -1;
			for (int i = 0; i < valks.size(); i++) {
				Point p = new Point((int) valks.get(i).x, (int) valks.get(i).y);
				if (valks.get(i).active && func.distance(p, mousePos) < SIZE / 2) { // Check if the mouse is on a valkyrie
					hovering = i;
					break;
				} ;
			} ;
		} ;
		if (mouseHeld.contains(MouseEvent.BUTTON1)) {
			if (mouseClicked.contains(MouseEvent.BUTTON1)) {
				// Detecting new clicks in the UI
				if (mousePos.x < SIDEBARWIDTH) {
					if (mousePos.y > 0 && mousePos.y < UIPARTITION) { // Click detection in trigger menu
						if (mousePos.x > MARGIN
								&& mousePos.x < MARGIN + SIDEBARWIDTH / 2 + BOXSIZE
								&& mousePos.y > MARGIN * 2 + LINEHEIGHT
								&& mousePos.y < MARGIN * 2 + LINEHEIGHT * (1 + Math.ceil(triggerData.size() / 2f))) {
							linePos = (int) (MARGIN * 2 + LINEHEIGHT); // Triggers
							int iterator = 0;
							for (Trigger trigger : triggerData.values()) {
								int x = (iterator % 2 == 0 ? MARGIN : MARGIN + SIDEBARWIDTH / 2);

								if (mousePos.x > x
										&& mousePos.x < x + BOXSIZE
										&& mousePos.y > linePos
										&& mousePos.y < linePos + TEXTOFFSET) {
									trigger.active = !trigger.active;
									activeChange = true;
								} ;

								if (iterator++ % 2 == 1) {
									linePos += LINEHEIGHT;
								} ;
							} ;
						} else if (mousePos.y > UIPARTITION - MARGIN - BOXSIZE && mousePos.y < UIPARTITION - MARGIN) { // In / Out
							if (mousePos.x > LONGESTNAME + MARGIN && mousePos.x < LONGESTNAME + MARGIN + BOXSIZE) {
								for (int i = 0; i < valks.size(); i++) {
									valks.get(i).activeOut = !outOn;
								} ;
								activeChange = true;
							} else if (mousePos.x > LONGESTNAME + MARGIN + BOXSIZE * 2 && mousePos.x < LONGESTNAME + MARGIN + BOXSIZE * 3) {
								for (int i = 0; i < valks.size(); i++) {
									valks.get(i).activeIn = !inOn;
								} ;
								activeChange = true;
							} ;
						} ;
					} else if (mousePos.y < HEIGHT) { // Click detection in scroll box
						if (mousePos.x > XBOX && mousePos.x < XBOX + BOXSIZE * 3) {
							float a = (HEIGHT - UIPARTITION) / LINEHEIGHT; // Equals the number of lines that can fit in the scroll box as a decimal
							int b = (int) Math.floor(scrollPercent * (valks.size() - a)); // Equals the index offset for changing the valks list; the starting index of dispalyed lines
							float c = (scrollPercent * (valks.size() * LINEHEIGHT - (HEIGHT - UIPARTITION))) % LINEHEIGHT; // Equals the pixel offset for each hitbox..?
							for (int i = 0; i < Math.floor(a) + 2; i++) { // Iterates through the lines that are shown in the scroll box, not the actual valks; offset is +1 for the floor && + 1 to account for when a line is partially off-screen
								float d = UIPARTITION - c + i * LINEHEIGHT + MARGIN / 4f; // Y starting point for each hitbox
								if (mousePos.y > d && mousePos.y < d + BOXSIZE) {
									if (mousePos.x < XBOX + BOXSIZE) {
										valks.get(i + b).activeOut = !valks.get(i + b).activeOut;
										activeChange = true;
									} else if (mousePos.x > XBOX + BOXSIZE * 2) {
										valks.get(i + b).activeIn = !valks .get(i + b).activeIn;
										activeChange = true;
									} ;
								} ;
							} ;
						} ;
					} ;
				} ;

				// Detecting new drag interactions
				if (dragging == -1) { // Check if nothing is being dragged
					if (hovering != -1) {
						Point p = new Point((int) valks.get(hovering).x, (int) valks.get(hovering).y);
						offset = func.pointSub(p, mousePos);
						dragging = hovering;
					} ;
					float a = scrollPercent * (HEIGHT - UIPARTITION - MARGIN * 4); // Check if the mouse is on the scrollbar
					if (dragging == -1 && mousePos.x > SIDEBARWIDTH - MARGIN * 2
							&& mousePos.x < SIDEBARWIDTH
							&& mousePos.y > UIPARTITION + a
							&& mousePos.y < UIPARTITION + a + MARGIN * 4) {
						dragging = -2;
						offset = new Point(0, (int) (UIPARTITION + a + MARGIN * 2 - mousePos.y));
					} ;
				} ;
			} ;

			// Determines how objects that are currently being dragged work
			if (dragging >= 0) { // If a valkyrie is being dragged...
				Valkyrie valk = valks.get(dragging);
				valk.x = mousePos.x + offset.x;
				valk.y = mousePos.y + offset.y;
				valk.xVel = 0f;
				valk.yVel = 0f;
			} else if (dragging == -2) { // If the scrollbar is being dragged...
				scrollPercent = func.clamp((mousePos.y - UIPARTITION - MARGIN * 2 + offset.y) / (float) (HEIGHT - UIPARTITION - MARGIN * 4), 0, 1);
				scrolled = true;
			} ;
		} else { // Dragging nothing
			dragging = -1;
		} ;
		if (hovering != -1 && mouseClicked.contains(MouseEvent.BUTTON3)) { // Hides valk
			valks.get(hovering).activeOut = false;
			valks.get(hovering).activeIn = false;
			activeChange = true;
		} ;
		mouseClicked.clear();
		return hovPrev;
	};

	private void updateObjects() {
		if (activeChange) {
			outOn = true;
			inOn = true;
			scrolled = true;

			for (Valkyrie valk : valks) {
				valk.active = false;
				if (!valk.activeOut) {
					outOn = false;
				} ;
				if (!valk.activeIn) {
					inOn = false;
				} ;
			} ;
			for (Connection connection : connections) { // user input --> valk i/o activity OR triggers activity --> connection activity --> valk activity
				Valkyrie out = valks.get(connection.out);
				Valkyrie in = valks.get(connection.in);
				ArrayList<String> directActive = connection.directActive;
				ArrayList<String> reverseActive = connection.reverseActive;
				directActive.clear();
				reverseActive.clear();

				checkActivity(out, in, directActive, connection.directTriggers);
				checkActivity(in, out, reverseActive, connection.reverseTriggers);
			} ;
		} ;
	}

	private void checkActivity(Valkyrie out, Valkyrie in, ArrayList<String> actives, ArrayList<String> triggers) {
		if (out.activeOut && in.activeIn) {
			for (String triggerName : triggers) {
				if (triggerData.get(triggerName).active) {
					actives.add(triggerName);
					out.active = true;
					in.active = true;
				} ;
			} ;
		} ;
	}

	private void createForces() {
		if (!keysHeld.contains(KeyEvent.VK_SPACE)) {
			for (Connection connection : connections) {
				Valkyrie out = valks.get(connection.out);
				Valkyrie in = valks.get(connection.in);

				if (out.active && in.active) {
					float dx = out.x - in.x;
					float dy = out.y - in.y;
					double distance = Math.hypot(dx, dy);
					double angle = Math.atan2(dy, dx);
					boolean connected = !(connection.directActive.isEmpty() && connection.reverseActive.isEmpty());
					if (distance < SPACING - LOOSE || // Creates pushing force if both valkyries are active
							(connected && // Creates pulling force if valkyries are connected
							distance > SPACING + LOOSE)) {
						float base = (float) (1f - distance / SPACING);
						float force = base * Math.abs(base); // Squares the value while retaining the sign
						if (distance < SPACING - LOOSE && connected) {
							force *= 4f;
						} ;
						dx = (float) (Math.cos(angle) * force);
						dy = (float) (Math.sin(angle) * force);
						out.xVel += dx;
						out.yVel += dy;
						in.xVel -= dx;
						in.yVel -= dy;
					} ;
				} ;
			} ;
		} ;
	}

	private void applyForces() {
		for (int i = 0; i < valks.size(); i++) {
			Valkyrie valk = valks.get(i);
			if (valk.active) {
				int spriteWidth = valk.sprite.getWidth();
				int spriteHeight = valk.sprite.getHeight();
				if (i != dragging) {
					valk.x = func.clamp(valk.xVel + valk.x, SIDEBARWIDTH + spriteWidth / 2, WIDTH - spriteWidth / 2);
					valk.y = func.clamp(valk.yVel + valk.y, SIZE / 2, HEIGHT - (spriteHeight - SIZE / 2));
					valk.xVel *= DAMPENER;
					valk.yVel *= DAMPENER;
				} else {
					valk.x = func.clamp(valk.x, SIDEBARWIDTH + spriteWidth / 2, WIDTH - spriteWidth / 2);
					valk.y = func.clamp(valk.y, SIZE / 2, HEIGHT - (spriteHeight - SIZE / 2));
				} ;
			} ;
		} ;
	}
	
	private void updateDarken(int hovPrev) {
		if (hovering != hovPrev) {
			for (Valkyrie valk : valks) {
				valk.darken = hovering != -1;
			} ;
			if (hovering == -1) {
				for (Connection connection : connections) {
					connection.darken = false;
				} ;
			} else {
				HashSet<Valkyrie> vu = new HashSet<Valkyrie>();
				HashSet<Valkyrie> vl = new HashSet<Valkyrie>(valks);
				connectUpper = new ArrayList<Connection>();
				connectLower = new ArrayList<Connection>();

				for (Connection connection : connections) {
					if (!connection.directActive.isEmpty() || !connection.reverseActive.isEmpty()) {
						connection.darken = true;
						Valkyrie out = valks.get(connection.out);
						Valkyrie in = valks.get(connection.in);
						if (connection.out == hovering || connection.in == hovering) {
							connection.darken = false;
							out.darken = false;
							in.darken = false;

							vu.add(out);
							vu.add(in);
							vl.remove(out);
							vl.remove(in);
							connectUpper.add(connection);
						} else {
							connectLower.add(connection);
						} ;
					} ;
				} ;

				valksUpper = new ArrayList<Valkyrie>(vu);
				valksLower = new ArrayList<Valkyrie>(vl);
			} ;
		} ;
	}

	private void drawUI(boolean updateUI) {
		// Scroll menu
		if (scrolled || updateUI) {
			int scrollPos = (int) (scrollPercent * (valks.size() * LINEHEIGHT - (HEIGHT - UIPARTITION)));
			UI2Part = func.deepCopy(UI2, 0, scrollPos, SIDEBARWIDTH, HEIGHT - UIPARTITION);
			Graphics2D g2 = UI2Part.createGraphics();

			linePos = MARGIN * 0.375f;
			for (Valkyrie valk : valks) {
				g2.setColor(Color.white);
				if (valk.activeOut) {
					g2.fillRect(XBOX + 4, (int) (-scrollPos + linePos + 4), BOXSIZE - 8, BOXSIZE - 8);
				} ;
				if (valk.activeIn) {
					g2.fillRect(XBOX + BOXSIZE * 2 + 4, (int) (-scrollPos + linePos + 4), BOXSIZE - 8, BOXSIZE - 8);
				} ;
				linePos += LINEHEIGHT;
			} ;
			g2.dispose();
			scrolled = false;
			g.drawImage(UI2Part, 0, UIPARTITION, null);

			g.setColor(grey[3]); // scroll cursor thingy
			g.fillRect(SIDEBARWIDTH - MARGIN * 2, (int) (UIPARTITION + scrollPercent * (HEIGHT - UIPARTITION - MARGIN * 4)), MARGIN * 2, MARGIN * 4);

			// Triggers menu
			if (activeChange || updateUI) {
				g.drawImage(UI1, 0, 0, null);

				UI1Part = new BufferedImage(UI1.getWidth(), UI1.getHeight(), BufferedImage.TYPE_INT_ARGB);
				g2 = UI1Part.createGraphics();

				linePos = MARGIN * 1.875f + LINEHEIGHT;
				g2.setColor(Color.white);
				int iterator = 0;
				for (Trigger trigger : triggerData.values()) {	
					if (trigger.active) {
						int x = (iterator % 2 == 0 ? MARGIN : MARGIN + SIDEBARWIDTH / 2) + 4;
						g2.fillRect(x, (int) (linePos + MARGIN / 8f + 4), BOXSIZE - 8, BOXSIZE - 8);
					} ;
					if (iterator++ % 2 == 1) {
						linePos += LINEHEIGHT;
					} ;
				} ;

				if (outOn) {
					g2.fillRect(XBOX + 4, (int) (UIPARTITION - LINEHEIGHT + 4 - MARGIN * 0.375f), BOXSIZE - 8, BOXSIZE - 8);
				} ;
				if (inOn) {
					g2.fillRect(XBOX + BOXSIZE * 2 + 4, (int) (UIPARTITION - LINEHEIGHT + 4 - MARGIN * 0.375f), BOXSIZE - 8, BOXSIZE - 8);
				} ;

				g2.dispose();
				activeChange = false;
				g.drawImage(UI1Part, 0, 0, null);
			} ;
		} ;
	};
	
	private void drawObjects(ArrayList<Connection> list1, ArrayList<Valkyrie> list2) {
		for (Connection connection : list1) {
			Valkyrie out = valks.get(connection.out);
			Valkyrie in = valks.get(connection.in);
			ArrayList<String> directActive = connection.directActive;
			ArrayList<String> reverseActive = connection.reverseActive;
			if (!directActive.isEmpty() || !reverseActive.isEmpty()) {
				float dx = out.x - in.x;
				float dy = out.y - in.y;
				double angle = Math.atan2(dy, dx);
				drawConnection(connection, in, directActive, dx, dy, angle);
				drawConnection(connection, out, reverseActive, -dx, -dy, angle + Math.PI);
				
				// draws line with the last rendered color
				g.drawLine((int) out.x, (int) out.y, (int) in.x, (int) in.y);
			} ;
		} ;
		
		for (Valkyrie valk : list2) {
			if (valk.active) {
				BufferedImage sprite = (valk.darken ? valk.spriteDark : valk.sprite);
				g.drawImage(sprite, (int) valk.x - sprite.getWidth() / 2, (int) valk.y - SIZE / 2, null);
			};
		};
	}

	private void drawConnection(Connection connection, Valkyrie valk, ArrayList<String> actives, float dx, float dy, double angle) {
		if (!actives.isEmpty()) {
			for (String triggerName : actives) {
				float coeff = triggerData.get(triggerName).coefficient;
				float pointCX = valk.x + dx * coeff;
				float pointCY = valk.y + dy * coeff;
				int[] pointX = new int[3];
				int[] pointY = new int[3];
				for (int i = 0; i < 3; i++) {
					double newAngle = angle + Math.PI * 2 / 3 * i;
					pointX[i] = (int) (pointCX - Math.cos(newAngle) * THICKNESS * 3);
					pointY[i] = (int) (pointCY - Math.sin(newAngle) * THICKNESS * 3);
				} ;

				Color drawColor = triggerData.get(triggerName).color;
				if (connection.darken) {
					drawColor = func.colorMultiply(drawColor, 0.75f, true);
				} ;
				g.setColor(drawColor);
				g.fillPolygon(pointX, pointY, 3);
			} ;
		} ;
	};
};