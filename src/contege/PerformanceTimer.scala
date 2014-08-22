package contege

import contege.seqgen.Prefix
import contege.seqgen.Suffix
import scala.util.MurmurHash
import scala.collection.mutable.HashMap
import scala.collection.SortedMap
import java.util.concurrent.TimeUnit
import com.sun.tools.doclets.internal.toolkit.Configuration

class TestRun {

	val runningTimes = HashMap[Int, Long]()
	val runningTimesSeries = HashMap[Int, List[Long]]()
	var outString = ""

	def setVersionRunningTimesSeries(version: Int, runningTimes: List[Long]) {
		runningTimesSeries(version) = runningTimes
	}

	def setVersionRunningTime(version: Int, runningTime: Long) {
		runningTimes(version) = runningTime
	}

	def valid: Boolean = {
		//return repetitions(1).length == repetitions(2).length
		return (runningTimes.keySet.contains(1) && runningTimes.keySet.contains(2))
	}

	def calculateResult(writer: FileWriter, runID: String, nrThreads: Int, config: Config): (Int, Int, Int, Double) = {
		var tight = 0.05
		var clear = 0.1
		var veryClear = 0.25
		/*
		if (config.noisyEnvironment) {
			tight = 0.05
			clear = 0.1
			veryClear = 0.25
		}
		*/
		
		var tightWinner = 0
		var clearWinner = 0
		var veryClearWinner = 0
		var v1 = runningTimes(1)
		var v2 = runningTimes(2)
		var fasterVersion = if (v1 < v2) { 1 }  else { 2 }
		var ratio = if (v1 < v2) {
			(v2.toDouble / v1.toDouble) - 1
		} else {
			(v1.toDouble / v2.toDouble) - 1
		}
		var divider = "*" * 30 + "\n"
		var v1String = "v1: " + v1 + " (" + runningTimesSeries(1).toString + ")"
		var v2String = "v2: " + v2 + " (" + runningTimesSeries(2).toString + ")"
		var ratioString = " - ratio: " + ratio + " (" + nrThreads + ")"
		if (ratio >= tight) {
			tightWinner = fasterVersion
			if (ratio >= clear) {
				clearWinner = fasterVersion
				if (ratio >= veryClear) {
					veryClearWinner = fasterVersion
				}
			}
		}
		writer.appendResultMsg(divider)
		writer.appendResultMsg(outString)
		writer.appendResultMsg("nrThreads: " + nrThreads)
		writer.appendResultMsg(v1String)
		writer.appendResultMsg(v2String)
		writer.appendResultMsg(ratioString)
		writer.appendResultMsg(divider)
		return (tightWinner, clearWinner, veryClearWinner, ratio)
	}
}

class NrThreadsToResult {
	val testRuns = new HashMap[String, TestRun]()

	def getTestRun(runID: String): TestRun = {
		var testRun: TestRun = null
		if (testRuns.contains(runID)) {
			testRun = testRuns(runID)
		} else {
			testRun = new TestRun()
			testRuns(runID) = testRun
		}
		return testRun
	}

	def calculateResult(writer: FileWriter, nrThreads: Int, config: Config): (String, Int) = {
		var noWinner = 0
		var v1Winner = 0
		var v2Winner = 0
		var tightV1Winner = 0
		var tightV2Winner = 0
		var clearV1Winner = 0
		var clearV2Winner = 0
		var veryClearV1Winner = 0
		var veryClearV2Winner = 0
		var ratios = List[Double]()


		testRuns.foreach((entry) => {
			val id = entry._1
			val run = entry._2
			if (run.valid) {
				val (tightWinner, clearWinner, veryClearWinner, ratio) = run.calculateResult(writer, id, nrThreads, config)
				ratios ::= ratio
				if (tightWinner == 1) {
					v1Winner += 1
					if (clearWinner == 1) {
						if (veryClearWinner == 1) {
							veryClearV1Winner += 1
						} else {
							clearV1Winner += 1
						}
					} else {
						tightV1Winner += 1
					}
				} else if (tightWinner == 2) {
					v2Winner += 1
					if (clearWinner == 2) {
						if (veryClearWinner == 2) {
							veryClearV2Winner += 1
						} else {
							clearV2Winner += 1
						}
					} else {
						tightV2Winner += 1
					}
				} else {
					noWinner += 1
				}
			} else {
				writer.appendResultMsg(" - invalid run (" + id + ")")
			}
		})
		
		var tight = 5
		var clear = 10
		var veryClear = 25
		/*
		if (config.noisyEnvironment) {
			tight = 5
			clear = 10
			veryClear = 25
		}
		*/

		var winner = 0
		var winnerString = "Nr of Threads: " + nrThreads + "\n"
		winnerString += "========================\n"
		winnerString += "ratio average: " + avg(ratios) + "\n"
		winnerString += "winner none (" + noWinner + ")\n"
		winnerString += "winner v1 (-" + tight + "%: " + tightV1Winner + ", -" + clear + "%: " + clearV1Winner + ", -" + veryClear + "%: " + veryClearV1Winner + ")\n"
		winnerString += "winner v2 (+" + tight + "%: " + tightV2Winner + ", +" + clear + "%: " + clearV2Winner + ", +" + veryClear + "%: " + veryClearV2Winner + ")\n\n"

		if ((v1Winner >= noWinner && v1Winner > 1) || (v2Winner >= noWinner && v2Winner > 1)) {
			if (v1Winner > v2Winner) {
				winner = 1
			} else if (v2Winner > v1Winner) {
				winner = 2
			}
		}

		return (winnerString, winner)
	}
	
	private def avg(ratios: List[Double]): Double = {
      	ratios.foldLeft(0.0)(_+_) / ratios.length.toDouble
	}
}

class Results {

	var nrThreadsToResults = SortedMap[Int, NrThreadsToResult]()

	def getTestRun(nrThreads: Int, runID: String): TestRun = {
		if (nrThreadsToResults.contains(nrThreads)) {
			return nrThreadsToResults(nrThreads).getTestRun(runID)
		} else {
			val nrThreadsToResult = new NrThreadsToResult()
			nrThreadsToResults += (nrThreads -> nrThreadsToResult)
			return nrThreadsToResult.getTestRun(runID)
		}
	}

	def calculateResult(writer: FileWriter, config: Config): Int = {
		var noWinner = 0
		var v1Winner = 0
		var v2Winner = 0
		var threadWinners = ""

		nrThreadsToResults.foreach(entry => {
			val nrThreads = entry._1
			val results = entry._2

			val (winnerString, winner) = results.calculateResult(writer, nrThreads, config)
			threadWinners = threadWinners + winnerString + "\n"

			if (winner == 1) {
				v1Winner += 1
			} else if (winner == 2) {
				v2Winner += 1
			} else {
				noWinner += 1
			}
		})
		var winner = 0

		var winnerString = " --- winner none"
		if (v1Winner > 0 || v2Winner > 0) {
			if (v1Winner > v2Winner) {
				winnerString = " --- winner v1"
				winner = 1
			} else if (v2Winner > v1Winner) {
				winnerString = " --- winner v2"
				winner = 2
			}
		}

		val winnersString = "Total v1Winner: " + v1Winner + ", v2Winner: " + v2Winner + ", noWinner: " + noWinner
		println(threadWinners)
		println(winnerString)
		println(winnersString)
		writer.appendResultMsg(threadWinners)
		writer.appendResultMsg(winnersString)
		writer.appendResultMsg(winnerString)

		return winner
	}
}

class PerformanceTimer(config: Config) {
	// TODO: only count a test if there are results for each number of threads

	val results = new Results()

	/*
	def recordTestRunRepetition(version: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, runTime: Long, nrThreads: Int) {
		val testRun = results.getTestRun(nrThreads, generateID(prefix, suffix1, suffix2))
		testRun.outString = "Prefix:\n" + prefix.toString + ">>>>>>>>>>\nSuffixV1:\n" + suffix1.toString + "<<<<<<<<<<\nSuffixV2:\n" + suffix2.toString + "\n"
		testRun.addRepetition(version, runTime)
	}
	*/
	
	def recordTestRunSeries(version: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, runID: String, runningTimes: List[Long], nrThreads: Int) {
		val testRun = results.getTestRun(nrThreads, runID)
		testRun.outString = "TestHash:" + runID + "\nPrefix:\n" + prefix.toString + "\n>>>>>>>>>>\nSuffixV1:\n" + suffix1.toString + "<<<<<<<<<<\nSuffixV2:\n" + suffix2.toString + "\n"
		testRun.setVersionRunningTimesSeries(version, runningTimes)
	}

	def recordAvgTestRun(version: Int, prefix: Prefix, suffix1: Suffix, suffix2: Suffix, runID: String, runningTime: Long, nrThreads: Int) {
		val testRun = results.getTestRun(nrThreads, runID)
		testRun.outString = "TestHash:" + runID + "\nPrefix:\n" + prefix.toString + "\n>>>>>>>>>>\nSuffixV1:\n" + suffix1.toString + "<<<<<<<<<<\nSuffixV2:\n" + suffix2.toString + "\n"
		testRun.setVersionRunningTime(version, runningTime)
	}



	def write(config: Config) = {
		config.performanceSummaryWriter.appendResultMsg("\n==== Results for " + config.projectId + " ====")
		results.calculateResult(config.performanceSummaryWriter, config)
	}
}