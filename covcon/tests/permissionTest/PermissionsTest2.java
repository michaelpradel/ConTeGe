package permissionTest;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

public class PermissionsTest2 {
	public static void main(String[] args) throws Throwable {

		ClassLoader unsafeLoader = "".getClass().getClassLoader();
		URL[] urls = new URL[1];
		urls[0] = new URL("file:///home/m/research/experiments/java_permissions/rt_1.7.0_3_copy.jar");
		ClassLoader safeLoader = new URLClassLoader(urls);
		
		Class cls = Class.forName("java.io.FileOutputStream", true, safeLoader);
		
		for (Constructor constr : cls.getConstructors()) {
			if (constr.getParameterTypes().length == 1 &&
					constr.getParameterTypes()[0].equals("java.lang.String")) {
				constr.newInstance("/tmp/test");
			}
		}
		
	}
}
