package permissionTest;

import java.lang.reflect.Method;


public class PermissionsTest {

	public static void main(String[] args) throws Throwable {
//		Class.forName("sun.misc.Unsafe");
		
//		ClassLoaderHelper2.load("sun.misc.Unsafe");
		
		API a = new API();
		Method m = a.getClass().getDeclaredMethods()[0];
		
//		ClassLoaderHelper2.invoke(m, a);
		
		
	}

}
