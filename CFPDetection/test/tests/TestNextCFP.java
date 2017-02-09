package tests;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;

import org.junit.Test;

import cfp.PotentialCFPs;
import cfp.NextCFP;
import cfp.helper.bean.CoveredTried;

public class TestNextCFP {

	@Test
	public void testPotentialCFPs() {
		PotentialCFPs p = new PotentialCFPs();
		p.writePotentialCFPs("m1@m2@m3@m4");
		
		PotentialCFPs.potCFP.put("m1@m1",new CoveredTried(new BigInteger("11"), new BigInteger("11")));
		PotentialCFPs.potCFP.put("m1@m2",new CoveredTried(new BigInteger("8"), new BigInteger("5")));
		PotentialCFPs.potCFP.put("m1@m3",new CoveredTried(new BigInteger("10"), new BigInteger("10")));
		PotentialCFPs.potCFP.put("m1@m4",new CoveredTried(new BigInteger("11"), new BigInteger("11")));
		PotentialCFPs.potCFP.put("m2@m2",new CoveredTried(new BigInteger("1"), new BigInteger("5")));
		PotentialCFPs.potCFP.put("m2@m3",new CoveredTried(new BigInteger("0"), new BigInteger("4")));
		PotentialCFPs.potCFP.put("m2@m4",new CoveredTried(new BigInteger("10"), new BigInteger("2")));
		PotentialCFPs.potCFP.put("m3@m3",new CoveredTried(new BigInteger("9"), new BigInteger("10")));
		PotentialCFPs.potCFP.put("m3@m4",new CoveredTried(new BigInteger("7"), new BigInteger("3")));
		PotentialCFPs.potCFP.put("m4@m4",new CoveredTried(new BigInteger("10"), new BigInteger("1")));
		
		NextCFP nextCFP = new NextCFP();
		nextCFP.potNextInitialPopulated=true;
		
		assertEquals(nextCFP.writeNextCFP(1, 2, 1), 2);
		assertEquals(NextCFP.nextCFPMethod1, "m4");
		assertEquals(NextCFP.nextCFPMethod2, "m4");		
	}
}
