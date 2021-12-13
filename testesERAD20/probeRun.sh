#!/bin/bash
for ((infraSize=4; infraSize<=12; infraSize+=1))
do
	for telemetryItemsRouter in 2 4 6 8
	do
		for ((capacityProbe=20; capacityProbe<=80; capacityProbe+=20))
		do
			echo "infraSize = " $infraSize " telemetryItemsRouter = " $telemetryItemsRouter " capacityProbe = " $capacityProbe
			nohup java -Djava.library.path="/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux/" -jar testesERAD.jar $infraSize $capacityProbe 50 $telemetryItemsRouter 20 >> ArquivoSaida 2>/dev/null
		done
	done
done