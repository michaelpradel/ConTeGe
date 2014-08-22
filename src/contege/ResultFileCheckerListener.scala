package contege
import java.io.PrintStream
import java.io.File

class ResultFileCheckerListener(filePath: String, fileName: String) extends CheckerListener {

	private val directory = new File(filePath)
	
	if (!directory.exists) {
		directory.mkdirs
	}

	private var file = filePath + "/" + fileName

	private val ps = new PrintStream(file)

	def appendResultMsg(s: String) = {
		ps.println(s)
	}

	def notifyDoneAndBugFound(testCode: String) = {
		ps.flush
		ps.close
	}

	def notifyDoneNoBug = {
		ps.flush
		ps.close
	}

	// ignore everything that is not result-related
	def updateNbGeneratedTests(nb: Long) = {}
	def appendStatusMsg(s: String) = {}

}