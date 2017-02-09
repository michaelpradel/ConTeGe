package cfp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import cfp.helper.bean.CoveredTried;

public class CoverageMeasurement {

	public static long testStartTime = System.currentTimeMillis()/1000L;
	
	public static long nbTestsGenerated=0;

	public static boolean measureCoverage(long testsPerformedCurrSeed) {

		int cfpCovered = 0;

		for (CoveredTried coveredTried : PotentialCFPs.potCFP.values()) {
			if (coveredTried.getCovered().compareTo(BigInteger.ZERO) > 0) {
				cfpCovered++;
			}
		}
		
		double percentageCovered = (100.0 * cfpCovered) / PotentialCFPs.potCFP.size();
		
		long totalTestsGenerated=testsPerformedCurrSeed + nbTestsGenerated;
		
		long totalTimeTaken=((System.currentTimeMillis() / 1000L) - testStartTime);

		System.out.println("NbMethodPairsCovered@" + cfpCovered + ":"
				+ totalTestsGenerated + ":"
				+ totalTimeTaken + ":" + percentageCovered);
		
		if(percentageCovered==100.0) {
			return true;
		}
		return false;
	}

	public static void writecfpCovered(long testsPerformedCurrSeed) {

		PrintWriter cfpFile = null;
		PrintWriter testsGeneratedTime = null;
		try {
			cfpFile = new PrintWriter(new FileOutputStream(new File("cfpCovered.txt"), false /* append = true */)); 
			testsGeneratedTime = new PrintWriter(new FileOutputStream(new File("testsGeneratedTime.txt"), false /* append = true */)); 
		} catch (IOException e) {
		}

		StringBuffer sb = new StringBuffer("");
		for (String cfpPair : PotentialCFPs.potCFP.keySet()) {
			CoveredTried coveredTried = PotentialCFPs.potCFP.get(cfpPair);
			if (coveredTried.getCovered().compareTo(BigInteger.ZERO) > 0) {
				sb.append(cfpPair);
				sb.append("\n");
			}
		}
		
		cfpFile.println(sb.toString());
		long totalTestsGenerated=testsPerformedCurrSeed + nbTestsGenerated;
		testsGeneratedTime.println(totalTestsGenerated+"@"+testStartTime);
		testsGeneratedTime.close();
		cfpFile.close();
	}
	
	public static void updateFields() {
		BufferedReader br=null;
		try {
			br = new BufferedReader(new FileReader("testsGeneratedTime.txt"));
			String line;
			line = br.readLine();
			nbTestsGenerated=Long.parseLong(line.split("@")[0]);
			testStartTime=Long.parseLong(line.split("@")[1]);
		    br.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
}
