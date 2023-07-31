import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Valkyrie {
	String name = "", type = "", conflictID = "";
	ArrayList<String> effects = new ArrayList<String>(),
			triggers = new ArrayList<String>();
	BufferedImage sprite, spriteDark;
	float x, y, xVel, yVel;
	boolean active = true, activeOut = true, activeIn = true, darken = false;
	
	public Valkyrie() {};
}
