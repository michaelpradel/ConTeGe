package contege.seqgen

import contege.GlobalState

class SeqTest(global: GlobalState) extends AbstractCallSequence[SeqTest](global) {

    def equivalentTo(that: AbstractCallSequence[_]): Boolean = {
        throw new RuntimeException("TODO")
    }
	
    def copy: SeqTest = {
        throw new RuntimeException("TODO")
    } 
    
	def getCutVariable: Variable = {
	    return null;
	}

	
    
}