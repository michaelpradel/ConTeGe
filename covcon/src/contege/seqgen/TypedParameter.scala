package contege.seqgen

class TypedParameter(val typ: String, val valueAsString: String) {
	def getConstant = {
	    val value = typ match {
	        case "java.lang.String" => valueAsString
	        // TODO add other types (all primitives, at least)
	    }
	    
	    new Constant(value)
	}
}