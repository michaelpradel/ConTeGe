package microbenchmark.simplecopy;

import java.util.ArrayList;

public class CUTCopy {

	private ArrayList shared = new ArrayList();
	
	public void toggleListNull() {
		if (shared == null) {
			shared = new ArrayList();
		} else {
			shared = null;
		}
	}
	
	public int testAndUse() {
		if (shared != null) {
			// preempt here, please
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return shared.size();  // potential NPE
		}
		return -1;
	}
	
}
