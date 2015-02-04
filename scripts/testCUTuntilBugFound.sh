#!/bin/bash

benchmarkDir=$1
seedBase=$2

cut=`cat ${benchmarkDir}/cut.txt`
echo "Testing ${cut} until a bug is found (or until maxSuffixGenTries reached)"

for seedOffset in $(seq 0 99)
do
  maxSuffixGenTries="0"
  if [ "$seedOffset" -lt 10 ]
  then
    maxSuffixGenTries="10"
  elif [ "$seedOffset" -lt 20 ]
  then
    maxSuffixGenTries="20"
  elif [ "$seedOffset" -lt 30 ]
  then
    maxSuffixGenTries="50"
  elif [ "$seedOffset" -lt 40 ]
  then
    maxSuffixGenTries="200"
  else
    maxSuffixGenTries="500"
  fi

  let seed=$seedBase+$seedOffset

  echo "Seed: ${seed}, maxSuffixGenTries: ${maxSuffixGenTries}"

  ./scripts/testWithSeed.sh $seed $maxSuffixGenTries $benchmarkDir
  
  wc=`wc -l results/${cut}_seed${seed}_tries${maxSuffixGenTries}.result`
  lines=`echo ${wc} | cut -d" " -f1`
  if [ $lines -gt "2" ]
  then
    echo "Found BUG! Stopping to test."
    exit 0
  else
    echo "         ... nothing"
  fi
done
