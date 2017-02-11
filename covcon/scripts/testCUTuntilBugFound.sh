#!/bin/bash

benchmarkDir=$1
seedBase=$2
c=$3
reportFile=$4
testToExecute=$5
maxRuns=$6
archiveDir=$7

rm -rf Instrument_Traces/

cut=`cat ${benchmarkDir}/cut.txt`
echo "Testing ${cut} until a bug is found ("${c}"/"${maxRuns}")"

maxSuffixGenTries="10"

timeStart=$(date +%s)
timeout $timeout ./scripts/testWithSeed.sh $seedBase $maxSuffixGenTries $benchmarkDir
timeEnd=$(date +%s) 
timeTaken=$(( timeEnd - timeStart )) 

./scripts/time_fetch.sh results/${cut}_seed${seedBase}_tries${maxSuffixGenTries}.out results/${cut}_seed${seedBase}_tries${maxSuffixGenTries}.result $timeTaken ${reportFile} ${testToExecute} ${c} $archiveDir

wc=`wc -l results/${cut}_seed${seedBase}_tries${maxSuffixGenTries}.result`
lines=`echo ${wc} | cut -d" " -f1`

mv results/${cut}_seed${seedBase}_tries${maxSuffixGenTries}.result $archiveDir/${cut}_seed${seedBase}_tries${maxSuffixGenTries}_count${c}_${timeStart}.result
mv results/${cut}_seed${seedBase}_tries${maxSuffixGenTries}.out $archiveDir/${cut}_seed${seedBase}_tries${maxSuffixGenTries}_count${c}_${timeStart}.out

if [ "$lines" -gt "2" ]
then
  echo "Found BUG! Stopping to test."
  exit 0
else
  echo "         ... nothing"
fi



