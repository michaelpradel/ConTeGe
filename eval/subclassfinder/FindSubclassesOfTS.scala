package subclassfinder

import java.io.File
import java.io.FileInputStream
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import java.lang.reflect.Method

object FindSubclassesOfTS extends App {

    val dir = "/home/m/temp/qualitasJars"
        
   	var nbClasses = 0
   	var trivialSubs = 0
   	var nonTrivialSubs = 0
   	var nonPublicSubs = 0
   	var abstractSubs = 0
    val tsClasses = Set("java/util/Vector", "java/util/Stack", "java/util/Timer", "java/util/Properties", "java/util/logging/Logger", "java/util/logging/LogManager", "java/util/concurrent/ConcurrentHashMap", "java/util/concurrent/ArrayBlockingQueue", "java/util/concurrent/ConcurrentLinkedQueue", "java/util/concurrent/ConcurrentSkipListMap", "java/util/concurrent/ConcurrentSkipListSet", "java/util/concurrent/CopyOnWriteArrayList", "java/util/concurrent/CopyOnWriteArraySet", "java/util/concurrent/DelayQueue", "java/util/concurrent/LinkedBlockingDeque", "java/util/concurrent/LinkedBlockingQueue", "java/util/concurrent/PriorityBlockingQueue", "java/util/concurrent/SynchronousQueue", "java/util/concurrent/atomic/AtomicBoolean", "java/util/concurrent/atomic/AtomicInteger", "java/util/concurrent/atomic/AtomicIntegerArray", "java/util/concurrent/atomic/AtomicIntegerFieldUpdater", "java/util/concurrent/atomic/AtomicLong", "java/util/concurrent/atomic/AtomicLongArray", "java/util/concurrent/atomic/AtomicLongFieldUpdater", "java/util/concurrent/atomic/AtomicMarkableReference", "java/util/concurrent/atomic/AtomicReference", "java/util/concurrent/atomic/AtomicReferenceArray", "java/util/concurrent/atomic/AtomicReferenceFieldUpdater", "java/util/concurrent/atomic/AtomicStampedReference")
    val ts2sub = Map[String, Set[String]]()
        
    FileUtils.listFiles(new File(dir), Array("class"), true).foreach(classFile => {
        nbClasses += 1
        try {
	        val is = new FileInputStream(classFile)
	        val reader = new ClassReader(is)
	        val sup = reader.getSuperName
	        if (tsClasses.contains(sup)) {
	            // check if the class overrides at least one method
	            if (isMethodToTest(reader, sup)) {
	            	ts2sub.getOrElseUpdate(sup, Set[String]()).add(classFile.getAbsolutePath)
	            	nonTrivialSubs += 1
	            } else {
	                trivialSubs += 1
	            }
	        }
	        is.close    
        } catch {
            case e: ArrayIndexOutOfBoundsException => // ignore; seems to be a bug in ASM
        }
        
        if (nbClasses % 500 == 0) print
    })
    
    def isMethodToTest(reader: ClassReader, superClassName: String): Boolean = {
        val classNode = new ClassNode
        reader.accept(classNode, 0)
        
        // check if class is public and concrete
        if ((classNode.access & Opcodes.ACC_PUBLIC) != Opcodes.ACC_PUBLIC) {
            nonPublicSubs += 1
            return false
        }
        if ((classNode.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
            abstractSubs += 1
            return false
        }
        
        // check if it overrides any methods from superclass
        val superMethods = Class.forName(superClassName.replace('/', '.')).getMethods
        val methods = classNode.methods.map(_.asInstanceOf[MethodNode])
        
        methods.exists(m => {
        	superMethods.exists(superM => superM.getName == m.name && sameParamTypes(superM, m))
        })
    }
    
    def sameParamTypes(javaM: Method, asmM: MethodNode): Boolean = {
        val aTypes = Type.getArgumentTypes(asmM.desc)
        val jTypes = javaM.getParameterTypes
        if (jTypes.size != aTypes.size) return false
        for (i <- 0 to aTypes.size - 1) {
            if (jTypes(i).getName != aTypes(i).getClassName) return false
        }
        true
    }
    
    def print = {
        println("\n--------------------------------")
	    println("Total classes           : "+nbClasses)
	    println("Trivial subclasses      : "+trivialSubs)
	    println("   Non-public           : "+nonPublicSubs)
	    println("   Abstract             : "+abstractSubs)
	    println("Nontrivial subclasses   : "+nonTrivialSubs)
	    
	    for ((ts, subs) <- ts2sub) {
	    	println("Subclasses of "+ts+": "+subs.size)
	    	subs.foreach(s => println("  "+s))
	    }
        println("+++++++++++++++++++++++++++++++++")
    }    
    
}

