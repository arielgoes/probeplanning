package main;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.HeuristicAugmentMerge;
import heuristics.KCombinations;
import heuristics.OptPathPlan;

public class Teste {

	public static void main(String[] args) throws IloException, FileNotFoundException, CloneNotSupportedException{
		
		//Parameters
		int networkSize = Integer.parseInt(args[0]); //size of the network (i.e., # of nodes)
		int capacityProbe = Integer.parseInt(args[1]); //available space in a given flow (e.g., # of bytes)	
		int maxProbes = Integer.parseInt(args[2]); //max ammount of probes allowed to solve the path generation
		int telemetryItemsRouter = Integer.parseInt(args[3]); //number of telemetry items per router 
		int maxSizeTelemetryItemsRouter = Integer.parseInt(args[4]); //max size of a given telemetry item (in bytes)
		int initSizeComb = Integer.parseInt(args[5]); // initial size of the combinations
		int maxSizeComb = Integer.parseInt(args[6]); // max size of the combinations
		int numThreads = Integer.parseInt(args[7]); //max number of threads allowed
		int subProblemTimeLimit = Integer.parseInt(args[8]); //maximum time to solve a subproblem
		int globalTimeLimit = Integer.parseInt(args[9]); //global time to cover the whole network
		int contIterNotImprovMax = Integer.parseInt(args[10]); //# of iterations without any improvement (i.e., no path reduction)
		int combSize = Integer.parseInt(args[11]); //combinations of size 'k', used on performed statistics
		int n_internal = Integer.parseInt(args[12]); //internal number of iterations
		
		//double seed = 123;
		double seed = System.currentTimeMillis();
		
		NetworkInfrastructure infra = null;
		EdgeRandomization modelER = null;
		OptPathPlan pathPlanCycles = null;
		FixOptPInt fixOpt = null;
		AlgorithmOpt opt = null; //due to statistics parameters
		ArrayList<int[]> collectors = new ArrayList<int[]>();
		
		KCombinations kComb = new KCombinations();
		int[] array = new int[networkSize];
		for(int i = 0; i < networkSize; i++) {
			array[i] = i;
		}
		
		String pathInstance = ""; //used in case one desires to parse a data file as input
		
		//creating infrastructure and generating a random topology
		infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, (long) seed);
		infra.filePath = pathInstance;
		infra.generateRndTopology(0.7);

		
		//item size verification
		int itemSize = Integer.MIN_VALUE;
		for(int k = 0; k < infra.sizeTelemetryItems.length; k++) {
			if(infra.sizeTelemetryItems[k] > itemSize) {
				itemSize = infra.sizeTelemetryItems[k];
			}
		}
		
		if(itemSize > (capacityProbe - 2)) { //infeasible
			System.out.println("-0" + ";" + "NaN" + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
		}
		
		else {
			int bestVal = Integer.MAX_VALUE;
			EdgeRandomization bestER = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes);
			double timeER = 0;
			double timeER_total = 0;
			
			for(int i = 0; i < n_internal; i++) {
				//ER
				
				long bestSeed = System.currentTimeMillis();
				timeER = System.nanoTime();
				modelER = new EdgeRandomization(infra, capacityProbe, (long) bestSeed, maxProbes);
				modelER.runER();
				if(modelER.cycles.size() < bestVal) {
					bestVal = modelER.cycles.size();
					bestER = modelER;
				}
				
				timeER = (System.nanoTime() - timeER)*0.000000001;
				timeER_total+= timeER;
			}
			
			//INFOCOMM probe cycles
			/*double timeOPPCycles = System.nanoTime(); 
			pathPlanCycles = new OptPathPlan(infra, capacityProbe, (long) seed, true);
			pathPlanCycles.adaptToLinks();
			timeOPPCycles = (System.nanoTime() - timeOPPCycles)*0.000000001;*/
			
			
			//FixOpt
			/*double timeFixOpt = System.nanoTime();
			ArrayList<Integer> sinks = new ArrayList<Integer>();
			for(int i = 0; i < networkSize; i++) sinks.add(i);
			fixOpt = new FixOptPInt(infra, capacityProbe, maxProbes, numThreads, (long) seed, subProblemTimeLimit, 
					globalTimeLimit, initSizeComb, maxSizeComb, contIterNotImprovMax);
			double fixOptSol = fixOpt.run(pathPlanCycles.Q);
			timeFixOpt = (System.nanoTime() - timeFixOpt)*0.000000001;*/
			
			//Statistics
			Statistics sts = new Statistics();
			if(networkSize < 100) {
			
				for(int comb = 1; comb <= combSize; comb++) {
					collectors = null;
					collectors = kComb.enumKCombos(array, comb);
				
					// -Metric 1: Data overhead: collector's distance (minimum distance)
					int bestMinDistCollectorER = Integer.MAX_VALUE;
					int bestMinDistCollectorOPP_Cycles = Integer.MAX_VALUE;
					int bestMinDistCollectorFixOpt = Integer.MAX_VALUE;
					int minDistCollectorER = Integer.MAX_VALUE;
					int minDistCollectorOPP_Cycles = Integer.MAX_VALUE;
					int minDistCollectorFixOpt = Integer.MAX_VALUE;
					int bestK_ER = Integer.MAX_VALUE;
					int bestK_OPP_Cycles = Integer.MAX_VALUE;
					int bestK_FixOpt = Integer.MAX_VALUE;
					
					//search for the best combination subset and set it as the set of collectors
					for(int k = 0; k < collectors.size(); k++) {
						minDistCollectorER = sts.transportationOverhead(bestER, bestER.cycles, fixOpt, opt, collectors.get(k), maxProbes, 2);
						minDistCollectorOPP_Cycles = sts.transportationOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(k), maxProbes, 2);
						minDistCollectorFixOpt = sts.transportationOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(k), maxProbes, 1);
			
						if(minDistCollectorER < bestMinDistCollectorER) {
							bestMinDistCollectorER = minDistCollectorER;
							bestK_ER = k;
						}
						
						if(minDistCollectorOPP_Cycles < bestMinDistCollectorOPP_Cycles) {
							bestMinDistCollectorOPP_Cycles = minDistCollectorOPP_Cycles;
							bestK_OPP_Cycles = k;
						}
						
						if(minDistCollectorFixOpt < bestMinDistCollectorFixOpt) {
							bestMinDistCollectorFixOpt = minDistCollectorFixOpt;
							bestK_FixOpt = k;
						}
						
					}
					
					//maping (collector, # of probes)
					HashMap<Integer,Integer> mapCollectorER = new HashMap<Integer,Integer>();
					HashMap<Integer,Integer> mapCollectorOPP_Cycles = new HashMap<Integer,Integer>();
					HashMap<Integer,Integer> mapCollectorFixOpt = new HashMap<Integer,Integer>();
					
					for(Integer collector: collectors.get(bestK_ER)) {
						mapCollectorER.put(collector, 0);
					}
					
					for(Integer collector: collectors.get(bestK_OPP_Cycles)) {
						mapCollectorOPP_Cycles.put(collector, 0);
					}
					
					for(Integer collector: collectors.get(bestK_FixOpt)) {
						mapCollectorFixOpt.put(collector, 0);
					}
					
					
					// -Metric 2: Collector overhead: # of probes per collector
					double collectorOverheadER[] = new double[3];
					double collectorOverheadOPP_Cycles[] = new double[3];
					double collectorOverheadFixOpt[] = new double[3];
					collectorOverheadER = sts.collectorOverhead(bestER, bestER.cycles, fixOpt, opt, collectors.get(bestK_ER), mapCollectorER, maxProbes, 2);
					collectorOverheadOPP_Cycles = sts.collectorOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(bestK_OPP_Cycles), mapCollectorOPP_Cycles, maxProbes, 2);
					collectorOverheadFixOpt = sts.collectorOverhead(modelER, pathPlanCycles.Q, fixOpt, opt, collectors.get(bestK_FixOpt), mapCollectorFixOpt, maxProbes, 1);
	
					String mapCollectorER_collectors = new String();
					String mapCollectorER_probes = new String();
					for(Integer collector: collectors.get(bestK_ER)) {
						mapCollectorER_collectors += ";" + collector;
						mapCollectorER_probes += ";" + mapCollectorER.get(collector);
						
					}
					
					String mapCollectorOPPcycles_collectors = new String();
					String mapCollectorOPPcycles_probes = new String();
					for(Integer collector: collectors.get(bestK_OPP_Cycles)) {
						mapCollectorOPPcycles_collectors += ";" + collector;
						mapCollectorOPPcycles_probes += ";" + mapCollectorOPP_Cycles.get(collector);
					}
					
					String mapCollectorFixOpt_collectors = new String();
					String mapCollectorFixOpt_probes = new String();
					for(Integer collector: collectors.get(bestK_FixOpt)) {
						mapCollectorFixOpt_collectors += ";" + collector;
						mapCollectorFixOpt_probes += ";" + mapCollectorFixOpt.get(collector);
					}
					
					
					// -Metric 3: Average Probe Usage: (min, max, avg)
					double[] probeUsageER = new double[3];
					double[] probeUsageOPP_Cycles = new double[3];
					double[] probeUsageFixOpt = new double[3];
					probeUsageER = sts.probeUsage(fixOpt, opt, maxProbes, bestER.cycles, capacityProbe, 2);
					probeUsageOPP_Cycles = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 2);
					probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 1);
					
					// -Metric 4: Device overhead: # of probes/paths pass through devices (min, max, avg)
					double[] devOverheadER = new double[3];
					double[] devOverheadOPP_Cycles = new double[3];
					double[] devOverheadFixOpt = new double[3];
					devOverheadER = sts.devOverhead(fixOpt, opt, bestER.cycles, maxProbes, networkSize, 2);
					devOverheadOPP_Cycles = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 2);
					devOverheadFixOpt = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 1);
					
					// -Metric 5: Link overhead: # of probes/paths pass through links (min, max, avg)
					double[] linkOverheadER = new double[3];
					double[] linkOverheadOPP_Cycles = new double[3];
					double[] linkOverheadFixOpt = new double[3];
					linkOverheadER = sts.linkOverhead(infra, fixOpt, opt, maxProbes, bestER.cycles, networkSize, 2);
					linkOverheadOPP_Cycles = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 2);
					linkOverheadFixOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 1);
					
					System.out.println("ER" + ";" + bestER.cycles.size() + ";" + timeER_total
							+ ";" + bestER.infra.size + ";" + bestER.infra.telemetryItemsRouter
							+ ";" + bestER.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
							+ ";" + minDistCollectorER + ";" + collectorOverheadER[0] + ";" + collectorOverheadER[1]
									+ ";" + collectorOverheadER[2] + ";" + probeUsageER[0] + ";" + probeUsageER[1] 
											+ ";" + probeUsageER[2] + ";" + devOverheadER[0] + ";" + devOverheadER[1]
													+ ";" + devOverheadER[2] + ";" + linkOverheadER[0] + ";" + linkOverheadER[1]
															+ ";" + linkOverheadER[2] + ";" + comb + ";" + bestER.seed + ";"
															+ "COL" + mapCollectorER_collectors + ";" + "#PRB" + mapCollectorER_probes);
					
					/*System.out.println("OPPcycles" + ";" + pathPlanCycles.Q.size() + ";" + timeOPPCycles
							+ ";" + pathPlanCycles.infra.size + ";" + pathPlanCycles.infra.telemetryItemsRouter
							+ ";" + pathPlanCycles.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
							+ ";" + minDistCollectorOPP_Cycles + ";" + collectorOverheadOPP_Cycles[0] + ";" + collectorOverheadOPP_Cycles[1]
									+ ";" + collectorOverheadOPP_Cycles[2] + ";" + probeUsageOPP_Cycles[0] + ";" + probeUsageOPP_Cycles[1] 
											+ ";" + probeUsageOPP_Cycles[2] + ";" + devOverheadOPP_Cycles[0] + ";" + devOverheadOPP_Cycles[1]
													+ ";" + devOverheadOPP_Cycles[2] + ";" + linkOverheadOPP_Cycles[0] + ";" + linkOverheadOPP_Cycles[1]
															+ ";" + linkOverheadOPP_Cycles[2] + ";" + comb + ";" + pathPlanCycles.seed + ";"
															+ "COL" + mapCollectorOPPcycles_collectors + ";" + "#PRB" + mapCollectorOPPcycles_probes);*/
					
					/*System.out.println("FixOpt" + ";" + fixOptSol + ";" + timeFixOpt
							+ ";" + fixOpt.infra.size + ";" + fixOpt.infra.telemetryItemsRouter
							+ ";" + fixOpt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
							+ ";" + minDistCollectorFixOpt + ";" + collectorOverheadFixOpt[0] + ";" + collectorOverheadFixOpt[1]
									+ ";" + collectorOverheadFixOpt[2] + ";" + probeUsageFixOpt[0] + ";" + probeUsageFixOpt[1] 
											+ ";" + probeUsageFixOpt[2] + ";" + devOverheadFixOpt[0] + ";" + devOverheadFixOpt[1]
													+ ";" + devOverheadFixOpt[2] + ";" + linkOverheadFixOpt[0] + ";" + linkOverheadFixOpt[1]
															+ ";" + linkOverheadFixOpt[2] + ";" + comb + ";" + fixOpt.seed + ";"
															+ "COL" + mapCollectorFixOpt_collectors + ";" + "#PRB" + mapCollectorFixOpt_probes);*/
				}
			}//end-if
			else {
				
				// -Metric 3: Average Probe Usage: (min, max, avg)
				double[] probeUsageER = new double[3];
				//double[] probeUsageOPP_Cycles = new double[3];
				//double[] probeUsageFixOpt = new double[3];
				probeUsageER = sts.probeUsage(fixOpt, opt, maxProbes, bestER.cycles, capacityProbe, 2);
				//probeUsageOPP_Cycles = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 2);
				//probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 1);
				
				// -Metric 4: Device overhead: # of probes/paths pass through devices (min, max, avg)
				double[] devOverheadER = new double[3];
				//double[] devOverheadOPP_Cycles = new double[3];
				//double[] devOverheadFixOpt = new double[3];
				devOverheadER = sts.devOverhead(fixOpt, opt, bestER.cycles, maxProbes, networkSize, 2);
				//devOverheadOPP_Cycles = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 2);
				//devOverheadFixOpt = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 1);
				
				// -Metric 5: Link overhead: # of probes/paths pass through links (min, max, avg)
				double[] linkOverheadER = new double[3];
				//double[] linkOverheadOPP_Cycles = new double[3];
				//double[] linkOverheadFixOpt = new double[3];
				linkOverheadER = sts.linkOverhead(infra, fixOpt, opt, maxProbes, bestER.cycles, networkSize, 2);
				//linkOverheadOPP_Cycles = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 2);
				//linkOverheadFixOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 1);
				
				System.out.println("ER" + ";" + bestER.cycles.size() + ";" + timeER_total
						+ ";" + bestER.infra.size + ";" + bestER.infra.telemetryItemsRouter
						+ ";" + bestER.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
						+ ";" + "X" + ";" + "X" + ";" + "X"
								+ ";" + "X" + ";" + probeUsageER[0] + ";" + probeUsageER[1] 
										+ ";" + probeUsageER[2] + ";" + devOverheadER[0] + ";" + devOverheadER[1]
												+ ";" + devOverheadER[2] + ";" + linkOverheadER[0] + ";" + linkOverheadER[1]
														+ ";" + linkOverheadER[2] + ";" + "X" + ";" + bestER.seed + ";"
														+ "COL" + "X" + ";" + "#PRB" + "X");
				
				/*System.out.println("OPPcycles" + ";" + pathPlanCycles.Q.size() + ";" + timeOPPCycles
						+ ";" + pathPlanCycles.infra.size + ";" + pathPlanCycles.infra.telemetryItemsRouter
						+ ";" + pathPlanCycles.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
						+ ";" + "X" + ";" + "X" + ";" + "X"
								+ ";" + "X" + ";" + probeUsageOPP_Cycles[0] + ";" + probeUsageOPP_Cycles[1] 
										+ ";" + probeUsageOPP_Cycles[2] + ";" + devOverheadOPP_Cycles[0] + ";" + devOverheadOPP_Cycles[1]
												+ ";" + devOverheadOPP_Cycles[2] + ";" + linkOverheadOPP_Cycles[0] + ";" + linkOverheadOPP_Cycles[1]
														+ ";" + linkOverheadOPP_Cycles[2] + ";" + "X" + ";" + pathPlanCycles.seed + ";"
														+ "COL" + "X" + ";" + "#PRB" + "X");*/
				
				/*System.out.println("FixOpt" + ";" + fixOptSol + ";" + timeFixOpt
						+ ";" + fixOpt.infra.size + ";" + fixOpt.infra.telemetryItemsRouter
						+ ";" + fixOpt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe 
						+ ";" + "X" + ";" + "X" + ";" + "X"
								+ ";" + "X" + ";" + probeUsageFixOpt[0] + ";" + probeUsageFixOpt[1] 
										+ ";" + probeUsageFixOpt[2] + ";" + devOverheadFixOpt[0] + ";" + devOverheadFixOpt[1]
												+ ";" + devOverheadFixOpt[2] + ";" + linkOverheadFixOpt[0] + ";" + linkOverheadFixOpt[1]
														+ ";" + linkOverheadFixOpt[2] + ";" + "X" + ";" + fixOpt.seed + ";"
			
														+ "COL" + "X" + ";" + "#PRB" + "X");*/
			}
		}
	}
}
