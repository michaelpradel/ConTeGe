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
import scala.collection.mutable.Set

/**
 * Central point to load classes under test (and helper classes to use the classes under test).
 * Always uses the putClassLoader, which may impose a stronger security policy
 * than the standard class loader.
 * 
 */
class TypeManager(cut: String, envClasses: Seq[String], val putClassLoader: ClassLoader, random: Random) {
	
    val primitiveProvider = new PrimitiveProvider(random)
  
	private val type2Atoms = Map[String, ArrayList[Atom]]() // includes subtyping
	private val type2AtomsPrecise = Map[String, ArrayList[Atom]]() // only precise matches
	private val allClasses = new ArrayList[String]
	allClasses.addAll(envClasses)
	allClasses.add(cut)
	allClasses.foreach(cls => {
		constructors(cls).foreach(atom => if(atom.returnType.isDefined) {
			allSuperTypes(atom.returnType).foreach(typ => {
				type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)	
			})
			type2AtomsPrecise.getOrElse(atom.returnType.get, new ArrayList[Atom]).add(atom)
		})
			
		methods(cls).foreach(atom => if(atom.returnType.isDefined) {
			allSuperTypes(atom.returnType).foreach(typ => {
			    type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)
			})			
			type2AtomsPrecise.getOrElseUpdate(atom.returnType.get, new ArrayList[Atom]).add(atom)
		})			
		
		fieldGetters(cls).foreach(atom => {
			assert(atom.returnType.isDefined) // each field should have a type
			allSuperTypes(atom.returnType).foreach(typ => {
				type2Atoms.getOrElseUpdate(typ, new ArrayList[Atom]).add(atom)
			})		
			type2AtomsPrecise.getOrElseUpdate(atom.returnType.get, new ArrayList[Atom]).add(atom)
		})
	})

	println("TypeManager has indexed "+allClasses.size+" classes")
	
	private var cutMethods_ = methods(cut)
	
	def cutMethods = {
	    cutMethods_
	}
	
	def filterCUTMethods(oracleClass: String) = {
	    val result = new ArrayList[MethodAtom]
	    val oracleClassMethods = methods(oracleClass)
	    result.addAll(cutMethods_.filter(cm => oracleClassMethods.exists(om => cm.methodName == om.methodName && cm.paramTypes == om.paramTypes)))
	    cutMethods_ = result
	}
	
	def atomGivingType(typ: String): Option[Atom] = {
		type2Atoms.get(typ) match {
			case Some(atoms) => Some(atoms(random.nextInt(atoms.size)))
			case None => None
		}
	}
	
	def atomGivingTypeWithDowncast(typ: String): Option[Atom] = {
	    val allFromTypes = allSuperTypes(Some(typ)).toSet
	    val filteredFromTypes = if (allFromTypes.contains("java.lang.Object") && allFromTypes.size > 1) {
	        allFromTypes.filter(_ != "java.lang.Object")
	    } else allFromTypes
	    
	    val allPotentialAtoms = Set[Atom]()
	    filteredFromTypes.foreach(fromType => allPotentialAtoms.addAll(allAtomsGivingPreciseType(fromType)))
	    val potentialAtoms = allPotentialAtoms.filter(_.isConstructor == false)
	    
	    if (potentialAtoms.size == 0) None
	    else Some(random.chooseOne(potentialAtoms.toSet))
	}
	
	def allAtomsGivingType(typ: String): ArrayList[Atom] = {
		type2Atoms.getOrElse(typ, new ArrayList[Atom])
	}
	
	def allAtomsGivingPreciseType(typ: String): ArrayList[Atom] = {
		type2AtomsPrecise.getOrElse(typ, new ArrayList[Atom])
	}

	/**
	 * Names of all supertypes (classes and interfaces), including the class itself.
	 */
	private def allSuperTypes(clsNameOpt: Option[String]): Seq[String] = {
		if (!clsNameOpt.isDefined) List[String]()
		else {
		    if (Util.primitiveTypes.contains(clsNameOpt.get)) List(clsNameOpt.get)
		    else {
			    val cls = Class.forName(clsNameOpt.get, true, putClassLoader)
			    allSuperTypes(cls)    
		    }
		}
	}
	    
	private def allSuperTypes(cls: Class[_]): Seq[String] = {
		val superTypes = new ArrayList[String]
		superTypes.add(cls.getName)
		if(cls.getName != "java.lang.Object") {
			cls.getInterfaces.foreach(itf => {
				superTypes.addAll(allSuperTypes(itf))
			})
			val sup = cls.getSuperclass
			if (sup != null) {
				superTypes.addAll(allSuperTypes(sup))
			}
		}
		superTypes.sortWith((t1,t2) => t1 < t2)
		superTypes
	}
	
	def constructors(clsName: String): Seq[ConstructorAtom] = {
		new ClassReader(Class.forName(clsName, true, putClassLoader)).readConstructorAtoms		
	}
	
	def methods(clsName: String): Seq[MethodAtom] = {
		new ClassReader(Class.forName(clsName, true, putClassLoader)).readMethodAtoms
	}
	
	def fieldGetters(clsName: String): Seq[FieldGetterAtom] = {
		new ClassReader(Class.forName(clsName, true, putClassLoader)).readFieldGetterAtoms
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
		result.sortWith((x,y) => x < y)
	}
	
}