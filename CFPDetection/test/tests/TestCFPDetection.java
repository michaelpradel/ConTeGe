package tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import cfp.CFPDetection;
import cfp.PotentialCFPs;
import cfp.helper.bean.CoveredTried;

public class TestCFPDetection {
	
	public static void copyFolder(File srcfolder, File destfolder) {

		if (srcfolder.isDirectory()) {
			// if directory not exists, create it
			if (!destfolder.exists()) {
				destfolder.mkdir();
			}
			// list all the directory contents
			String files[] = srcfolder.list();
			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(srcfolder, file);
				File destFile = new File(destfolder, file);
				// recursive copy
				copyFolder(srcFile, destFile);
			}
		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in;
			OutputStream out;
			try {
				in = new FileInputStream(srcfolder);
				out = new FileOutputStream(destfolder);
				byte[] buffer = new byte[1024];
				int length;
				// copy the file content in bytes
				while ((length = in.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}
				in.close();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testDetectCFPs() {
		PotentialCFPs p = new PotentialCFPs();
		p.writePotentialCFPs("m1@m2@m3@m4@m5@m6@m7@m8");
		File srcfolder = new File("." + File.separator + "TestDetectCFPs");
		File destfolder = new File("." + File.separator + "Instrument_Traces");
		copyFolder(srcfolder, destfolder);
		CFPDetection cfpDetection = new CFPDetection();
		cfpDetection.detectCFP("CFP", "Instrument_Traces");
		
		for(String key : PotentialCFPs.potCFP.keySet()) {
			CoveredTried cv = PotentialCFPs.potCFP.get(key);
			if(key.equals("m1@m5") || key.equals("m1@m6") || key.equals("m2@m5") || key.equals("m3@m5") || key.equals("m4@m5") || key.equals("m5@m7") || key.equals("m2@m6") || key.equals("m3@m6") || key.equals("m4@m6")) {
				assertEquals(cv.getCovered().intValue(), 1);
			} else {
				assertEquals(cv.getCovered().intValue(), 0);
			}
			
		}
	}
}
