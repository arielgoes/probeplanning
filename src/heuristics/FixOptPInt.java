package heuristics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import main.NetworkInfrastructure;

public class FixOptPInt {
	public NetworkInfrastructure infra;
	public int capacityProbe;
	public int maxProbes;
	public long seed;
	private int numThreads;
	
	private int subProblemTimeLimit; 
	private int globalTimeLimit; 
	private int contIterNotImprovMax; 
	private int initSizeComb;
	private int maxSizeComb;
	
	public int[][][] xMetrics;
	public int[][][] zMetrics;
	
	public FixOptPInt(NetworkInfrastructure infra, int capacityProbe, int maxProbes, int numThreads
						, long seed, int subProblemTimeLimit, int globalTimeLimit, int initSizeComb, int maxSizeComb, int contIterNotImprovMax) {
		this.capacityProbe = capacityProbe;
		this.maxProbes = maxProbes;
		this.infra = infra;
		this.seed = seed;
		this.numThreads = numThreads;
		
		this.subProblemTimeLimit = subProblemTimeLimit;
		this.globalTimeLimit = globalTimeLimit;
		
		this.contIterNotImprovMax = contIterNotImprovMax;
		
		this.initSizeComb = initSizeComb; 
		this.maxSizeComb = maxSizeComb;
		
	}
	
	private int chooseCandidate(ArrayList<int[]> combinationSet, int[] residualCapCycle, Hashtable<Integer, Set<Integer>> nodeSet ) {
		
		int iBest = -1;
		int residualBest = Integer.MAX_VALUE;
		int countMaxBest = 0;
		
		for(int i = 0; i < combinationSet.size(); i++) {
			//cycles
			int[] countNodes = new int[this.infra.size];
			int[] currComb = combinationSet.get(i);
			int residualAux = 0;
			
			for(int j = 0; j < currComb.length; j++) {
				residualAux += residualCapCycle[currComb[j]];
				Set<Integer> nodes = nodeSet.get(currComb[j]);
				for(Integer k : nodes) {
					countNodes[k]++;
				}
			}
			
			int countMax = 0;
			
			for(int j = 0; j < this.infra.size; j++) {
				if (countNodes[j] == currComb.length) countMax++;
			}
			
			if(residualAux < residualBest && countMax > 0) {
				residualBest = residualAux;
				iBest = i;
				countMaxBest = countMax;
			}
		}
		
		for(int i = 0; i < combinationSet.size(); i++) {
			//cycles
			int[] countNodes = new int[this.infra.size];
			int[] currComb = combinationSet.get(i);
			int residualAux = 0;
			
			for(int j = 0; j < currComb.length; j++) {
				residualAux += residualCapCycle[currComb[j]];
				Set<Integer> nodes = nodeSet.get(currComb[j]);
				for(Integer k : nodes) {
					countNodes[k]++;
				}
			}
			
			int countMax = 0;
			
			for(int j = 0; j < this.infra.size; j++) {
				if (countNodes[j] == currComb.length) countMax++;
			}
			
			if(residualAux == residualBest && countMax > countMaxBest) {
				residualBest = residualAux;
				iBest = i;
				countMaxBest = countMax;
			}
		}
		
		
		//System.out.println(" "); 
		//System.out.println("Residual capacity: " + residualBest + " / Capacity: " + combinationSet.get(0).length * this.capacityProbe + " / " + (float)residualBest / (combinationSet.get(0).length * this.capacityProbe));
		//System.out.println(" "); 
		return iBest;
	}
	
	public double run(ArrayList<Cycle> cyclesER) throws IloException {		
		
		long startTime = System.nanoTime();
		
		this.maxProbes = cyclesER.size();
		
		this.xMetrics = new int[this.maxProbes][this.infra.size][this.infra.size];
		this.zMetrics = new int[this.maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		/*Integer[][][] xSol = new Integer[this.maxProbes][this.infra.size][this.infra.size]; //probe route (probe, node, node)
		Integer[][][] zSol = new Integer[this.maxProbes][this.infra.telemetryItemsRouter][this.infra.size]; //probe collected items (probe, item, node) 
		
		//initialize variables;
		for(int i = 0; i < this.maxProbes; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				for(int k = 0; k < this.infra.size; k++) {
					zSol[i][j][k] = 0;
				}
			}
			
			for(int j = 0; j < this.infra.size; j++) {
				for(int k = 0; k < this.infra.size; k++) {
					xSol[i][j][k] = 0;
				}
			}
		}*/
		
		//Cplex variables;
		IloCplex cplexModel = new IloCplex(); //cplex model
		
		IloNumVar[][][] x = new IloNumVar[this.maxProbes][this.infra.size][this.infra.size];
		IloNumVar[][][] z = new IloNumVar[this.maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		for(int p = 0; p < this.maxProbes; p++) {
			for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplexModel.boolVarArray(this.infra.size);	
			}
			
			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				z[p][v] = cplexModel.boolVarArray(this.infra.size);
			}
			
		}
		
		//Copy initial solution;
		/*for(int p = 0; p < cyclesER.size(); p++) {
			for(int j = 0; j < cyclesER.get(p).nodes.size() - 1; j++) {
				xSol[p][cyclesER.get(p).nodes.get(j)][cyclesER.get(p).nodes.get(j+1)] = 1;
			}
		
			for(int j = 0; j < cyclesER.get(p).itemPerCycle.size(); j++) {
				zSol[p][cyclesER.get(p).itemPerCycle.get(j).item][cyclesER.get(p).itemPerCycle.get(j).device] = 1;
			}
			
		}*/
		
		
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX = new Hashtable<Integer, ArrayList<IloConstraint>>();		
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ = new Hashtable<Integer, ArrayList<IloConstraint>>();
		
		Hashtable<Integer, Set<Integer>> nodeSet = new Hashtable<Integer, Set<Integer>>(); 
		
		//Fixed all values from initial solution
		for(int p = 0; p < cyclesER.size(); p++) {
			int iAux, jAux;
			
			Hashtable<String, Integer> hasVarX = new Hashtable<String, Integer>();
			ArrayList<IloConstraint> resX = new ArrayList<IloConstraint>();
			
			Set<Integer> nodes = new HashSet<Integer>(); 
			
			for(int j = 0; j < cyclesER.get(p).links.size(); j++) {
				
				
				iAux =  (int) cyclesER.get(p).links.get(j).first;
				jAux =  (int) cyclesER.get(p).links.get(j).second;
				
				nodes.add(iAux);
				nodes.add(jAux);
				
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, x[p][iAux][jAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				resX.add(eq);
				
				hasVarX.put(new String(Integer.toString(p) + Integer.toString(iAux) + Integer.toString(jAux)), 1);
				this.xMetrics[p][iAux][jAux] = 1; 
			}
			
			nodeSet.put(p, nodes);
			
			
			//In case variable was not used, I set it to zero.
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1 && hasVarX.get(Integer.toString(p) + Integer.toString(i) + Integer.toString(j)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, x[p][i][j]);
						IloConstraint eq = cplexModel.addEq(expr, 0);
						resX.add(eq);
						
					}
				}
			}
			
			hashVariableX.put(p, resX);
			
			
			//insert collected items by the device into a second hash
			ArrayList<IloConstraint> resZ = new ArrayList<IloConstraint>();
			Hashtable<String, Integer> hasVarZ = new Hashtable<String, Integer>();
			
			for(int j = 0; j < cyclesER.get(p).itemPerCycle.size(); j++) {
				iAux = cyclesER.get(p).itemPerCycle.get(j).device;
				jAux = cyclesER.get(p).itemPerCycle.get(j).item;
				zMetrics[p][jAux][iAux] = 1;
			}
			/*for(int j = 0; j < cyclesER.get(p).itemPerCycle.size(); j++) {
					
				iAux = cyclesER.get(p).itemPerCycle.get(j).device;
				jAux = cyclesER.get(p).itemPerCycle.get(j).item;
						
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, z[p][jAux][iAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				resZ.add(eq);
				hasVarZ.put(new String(Integer.toString(p) + Integer.toString(jAux) + Integer.toString(iAux)), 1);
				
			}
			
			hashVariableZ.put(p, resZ);
			//Integer[][][] zSol = new Integer[this.maxProbes][this.infra.telemetryItemsRouter][this.infra.size]; //probe collected items (probe, item, node) 
			
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
					if(hasVarZ.get(Integer.toString(p) + Integer.toString(j) + Integer.toString(i)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, z[p][j][i]);
						IloConstraint eq = cplexModel.addEq(expr, 0);
						resX.add(eq);
						
					}
				}
			}*/
			
		}
		
		//Adding available sinks
		ArrayList<Integer> sinks = new ArrayList<Integer>();
		for(int i = 0; i < this.infra.size; i++) sinks.add(i);
		
		//Residual cycle's capacity
		int[] residualCapCycle = new int[this.maxProbes];
		
		buildPintModel(this.maxProbes, this.capacityProbe, residualCapCycle, sinks, x, z, cplexModel);
		
		double bestSol = cplexModel.getObjValue();
		
		boolean hasImproved = false;
		
		KCombinations kComb = new KCombinations();
		
		
		//all cycles.
		Hashtable<Integer, Integer> activeCycles = new Hashtable<Integer, Integer>();
		int[] array;
		array = new int[cyclesER.size()];
		for(int i = 0; i < array.length; i++) {
			array[i] = i;
			activeCycles.put(i, 1);
		}
		
		//implement optimization
		long startTime2 = System.currentTimeMillis()/1000; // start time in seconds;
		
		int auxPermK = 1;
		boolean shouldStop = false;
		
				
		for(int permK = this.initSizeComb; permK <= this.maxSizeComb; permK++) {
			
			int contIterNotImprov = 0;
			ArrayList<int[]> kCombList = new ArrayList<int[]>();
			//It means globalTimeLimit has been exceed.
			if(shouldStop) break;
			
			//System.out.println("k: " + permK);
			kCombList = kComb.enumKCombos(array, permK);
			
			int contUsedCombinations = 0;
			
			while(kCombList.size() > 0) {
			//for(int i = 0; i < kCombList.size(); i++) {
				
				//select best candidate
				
				int i = chooseCandidate(kCombList, residualCapCycle, nodeSet);
				
				if(i == -1) break;
				
				contUsedCombinations++;
				boolean shouldRestart = false;
				
				hasImproved = false;
				
				ArrayList<IloConstraint> res = new ArrayList<IloConstraint>();
			
				//remove cycles' constraints
				//System.out.println(kCombList.size());
				//System.out.println(kCombList.get(i).length);
				for(int j = 0; j < kCombList.get(i).length; j++) {
					setFreeVariables(kCombList.get(i)[j], hashVariableX, hashVariableZ, cplexModel);
				}
				
				cplexModel.setParam(IloCplex.DoubleParam.TiLim, this.subProblemTimeLimit);
				
				Hashtable<String, Integer> currentValueVarX = new Hashtable<String, Integer>();		
				Hashtable<String, Integer> currentValueVarZ = new Hashtable<String, Integer>();	
				
				if(cplexModel.solve()) {
					
					//System.out.println(cplexModel.getStatus() + " objValue: " + cplexModel.getObjValue());
					
					if(cplexModel.getObjValue() < bestSol) {
						
						//Update residual capacity
						for(int p = 0; p < maxProbes; p++) {
							int residualAux = 0;
							for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
								for(int iAux = 0; iAux < this.infra.size; iAux++) {
									if (cplexModel.getValue(z[p][v][iAux]) > 0 ) {
										residualAux += this.infra.sizeTelemetryItems[v];
									}
								}
							}
							for(int iAux = 0; iAux < this.infra.size; iAux++) {
								for(int j = 0; j < this.infra.size; j++) {
									if (this.infra.graph[iAux][j] == 1 && cplexModel.getValue(x[p][iAux][j]) > 0 ) {
										residualAux++;
									}
								}
							}
							residualCapCycle[p] = residualAux;
						}
						
						bestSol = cplexModel.getObjValue();
						
						//Saving solution and updating active cycles
						for(int p = 0; p < kCombList.get(i).length; p++) {
							
							boolean isActive = false;
							int indexProbe = kCombList.get(i)[p];
							Set<Integer> newNodes = new HashSet<Integer>();
							
							for(int k = 0; k < this.infra.size; k++) {
								for(int l = 0; l < this.infra.size; l++) {
									if(this.infra.graph[k][l] == 1 && cplexModel.getValue(x[indexProbe][k][l]) > 0) {
										this.xMetrics[indexProbe][k][l] = 1;
										isActive = true;
										newNodes.add(k);
										newNodes.add(l);
										currentValueVarX.put(Integer.toString(indexProbe) + Integer.toString(k) + Integer.toString(l), 1);
									}else {
										this.xMetrics[indexProbe][k][l] = 0;
									}
								}
								for(int l = 0; l < this.infra.telemetryItemsRouter; l++) {
									if(cplexModel.getValue(z[indexProbe][l][k]) > 0) {
										this.zMetrics[indexProbe][l][k] = 1;
									}else {
										this.zMetrics[indexProbe][l][k] = 0;
									}
								}
							}
							nodeSet.remove(indexProbe);
							nodeSet.put(indexProbe, newNodes);
							//if not active/used, then I removed from possible cycles to "merge"
							if(!isActive) {
								activeCycles.remove(indexProbe);
								
							}
						}
						
						array = new int[activeCycles.size()];
						int l = 0;
						for(Integer k : activeCycles.keySet()) {
							array[l++] = k;
						}
						
						hasImproved = true;
						
					}else {
						//If has not improved... reinsert constraints
						for(int j = 0; j < kCombList.get(i).length; j++) {	
							setFixVariables(kCombList.get(i)[j], hashVariableX, hashVariableZ, cplexModel);
						}
					}
				}else {
					//If has not solved... reinsert constraints
					for(int j = 0; j < kCombList.get(i).length; j++) {	
						setFixVariables(kCombList.get(i)[j], hashVariableX, hashVariableZ, cplexModel);
					}
				}
				
				//effective...
				if(hasImproved) {
					
					shouldRestart = true;
					hasImproved = false;
					auxPermK+=1; //auxiliar variable to print 'permK'
					
					int indexProbe; 
					for(int j = 0; j < kCombList.get(i).length; j++) {	
						
						indexProbe = kCombList.get(i)[j];
						ArrayList<IloConstraint> resX = new ArrayList<IloConstraint>();
						ArrayList<IloConstraint> resZ = new ArrayList<IloConstraint>();
						
						for(int k = 0; k < this.infra.size; k++) {
							for(int l = 0; l < this.infra.size; l++) {
								if(this.infra.graph[k][l] == 1 
										&& currentValueVarX.contains(Integer.toString(indexProbe) 
																		+ Integer.toString(k) + Integer.toString(l))){
									IloLinearNumExpr expr = cplexModel.linearNumExpr();
									expr.addTerm(1.0, x[indexProbe][k][l]);
									IloConstraint eq = cplexModel.addEq(expr, 1);
									resX.add(eq);
								}else {
									IloLinearNumExpr expr = cplexModel.linearNumExpr();
									expr.addTerm(1.0, x[indexProbe][k][l]);
									IloConstraint eq = cplexModel.addEq(expr, 0);
									resX.add(eq);
								}
							}
							/*for(int l = 0; l < this.infra.telemetryItemsRouter; l++) {
								if(currentValueVarZ.contains(Integer.toString(indexProbe) + Integer.toString(l) + Integer.toString(k))) {
									IloLinearNumExpr expr = cplexModel.linearNumExpr();
									expr.addTerm(1.0, z[indexProbe][l][k]);
									IloConstraint eq = cplexModel.addEq(expr, 1);
									resZ.add(eq);
								}else {
									IloLinearNumExpr expr = cplexModel.linearNumExpr();
									expr.addTerm(1.0, z[indexProbe][l][k]);
									IloConstraint eq = cplexModel.addEq(expr, 0);
									resZ.add(eq);
								}
							}*/
						}
						
						hashVariableX.put(indexProbe, resX);
						hashVariableZ.put(indexProbe, resZ);
						
					}
					
					
			
				}else {
					contIterNotImprov++;
				}
				
				//System.out.println("..." + permK + "/" + contUsedCombinations + "/" + kCombList.size() );
				
				if(((System.currentTimeMillis()/1000) - startTime2) > this.globalTimeLimit) {
					shouldStop = true;
					shouldRestart = true;
				}
				
				if(shouldRestart) {
					permK = this.initSizeComb - 1;
					break;
				}
				
				if(contIterNotImprov == contIterNotImprovMax) {
					break;
					
				}
				
				kCombList.remove(i);
			}
			
		}
		
		
		/*System.out.println("FixOPT;" + bestSol + ";" + (System.nanoTime() - startTime)*0.000000001 + ";" 
				+ this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter 
				+ ";" + maxProbes + ";" + capacityProbe + ";" + this.seed + ";" + auxPermK);*/
		
		return bestSol;
	}
	
	public void setFixVariables(int idProbe, Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX,
			Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ, IloCplex cplexModel) throws IloException {
		
		ArrayList<IloConstraint> res = hashVariableX.get(idProbe);
		for(int k = 0; k < res.size(); k++) {
			cplexModel.add(res.get(k));
		}
		
		/*ArrayList<IloConstraint> resZ = hashVariableZ.get(idProbe);
		for(int k = 0; k < resZ.size(); k++) {
			cplexModel.add(resZ.get(k));
		}*/
		
	}
	
	public void setFreeVariables(int idProbe, Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX,
			Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ, IloCplex cplexModel) throws IloException {
		
		//Remove X constraints
		ArrayList<IloConstraint> res = hashVariableX.get(idProbe);
		
		IloConstraint[] cons1 = null;
		
		if(res != null) {
			cons1 = new IloConstraint[res.size()];
			for(int k = 0; k < res.size(); k++) {
				cons1[k] = res.get(k);
			}
		}
		
		if(cons1 != null)
			cplexModel.remove(cons1);
		
		
		//Remove Z constraints
		/*ArrayList<IloConstraint> resZ = hashVariableZ.get(idProbe);
		
		IloConstraint[] cons2 = null;
		
		if(resZ != null) {
			cons2 = new IloConstraint[resZ.size()];
			for(int k = 0; k < resZ.size(); k++) {
				cons2[k] = resZ.get(k);
			}
		}
		
		if(cons2 != null)
			cplexModel.remove(cons2);
		*/
		
	}
	
	
	public List<List<Integer>> subsets(int[] nums) {
		List<List<Integer>> list = new ArrayList<>();
		subsetsHelper(list, new ArrayList<>(), nums, 0);
		return list;
	}
	
	
	private void subsetsHelper(List<List<Integer>> list , List<Integer> resultList, int [] nums, int start){
		list.add(new ArrayList<>(resultList));
		for(int i = start; i < nums.length; i++){
          
			resultList.add(nums[i]);
			subsetsHelper(list, resultList, nums, i + 1);
			resultList.remove(resultList.size() - 1);
		}
	}
	
	
	private ArrayList<Integer> notInQ(List<Integer> list) {
		
		int[] nodes = new int[this.infra.size];
		for(int i = 0; i < this.infra.size; i++) {
			nodes[i] = i;
		}
		
		for(int i = 0; i < list.size(); i++) {
			nodes[list.get(i)] = -1; 
		}
		
		ArrayList<Integer> nodesNotInQ = new ArrayList<Integer>();
		
		for(int i = 0; i < this.infra.size; i++) {
			if(nodes[i] != -1) nodesNotInQ.add(i);
		}
		
		return nodesNotInQ;
			
	}
	
	
	public void buildPintModel(int maxProbes, int capacityProbe, int[] residualCapCycle, ArrayList<Integer> sinks, 
			IloNumVar[][][]x, IloNumVar[][][] z, IloCplex cplex) throws IloException {

		
		long startTime = System.nanoTime();
		
		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
		}
		
		//System.out.println("Building Model");

		//Constraint (5) : x[i][j] = x[j][i
		for(int i = 0; i < this.infra.size; i++) {
			for(int p = 0; p < maxProbes; p++) {

				IloLinearNumExpr r1 = cplex.linearNumExpr();
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[i][j] == 1) {
						r1.addTerm(1.0, x[p][i][j]);
					}
				}
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[j][i] == 1) {
						r1.addTerm(-1.0, x[p][j][i]);
					}
				}

				cplex.addEq(r1, 0);
			}

		}

				
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {

				if(i!=j && this.infra.graph[i][j] == 1 && this.infra.graph[j][i] == 1) {

						IloLinearNumExpr r1 = cplex.linearNumExpr();

						for(int p = 0; p < maxProbes; p++) {
							r1.addTerm(1.0 , x[p][i][j]);
							r1.addTerm(1.0 , x[p][j][i]);
						}

						cplex.addGe(r1, 1);

				}else if(i!=j && this.infra.graph[i][j] == 1){

						IloLinearNumExpr r1 = cplex.linearNumExpr();

						for(int p = 0; p < maxProbes; p++) {
							r1.addTerm(1.0 , x[p][i][j]);
						}

						cplex.addGe(r1, 1);

				}else if (i!=j && this.infra.graph[j][i] == 1){

						IloLinearNumExpr r1 = cplex.linearNumExpr();

						for(int p = 0; p < maxProbes; p++) {
							r1.addTerm(1.0 , x[p][j][i]);
						}

						cplex.addGe(r1, 1);
				}
				
			}

		}
		
		
		/*origianl contraints*/
		//Constraints 2
				for(int p = 0; p < maxProbes; p++) {
					for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
						for(int i = 0; i < this.infra.size; i++) {

							IloLinearNumExpr r1 = cplex.linearNumExpr();

							for(int j = 0; j < this.infra.size; j++) {
								if(i != j && this.infra.graph[j][i] == 1) {
									r1.addTerm(1.0, x[p][j][i]);
								}
							}

							cplex.addLe(z[p][v][i], r1);

						}
					}
				}

				////Constraints 3
				for(int p = 0; p < maxProbes; p++) {
					for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
						for(int i = 0; i < this.infra.size; i++) {
							IloLinearNumExpr r1 = cplex.linearNumExpr();
							r1.addTerm(1.0, np[p]);
							cplex.addLe(z[p][v][i], r1);

						}
					}
				}
				//Constraint 3 --- ensuring that cycle is counted when there is no item being collected
				for(int p = 0; p < maxProbes; p++) {
					for(int i = 0; i < this.infra.size; i++) {
						for(int j = 0; j < this.infra.size; j++) {
							if(this.infra.graph[i][j] == 1) {
								IloLinearNumExpr r1 = cplex.linearNumExpr();
								r1.addTerm(1.0, np[p]);
								cplex.addLe(x[p][i][j], r1);
							}
							
						}
					}
				}


				for(int i = 0; i < this.infra.size; i++) {

					for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {

						if(this.infra.items[i][v] == 1) {

							IloLinearNumExpr r1 = cplex.linearNumExpr();

							for(int p = 0; p < maxProbes; p++) {

								r1.addTerm(1.0, z[p][v][i]);

							}

							cplex.addEq(r1, 1);
						}	

					}
				}

				//constraint (5.1)
				for(int p = 0; p < maxProbes; p++) {

					IloLinearNumExpr r1 = cplex.linearNumExpr();

					for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {

						for(int i = 0; i < this.infra.size; i++) {

							if(this.infra.items[i][v] == 1) {
								r1.addTerm(this.infra.sizeTelemetryItems[v], z[p][v][i]);
							}

						}
					}
					
					for(int i = 0; i < this.infra.size; i++) {
						for(int j = 0; j < this.infra.size; j++) {
							if(this.infra.graph[i][j] == 1) r1.addTerm(1.0, x[p][i][j]);
								
						}
					}

					cplex.addLe(r1, capacityProbe);

				}
		/*origianl contraints*/
		
		//teste subotour elimination
		
		int auxM = 100;//2*maxProbes*maxProbes;
		
		IloNumVar[][][] y = new IloNumVar[this.infra.size][this.infra.size][maxProbes];
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				y[i][j] = cplex.numVarArray(maxProbes, 0, Integer.MAX_VALUE);
			}
		}
		IloNumVar[][] b = new IloNumVar[this.infra.size][maxProbes];
		for(int i = 0; i < this.infra.size; i++) {
			b[i] = cplex.boolVarArray(maxProbes);
		}

		//first constraint
		for(int i = 0; i < this.infra.size; i++) {
			for(int k = 0; k < maxProbes; k++) {
			
				IloLinearNumExpr r1 = cplex.linearNumExpr();
				
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[j][i] == 1) {
						r1.addTerm(1.0, x[k][j][i]);
					}
				}
				
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1) {
						r1.addTerm(1.0, x[k][i][j]);
						
					}
				}
				
				IloLinearNumExpr r2 = cplex.linearNumExpr();
				r2.addTerm(auxM, b[i][k]);
				cplex.addLe(r1, r2);
				
			}
			
		}
		
		int contAux = 0;
		//second constraint
		
		
		for(int k = 0; k < maxProbes; k++) {
					
			
			for(int i = 0; i < this.infra.size; i++) {
				
				if(!sinks.contains(i)) {
				//if(i != sinks.get(contAux%sinks.size())) {
							
						
					IloLinearNumExpr r1 = cplex.linearNumExpr();
					//todos tem origem em 0
						
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[i][j] == 1) {
							r1.addTerm(1.0, y[i][j][k]);
						}
					}
						
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[j][i] == 1) {
							r1.addTerm(-1.0, y[j][i][k]);
						}
					}
					IloLinearNumExpr r2 = cplex.linearNumExpr();
					r2.addTerm(-1.0, b[i][k]);
					cplex.addEq(r1, r2);
					
				}else {
					//System.out.println("Sink: " + i);
				}
			}
					
			contAux++;
		}
		
		
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				for(int k = 0; k < maxProbes; k++) {
					if(this.infra.graph[i][j] == 1) {
						IloLinearNumExpr r1 = cplex.linearNumExpr();
						r1.addTerm(auxM, x[k][i][j]);
						cplex.addLe(y[i][j][k], r1);
					}
					
					
				}
			}
		}
			
		//adding variables to the model
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				for(int k = 0; k < maxProbes; k++) {
					cplex.add(x[k][i][j]);
				}
			}
		}
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				for(int k = 0; k < maxProbes; k++) {
					cplex.add(y[i][j][k]);
				}
			}
		}
		
		
		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		for(int p = 0; p < maxProbes; p++) {
			obj.addTerm(1, np[p]);
		}

		cplex.addMinimize(obj);
		cplex.setParam(IloCplex.IntParam.Threads, numThreads);
		cplex.setOut(null);


		if(cplex.solve()) {
			
			for(int p = 0; p < maxProbes; p++) {
				int residualAux = 0;
				
				for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
					for(int i = 0; i < this.infra.size; i++) {
						if (cplex.getValue(z[p][v][i]) > 0 ) {
							residualAux += this.infra.sizeTelemetryItems[v];
						}
					}
				}
				
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if (this.infra.graph[i][j] == 1 && cplex.getValue(x[p][i][j]) > 0 ) {
							residualAux++;
						}
					}
				}
				residualCapCycle[p] = residualAux;
			}

		}else {
			//System.out.println(cplex.getStatus());
		}


	}
}
