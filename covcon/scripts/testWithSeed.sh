#!/bin/bash

### arguments
seed=$1
maxSuffixGenTries=$2
benchmarkDir=$3

pwd=`pwd`

covcon="${pwd}/ownLibs/covcon.jar"
covconLibs="${pwd}/lib/scala-lib-2.10.2.jar:${pwd}/lib/asm-tree-4.0.jar:${pwd}/lib/asm-4.0.jar:${pwd}/lib/tools.jar:${pwd}/lib/testSkeleton.jar:${pwd}/lib/commons-io-2.0.1.jar:${pwd}/lib/jpf.jar:${pwd}/lib/bcel-5.2.jar"
covconOwnLibs="${pwd}/ownLibs/javaModel.jar:${pwd}/ownLibs/clinitRewriter.jar:${pwd}/ownLibs/cfp_detection.jar"

bmJar=`find ${benchmarkDir}/jar/ -name "*.jar" | xargs | sed -e "s/ /:/g"`
bmLib=`find ${benchmarkDir}/lib/ -name "*.jar" | xargs | sed -e "s/ /:/g"`
envTypes="${benchmarkDir}/env_types.txt"
cut=`cat ${benchmarkDir}/cut.txt`
selectedCutMethods=""
if [ -e ${benchmarkDir}/selectedCutMethods.txt ]
then
  selectedCutMethods="${benchmarkDir}/selectedCutMethods.txt"
fi

2>&1

mainClass="contege.ClassTester"

java -cp ${covconLibs}:${covcon}:${covconOwnLibs}:${bmJar}:${bmLib} ${mainClass} ${cut} ${envTypes} ${seed} ${maxSuffixGenTries} results/${cut}_seed${seed}_tries${maxSuffixGenTries}.result false ${selectedCutMethods} > results/${cut}_seed${seed}_tries${maxSuffixGenTries}.out
