package microbenchmark.simplewithIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CUT {

	private ArrayList shared = new ArrayList();
	
	public void toggleListNull() {
		File f = new File("/tmp/blubbdibubb");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {}
		}
		
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
