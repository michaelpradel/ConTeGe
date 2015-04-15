#!/bin/bash

pwd=`pwd`
bmBaseDir="${pwd}/benchmarks/pldi2012"

for benchmarkDir in `find ${bmBaseDir} -mindepth 1 -maxdepth 1 -type d`
do
  cut=`cat ${benchmarkDir}/cut.txt`
  rm -rf results/time_to_bug_${cut}.result
  touch results/time_to_bug_${cut}.result

  echo ">>>> Testing ${cut} with different seed bases..."
  for seedBase in $(seq 0 100 900)
  do
  echo ">> Seed base ${seedBase}..."
    echo "Seed base ${seedBase}:" >> results/time_to_bug_${cut}.result
    /usr/bin/time -f "Wallclock: %E, User mode: %U, Kernel mode: %S (all seconds)" -o results/time_to_bug_${cut}.result -a ./scripts/testCUTuntilBugFound.sh ${benchmarkDir} ${seedBase}
    echo "----------------------------------" >> results/time_to_bug_${cut}.result
  done
done
