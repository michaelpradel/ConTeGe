package contege.seqgen

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import contege.ClassReader
import contege.Random
import contege.Atom
import contege.ConstructorAtom
import contege.MethodAtom
import contege.Util
import contege.Config
import javamodel.util.TypeResolver
import contege.FieldGetterAtom
import javamodel.staticc.UnknownType
import java.io.File
import contege.CustomClassLoader
import contege.GlobalState

/**
 * Central point to load classes under test (and helper classes to use the classes under test).
 * Always uses the putClassLoader, which may impose a stronger security policy
 * than the standard class loader.
 *
 */
class TypeManager(cut: String, envClasses: Seq[String], val classLoaderV0: CustomClassLoader, val classLoaderV1: CustomClassLoader,
	val classLoaderV2: CustomClassLoader, random: Random, config: Config, excludedMethods: List[String]) {

	val primitiveProvider = new PrimitiveProvider(random)

	private val type2Atoms = Map[String, ArrayList[Atom]]()
	private val allClasses = new ArrayList[String]

	allClasses.addAll(envClasses)
	allClasses.add(cut)
	allClasses.foreach(cls => {
		constructors(cls).foreach(atom => if (atom.returnType.isDefined) {
			allSuperTypes(atom.returnType).foreach(typ => {
				if (config.debugLevel > 0) {
					println("constructor for typ: " + typ + " found: " + atom.toString)
				}
				type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)
			})
			if (config.debugLevel > 0) {
				println("cls: " + cls + " con: " + atom.toString)
			}

		})

		methods(cls).foreach(atom => {
			allSuperTypes(atom.returnType).foreach(typ => {
				type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)
			})
			if (config.debugLevel > 0) {
				println("cls: " + cls + " mth: " + atom.toString)
			}

		})

		fieldGetters(cls).foreach(atom => {
			if (config.debugLevel > 0) {
				println("cls: " + cls + " fld: " + atom.toString)
			}
			assert(atom.returnType.isDefined) // each field should have a type
			allSuperTypes(atom.returnType).foreach(typ => {
				type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)
			})
		})
	})

	private var cutMethods_ = methods(cut)
	assert(!cutMethods_.isEmpty, cut)

	def cutMethods = cutMethods_

	def filterCUTMethods(oracleClass: String) = {
		val result = new ArrayList[MethodAtom]
		val oracleClassMethods = methods(oracleClass)
		result.addAll(cutMethods_.filter(cm => oracleClassMethods.exists(om => cm.methodName == om.methodName && cm.paramTypes == om.paramTypes)))
		cutMethods_ = result
	}

	def atomGivingType(typ: String): Option[Atom] = {
		type2Atoms.get(typ) match {
			case Some(atoms) => {
				Some(atoms(random.nextInt(atoms.size)))
			}
			case None => None
		}
	}

	def allAtomsGivingType(typ: String): ArrayList[Atom] = {
		type2Atoms.getOrElse(typ, new ArrayList[Atom])
	}

	/**
	 * Names of all supertypes (classes and interfaces), including the class itself.
	 */
	private def allSuperTypes(clsNameOpt: Option[String]): Seq[String] = {
		if (!clsNameOpt.isDefined) List[String]()
		else {
			if (Util.primitiveTypes.contains(clsNameOpt.get)) List(clsNameOpt.get)
			else {
				val clsV0 = Class.forName(clsNameOpt.get, true, classLoaderV0)
				val clsV1 = Class.forName(clsNameOpt.get, true, classLoaderV1)
				val clsV2 = Class.forName(clsNameOpt.get, true, classLoaderV2)
				var result = Set[String]()
				allSuperTypes(clsV0).foreach(clsString => {
					if (allSuperTypes(clsV1).contains(clsString) && allSuperTypes(clsV2).contains(clsString)) {
						result.add(clsString)
					}
				})
				return result.toSeq
			}
		}
	}

	private def allSuperTypes(cls: Class[_]): Seq[String] = {
		val superTypes = new ArrayList[String]
		superTypes.add(cls.getName)
		if (cls.getName != "java.lang.Object") {
			cls.getInterfaces.foreach(itf => {
				superTypes.addAll(allSuperTypes(itf))
			})
			val sup = cls.getSuperclass
			if (sup != null) {
				superTypes.addAll(allSuperTypes(sup))
			}
		}
		superTypes.sortWith((t1, t2) => t1 < t2)
		superTypes
	}

	def constructors(clsName: String): Seq[ConstructorAtom] = {
		val constructorsV0 = new ClassReader(Class.forName(clsName, true, classLoaderV0)).readConstructorAtoms
		val constructorsV1 = new ClassReader(Class.forName(clsName, true, classLoaderV1)).readConstructorAtoms
		val constructorsV2 = new ClassReader(Class.forName(clsName, true, classLoaderV2)).readConstructorAtoms
		var result = Set[ConstructorAtom]()
		constructorsV0.foreach(constructor => {
			if (constructorsV1.contains(constructor) && constructorsV2.contains(constructor) && !excludedMethods.contains(constructor.toString)) {
				result.add(constructor)
			}
		})
		return result.toSeq
	}

	def methods(clsName: String): Seq[MethodAtom] = {
		val methodsV0 = new ClassReader(Class.forName(clsName, true, classLoaderV0)).readMethodAtoms
		val methodsV1 = new ClassReader(Class.forName(clsName, true, classLoaderV1)).readMethodAtoms
		val methodsV2 = new ClassReader(Class.forName(clsName, true, classLoaderV2)).readMethodAtoms
		var result = Set[MethodAtom]()
		methodsV0.foreach(method => {
			if (methodsV1.contains(method) && methodsV2.contains(method) && !excludedMethods.contains(method.toString)) {
				result.add(method)
			}
		})
		return result.toSeq
	}

	def fieldGetters(clsName: String): Seq[FieldGetterAtom] = {
		val gettersV0 = new ClassReader(Class.forName(clsName, true, classLoaderV0)).readFieldGetterAtoms
		val gettersV1 = new ClassReader(Class.forName(clsName, true, classLoaderV1)).readFieldGetterAtoms
		val gettersV2 = new ClassReader(Class.forName(clsName, true, classLoaderV2)).readFieldGetterAtoms
		var result = Set[FieldGetterAtom]()
		gettersV0.foreach(getter => {
			if (gettersV1.contains(getter) && gettersV2.contains(getter) && !excludedMethods.contains(getter.toString)) {
				result.add(getter)
			}
		})
		return result.toSeq
	}

	def allSuperTypesAndItself(typeName: String) = {
		val result = new ArrayList[String]()
		result.add(typeName)
		if (!Util.primitiveTypes.contains(typeName)) {
			val typ = TypeResolver.resolve(typeName)
			if (typ != null && typ != UnknownType) {
				result.addAll(typ.allSuperTypes.map(_.qualName))
			}
		}
		result.sortWith((x, y) => x < y)
	}

}