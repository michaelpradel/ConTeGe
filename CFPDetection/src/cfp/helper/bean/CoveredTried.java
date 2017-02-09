package cfp.helper.bean;

import java.math.BigInteger;

public class CoveredTried {
	
	private BigInteger covered=BigInteger.ZERO;
	
	private BigInteger tried=BigInteger.ZERO;

	public CoveredTried(BigInteger covered, BigInteger tried) {
		super();
		this.covered = covered;
		this.tried = tried;
	}

	public BigInteger getCovered() {
		return covered;
	}

	public void setCovered(BigInteger covered) {
		this.covered = covered;
	}
	
	public BigInteger getTried() {
		return tried;
	}

	public void setTried(BigInteger tried) {
		this.tried = tried;
	}
	
	

}
