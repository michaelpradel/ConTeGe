package microbenchmark.subclassing;

import java.util.ArrayList;
import java.util.List;

public class SuperWithInterface implements MyInterface {

	protected List<Integer> state = new ArrayList<Integer>();
	
	public int computeInt() {
		int sum = 0;
		synchronized (this) {
			for (Integer i : state) {
				sum += i;
			}	
		}
		return sum;
	}
	
	public void changeInternalState(int i) {
		synchronized (this) {
			if (state.size() > 2) {
				state.clear();
			}
			state.add(i);
		}
	}
	
}
