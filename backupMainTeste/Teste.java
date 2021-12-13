package main;
import java.io.FileNotFoundException;

import ilog.concert.IloException;
import heuristics.CARP;
import heuristics.EdgeRandomization;
import heuristics.MyPair;

public class Teste {

	public static void main(String[] args) throws IloException, FileNotFoundException {
		/*
		//Parameters
		//variar de 5 até 20
		//int networkSize = 5; //routers
		String networkSize = args[0];
		
		//variar de 10 até 40
		//int capacityProbe = 20;    			    //available space in a given flow (e.g., # of bytes)
		String capacityProbe = args[1];
		
		//variar de 10 até 60
		//int maxProbes = 50;
		String maxProbes = args[2];
		
		//variar de 1 até 10
		//int telemetryItemsRouter = 6;	        //number of telemetry items per router
		String telemetryItemsRouter = args[3];
		
		//ser igual a capacidade do probe
		//int maxSizeTelemetryItemsRouter = 20; 	//max size of a given telemetry item (in bytes)
		String maxSizeTelemetryItemsRouter = args[4];
		
		long seed = 123; 
		
		String pathInstance = "/Users/mcluizelli/eclipse-workspace/INTelemetry/instances/hs/hs1.txt";
		
		//NetworkInfrastructure infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, seed);
		NetworkInfrastructure infra = new NetworkInfrastructure(Integer.parseInt(networkSize), pathInstance,
				Integer.parseInt(telemetryItemsRouter), Integer.parseInt(maxSizeTelemetryItemsRouter), seed);
		
		//infra.generateToyTopology(networkSize);
		infra.filePath = pathInstance;
		infra.generateRndTopology(0.4);
		//infra.loadTopologyTxt();
		
		AlgorithmOpt opt = new AlgorithmOpt(infra);
		
		//opt.buildCPPTelemetry(maxProbes, capacityProbe);
		opt.buildCPPTelemetry(Integer.parseInt(maxProbes), Integer.parseInt(capacityProbe));*/
		
		
		//teste
		//System.out.println("\n\nCARP...");
		//CARP_Ariel carp = new CARP_Ariel();
		//carp.run();
		
		System.out.println("\n\nEdge Randomization...");
		EdgeRandomization model = new EdgeRandomization();
		model.runER();
		model.printCircuits();		
		
	}

}
