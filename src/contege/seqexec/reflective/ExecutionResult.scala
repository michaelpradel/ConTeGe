package contege.seqexec.reflective

/**
 * Result of executing an Atom reflectively.
 */
abstract class ExecutionResult

case class Exception(t: Throwable) extends ExecutionResult

case class Normal(returnValue: Object) extends ExecutionResult

object OK extends ExecutionResult