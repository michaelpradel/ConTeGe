# Efficient Detection of Thread Safety Violations via Coverage-Guided Generation of Concurrent Tests

CovCon is a coverage-guided version of the ConTeGe test generator for finding bugs in thread-safe classes.

## Abstract

As writing concurrent programs is challenging, developers often rely on thread-safe classes, which encapsulate most synchronization issues. Testing such classes is crucial to ensure the correctness of concurrent programs. An effective approach to uncover otherwise missed concurrency bugs is to automatically generate concurrent tests. Existing approaches either create tests randomly, which is inefficient, build on a computationally expensive analysis of potential concurrency bugs exposed by sequential tests, or focus on exposing a particular kind of concurrency bugs, such as atomicity violations. This paper presents CovCon, a coverage-guided approach to generate concurrent tests. The key idea is to measure how often pairs of methods have already been executed concurrently and to focus the test generation on infrequently or not at all covered pairs of methods. The approach is independent of any particular bug pattern, allowing it to find arbitrary concurrency bugs, and is computationally inexpensive, allowing it to generate many tests in short time. We apply CovCon to 18 thread-safe Java classes, and it detects concurrency bugs in 17 of them. Compared to five state of the art approaches, CovCon detects more bugs than any other approach while requiring less time. Specifically, our approach finds bugs faster in 38 of 47 cases, with speedups of at least 4x for 22 of 47 cases.

## Paper

A paper on CovCon has been accepted as a full research paper at ICSE 2017.

## Installation and Usage

Check out this project and switch to the CovCon branch:
`git clone https://github.com/michaelpradel/ConTeGe.git`
`cd ConTeGe`
`git checkout CovCon`

The project consists of three parts, each stored in a separate folder:
  * *CovCon*: Generates concurrent unit tests
  * *Instrumentor*: Source-level instrumentation tool
  * *CFPDetection*: Analyzes coverage information and prioritizes method pairs

### CovCon

CovCon is the main tool for finding bugs in a given class under test (CUT). To run CovCon on the benchmark classes decribed in the paper, run the following commands:
`cd covcon`
`./scripts/testTool.sh CovCon`
Report files are generated in `concon/report`. An additional result log is generated in `covcon/res`. By default, the timeout is 3,600 seconds. Update `covcon/scripts/testTool.sh` to modify the timeout.

You can add additional benchmark classes by adding them into the `covcon/benchmarks/instrumented` directory. Update `covcon/scripts/testRun.sh` to add analyze benchmark in the script. Some example benchmarks from our evaluation have already been added to the `covcon/benchmarks/instrumented` and `covcon/scripts/testRun.sh`.

The source code for CovCon is in `covcon/src`. The `covcon.jar` should be updated in `covcon/ownLibs` after changing the source code.

### Instrumentor 

The instrumentor adds instrumentation statements to the class under test and its concrete super classes. It is implemented as an Eclipse plugin that performs source-level instrumentation. To use it, install `Instrumentor/plugins/Instrumentor_1.0.0.201506170128.jar` as a plugin into Eclipse. Then, right click on the class that needs to be instrumented and select "Instrument" in the context menu. After instrumentation, the current project will contain the instrumented class, while a new project your_project_name_old gets created with the uninstrumented class.

The source code for the instrumentor is in `Instrumentor/Instrumentor/src`.

### CFPDetection

This part of the project detects covered method pairs and prioritizes the set of method pairs to steer the test generation. The source code for is in `CFPDetection/src`.  The `cfp_detection.jar` should be updated in `covcon/ownLibs` after modifying the source code. 
	
## Contact

For any questions please contact:

* [Ankit Choudhary](c.ankit@outlook.com) 
* [Shan Lu](http://people.cs.uchicago.edu/~shanlu/)
* [Michael Pradel](http://mp.binaervarianz.de/)
