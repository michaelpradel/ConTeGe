package contege

trait Finalizer {
    var currentTest: Option[String] = None
    
	def finalizeAndExit(bugFound: Boolean)
}