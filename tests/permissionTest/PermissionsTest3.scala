package permissionTest

import java.net.URLClassLoader
import java.net.URL

object PermissionsTest3 extends App {

    val unsafeLoader = getClass.getClassLoader
    val urls = Array(new URL("file:///home/m/research/experiments/java_permissions/rt_1.7.0_3_copy.jar"))
    val safeLoader = new URLClassLoader(urls)
    
    val cls = Class.forName("java.io.FileOutputStream", true, safeLoader)
    
    val constructor = cls.getConstructors().find(c => c.getParameterTypes.length == 1 && c.getParameterTypes.apply(0).getName == "java.lang.String").get
    
    constructor.newInstance("/tmp/test")
    
}