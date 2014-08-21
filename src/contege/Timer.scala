package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList

class Timer {

	private val task2Sum = Map[String, Long]()
	
	private val task2Start = Map[String, Long]()
		
	def start(task: String) = {
		task2Start.put(task, System.currentTimeMillis)
	}
	
	def stop(task: String) = {
		val now = System.currentTimeMillis
		val startTime = task2Start(task)
		var sum = task2Sum.getOrElse(task, 0L)
		sum += (now - startTime)
		task2Sum.put(task, sum)
	}
	
	def print = {
		val sorted = task2Sum.toList.sortWith((x,y) => x._2 > y._2)
		println("--- Timer: ")
		for ((task, time) <- sorted) {
			println(time+" --- "+task)
		}
		println("-----------(end)")
	}
	
	
}