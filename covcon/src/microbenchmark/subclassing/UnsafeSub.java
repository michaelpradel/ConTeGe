package microbenchmark.subclassing;

public class UnsafeSub extends ImmutableSuper {
	
	public UnsafeSub(Object o) {
		super(o);
	}
	
	public void replace(Object y) {
		l.clear();
//		try {
//			Thread.sleep(20);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		// switch to other thread
		l.add(y);
	}
}
