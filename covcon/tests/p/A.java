package p;

public class A {

	public A(B b) {
		if (b == null) throw new IllegalArgumentException();
	}
	
	public Target getTarget(String s) {
		if (s.length() > 2) return new Target(s);
		else return null;
	}
	
	public String otherMethod() {
		return "";
	}
	
	public void yetAnotherMethod(int i) {
		
	}
}
 