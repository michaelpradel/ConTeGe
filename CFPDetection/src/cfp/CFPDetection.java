package cfp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import cfp.helper.bean.CoveredTried;
import cfp.helper.bean.FileBean;

/**
 * 
 * Class which detects which CFP have executed concurrently in an execution(s)
 * 
 * @author Ankit
 * 
 */
public class CFPDetection {

	/** Map to hold all the trace records keyed on thread id */
	public HashMap<String, LinkedList<String>> fileRecords = new HashMap<String, LinkedList<String>>();

	/** List to hold the the logical timestamp of the last record in each file */
	public LinkedList<Integer> lastRecordPerFile = new LinkedList<Integer>();

	/**
	 * Map which keeps track of currently active methods from trace records
	 * keyed on thread id
	 */
	public HashMap<String, LinkedList<String>> runningThreadMap = new HashMap<String, LinkedList<String>>();

	/**
	 * Detects which CFP executed concurrently.
	 * 
	 * @param fileStart
	 */
	public void detectCFP(String fileStart, String dirName) {

		// Holds all the trace records after filtering out the end record which
		// correspond to sequential execution
		FileBean[] traceRecordArray = null;

		runningThreadMap.clear();

		// Get all trace records from each file and populate fileRecords and
		// lastRecordPerFile
		BufferedReader br = null;
		File folder = new File("." + File.separator + dirName);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null) {
			for (int i = 0; i < listOfFiles.length; i++) {
				File file = listOfFiles[i];
				if (file.isFile() && file.getName().startsWith(fileStart)) {
					try {
						br = new BufferedReader(new FileReader(file));
						String line = null;
						String lastLine = null;
						while ((line = br.readLine()) != null) {
							LinkedList<String> getRecordsForFile = null;
							if (fileRecords.containsKey(file.getName())) {
								getRecordsForFile = fileRecords.get(file
										.getName());
							} else {
								getRecordsForFile = new LinkedList<String>();
							}
							getRecordsForFile.add(line);
							fileRecords.put(file.getName(), getRecordsForFile);
							lastLine = line;
						}

						if (lastLine != null) {
							String actualLine[] = lastLine.split("@");
							lastRecordPerFile.add(Integer
									.parseInt(actualLine[4]));
						}
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// Populate traceRecordArray after filtering the records with
			// greater timestamp than the second-last thread in execution
			if (lastRecordPerFile.size() > 1) {
				Collections.sort(lastRecordPerFile);
				int secondLastTS = lastRecordPerFile.get(lastRecordPerFile
						.size() - 2);
				lastRecordPerFile.clear();

				traceRecordArray = new FileBean[secondLastTS];

				for (String k : fileRecords.keySet()) {
					for (String line : fileRecords.get(k)) {
						String actualLine[] = line.split("@");
						int globalTS = Integer.parseInt(actualLine[4]);
						if (globalTS <= secondLastTS) {
							FileBean fileBean = new FileBean(actualLine[0],
									actualLine[1], actualLine[2],
									Integer.parseInt(actualLine[4]), k);
							traceRecordArray[Integer.parseInt(actualLine[4]) - 1] = fileBean;
						} else {
							break;
						}
					}
					runningThreadMap.put(k, new LinkedList<String>());
				}
				fileRecords.clear();
			}
		}

		// Delete trace files
		delete("." + File.separator + "Instrument_Traces");

		// Detect which CFPs executed concurrently
		if (traceRecordArray != null) {
			for (FileBean fileBean : traceRecordArray) {
				if (fileBean != null) {
					String desc = fileBean.getDesc();
					String threadId = fileBean.getThreadId();
					String methodName = fileBean.getMethodName();
					if ("Start method".equals(desc)) {
						// Check which all methods in other threads are
						// currently executing and increment covered counter for
						// them
						for (String threadIdKey : runningThreadMap.keySet()) {
							if (!(threadIdKey.equals(threadId))) {
								for (String methodNameMap : runningThreadMap
										.get(threadIdKey)) {
									String cfpMethod1 = methodName + "@"
											+ methodNameMap;
									String cfpMethod2 = methodNameMap + "@"
											+ methodName;
									if (PotentialCFPs.potCFP
											.containsKey(cfpMethod1)) {
										CoveredTried ctTmp = PotentialCFPs.potCFP
												.get(cfpMethod1);
										PotentialCFPs.potCFP
												.put(cfpMethod1,
														new CoveredTried(
																BigInteger.ONE
																		.add(ctTmp
																				.getCovered()),
																ctTmp.getTried()));

									} else if (PotentialCFPs.potCFP
											.containsKey(cfpMethod2)) {
										CoveredTried ctTmp = PotentialCFPs.potCFP
												.get(cfpMethod2);
										PotentialCFPs.potCFP
												.put(cfpMethod2,
														new CoveredTried(
																BigInteger.ONE
																		.add(ctTmp
																				.getCovered()),
																ctTmp.getTried()));
									}
								}
							}
						}

						// Add the current trace record method to
						// runningThreadMap
						LinkedList<String> listTmp = runningThreadMap
								.get(threadId);
						listTmp.add(methodName);
						runningThreadMap.put(threadId, listTmp);

					}
					if ("End method".equals(desc)) {
						// Delete the current trace record method from
						// runningThreadMap
						LinkedList<String> listTmp = runningThreadMap
								.get(threadId);
						listTmp.removeLastOccurrence(methodName);
						runningThreadMap.put(threadId, listTmp);
					}
				}
			}
		}
	}

	/**
	 * Deletes all trace files in dir
	 * 
	 * @param dir
	 */
	public static void delete(String dir) {
		File folder = new File(dir);

		if (folder.exists()) {
			for (File temp : folder.listFiles()) {
				temp.delete();
			}

			folder.delete();
		}
	}
}
