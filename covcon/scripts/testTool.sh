#!/bin/bash

if [ "$1" = "CovCon" ]
then
	export toolName="CovCon"
	export timeout="3600"
fi

./scripts/testRun.sh


