package cfp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;

import cfp.helper.bean.CoveredTried;

/**
 * The class initializes potCFP map which maps the CFP to its tried and covered count
 * 
 * @author Ankit
 *
 */
public class PotentialCFPs {

	/** Map to hold cfp and its tried and covered count */
	public static HashMap<String, CoveredTried> potCFP = new HashMap<String, CoveredTried>();

	/**
	 * Accepts all the methods separated by @ and initializes the map with 
	 * Key - method 
	 * Value - as its tried and covered count (initially zero)
	 * @param cutMethods
	 */
	public void writePotentialCFPs(String cutMethods) {
		String[] methodList = cutMethods.split("@");
		for (String method1 : methodList) {
			for (String method2 : methodList) {
				if (method1.compareTo(method2) <= 0) {
					if ((potCFP == null)
							|| (!(potCFP.containsKey(method1 + "@" + method2)))) {
						potCFP.put(method1 + "@" + method2, new CoveredTried(
								BigInteger.ZERO, BigInteger.ZERO));
					}
				} else {
					if ((potCFP == null)
							|| (!(potCFP.containsKey(method2 + "@" + method1)))) {
						potCFP.put(method2 + "@" + method1, new CoveredTried(
								BigInteger.ZERO, BigInteger.ZERO));
					}
				}
			}
		}
		
		BufferedReader br=null;
		try {
			br = new BufferedReader(new FileReader("cfpCovered.txt"));
			String line;
			line = br.readLine();

			while (line != null) {
				if(potCFP.containsKey(line)) {
					potCFP.put(line, new CoveredTried(BigInteger.ONE, BigInteger.ZERO));
				}
				line = br.readLine();
			}
		    br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		
		System.out.println("PotCFP Size - " + potCFP.size());
	}
	
	/**
	 * Used for performance analysis to trace the number of CFPs which were tried more, less and 
	 * the number of tests that were generated extra for the cfps tried more times.
	 * @param cfp
	 */
	public static void getCountOfMoreTried(String cfp) {
		
		int tried=PotentialCFPs.potCFP.get(cfp).getTried().intValue();
		int cnt=0;
		int cntCFPExtra=0;
		int cntCFPLess=0;
		for(CoveredTried cv : potCFP.values()) {
			if(cv.getTried().intValue() >= tried+1) {
				cntCFPExtra++;
				cnt=(cv.getTried().intValue()-tried) + cnt;
			}
			if(cv.getTried().intValue() <= tried-2) {
				cntCFPLess++;
			}
		}
		System.out.println("Methods tried extra -" + cntCFPExtra);
		System.out.println("Methods tried less -" + cntCFPLess);
		System.out.println("Extra Try Count -" + cnt);
	}

}
