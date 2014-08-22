package microbenchmark.subclassing;

import java.util.NoSuchElementException;
import java.util.Vector;

@SuppressWarnings("serial")
public class BrokenVector extends Vector<Object> {

	public Object firstElement() {  // 'synchronized' removed
		if (elementCount == 0) {
			throw new NoSuchElementException();
		}
		return elementData[0];
	}
	
	public synchronized int size() {
		if (elementCount > 0) {     // new code inserted (but essentially a no-op)
			Object o = remove(0);
			trimToSize();
			insertElementAt(o, 0);
		}
		return elementCount;
	}
	
}
