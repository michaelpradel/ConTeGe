package cfp;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cfp.helper.bean.CoveredTried;

public class NextCFP {

	/** First method in the pair */
	public static String nextCFPMethod1 = null;

	/** Second method in the pair */
	public static String nextCFPMethod2 = null;

	/** List of all CFPs to be tried next */
	public List<String> potNext = new LinkedList<String>();

	/** Flag to check if the CFPs have been initially put into potNext */
	public boolean potNextInitialPopulated = false;

	/**
	 * Random prioritizer which selects CFPs randomly from the potNext
	 * 
	 * @param concRunRepetitions
	 * @return seed for next try
	 */
	public int random(int concRunRepetitions) {
		Random random = new Random();
		int index = random.nextInt(PotentialCFPs.potCFP.size());
		List<String> keys=new LinkedList<String>(PotentialCFPs.potCFP.keySet());
		String cfpString = keys.get(index);
		String[] methods = cfpString.split("@");
		nextCFPMethod1 = methods[0];
		nextCFPMethod2 = methods[1];
		System.out
				.println("NextCFP : " + nextCFPMethod1 + "@" + nextCFPMethod2);

		// Increment Tried Count for the CFP selected
		CoveredTried cv = PotentialCFPs.potCFP.get(cfpString);
		BigInteger triedCnt = cv.getTried().add(BigInteger.ONE);
		BigInteger coveredCnt = cv.getCovered();
		PotentialCFPs.potCFP.put(cfpString, new CoveredTried(coveredCnt,
				triedCnt));

		return triedCnt.intValue();
	}

	/**
	 * Prioritizer which prioritizes based on absolute difference of tried and
	 * covered count.
	 * 
	 * @param concRunRepetitions
	 * @param sizeofPriortizerList
	 * @return seed for next try
	 */
	public int enhancedCovTried(int concRunRepetitions, int sizeofPriortizerList) {

		// Check if initial list of all CFP is populated. If not populate it.
		if (!potNextInitialPopulated) {
			for (String k : PotentialCFPs.potCFP.keySet()) {
				if ((BigInteger.ZERO.equals(PotentialCFPs.potCFP.get(k)
						.getTried()) && BigInteger.ZERO
						.equals(PotentialCFPs.potCFP.get(k).getCovered()))) {
					potNext.add(k + ":0:0");
				}
			}
			potNextInitialPopulated = true;
		}

		int index = 0;
		String cfpString = null;

		// If there are CFPs in potNext, remove one and use it as next cfp to
		// try
		if (potNext.size() > 0) {
			Random random = new Random();
			index = random.nextInt(potNext.size());
			cfpString = potNext.remove(index);
		} else {
			// Prioritize all CFPs
			int potCFPSize = PotentialCFPs.potCFP.size();
			BigInteger[][] cfpScore = new BigInteger[potCFPSize][2];
			String[] cfp = new String[potCFPSize];
			potNext.clear();
			int i = 0;
			for (String k : PotentialCFPs.potCFP.keySet()) {
				BigInteger triedTimes = PotentialCFPs.potCFP.get(k).getTried();
				BigInteger coveredTimes = PotentialCFPs.potCFP.get(k)
						.getCovered();
				BigInteger coveredTriedDiff = triedTimes.subtract(coveredTimes)
						.abs();
				if (coveredTriedDiff.compareTo(BigInteger.ONE) < 0) {
					coveredTriedDiff = BigInteger.ONE;
				}
				BigInteger numTried = triedTimes;
				if (BigInteger.ZERO.equals(numTried)) {
					numTried = BigInteger.ONE;
				}
				BigInteger probNextCFP = (coveredTriedDiff.multiply(numTried));
				cfpScore[i][0] = probNextCFP;
				cfpScore[i][1] = BigInteger.valueOf(i);
				cfp[i] = k + ":" + triedTimes + ":" + coveredTimes;
				i++;
			}
			Arrays.sort(cfpScore, new Comparator<BigInteger[]>() {
				@Override
				public int compare(BigInteger[] arg0, BigInteger[] arg1) {
					return arg0[0].compareTo(arg1[0]);
				}
			});
			sizeofPriortizerList = (potCFPSize < (sizeofPriortizerList * 4)) ? Math
					.max((potCFPSize / 4), 1) : sizeofPriortizerList;
			// Add n cfps to potNext with least score
			for (int ind = 0; ind < sizeofPriortizerList; ind++) {
				potNext.add(cfp[cfpScore[ind][1].intValue()]);
			}
			cfpString = potNext.remove(new Random().nextInt(potNext.size()));
		}

		String[] cfpDetails = cfpString.split(":");
		String[] methods = cfpDetails[0].split("@");
		nextCFPMethod1 = methods[0];
		nextCFPMethod2 = methods[1];
		System.out.println("NextCFP : " + nextCFPMethod1 + "@" + nextCFPMethod2
				+ ":" + cfpDetails[1] + ":" + cfpDetails[2]);

		// Increment tried count for the CFP selected.
		CoveredTried cv = PotentialCFPs.potCFP.get(cfpDetails[0]);

		BigInteger triedCnt = cv.getTried().add(BigInteger.ONE);
		BigInteger coveredCnt = cv.getCovered();
		PotentialCFPs.potCFP.put(cfpDetails[0], new CoveredTried(coveredCnt,
				triedCnt));

		return triedCnt.intValue();
	}

	/**
	 * Choose the prioritizer for prioritizing
	 * 
	 * @param concRunRepetitions
	 * @param priortizer
	 * @param sizeofPriortizerList
	 * @return seed for next try
	 */
	public int writeNextCFP(int concRunRepetitions, int priortizer,
			int sizeofPriortizerList) {
		switch (priortizer) {
		case 1:
			return random(concRunRepetitions);
		case 2:
			return enhancedCovTried(concRunRepetitions, sizeofPriortizerList);
		default:
			return enhancedCovTried(concRunRepetitions, sizeofPriortizerList);
		}

	}
}
