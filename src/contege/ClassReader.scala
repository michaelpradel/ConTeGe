package contege

import scala.collection.mutable.Set
import java.lang.reflect._

/**
 * Reads methods, constructors, and fields of a class,
 * ignoring all members that can't be called in a generated test
 * (i.e., only public, non-abstract etc. members are considered).
 */
class ClassReader(val cls: Class[_]) {

	def readMethodAtoms = {
		val atoms = Set[MethodAtom]()
		cls.getMethods.foreach(m => if (Modifier.isPublic(m.getModifiers) &&
			!m.isSynthetic &&
			!Modifier.isAbstract(m.getModifiers) &&
			m.getName != "XXXmyClinitXXX" &&
			m.getDeclaringClass.getName != "java.lang.Object" &&
			!ExcludedMethods.methods.contains(m.toString)) {
			atoms.add(new MethodAtom(cls, m))
		})
		atoms.toSeq.sortWith((x, y) => x.toString < y.toString)
	}

	def readConstructorAtoms = {
		val atoms = Set[ConstructorAtom]()
		if (!Modifier.isAbstract(cls.getModifiers)) {
			cls.getConstructors.foreach(c => if (Modifier.isPublic(c.getModifiers) &&
				!c.isSynthetic &&
				!Modifier.isAbstract(c.getModifiers)) atoms += new ConstructorAtom(cls, c))
		}
		atoms.toSeq.sortWith((x, y) => x.toString < y.toString)
	}

	def readFieldGetterAtoms = {
		val atoms = Set[FieldGetterAtom]()
		cls.getFields.foreach(f => if (Modifier.isPublic(f.getModifiers) &&
			!f.isSynthetic &&
			!Modifier.isAbstract(f.getModifiers)) atoms += new FieldGetterAtom(cls, f))
		atoms.toSeq.sortWith((x, y) => x.toString < y.toString)
	}

}
