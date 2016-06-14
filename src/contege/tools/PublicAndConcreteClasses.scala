package contege.tools

import scala.collection.JavaConversions._
import java.io.BufferedReader
import java.io.FileReader
import java.util.ArrayList
import java.lang.reflect.Modifier
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.PrintWriter
import java.util.jar.JarFile

/**
 * Find all public and concrete classes in a jar file.
 * Arguments:
 *  - a .jar file or a .txt file with class names
 *  - the destination for the output file
 * The jar file to be analyzed must also be in the classpath.
 */
object PublicAndConcreteClasses {
  def main(args: Array[String]): Unit = {

    assert(args.length == 2)

    val allClasses = new ArrayList[String]
    if (args(0).endsWith(".jar")) {
      val jarFile = new JarFile(args(0))
      val enum = jarFile.entries
      while (enum.hasMoreElements) {
        val entry = enum.nextElement();
        val name = entry.getName();
        if (name.matches("^.*\\.class$")) {
          allClasses.add(name.replaceAll("/", ".").substring(0, name.length - 6));
        }
      }
      jarFile.close
    } else {
      val allClassesFile = args(0)
      val r = new BufferedReader(new FileReader(allClassesFile))
      var line = r.readLine
      while (line != null) {
        allClasses.add(line)
        line = r.readLine
      }
      r.close
    }

    val out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(args(1))))
    allClasses.foreach(className => {
      try {
        val cls = Class.forName(className, true, getClass.getClassLoader)
        if (Modifier.isPublic(cls.getModifiers) &&
          !Modifier.isAbstract(cls.getModifiers) /* &&
	    			cls.getConstructors.exists(constr => Modifier.isPublic(constr.getModifiers))*/ ) {
          out.println(className)
        }
      } catch {
        case _: Throwable => // ignore unloadable classes
      }
    })
    out.close

  }
}
