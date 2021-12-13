package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import heuristics.CARP_Victor;
import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.OptPathPlan;
import heuristics.Pair;
import ilog.concert.IloException;

public class Statistics {
	int maxProbes;
	int networkSize;
	int capacityProbe;
	NetworkInfrastructure infra;
	int collector;
	
	//results
	public String er = "";
	public String optimalER = "";
	public String infocomAriel = "";
	public String result;
	
	public Statistics() {
		
	}
	
	public Statistics(int maxProbes, int networkSize, int capacityProbe, NetworkInfrastructure infra) {
		this.maxProbes = maxProbes;
		this.networkSize = networkSize;
		this.capacityProbe = capacityProbe;
		this.infra = infra;
	}
	
	public String runCARP(CARP_Victor model) {
		this.result = ";";
		this.result += model.cycles.size() + ";";
		this.result += model.time + ";" + this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter + ";" + this.maxProbes + ";" + this.capacityProbe + ";";
		this.result += transportationOverheadCycles(model.cycles, false);
		this.result += model.cycles.size() + ";" + model.cycles.size() + ";" + model.cycles.size() + ";";   // collector overhead with only one collector
		this.result += probeUsageCycles(model.cycles);
		this.result += deviceOverheadCycles(model.cycles);
		this.result += linkOverheadCycles(model.cycles);
		this.result += transportationOverheadCycles(model.cycles, true);
		this.result += this.infra.seed + ";";
		return this.result;
	}
	
	public String runOPP(OptPathPlan opp, boolean multipleCollectors) {
		this.result = ";";
		this.result += opp.Q.size() + ";";
		this.result += opp.time + ";" + this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter + ";" + this.maxProbes + ";" + this.capacityProbe + ";";
		this.result += transportationOverheadCycles(opp.Q, false) + ";";
		this.result += opp.Q.size() + ";";       //collector overhead
		this.result += probeUsageCycles(opp.Q);
		this.result += deviceOverheadCycles(opp.Q);
		this.result += linkOverheadCycles(opp.Q);
		this.result += transportationOverheadCycles(opp.Q, true) + ";";
		this.result += this.infra.seed + ";";
		return this.result;
	}
	
	public String runOptimal(AlgorithmOpt opt, int collector) throws IloException {
		this.result = ";";
		this.result += (int) opt.cplex.getObjValue() + ";";
		this.result += opt.time + ";" + this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter + ";" + this.maxProbes + ";" + this.capacityProbe + ";";
		this.result += "0;";
		this.result += (int) opt.cplex.getObjValue() + ";";     //collector overhead
		this.result += probeUsageCplex(opt);
		this.result += deviceOverheadCplex(opt);
		this.result += linkOverheadCplex(opt);
		this.result += collector + ";";
		this.result += this.infra.seed + ";";
		return this.result;
	}
	
	public String transportationOverheadCycles(ArrayList<Cycle> cycles, boolean getCollector) {
		int min = Integer.MAX_VALUE;
		int collector = -1;
		String transpOverhead = "";
		for(int i = 0; i < this.networkSize; i++) {
			int transportationCost = 0;
			for(Cycle c: cycles) {
				int cost = this.infra.getShortestPath(c.nodes.get(0), i).size();
				if(cost < 0) {
					cost = 0;
				}
				transportationCost += cost;
			}
			if(transportationCost < min) {
				min = transportationCost;
				collector = i;
			}
		}
		if(getCollector) {
			transpOverhead = collector + ";";
		}else {			
			transpOverhead = min + ";";
		}
		return transpOverhead;
	}
	
	public String probeUsageCycles(ArrayList<Cycle> cycles) {
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		for(Cycle c: cycles) {
			avg += c.capacity_used;
			if(c.capacity_used < min) {
				min = c.capacity_used;
			}
			if(c.capacity_used > max) {
				max = c.capacity_used;
			}
		}
		avg = avg / (this.capacityProbe * cycles.size());
		String probeUsage = min + ";" + max + ";" + avg + ";";
		return probeUsage;
	}
	
	private String deviceOverheadCycles(ArrayList<Cycle> cycles) {
		int[] devOverhead = new int[this.networkSize];
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		for(Cycle c: cycles) {
			for(int i = 0; i < c.nodes.size(); i++) {
				devOverhead[c.nodes.get(i)]++;
			}
		}
		for(int i = 0; i < this.networkSize; i++) {
			avg += devOverhead[i];
			if(devOverhead[i] < min) {
				min = devOverhead[i];
			}
			if(devOverhead[i] > max) {
				max = devOverhead[i];
			}
		}
		avg = avg / this.networkSize;
		String deviceOverhead = min + ";" + max + ";" + avg + ";";
		return deviceOverhead;
	}
	
	public String linkOverheadCycles(ArrayList<Cycle> cycles) {
		int numLinks = 0;
		for(int i = 0; i < this.networkSize; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				if(this.infra.graph[i][j] == 1) {
					numLinks++;
				}
			}
		}
		
		int[][] linkOverhead = new int[this.networkSize][this.networkSize];
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		for(Cycle c: cycles) {
			for(Pair<Integer, Integer> p: c.links) {
				linkOverhead[(int) p.first][(int) p.second]++;
				linkOverhead[(int) p.second][(int) p.first]++;
			}
		}
		for(int i = 0; i < this.networkSize; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				if(linkOverhead[i][j] != 0) {					
					avg += linkOverhead[i][j];
					if(linkOverhead[i][j] < min) {
						min = linkOverhead[i][j];
					}
					if(linkOverhead[i][j] > max) {
						max = linkOverhead[i][j];
					}
				}
			}
		}
		avg = avg / numLinks;
		
		String linkOverheads = min + ";" + max + ";" + avg + ";";
		
		return linkOverheads;
	}
	
	public String probeUsageCplex(AlgorithmOpt opt) {
		int probes = 0;
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		
		for(int i = 0; i < this.maxProbes; i++) {
			int cap = 0;
			for(int j = 0; j < this.networkSize; j++) {
				for (int k = 0; k < this.networkSize; k++) {
					if(opt.xMetrics[i][j][k] == 1) {
						cap++;
					}
				}
			}
			
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				for(int k = 0; k < this.networkSize; k++) {
					if(opt.zMetrics[i][j][k] == 1) {
						cap += this.infra.sizeTelemetryItems[j];
					}
				}
			}
			if(cap != 0) {
				probes++;
				avg += cap;
				if(cap > max) {
					max = cap;
				}
				if(cap < min) {
					min = cap;
				}
			}
		}
		avg = avg/(this.capacityProbe * probes);
		
		String probeUsage = min + ";" + max + ";" + avg + ";";
		return probeUsage;
	}
	
	public String deviceOverheadCplex(AlgorithmOpt opt) {
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		int[] devOverhead = new int[this.networkSize];
		for(int i = 0; i < this.maxProbes; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				for(int k = 0; k < this.networkSize; k++) {
					if(opt.xMetrics[i][j][k] == 1) {
						devOverhead[k]++;
					}
				}
			}
		}
		for(int i = 0; i < this.networkSize; i++) {
			avg += devOverhead[i];
			if(devOverhead[i] < min) {
				min = devOverhead[i];
			}
			if(devOverhead[i] > max) {
				max = devOverhead[i];
			}
		}
		avg = avg / this.networkSize;
		
		String deviceOverhead = min + ";" + max + ";" + avg + ";";
		return deviceOverhead;
	}
	
	public String linkOverheadCplex(AlgorithmOpt opt) {
		int numLinks = 0;
		for(int i = 0; i < this.networkSize; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				if(this.infra.graph[i][j] == 1) {
					numLinks++;
				}
			}
		}
		int[][] overhead = new int[this.networkSize][this.networkSize];
		int min = Integer.MAX_VALUE;
		float avg = 0;
		int max = Integer.MIN_VALUE;
		
		for(int i = 0; i < this.maxProbes; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				for(int k = 0; k < this.networkSize; k++) {
					if(opt.xMetrics[i][j][k] == 1) {
						overhead[j][k]++;
					}
				}
			}
		}
		for(int i = 0; i < this.networkSize; i++) {
			for(int j = 0; j < this.networkSize; j++) {
				if(overhead[i][j] != 0) {					
					avg += overhead[i][j];
					if(overhead[i][j] < min) {
						min = overhead[i][j];
					}
					if(overhead[i][j] > max) {
						max = overhead[i][j];
					}
				}
			}
		}
		avg = avg / numLinks;
		String linkOverhead = min + ";" + max + ";" + avg + ";";
		return linkOverhead;
	}
	
	
	
	//parte do ariel
	// -Métrica 1: Sobrecarga de dados: distância coletor (mínima)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	
	public int transportationOverhead(EdgeRandomization modelER, ArrayList<Cycle> paths, FixOptPInt fixOpt, AlgorithmOpt opt, int[] collectors, int maxProbes, int mode) {
		int minDistCollector = 0;
		
		if(mode < 0 || mode > 2) {
			System.out.println("ERROR: \"transportationOverHead\" invalid option!");
			return -1;
		}else if(mode == 0) { //opt case
			ArrayList<Cycle> pathsOpt = new ArrayList<Cycle>();
			for(int i = 0; i < maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
		
			for(int p = 0; p < pathsOpt.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsOpt.get(p).nodes.size(); n++) {
						int node = pathsOpt.get(p).nodes.get(n);
					
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}

			pathsOpt = null;
			
		}else if(mode == 1) { //fixOpt case
			ArrayList<Cycle> pathsFixOpt = new ArrayList<Cycle>();
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)fixOpt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsFixOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
		
			for(int p = 0; p < pathsFixOpt.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsFixOpt.get(p).nodes.size(); n++) {
						int node = pathsFixOpt.get(p).nodes.get(n);
					
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}

			pathsFixOpt = null;
			
		}else if(mode >= 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < paths.get(p).nodes.size(); n++) {
						int node = paths.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
						}
					}				
				}
				minDistCollector += minDist;
			}
			
		}
		
		return minDistCollector;
	}
	
	
	// -Métrica 2: (sobrecarga do coletor) número de probes por coletor
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] collectorOverhead(EdgeRandomization modelER, ArrayList<Cycle> paths, FixOptPInt fixOpt, AlgorithmOpt opt, int[] collectors, HashMap<Integer, Integer> mapCollector, int maxProbes, int mode) {
		
		double[] collectorOverhead = new double[3];
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		int closestDepot = -1;
		
		if(mode < 0 || mode > 2) {
			System.out.println("ERROR: \"collectorOverhead\" invalid option!");
			for(int i = 0; i < collectorOverhead.length; i++) {
				collectorOverhead[i] = -1;
				return collectorOverhead;
			}
		}
		else if(mode == 0) { //opt case
			ArrayList<Cycle> pathsOpt = new ArrayList<Cycle>();
			for(int i = 0; i < maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
			
			
			for(int p = 0; p < pathsOpt.size(); p++) {	
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < pathsOpt.get(p).nodes.size(); n++) {
						int node = pathsOpt.get(p).nodes.get(n);
						//System.out.println("node: " + node);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
							//System.out.println("node == collector: " + node);
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);
				
			}
			
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();

			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;
			
		}
		else if (mode == 1) { //fixOpt case
			ArrayList<Cycle> pathsFixOpt = new ArrayList<Cycle>();
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				boolean canAdd = false;
				Cycle c = new Cycle();
				HashSet<Integer> nodes = new HashSet<Integer>();
				
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							canAdd = true;
							nodes.add(j);
							nodes.add(k);
							//System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
							//System.out.println();
						}
					}
				}
				
				if(canAdd) {
					ArrayList<Integer> cnodes = new ArrayList<>(nodes);
					c.nodes = cnodes;
					//System.out.println("c.nodes: " + c.nodes);
					pathsFixOpt.add(c);
					nodes = null;
					cnodes = null;
					c = null;
					canAdd = false;
				}
			}
			
			
			for(int p = 0; p < pathsFixOpt.size(); p++) {	
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					
					for(int n = 0; n < pathsFixOpt.get(p).nodes.size(); n++) {
						int node = pathsFixOpt.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);
				
			}
			
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();

			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;
		}
		
		else if(mode == 2) { //heur case
			for(int p = 0; p < paths.size(); p++) {
								
				int minDist = Integer.MAX_VALUE;
				
				for(Integer collector: collectors) {
					for(int n = 0; n < paths.get(p).nodes.size(); n++) {
						int node = paths.get(p).nodes.get(n);
						
						if(node == collector) {
							minDist = 0;
							closestDepot = collector;
						}
						else if(node != collector && modelER.costShortestPath[node][collector] < minDist) {
							minDist = modelER.costShortestPath[node][collector]; // metric 1
							closestDepot = collector;
						}
					}
				}
				mapCollector.put(closestDepot, mapCollector.get(closestDepot) + 1);	
			}
			
			//System.out.println(Arrays.asList(mapCollector)); // method 1
			for(Map.Entry<Integer, Integer> entry: mapCollector.entrySet()) {
				if(entry.getValue() < min) {
					min = entry.getValue();
				}
				
				if(entry.getValue() > max) {
					max = entry.getValue();
				}
				
				avg += entry.getValue();
			}
			avg /= mapCollector.size();
			
			collectorOverhead[0] = min;
			collectorOverhead[1] = max;
			collectorOverhead[2] = avg;

		}
		
		return collectorOverhead;
	}
	
	
	// -Métrica 5: Utilização média dos probes (min, max, medio)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] probeUsage(FixOptPInt fixOpt, AlgorithmOpt opt, int maxProbes, ArrayList<Cycle> paths, int capacityProbe, int mode) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		double[] statistics = new double[3];
		
		if(mode == 0) { //opt case
			int probes = 0;
			for(int i = 0; i < maxProbes; i++) {
				int cap = 0;
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						System.out.printf("opt.xMetrics[%d][%d][%d]: %d", i, j, k, (int)opt.xMetrics[i][j][k]);
						System.out.println();
						if(opt.xMetrics[i][j][k] == 1) {
							cap++;
						}
					}
				}
				
				for(int j = 0; j < opt.infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.zMetrics[i][j][k] == 1) {
							cap += opt.infra.sizeTelemetryItems[j];
						}
					}
				}
								
				if(cap != 0) {	
					probes++;
					avg += cap;
					if(cap > max) {
						max = cap;
					}
					if(cap < min) {
						min = cap;
					}
				}
			}
			avg = avg/(capacityProbe * probes);
		}
		else if(mode == 1) { //fixOpt case
			int probes = 0;
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				int cap = 0;
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							cap++;
						}
					}
				}
				
				for(int j = 0; j < fixOpt.infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.zMetrics[i][j][k] == 1) {
							cap += fixOpt.infra.sizeTelemetryItems[j];
						}
					}
				}
								
				if(cap != 0) {	
					probes++;
					avg += cap;
					if(cap > max) {
						max = cap;
					}
					if(cap < min) {
						min = cap;
					}
				}
			}
			
			avg = avg/(capacityProbe * probes);
			
		}
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				if(paths.get(p).capacity_used < min) {
					min = paths.get(p).capacity_used;
				}
				
				if(paths.get(p).capacity_used > max) {
					max = paths.get(p).capacity_used;
				}
				
				avg += paths.get(p).capacity_used;
			}
			
			avg = avg/(capacityProbe * paths.size());

		}
		
		statistics[0] = min;
		statistics[1] = max;
		statistics[2] = avg;
		
		return statistics;
	}
	
	
	// -Métrica 6: Sobrecarga de dispositivo: quantos probes/caminhos passam pelo dispositivo (min, max, medio)
	//mode 0: opt, mode 1: fixOpt, mode 2: heur
	public double[] devOverhead(FixOptPInt fixOpt, AlgorithmOpt opt, ArrayList<Cycle> paths, int maxProbes, int networkSize, 
			int mode) {
		int[] devicesUsage = new int[networkSize];
		double[] devOverhead = new double[3];
		
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;

		
		if(mode == 0) { //opt case
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							devicesUsage[k]++;
						}
					}
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				avg += devicesUsage[d];
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
			}
			avg /= devicesUsage.length;
			
		}
		
		else if(mode == 1){ //fixOpt case
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							devicesUsage[k]++;
						}
					}
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				avg += devicesUsage[d];
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
			}
			avg /= devicesUsage.length;
		}
		
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				for(int d = 0; d < paths.get(p).nodes.size(); d++) {
					devicesUsage[paths.get(p).nodes.get(d)]++;
				}
			}
			
			for(int d = 0; d < devicesUsage.length; d++) {
				if(devicesUsage[d] < min) {
					min = devicesUsage[d];
				}
				if(devicesUsage[d] > max) {
					max = devicesUsage[d];
				}
				
				avg += devicesUsage[d];
			}
			avg /= devicesUsage.length; 
		}
		
		
		devOverhead[0] = min;
		devOverhead[1] = max;
		devOverhead[2] = avg;
		
		return devOverhead;
	}
	
	
	// -Métrica 7: Sobrecarga de enlace: quantos probes/caminhos passam pelo enlace (min, max, medio)
	
	public double[] linkOverhead(NetworkInfrastructure infra, FixOptPInt fixOpt, AlgorithmOpt opt, int maxProbes,
			ArrayList<Cycle> paths, int networkSize, int mode) {
		
		double [] linkOverhead = new double[3];
		int[][] linksUsage = new int[networkSize][networkSize];
		int numEdges = 0;

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		
		if(mode == 0) { //opt case
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < opt.infra.size; j++) {
					for(int k = 0; k < opt.infra.size; k++) {
						if(opt.xMetrics[i][j][k] == 1) {
							numEdges++;
							linksUsage[j][k]++;
						}
					}
				}
			}
			
		}
		
		else if(mode == 1) { //fixOpt case
			for(int i = 0; i < fixOpt.maxProbes; i++) {
				for(int j = 0; j < fixOpt.infra.size; j++) {
					for(int k = 0; k < fixOpt.infra.size; k++) {
						if(fixOpt.xMetrics[i][j][k] == 1) {
							numEdges++;
							linksUsage[j][k]++;
						}
					}
				}
			}
		}
		
		else if(mode == 2){ //heur case
			for(int p = 0; p < paths.size(); p++) {
				for(Pair<Integer,Integer> e: paths.get(p).links) {
					linksUsage[e.first][e.second]++;
					linksUsage[e.second][e.first]++;
				}
			}
		}
		
		for(int i = 0; i < networkSize; i++) {
			for(int j = 0; j < networkSize; j++) {
				if(linksUsage[i][j] > 0) {	
					numEdges++;
					avg += linksUsage[i][j];
				}
				
				if(linksUsage[i][j] > 0 && linksUsage[i][j] < min) {
					min = linksUsage[i][j];
				}
				
				if(linksUsage[i][j] > 0 && linksUsage[i][j] > max) {
					max = linksUsage[i][j];
				}
			}
		}
		avg /= numEdges;	
		
		linkOverhead[0] = min;
		linkOverhead[1] = max;
		linkOverhead[2] = avg;
		
		return linkOverhead;		
	}
	
}