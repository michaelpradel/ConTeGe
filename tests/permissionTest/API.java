package permissionTest;

import java.io.FileOutputStream;

public class API {

	public void m() throws Throwable {
		new FileOutputStream("/tmp/test");
	}
}
