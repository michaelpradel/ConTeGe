package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cfp.PotentialCFPs;

public class TestPotentialCFPs {

	@Test
	public void testPotentialCFPs() {
		PotentialCFPs p = new PotentialCFPs();
		p.writePotentialCFPs("m1@m2@m3@m4@m5@m6@m7@m8");
		
		assertEquals(36, PotentialCFPs.potCFP.size());
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m1"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m2"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m3"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m4"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m5"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m1@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m2"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m3"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m4"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m5"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m2@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m3"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m4"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m5"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m3@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m4@m4"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m4@m5"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m4@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m4@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m4@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m5@m5"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m5@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m5@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m5@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m6@m6"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m6@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m6@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m7@m7"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m7@m8"));
		assertEquals(true, PotentialCFPs.potCFP.containsKey("m8@m8"));
		
	}
}
