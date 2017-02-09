# Efficient Detection of Thread Safety Violations via Coverage-Guided Generation of Concurrent Tests
by Ankit Choudhary, Shan Lu and Michael Pradel
ICSE 2017
==================================================================================================

# CovCon:
	* The main tool for finding bugs in a given CUT.
	* Run covcon/scripts/testTool.sh for CovCon to start up. 
	* Add benchmarks to test in covcon/benchmarks/instrumented directory.
	* Report file would be generated in concon/report.
	* Result log files would be generated in covcon/res.
	* Current time-out time is 3600 seconds. Update covcon/scripts/testTool.sh to modify the time-out time.
	* Update covcon/scripts/testRun.sh to add the benchmark in the script.
	* Some example benchmarks from our evaluation has been added to the covcon/benchmarks/instrumented and covcon/scripts/testRun.sh
	* Source code for CovCon is in covcon/src. 
	* The covcon.jar should be updated in covcon/ownLibs in case cource code is modified for changes to take effect.
	

# Instrumentor: 
	* Instrumentor instruments CUT and it's concrete super classes. 
	* Add plugin Instrumentor/plugins/Instrumentor_1.0.0.201506170128.jar to Eclipse. 
	* Right click on the CUT that needs to be instrumented.
	* Click on the button "Instrument" from right click menu.
	* The current project would have instrumented CUT while a new project <project_name>_old would be created with the uninstrumented CUT.
	* Source code for Instrumentor is in Instrumentor/Instrumentor/src.

# CFPDetection:
	* CFPDetection detects covered method pairs and prioritizes the set of method pairs.
	* Source code for CFPDetction is in CFPDetection/src.
	* The cfp_detection.jar should be updated in covcon/ownLibs in case source code is modified for changes to take effect. 
	
For further quesries contact: 
	Ankit Choudhary - c.ankit@outlook.com
	Shan Lu - shanlu@uchicago.edu 
	Michael Pradel - michael@binaervarianz.de
