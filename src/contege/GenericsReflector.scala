package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.lang.reflect._
import scala.collection.JavaConversions._

/**
 * Computes (parameter|return) types of methods, constructors and fields
 * while considering statically declared type parameters of superclasses.
 * This information is not easily available with the Java reflection facilities.
 * All returned types are erased types, i.e. types that can be represented
 * by a java.lang.Class.   
 *
 * See http://www.artima.com/weblogs/viewpost.jsp?thread=208860
 */
object GenericsReflector {

    def getParameterTypes(m: Method, receiverCls: Class[_]): Seq[String] = {
        val typeVar2Class = resolveTypeVars(receiverCls)
        val result = new ArrayList[String]()
        val rawParamTypes = m.getParameterTypes
        var pos = 0
        m.getGenericParameterTypes.foreach(pt => {
            val cls = if (pt.isInstanceOf[TypeVariable[_]]) {
                          if (typeVar2Class.contains(pt.asInstanceOf[TypeVariable[_]])) typeVar2Class(pt.asInstanceOf[TypeVariable[_]])
                          else rawParamTypes(pos)
                      } else {
                    	  var c = getClass(pt)
                    	  if (c == null) c = rawParamTypes(pos) // may happen for arrays of unbounded type parameters
                    	  c
                      }	
            result.add(cls.getName)
            pos += 1
        })
        result
    }
    
    def getReturnType(m: Method, receiverCls: Class[_]): String = {
        val typeVar2Class = resolveTypeVars(receiverCls)
        val genRetType = m.getGenericReturnType
        val cls = if (genRetType.isInstanceOf[TypeVariable[_]] && typeVar2Class.contains(genRetType)) typeVar2Class(genRetType.asInstanceOf[TypeVariable[_]])
                  else m.getReturnType
        cls.getName
    }
    
    def getType(f: Field, receiverCls: Class[_]): String = {
        val typeVar2Class = resolveTypeVars(receiverCls)
        val genFieldType = f.getGenericType
        val cls = if (genFieldType.isInstanceOf[TypeVariable[_]] && typeVar2Class.contains(genFieldType.asInstanceOf[TypeVariable[_]])) typeVar2Class(genFieldType.asInstanceOf[TypeVariable[_]])
                  else f.getType
        cls.getName
    }
    
    def getParameterTypes(c: Constructor[_], receiverCls: Class[_]): Seq[String] = {
        val typeVar2Class = resolveTypeVars(receiverCls)
        val result = new ArrayList[String]()
        val rawParamTypes = c.getParameterTypes
        var pos = 0
        c.getGenericParameterTypes.foreach(pt => {
            val cls = if (pt.isInstanceOf[TypeVariable[_]]) {
                          if (typeVar2Class.contains(pt.asInstanceOf[TypeVariable[_]])) typeVar2Class(pt.asInstanceOf[TypeVariable[_]])
                          else rawParamTypes(pos)
                      } else {
                    	  var c = getClass(pt)
                    	  if (c == null) c = rawParamTypes(pos) // may happen for arrays of unbounded type parameters
                    	  c
                      }	
            result.add(cls.getName)
            pos += 1
        })
        result
    }
    
    private def getClass(t: Type): Class[_] = {
	    if (t.isInstanceOf[Class[_]]) {
	    	return t.asInstanceOf[Class[_]]
	    } else if (t.isInstanceOf[ParameterizedType]) {
	    	return getClass(t.asInstanceOf[ParameterizedType].getRawType)
	    } else if (t.isInstanceOf[GenericArrayType]) {
	    	val componentType = t.asInstanceOf[GenericArrayType].getGenericComponentType
	    	val componentClass = getClass(componentType)
	    	if (componentClass != null ) return Array.newInstance(componentClass, 0).getClass
	    	else return null
	    } else return null
	}
    
    private def resolveTypeVars(cls: Class[_]) = {
        // step 1: walk up class hierarchy to map type vars to their values (which again can be type vars)
    	val typeVar2Type = Map[TypeVariable[_], Type]()
        var currentCls: Type = cls // set to null if we reach java.lang.Object
        while (currentCls != null) {
            if (currentCls.isInstanceOf[Class[_]]) {
                currentCls = currentCls.asInstanceOf[Class[_]].getGenericSuperclass
            } else {
                val parameterizedType = currentCls.asInstanceOf[ParameterizedType]
                val rawCls = parameterizedType.getRawType.asInstanceOf[Class[_]]
                val actTypeParams = parameterizedType.getActualTypeArguments
                var pos = 0
                rawCls.getTypeParameters.foreach(typeVar => {
                    typeVar2Type.put(typeVar, actTypeParams(pos))
                    pos += 1
                })
                currentCls = rawCls.getGenericSuperclass
            }
        }
        
        // step 2: recursively resolve type vars and map them to a Class
    	val typeVar2Class = Map[TypeVariable[_], Class[_]]()
    	typeVar2Type.keys.foreach(typeVar => {
    	    var t = typeVar2Type(typeVar)
    	    while (t.isInstanceOf[TypeVariable[_]] && typeVar2Type.contains(t.asInstanceOf[TypeVariable[_]])) {
    	        t = typeVar2Type(t.asInstanceOf[TypeVariable[_]])
    	    }
    	    val cls = getClass(t)
    	    if (cls != null) typeVar2Class.put(typeVar, cls)    	    
    	})
    	typeVar2Class
    }
    
}

