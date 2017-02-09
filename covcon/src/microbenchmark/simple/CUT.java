package microbenchmark.simple;

import java.util.Random;

public class CUT {

	private Object o = new Object();
	
	public void toggle() {
		if (o == null) o = new Object();
		else o = null;
	}
	
	public int testAndUse() {
		if (o != null) return new Random().nextInt() * o.hashCode();
		else return -1;	
	}
	
	
	
}
