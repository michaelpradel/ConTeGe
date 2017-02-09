package contege

import scala.collection.mutable.Set

object ExcludedMethods {

	val methods = Set[String]()

	// methods that block until they can return something -- lead to too many timeouts
	methods.add("public java.lang.Object java.util.concurrent.ArrayBlockingQueue.take() throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.ArrayBlockingQueue.put(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.LinkedBlockingDeque.take() throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.LinkedBlockingDeque.takeFirst() throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.LinkedBlockingDeque.takeLast() throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.LinkedBlockingDeque.put(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.LinkedBlockingDeque.putFirst(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.LinkedBlockingDeque.putLast(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.LinkedBlockingQueue.take() throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.LinkedBlockingQueue.put(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.PriorityBlockingQueue.take() throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.PriorityBlockingQueue.put(java.lang.Object) throws java.lang.InterruptedException")
	methods.add("public java.lang.Object java.util.concurrent.SynchronousQueue.take() throws java.lang.InterruptedException")
	methods.add("public void java.util.concurrent.SynchronousQueue.put(java.lang.Object) throws java.lang.InterruptedException")
	
	// setParent shouldn't be called from app code (cf docu) - setting parent to this leads to endless loop
	methods.add("public void java141.util.logging.Logger.setParent(java141.util.logging.Logger)")
	
}