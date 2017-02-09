package microbenchmark.timeout;

public class CUT {
	
	public void m() {
		
	}
	
	public void verySlow() {
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
