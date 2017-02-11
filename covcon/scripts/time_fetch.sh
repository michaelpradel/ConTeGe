#!/bin/bash

file=$1
resultFile=$2
timeTaken=$3
fileToWrite=$4
cut=$5
run=$6
archiveDir=$7

gen_tests=$(cat $file | grep "Generated Tests Count")
gen_cnt=$(echo "$gen_tests" | cut -d ":" -f2)

nextCFP=$(cat $file | grep "NextCFP" | tail -1)
nextCFPVal=$(echo $nextCFP | cut -d ":" -f2)
nextCFPTry=$(echo $nextCFP | cut -d ":" -f3)

interleavingCount=$(cat $file | grep "Sequential interleavings Count" | cut -d ":" -f2) 
failedCount=$(cat $file | grep "FailedConcRuns Count" | cut -d ":" -f2) 

exception=$(cat $resultFile | grep "Exception Found : " | head -1)
exceptionName=$(echo $exception | cut -d ":" -f2)

cutMethodsSize=$(cat $file | grep "CUTMethods Size" | tail -1 | cut -d "-" -f2)
potCFPSize=$(cat $file | grep "PotCFP Size" | tail -1 | cut -d "-" -f2)

state_changer=$(cat $file | grep "StateChanger" | tail -1)
state_changer_cnt=$(echo $state_changer | cut -d ":" -f2)

cat0=$(cat $file | grep "TimerFinal" | cut -d "@" -f2)
cat1=$(cat $file | grep "TimerFinal" | cut -d "@" -f3)
field1=$(echo $cat1 | cut -d ":" -f1)
val1=$(echo $cat1 | cut -d ":" -f2)
cat2=$(cat $file | grep "TimerFinal" | cut -d "@" -f4)
field2=$(echo $cat2 | cut -d ":" -f1)
val2=$(echo $cat2 | cut -d ":" -f2)
cat3=$(cat $file | grep "TimerFinal" | cut -d "@" -f5)
field3=$(echo $cat3 | cut -d ":" -f1)
val3=$(echo $cat3 | cut -d ":" -f2)
cat4=$(cat $file | grep "TimerFinal" | cut -d "@" -f6)
field4=$(echo $cat4 | cut -d ":" -f1)
val4=$(echo $cat4 | cut -d ":" -f2)
cat5=$(cat $file | grep "TimerFinal" | cut -d "@" -f7)
field5=$(echo $cat5 | cut -d ":" -f1)
val5=$(echo $cat5 | cut -d ":" -f2)
cat6=$(cat $file | grep "TimerFinal" | cut -d "@" -f8)
field6=$(echo $cat6 | cut -d ":" -f1)
val6=$(echo $cat6 | cut -d ":" -f2)
cat7=$(cat $file | grep "TimerFinal" | cut -d "@" -f9)
field7=$(echo $cat7 | cut -d ":" -f1)
val7=$(echo $cat7 | cut -d ":" -f2)

if [ "$field1" = "gen" ]
then
  valGenNow=$val1
fi
if [ "$field2" = "gen" ]
then
  valGenNow=$val2
fi
if [ "$field3" = "gen" ]
then
  valGenNow=$val3
fi
if [ "$field4" = "gen" ]
then
  valGenNow=$val4
fi
if [ "$field5" = "gen" ]
then
  valGenNow=$val5
fi
if [ "$field6" = "gen" ]
then
  valGenNow=$val6
fi
if [ "$field7" = "gen" ]
then
  valGenNow=$val7
fi

if [ "$field1" = "conc_exec" ]
then
  valConcNow=$val1
fi
if [ "$field2" = "conc_exec" ]
then
  valConcNow=$val2
fi
if [ "$field3" = "conc_exec" ]
then
  valConcNow=$val3
fi
if [ "$field4" = "conc_exec" ]
then
  valConcNow=$val4
fi
if [ "$field5" = "conc_exec" ]
then
  valConcNow=$val5
fi
if [ "$field6" = "conc_exec" ]
then
  valConcNow=$val6
fi
if [ "$field7" = "conc_exec" ]
then
  valConcNow=$val7
fi

if [ "$field1" = "cfp_det" ]
then
  valCfpDetNow=$val1
fi
if [ "$field2" = "cfp_det" ]
then
  valCfpDetNow=$val2
fi
if [ "$field3" = "cfp_det" ]
then
  valCfpDetNow=$val3
fi
if [ "$field4" = "cfp_det" ]
then
  valCfpDetNow=$val4
fi
if [ "$field5" = "cfp_det" ]
then
  valCfpDetNow=$val5
fi
if [ "$field6" = "cfp_det" ]
then
  valCfpDetNow=$val6
fi
if [ "$field7" = "cfp_det" ]
then
  valCfpDetNow=$val7
fi

if [ "$field1" = "next_cfp" ]
then
  valNextCFPNow=$val1
fi
if [ "$field2" = "next_cfp" ]
then
  valNextCFPNow=$val2
fi
if [ "$field3" = "next_cfp" ]
then
  valNextCFPNow=$val3
fi
if [ "$field4" = "next_cfp" ]
then
  valNextCFPNow=$val4
fi
if [ "$field5" = "next_cfp" ]
then
  valNextCFPNow=$val5
fi
if [ "$field6" = "next_cfp" ]
then
  valNextCFPNow=$val6
fi
if [ "$field7" = "next_cfp" ]
then
  valNextCFPNow=$val7
fi

if [ "$field1" = "pot_cfp" ]
then
  valPotCFPNow=$val1
fi
if [ "$field2" = "pot_cfp" ]
then
  valPotCFPNow=$val2
fi
if [ "$field3" = "pot_cfp" ]
then
  valPotCFPNow=$val3
fi
if [ "$field4" = "pot_cfp" ]
then
  valPotCFPNow=$val4
fi
if [ "$field5" = "pot_cfp" ]
then
  valPotCFPNow=$val5
fi
if [ "$field6" = "pot_cfp" ]
then
  valPotCFPNow=$val6
fi
if [ "$field7" = "pot_cfp" ]
then
  valPotCFPNow=$val7
fi

if [ "$field1" = "interleavings" ]
then
  valInterleavingsNow=$val1
fi
if [ "$field2" = "interleavings" ]
then
  valInterleavingsNow=$val2
fi
if [ "$field3" = "interleavings" ]
then
  valInterleavingsNow=$val3
fi
if [ "$field4" = "interleavings" ]
then
  valInterleavingsNow=$val4
fi
if [ "$field5" = "interleavings" ]
then
  valInterleavingsNow=$val5
fi
if [ "$field6" = "interleavings" ]
then
  valInterleavingsNow=$val6
fi
if [ "$field7" = "interleavings" ]
then
  valInterleavingsNow=$val7
fi

valCFP=$(( $valCfpDetNow+$valNextCFPNow+$valPotCFPNow ))
nextCFPTry=$(( $nextCFPTry+1 ))

echo "$toolName@"${cut}"@"${cutMethodsSize}"@"${potCFPSize}"@"${run}"@"${timeTaken}"@"${exceptionName// /}"@"${gen_cnt// /}"@"${valGenNow}"@"${valConcNow}"@"${valCFP}"@"${valInterleavingsNow}"@"${interleavingCount// /}"@"${failedCount// /}"@"${nextCFPVal// /}"@"${nextCFPTry}"@"${state_changer_cnt// /} >> $fileToWrite



