package contege;

public class CustomClassLoader extends ClassLoader {
	
	private ClassLoader systemClassLoader;
	private ClassLoader customClassLoader;
	
	public CustomClassLoader(ClassLoader systemClassLoader, ClassLoader customClassLoader) {
		super();
		this.systemClassLoader = systemClassLoader;
		this.customClassLoader = customClassLoader;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class loadClass(String name) {
		Boolean lookupInSystemClassLoader = false;
		Class returnClass = null;
		
		if (customClassLoader != null) {
			try {
				returnClass = customClassLoader.loadClass(name);
			} catch (ClassNotFoundException e) {
				lookupInSystemClassLoader = true;
			}
		}
		if (lookupInSystemClassLoader) {
			try {
				returnClass = systemClassLoader.loadClass(name);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}

		}
		return returnClass;
	}
}
