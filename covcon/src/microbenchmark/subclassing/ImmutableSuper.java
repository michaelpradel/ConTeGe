package microbenchmark.subclassing;

import java.util.ArrayList;
import java.util.List;

public class ImmutableSuper {

	protected final List l = new ArrayList();
	
	public ImmutableSuper(Object o) {
		l.add(o);
	}
	
	public ImmutableSuper(int notImportant, Object o) {
		this(o);
	}
	
	public Object get() {
//		try {
//			Thread.sleep(20);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		return l.get(0);
	}
	
	public void replace(Object y) {
		// do nothing, this class is immutable
	}
	
}
