package jdkTest;

import com.sun.jmx.mbeanserver.JmxMBeanServer;
import com.sun.jmx.mbeanserver.JmxMBeanServerBuilder;
import com.sun.jmx.mbeanserver.MBeanInstantiator;

public class MBeanInstantiator_findClass {

	public static void main(String[] args) throws Exception {
		JmxMBeanServer beanServer = (JmxMBeanServer) new JmxMBeanServerBuilder().newMBeanServer("", null, null);
		MBeanInstantiator beanInstantiator = beanServer.getMBeanInstantiator();
		ClassLoader a = null;
		Class contextClass = beanInstantiator.findClass("sun.org.mozilla.javascript.internal.Context", a);
		Class generatedClassLoaderClass = beanInstantiator.findClass("sun.org.mozilla.javascript.internal.GeneratedClassLoader", a);
		
		System.out.println(generatedClassLoaderClass);

	}

}
