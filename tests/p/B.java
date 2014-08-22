package p;

public class B implements IB {

	private B() {}
	
	public static IB createIB() {
		return new B();
	}
	
}
