package contege

class SubclassTesterStats {

    val succJPFRuns = new IncrementableCounter
    val inconclusiveJPFRuns = new IncrementableCounter
    
    def print = {
    	println("\n-------------------------\nSuccessful / inconclusive JPF runs: "+succJPFRuns.get+" / "+inconclusiveJPFRuns.get)
    }
    
}
