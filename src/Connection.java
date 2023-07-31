import java.util.ArrayList;

public class Connection {
	int out = 0, in = 0;
	ArrayList<String> directTriggers = new ArrayList<String>(),
			reverseTriggers = new ArrayList<String>(),
			directActive = new ArrayList<String>(),
			reverseActive = new ArrayList<String>();
	boolean darken = false;
	
	public Connection(int out, int in) {
		this.out = out;
		this.in = in;
	};
}
