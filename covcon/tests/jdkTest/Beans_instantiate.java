package jdkTest;

public class Beans_instantiate {

	public static void main(String[] args) throws Throwable {
		Object var6 = java.beans.Beans.instantiate(null, "sun.misc.Cache", null, null);
		System.out.println(var6.getClass());
		
		Class.forName("sun.misc.Cache");
	}

}
