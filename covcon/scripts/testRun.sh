#!/bin/bash

gen_report_file() {
  reportFile=$1
  testToExecute=$2

  timeTakenSum=0
  genTestsSum=0
  genTimeSum=0
  concTimeSum=0
  cfpTimeSum=0
  intTimeSum=0
  intCountSum=0
  failedCountSum=0
  tryCountSum=0
  timeTakenMax=0
  genTestsMax=0
  genTimeMax=0
  concTimeMax=0
  cfpTimeMax=0
  intTimeMax=0
  intCountMax=0
  failedCountMax=0
  tryCountMax=0

  firstLine=$(cat $reportFile | grep ${testToExecute} | head -1)
  timeTakenMin=$(echo $firstLine | cut -d "@" -f6)
  genTestsMin=$(echo $firstLine | cut -d "@" -f8)
  genTimeMin=$(echo $firstLine | cut -d "@" -f9)
  concTimeMin=$(echo $firstLine | cut -d "@" -f10)
  cfpTimeMin=$(echo $firstLine | cut -d "@" -f11)
  intTimeMin=$(echo $firstLine | cut -d "@" -f12)
  intCountMin=$(echo $firstLine | cut -d "@" -f13)
  failedCountMin=$(echo $firstLine | cut -d "@" -f14)
  tryCountMin=$(echo $firstLine | cut -d "@" -f17)

  while read line
  do
    echo $line | grep ${testToExecute} > /dev/null
    if [ $? -ne 0 ]
    then
      continue
    fi
    timeTaken=$(echo $line | cut -d "@" -f6)
    genTests=$(echo $line | cut -d "@" -f8)
    genTime=$(echo $line | cut -d "@" -f9)
    concTime=$(echo $line | cut -d "@" -f10)
    cfpTime=$(echo $line | cut -d "@" -f11)
    intTime=$(echo $line | cut -d "@" -f12)
    intCount=$(echo $line | cut -d "@" -f13)
    failedCount=$(echo $line | cut -d "@" -f14)
    tryCount=$(echo $line | cut -d "@" -f17)
    timeTakenSum=$(( timeTakenSum+timeTaken ))
    genTestsSum=$(( genTestsSum+genTests ))
    genTimeSum=$(( genTimeSum+genTime ))
    concTimeSum=$(( concTimeSum+concTime ))
    cfpTimeSum=$(( cfpTimeSum+cfpTime ))
    intTimeSum=$(( intTimeSum+intTime ))
    intCountSum=$(( intCountSum+intCount ))
    failedCountSum=$(( failedCountSum+failedCount ))
    tryCountSum=$(( tryCountSum+tryCount ))
    if [ "$timeTaken" -gt "$timeTakenMax" ]
    then
      timeTakenMax=$timeTaken
    fi
    if [ "$genTests" -gt "$genTestsMax" ]
    then
      genTestsMax=$genTests
    fi
    if [ "$genTime" -gt "$genTimeMax" ]
    then
      genTimeMax=$genTime
    fi
    if [ "$concTime" -gt "$concTimeMax" ]
    then
      concTimeMax=$concTime
    fi
    if [ "$cfpTime" -gt "$cfpTimeMax" ]
    then
      cfpTimeMax=$cfpTime
    fi
    if [ "$intTime" -gt "$intTimeMax" ]
    then
      intTimeMax=$intTime
    fi
    if [ "$intCount" -gt "$intCountMax" ]
    then
      intCountMax=$intCount
    fi
    if [ "$failedCount" -gt "$failedCountMax" ]
    then
      failedCountMax=$failedCount
    fi
    if [ "$tryCount" -gt "$tryCountMax" ]
    then
      tryCountMax=$tryCount
    fi   

    if [ "$timeTaken" -lt "$timeTakenMin" ]
    then
      timeTakenMin=$timeTaken
    fi
    if [ "$genTests" -lt "$genTestsMin" ]
    then
      genTestsMin=$genTests
    fi
    if [ "$genTime" -lt "$genTimeMin" ]
    then
      genTimeMin=$genTime
    fi
    if [ "$concTime" -lt "$concTimeMin" ]
    then
      concTimeMin=$concTime
    fi
    if [ "$cfpTime" -lt "$cfpTimeMin" ]
    then
      cfpTimeMin=$cfpTime
    fi
    if [ "$intTime" -lt "$intTimeMin" ]
    then
      intTimeMin=$intTime
    fi
    if [ "$intCount" -lt "$intCountMin" ]
    then
      intCountMin=$intCount
    fi
    if [ "$failedCount" -lt "$failedCountMin" ]
    then
      failedCountMin=$failedCount
    fi
    if [ "$tryCount" -lt "$tryCountMin" ]
    then
      tryCountMin=$tryCount
    fi 

  done < $reportFile

  timeTakenAvg=$(bc <<< "scale=2; $timeTakenSum/$maxRuns")
  genTestsAvg=$(bc <<< "scale=2; $genTestsSum/$maxRuns")
  genTimeAvg=$(bc <<< "scale=2; $genTimeSum/$maxRuns")
  concTimeAvg=$(bc <<< "scale=2; $concTimeSum/$maxRuns")
  cfpTimeAvg=$(bc <<< "scale=2; $cfpTimeSum/$maxRuns")
  intTimeAvg=$(bc <<< "scale=2; $intTimeSum/$maxRuns")
  intCountAvg=$(bc <<< "scale=2; $intCountSum/$maxRuns")
  failedCountAvg=$(bc <<< "scale=2; $failedCountSum/$maxRuns")
  tryCountAvg=$(bc <<< "scale=2; $tryCountSum/$maxRuns")

  echo "$toolName@"$testToExecute"@@@Max@"$timeTakenMax"@@"$genTestsMax"@"$genTimeMax"@"$concTimeMax"@"$cfpTimeMax"@"$intTimeMax"@"$intCountMax"@"$failedCountMax"@@@"$tryCountMax"@" >> ${reportFile}
  echo "$toolName@"$testToExecute"@@@Min@"$timeTakenMin"@@"$genTestsMin"@"$genTimeMin"@"$concTimeMin"@"$cfpTimeMin"@"$intTimeMin"@"$intCountMin"@"$failedCountMin"@@@"$tryCountMin"@" >> ${reportFile}
  echo "$toolName@"$testToExecute"@@@Avg@"$timeTakenAvg"@@"$genTestsAvg"@"$genTimeAvg"@"$concTimeAvg"@"$cfpTimeAvg"@"$intTimeAvg"@"$intCountAvg"@"$failedCountAvg"@@@"$tryCountAvg"@" >> ${reportFile}
  echo "" >> ${reportFile}
}


run_script () { 
  c=1
  benchmarkDir=$1
  testToExecute=$2
  maxRuns=$3
  reportFile=$4 

  currTime=$(date +%s)

  cut=`cat ${benchmarkDir}/cut.txt`

  while [ $c -le $maxRuns ]
  do
    seed=$(( (c-1)*100 ))
    ./scripts/testCUTuntilBugFound.sh ${benchmarkDir} $seed $c ${reportFile} ${testToExecute} ${maxRuns} $archiveDir
    c=$((c + 1))
  done

  gen_report_file $reportFile $testToExecute
}

time=$(date +%s)
archiveDir="res/$time"
mkdir -p "report/$time"
mkdir -p "$archiveDir"
reportFile="report/$time/report.csv" 

echo "Version@CUT@CUT Methods@Potential CFPs@Run@TimeTaken(s)@Exception@GeneratedTests@GenerationTime(ms)@ConcurrentExecutionTime(ms)@CFPTime(ms)@InterleavingTime(ms)@InterleavingTests@FailedTests@BuggyCFP1@BuggyCFP2@BuggyCFPTryCount@StateChangerCount" >> ${reportFile}

benchmarkDirParent="benchmarks/instrumented"

# Format
# run_script <benchmark_dir> <benchmark_name> <number of runs> <report file>
run_script "$benchmarkDirParent/XYSeries/" "XYSeries" 10 $reportFile
run_script "$benchmarkDirParent/BufferedInputStream/" "BufferedInputStream" 10 $reportFile
run_script "$benchmarkDirParent/PeriodAxis/" "PeriodAxis" 10 $reportFile
run_script "$benchmarkDirParent/Day/" "Day" 10 $reportFile
run_script "$benchmarkDirParent/NumberAxis/" "NumberAxis" 10 $reportFile
run_script "$benchmarkDirParent/PerUserPoolDataSource/" "PerUserPoolDataSource" 10 $reportFile
run_script "$benchmarkDirParent/SharedPoolDataSource/" "SharedPoolDataSource" 10 $reportFile
run_script "$benchmarkDirParent/XStream/" "XStream" 10 $reportFile
run_script "$benchmarkDirParent/TimeSeries/" "TimeSeries" 10 $reportFile
run_script "$benchmarkDirParent/XYPlot/" "XYPlot" 10 $reportFile
run_script "$benchmarkDirParent/Logger/" "Logger" 10 $reportFile
run_script "$benchmarkDirParent/SynchronizedMap/" "SynchronizedMap" 10 $reportFile
run_script "$benchmarkDirParent/ConcurrentHashMap/" "ConcurrentHashMap" 10 $reportFile
run_script "$benchmarkDirParent/StringBuffer/" "StringBuffer" 10 $reportFile


