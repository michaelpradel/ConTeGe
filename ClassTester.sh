#!/bin/sh

# ClassTester arguments:
#
# 0: the class under test (CUT)
# 1: file with helper types
# 2: random seed
# 3: max. nb of tries to generate suffixes (i.e. how long it should run)
# 4: result file (only written when a thread safety violation is found)
# 5: whether to reset static state before each test (only works when classes have been instrumented with ClinitRewriter)

#### Adapt to your environment:
scala="/home/m/scala/scala-2.9.1.final/lib/scala-library.jar"

#### Adapt to your environment:
testedJar="/home/m/temp/XYSeries/jar/jfreechart-0.9.8_rewritten.jar"
testedJarLibs="/home/m/temp/XYSeries/lib/jcommon-0.8.0.jar:/home/m/temp/XYSeries/lib/clinit.jar"
testedJarEnvTypes="/home/m/temp/XYSeries/env_types.txt"


pwd=`pwd`

contege="${pwd}/bin"
contegeLibs="${pwd}/lib/asm-tree-4.0.jar:${pwd}/lib/asm-4.0.jar:${pwd}/lib/tools.jar:${pwd}/lib/testSkeleton.jar:${pwd}/lib/commons-io-2.0.1.jar:${pwd}/lib/jpf.jar:${pwd}/lib/bcel-5.2.jar"
contegeOwnLibs="${pwd}/ownLibs/javaModel.jar:${pwd}/ownLibs/clinitRewriter.jar"


seed=0
maxSuffixGenTries=100

java -cp ${scala}:${contege}:${contegeLibs}:${contegeOwnLibs}:${testedJar}:${testedJarLibs} contege.ClassTester org.jfree.data.XYSeries ${testedJarEnvTypes} ${seed} ${maxSuffixGenTries} result.out false
