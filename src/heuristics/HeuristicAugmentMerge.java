package heuristics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.paukov.combinatorics3.CombinationGenerator;
import org.paukov.combinatorics3.Generator;
import org.paukov.combinatorics3.IGenerator;

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;
import main.NetworkInfrastructure;

public class HeuristicAugmentMerge {

	public NetworkInfrastructure infra;
	public int capacityProbe;
	public int maxProbes;

	public ArrayList<ArrayList<Tuple>> itemPerCycle;

	public Hashtable<Integer, Cycle> cycles;

	public HeuristicAugmentMerge(NetworkInfrastructure infra, int capacityProbe, int maxProbes) {

		this.infra = infra;
		this.maxProbes = maxProbes;

		this.capacityProbe = capacityProbe;

		this.itemPerCycle = new ArrayList<ArrayList<Tuple>>();

		this.cycles = new Hashtable<Integer, Cycle>();

	}

	//Verify if cycle 1 is equal to cycle 2
	private boolean isEqualCycle(ArrayList<Integer> cycle1, ArrayList<Integer> cycle2) {

		if(cycle1.size() != cycle2.size()) return false;

		boolean hasItem = false;
		for(int i = 0; i < cycle1.size(); i++) {
			hasItem = false;
			for(int j = 0; j < cycle2.size(); j++) {
				if(cycle1.get(i) == cycle2.get(j)) {
					hasItem = true;
					break;
				}
			}

			if(!hasItem) return false;

		}

		return true;

	}


	public void augmentMergeAllVariables() throws IloException {

		long timeInit = System.nanoTime();
		
		ArrayList<Integer> sinks = new ArrayList<Integer>();
		for(int k = 0; k < this.infra.size; k++) {
			sinks.add(k);
		}
		
		initialize();
		//verify if solution is feasible;
		
		long aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: " + aux );
		
		maxProbes = this.cycles.size() + 1;
	
		
		//Cplex variables
		IloCplex cplexModel = new IloCplex();
		
		IloNumVar[][][] x = new IloNumVar[maxProbes][this.infra.size][this.infra.size];
		IloNumVar[][][] z = new IloNumVar[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		Integer [][][] xSol = new Integer[maxProbes][this.infra.size][this.infra.size];
		Integer [][][] zSol = new Integer[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		for(int p = 0; p < maxProbes; p++) {
			
			for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplexModel.boolVarArray(this.infra.size);
			}

			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {

				z[p][v] = cplexModel.boolVarArray(this.infra.size);
			}
		
		}
		
		
		//<Integer> -> Cycle ID
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX = new Hashtable<Integer, ArrayList<IloConstraint>>();
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ = new Hashtable<Integer, ArrayList<IloConstraint>>();
		
		
		//Fixed all values from initial solution
		for(Integer p : this.cycles.keySet()) {
			int iAux, jAux;
			
			ArrayList<IloConstraint> res = new ArrayList<IloConstraint>();
			
			Hashtable<String, Integer> hasVarX = new Hashtable<String, Integer>();
			Hashtable<String, Integer> hasVarZ = new Hashtable<String, Integer>();
			
			
			for(int j = 0; j < this.cycles.get(p).links.size(); j++) {
					
				iAux =  (int)this.cycles.get(p).links.get(j).first;
				jAux =  (int)this.cycles.get(p).links.get(j).second;
					
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, x[p][iAux][jAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				res.add(eq);
				
				hasVarX.put(new String(Integer.toString(p) + Integer.toString(iAux) + Integer.toString(jAux)), 1);
				
			}
			
			//In case variable was not used, I set it to zero.
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(hasVarX.get(Integer.toString(p) + Integer.toString(i) + Integer.toString(j)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, x[p][i][j]);
						IloConstraint eq = cplexModel.addEq(expr, 0.0);
						res.add(eq);
					}
				}
			}
			
			hashVariableX.put(p, res);
			
			
			ArrayList<IloConstraint> res2 = new ArrayList<IloConstraint>();
			for(int j = 0; j < this.cycles.get(p).itemPerCycle.size(); j++) {
				
				iAux =  this.cycles.get(p).itemPerCycle.get(j).device;
				jAux =  this.cycles.get(p).itemPerCycle.get(j).item;
						
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, z[p][jAux][iAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				res2.add(eq);
				
				hasVarZ.put(new String(Integer.toString(p) + Integer.toString(jAux) + Integer.toString(iAux)), 1);
				
			}
			
			//In case variable was not used, I set it to zero.
			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				for(int i = 0; i < this.infra.size; i++) {
					if(hasVarZ.get(Integer.toString(p) + Integer.toString(v) + Integer.toString(i)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, z[p][v][i]);
						IloConstraint eq = cplexModel.addEq(expr, 0.0);
						res2.add(eq);
					}
				}
				
			}
			
			hashVariableZ.put(p, res2);
			
			
		}
		
		aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: (before building model) " + aux );
		
		buildPintModel(maxProbes, capacityProbe, sinks, x, z, cplexModel);
		
		aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: (after building model) " + aux );
		
		if(cplexModel.getStatus() == IloCplex.Status.Optimal) {
			
			double bestSolution = cplexModel.getObjValue();
			
			
			int control = 0;
			
			Random rnd = new Random();
			
			while(true) {
				
				System.out.println(" ");
				System.out.println("Iteration init");
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (before deleting constraints)"  + aux );
				
				ArrayList<Integer> mykey = new ArrayList<Integer>(this.cycles.keySet());
				
				int i;
				int j;
				int k;
				System.out.println( mykey.size() );
				do {
					i = mykey.get(rnd.nextInt(mykey.size()));
					j = mykey.get(rnd.nextInt(mykey.size()));
					k = mykey.get(rnd.nextInt(mykey.size()));
				}while(i == j || j == k || i == k);	
				
				System.out.println(i +  " " + j +  " " + k);
				
				setFreeVariables(i, hashVariableX, hashVariableZ, cplexModel);
				setFreeVariables(j, hashVariableX, hashVariableZ, cplexModel);
				setFreeVariables(k, hashVariableX, hashVariableZ, cplexModel);
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (after removing x,z)" + aux );
				
				ArrayList<Integer> indexProbes = new ArrayList<Integer>();
				indexProbes.add(i);
				indexProbes.add(j);
				indexProbes.add(k);
				
					
				ArrayList<IloConstraint> tourConstraints = addSubTourElimination(cplexModel,x, indexProbes);
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (after add subtour constraints)" + aux );
				
				if(cplexModel.solve()) {
					
					aux = (System.nanoTime() - timeInit) / 1000000;
					System.out.println("TimeElapsed: " + aux );
					
					if(cplexModel.getObjValue() < bestSolution) {
							
						System.out.println("Improved: " + cplexModel.getObjValue());
						
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: " + aux );
						
						bestSolution = cplexModel.getObjValue();
								
						//Copy solution from cplex variable to incumbent data strucutres...
						for(int probeId = 0; probeId < indexProbes.size(); probeId++) {
							hashVariableX.remove(indexProbes.get(probeId));
							hashVariableZ.remove(indexProbes.get(probeId));
						}
							
						Hashtable<String, Integer> hasVarZ = new Hashtable<String, Integer>();
							
						for(int p, probeId = 0; probeId < indexProbes.size(); probeId++) {
							p = indexProbes.get(probeId);
							for(int d1 = 0; d1 < this.infra.size; d1++) {
								for(int d2 = 0; d2 < this.infra.size; d2++) {
									if(this.infra.graph[d1][d2] == 1 && cplexModel.getValue(x[p][d1][d2]) == 1) {
										xSol[p][d1][d2] = 1;
									}else {
										xSol[p][d1][d2] = 0;
									}
								}
							}
								
							for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
								for(int d = 0; d < this.infra.size; d++) {
									if (cplexModel.getValue(z[p][v][d]) > 0 ) {
										zSol[p][v][d] = 1;
									}else {
										zSol[p][v][d] = 0;
									}
								}
							}
						}
						
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: (before add contraints)" + aux );
								
						//Rebuild hash
						for(int p, probeId = 0; probeId < indexProbes.size(); probeId++) {
								
							p = indexProbes.get(probeId);
							ArrayList<IloConstraint> res = new ArrayList<IloConstraint>();
							boolean isCycleActive = false;
							for(int d1 = 0; d1 < this.infra.size; d1++) {
								for(int d2 = 0; d2 < this.infra.size; d2++) {
									if(this.infra.graph[d1][d2] == 1){
										
										if(xSol[p][d1][d2] == 1) {
											IloLinearNumExpr expr = cplexModel.linearNumExpr();
											expr.addTerm(1.0, x[p][d1][d2]);
											IloConstraint eq = cplexModel.addEq(expr, 1.0);
											res.add(eq);
											isCycleActive = true;
										}else{
											IloLinearNumExpr expr = cplexModel.linearNumExpr();
											expr.addTerm(1.0, x[p][d1][d2]);
											IloConstraint eq = cplexModel.addEq(expr, 0.0);
											res.add(eq);
										}
									}
								}
							}
							
							if(isCycleActive) {
								hashVariableX.put(p, res);
							}else {
								this.cycles.remove(p);
							}
							
							
							isCycleActive = false;
							ArrayList<IloConstraint> res2 = new ArrayList<IloConstraint>();
							for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
								for(int d = 0; d < this.infra.size; d++) {
									if (zSol[p][v][d] == 1) {
										IloLinearNumExpr expr = cplexModel.linearNumExpr();
										expr.addTerm(1.0, z[p][v][d]);
										IloConstraint eq = cplexModel.addEq(expr, 1.0);
										res2.add(eq);
										isCycleActive = true;
									}else {
										IloLinearNumExpr expr = cplexModel.linearNumExpr();
										expr.addTerm(1.0, z[p][v][d]);
										IloConstraint eq = cplexModel.addEq(expr, 0.0);
										res2.add(eq);
									}
								}
							}
								
							if(isCycleActive) {
								hashVariableZ.put(p, res2);
							}
							
						}
							
						removeSubTourElimiation(cplexModel,tourConstraints);
						
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: (after adding contraints) " + aux );
						
						
							
					}else {
						removeSubTourElimiation(cplexModel,tourConstraints);
						setFixVariables(i, hashVariableX, hashVariableZ, cplexModel);
						setFixVariables(j, hashVariableX, hashVariableZ, cplexModel);
						setFixVariables(k, hashVariableX, hashVariableZ, cplexModel);
					}
						
				}else {
					
					System.out.println("Infeasible");
					
					removeSubTourElimiation(cplexModel,tourConstraints);
					setFixVariables(i, hashVariableX, hashVariableZ, cplexModel);
					setFixVariables(j, hashVariableX, hashVariableZ, cplexModel);
					setFixVariables(k, hashVariableX, hashVariableZ, cplexModel);
				}
					
			}
				
		}
			
	}
	
	public void printVariableZ(IloCplex cplex, IloNumVar[][][] z, IloNumVar[][][] x) throws UnknownObjectException, IloException {
		
		for(int p = 0; p < maxProbes; p++) {
		
			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				for(int i = 0; i < this.infra.size; i++) {
					if (cplex.getValue(z[p][v][i]) > 0 ) {
						System.out.println("z[" +  p + "][" + v + "][" + i + "] - 1");
					}
				}
			}
		}
		
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1 && cplex.getValue(x[p][i][j]) == 1) {
						System.out.println("x[" + p + "][" + i + "][" + j + "] = " + cplex.getValue(x[p][i][j]));
					}
				}
			}
		
		}
		
	}
	
	public void augmentMerge() throws IloException, CloneNotSupportedException {

		long timeInit = System.nanoTime();
		
		ArrayList<Integer> sinks = new ArrayList<Integer>();
		for(int k = 0; k < this.infra.size; k++) {
			sinks.add(k);
		}
		
		//initialize();
		
		OptPathPlan pathPlanCycles = new OptPathPlan(infra, capacityProbe, (long) 123, true);
		pathPlanCycles.adaptToLinks();
		
		for(int i = 0; i < pathPlanCycles.Q.size(); i++) this.cycles.put(i, pathPlanCycles.Q.get(i));
		
		long aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: " + aux );
		
		maxProbes = this.cycles.size() + 1;
	
		//Cplex model
		IloCplex cplexModel = new IloCplex();
		
		//Cplex variables
		IloNumVar[][][] x = new IloNumVar[maxProbes][this.infra.size][this.infra.size];
		IloNumVar[][][] z = new IloNumVar[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		//Aux variables 
		Integer [][][] xSol = new Integer[maxProbes][this.infra.size][this.infra.size];
		Integer [][][] zSol = new Integer[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
		
		//Allocate memory
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplexModel.boolVarArray(this.infra.size);
			}
			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				z[p][v] = cplexModel.boolVarArray(this.infra.size);
			}
		
		}
		
		//buildPintModel(maxProbes, capacityProbe, sinks, x, z, cplexModel);
		//System.exit(-1);
		
		
		//<Integer> -> Cycle ID
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX = new Hashtable<Integer, ArrayList<IloConstraint>>();
		Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ = new Hashtable<Integer, ArrayList<IloConstraint>>();
		
		
		//Fixed all values from initial solution
		for(Integer p : this.cycles.keySet()) {
			
			//System.out.println(p);
			int iAux, jAux;
			
			ArrayList<IloConstraint> res = new ArrayList<IloConstraint>();
			
			Hashtable<String, Integer> hasVarX = new Hashtable<String, Integer>();
			Hashtable<String, Integer> hasVarZ = new Hashtable<String, Integer>();
			
			
			for(int j = 0; j < this.cycles.get(p).links.size(); j++) {
					
				iAux =  (int)this.cycles.get(p).links.get(j).first;
				jAux =  (int)this.cycles.get(p).links.get(j).second;
					
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, x[p][iAux][jAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				res.add(eq);
				
				hasVarX.put(new String(Integer.toString(p) + Integer.toString(iAux) + Integer.toString(jAux)), 1);
				
				xSol[p][iAux][jAux] = 1;
				
			}
			
			
			//In case variable was not used, I set it to zero.
			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1 && hasVarX.get(Integer.toString(p) + Integer.toString(i) + Integer.toString(j)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, x[p][i][j]);
						IloConstraint eq = cplexModel.addEq(expr, 0);
						res.add(eq);
						
						xSol[p][i][j] = 0;
						
					}
				}
			}
			
			hashVariableX.put(p, res);
			
			/*
			ArrayList<IloConstraint> res2 = new ArrayList<IloConstraint>();
			for(int j = 0; j < this.cycles.get(p).itemPerCycle.size(); j++) {
				
				iAux =  this.cycles.get(p).itemPerCycle.get(j).device;
				jAux =  this.cycles.get(p).itemPerCycle.get(j).item;
						
				IloLinearNumExpr expr = cplexModel.linearNumExpr();
				expr.addTerm(1.0, z[p][jAux][iAux]);
				IloConstraint eq = cplexModel.addEq(expr, 1.0);
				res2.add(eq);
				
				hasVarZ.put(new String(Integer.toString(p) + Integer.toString(jAux) + Integer.toString(iAux)), 1);
				
				zSol[p][jAux][iAux] = 1;
				
			}
			
			//In case variable was not used, I set it to zero.
			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				for(int i = 0; i < this.infra.size; i++) {
					if(hasVarZ.get(Integer.toString(p) + Integer.toString(v) + Integer.toString(i)) == null){
						IloLinearNumExpr expr = cplexModel.linearNumExpr();
						expr.addTerm(1.0, z[p][v][i]);
						IloConstraint eq = cplexModel.addEq(expr, 0.0);
						res2.add(eq);
						
						zSol[p][v][i] = 0;
					}
				}
				
			}
			
			hashVariableZ.put(p, res2);
			*/
			
		}
		
		aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: (before building model) " + aux );
		
		buildPintModel(maxProbes, capacityProbe, sinks, x, z, cplexModel);
		
		for(int i = 0; i < this.cycles.size(); i++) {
			System.out.println(this.cycles.get(i).links);
			//System.out.println(this.cycles.get(i).itemPerCycle);
		}
		System.exit(-1);	
		//buildPintModel1Source(maxProbes, capacityProbe, x, z, cplexModel);
		//System.exit(-1);
		
		aux = (System.nanoTime() - timeInit) / 1000000;
		System.out.println("TimeElapsed: (after building model) " + aux );
		
		ArrayList<Integer> myIndex = new ArrayList<Integer>();
		myIndex.add(1);
		myIndex.add(2);
		myIndex.add(3);
		myIndex.add(4);
		
		Object[] t2 = Generator.combination(myIndex).simple(2).stream().toArray();
		Object[] t3 = Generator.combination(myIndex).simple(3).stream().toArray();
		
		int hasNotImproved = 0; 
		int hasNotImprovedMax = 10; //this should be a parameter afterwards
		
		if(cplexModel.getStatus() == IloCplex.Status.Optimal) {
			
			double bestSolution = cplexModel.getObjValue();
			
			Random rnd = new Random();
			
			//
			while(hasNotImproved < hasNotImprovedMax) {
				
				System.out.println(" ");
				System.out.println("Iteration init");
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (before deleting constraints)"  + aux );
				
				ArrayList<Integer> mykey = new ArrayList<Integer>(this.cycles.keySet());
				
				int i;
				int j;
				int k;
				
				do {
					i = mykey.get(rnd.nextInt(mykey.size()));
					j = mykey.get(rnd.nextInt(mykey.size()));
					k = mykey.get(rnd.nextInt(mykey.size()));
				}while(i == j || j == k || i == k);	
				
				
				ArrayList<Integer> indexProbes = new ArrayList<Integer>();
				indexProbes.add(i);
				indexProbes.add(j);
				indexProbes.add(k);
				
				System.out.println(i +  " " + j +  " " + k);
				
				System.out.println("TimeElapsed: (after removing x,z)" + aux );
				
				setFreeVariables(i, hashVariableX, hashVariableZ, cplexModel);
				setFreeVariables(j, hashVariableX, hashVariableZ, cplexModel);
				setFreeVariables(k, hashVariableX, hashVariableZ, cplexModel);
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (after removing x,z)" + aux );
					
				ArrayList<IloConstraint> tourConstraints = addSubTourElimination(cplexModel,x, indexProbes);
				
				aux = (System.nanoTime() - timeInit) / 1000000;
				System.out.println("TimeElapsed: (after add subtour constraints)" + aux );
				
				if(cplexModel.solve()) {
					
					aux = (System.nanoTime() - timeInit) / 1000000;
					System.out.println("TimeElapsed: " + aux );
					
					if(cplexModel.getObjValue() < bestSolution) {
						
						System.out.println("Improved: " + cplexModel.getObjValue());
						System.exit(-1);	
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: " + aux );
						
						bestSolution = cplexModel.getObjValue();
								
						//Copy solution from cplex variable to incumbent data strucutres...
						for(int probeId = 0; probeId < indexProbes.size(); probeId++) {
							hashVariableX.remove(indexProbes.get(probeId));
							hashVariableZ.remove(indexProbes.get(probeId));
						}
							
						Hashtable<String, Integer> hasVarZ = new Hashtable<String, Integer>();
							
						for(int p, probeId = 0; probeId < indexProbes.size(); probeId++) {
							p = indexProbes.get(probeId);
							for(int d1 = 0; d1 < this.infra.size; d1++) {
								for(int d2 = 0; d2 < this.infra.size; d2++) {
									if(this.infra.graph[d1][d2] == 1 && cplexModel.getValue(x[p][d1][d2]) > 0) {
										xSol[p][d1][d2] = 1;
									}else {
										xSol[p][d1][d2] = 0;
									}
								}
							}
								
							for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
								for(int d = 0; d < this.infra.size; d++) {
									if (cplexModel.getValue(z[p][v][d]) > 0 ) {
										zSol[p][v][d] = 1;
									}else {
										zSol[p][v][d] = 0;
									}
								}
							}
						}
						
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: (before add contraints)" + aux );
						
						
						//Rebuild hash
						for(int p, probeId = 0; probeId < indexProbes.size(); probeId++) {
								
							p = indexProbes.get(probeId);
							ArrayList<IloConstraint> res = new ArrayList<IloConstraint>();
							boolean isCycleActive = false;
							for(int d1 = 0; d1 < this.infra.size; d1++) {
								for(int d2 = 0; d2 < this.infra.size; d2++) {
									if(this.infra.graph[d1][d2] == 1){
										
										if(xSol[p][d1][d2] == 1) {
											IloLinearNumExpr expr = cplexModel.linearNumExpr();
											expr.addTerm(1.0, x[p][d1][d2]);
											IloConstraint eq = cplexModel.addEq(expr, 1.0);
											res.add(eq);
											isCycleActive = true;
										}else{
											IloLinearNumExpr expr = cplexModel.linearNumExpr();
											expr.addTerm(1.0, x[p][d1][d2]);
											IloConstraint eq = cplexModel.addEq(expr, 0.0);
											res.add(eq);
											
										}
									}
								}
							}
							
							if(isCycleActive) {
								hashVariableX.put(p, res);
								
							}else {
								this.cycles.remove(p);
							}
							
							
							isCycleActive = false;
							ArrayList<IloConstraint> res2 = new ArrayList<IloConstraint>();
							for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
								for(int d = 0; d < this.infra.size; d++) {
									if (zSol[p][v][d] == 1) {
										IloLinearNumExpr expr = cplexModel.linearNumExpr();
										expr.addTerm(1.0, z[p][v][d]);
										IloConstraint eq = cplexModel.addEq(expr, 1.0);
										res2.add(eq);
										isCycleActive = true;
									}else {
										IloLinearNumExpr expr = cplexModel.linearNumExpr();
										expr.addTerm(1.0, z[p][v][d]);
										IloConstraint eq = cplexModel.addEq(expr, 0.0);
										res2.add(eq);
									}
								}
							}
								
							if(isCycleActive) {
								hashVariableZ.put(p, res2);
							}
							
						}
							
						//removeSubTourElimiation(cplexModel,tourConstraints);
						
						aux = (System.nanoTime() - timeInit) / 1000000;
						System.out.println("TimeElapsed: (after adding contraints) " + aux );
						
						
							
					}else {
						//removeSubTourElimiation(cplexModel,tourConstraints);
						setFixVariables(i, hashVariableX, hashVariableZ, cplexModel);
						setFixVariables(j, hashVariableX, hashVariableZ, cplexModel);
						setFixVariables(k, hashVariableX, hashVariableZ, cplexModel);
					}
						
				}else {
					//removeSubTourElimiation(cplexModel,tourConstraints);
					setFixVariables(i, hashVariableX, hashVariableZ, cplexModel);
					setFixVariables(j, hashVariableX, hashVariableZ, cplexModel);
					setFixVariables(k, hashVariableX, hashVariableZ, cplexModel);
				}
					
			}
				
		}
			
	}
	
	public void setFreeVariables(int idProbe, Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX,
			Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ , IloCplex cplexModel) throws IloException {
		
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
		
		res = hashVariableZ.get(idProbe);
		cons1 = null;
		
		if(res != null) {
			cons1 = new IloConstraint[res.size()];
			for(int k = 0; k < res.size(); k++) {
				cons1[k] = res.get(k);
			}
		}
		
		if(cons1 != null)
			cplexModel.remove(cons1);
		
		//cplexModel.remove((IloAddable[]) res.toArray());
		/*if(res != null) {
			for(int k = 0; k < res.size(); k++) {
				cplexModel.remove(res.get(k));
			}
		}*/
		
		
	}
	
	public void setFixVariables(int idProbe, Hashtable<Integer, ArrayList<IloConstraint>> hashVariableX,
			Hashtable<Integer, ArrayList<IloConstraint>> hashVariableZ , IloCplex cplexModel) throws IloException {
		
		ArrayList<IloConstraint> res = hashVariableX.get(idProbe);
		for(int k = 0; k < res.size(); k++) {
			cplexModel.add(res.get(k));
		}
		
		res = hashVariableZ.get(idProbe);
		for(int k = 0; k < res.size(); k++) {
			cplexModel.add(res.get(k));
		}
		
	}
	
	public ArrayList<IloConstraint> addSubTourElimination(IloCplex cplex, IloNumVar[][][] x, ArrayList<Integer> idProbes) throws IloException {
		
		ArrayList<IloConstraint> addedConstraints = new ArrayList<IloConstraint>();
		
		//This is the number of probes being "merged"
		int[] nodesInfra = new int[this.infra.size];
		int numNodesProbe = 0;
		
		//We do this to count only once nodes...
		for(int i = 0; i < idProbes.size(); i++) {
			for(int j = 0; j < this.cycles.get(idProbes.get(i)).nodes.size(); j++) {
				nodesInfra[this.cycles.get(idProbes.get(i)).nodes.get(j)] = 1;
			}
		}
		
		for(int i = 0; i < this.infra.size; i++) numNodesProbe += nodesInfra[i];
		
		int[] nodes = new int[numNodesProbe];
		
		for(int i = 0, j = 0; i < this.infra.size; i++) {
			if(nodesInfra[i] == 1) nodes[j++] = i;
		}
		
		List<List<Integer>> powerset = subsets(nodes);
		
		
		IloNumVar[][][] y = new IloNumVar[maxProbes][2][powerset.size()];
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < 2; i++) {
				y[p][i] = cplex.boolVarArray(powerset.size());
			}
		}
		
		//Aqui não deve ser maxProbes... mas os indices dos ciclos...
		int p;
		
		for(int pIndex = 0; pIndex < idProbes.size(); pIndex++) {
			
			p = idProbes.get(pIndex);
			
			for(int k = 0; k < powerset.size(); k++) {

				if(powerset.get(k).size() >= 2 && powerset.get(k).size() < numNodesProbe) {
					//System.out.println(powerset.get(k).size());
					IloLinearNumExpr r1 = cplex.linearNumExpr();
					for(int i = 0; i < powerset.get(k).size(); i++) {
						for(int j = 0; j < powerset.get(k).size(); j++) {
							if(this.infra.graph[powerset.get(k).get(i)][powerset.get(k).get(i)] == 1) {
								r1.addTerm(1.0, x[p][powerset.get(k).get(i)][powerset.get(k).get(j)]);
							}
						}
					}
					r1.addTerm(-1.0*Math.pow(this.infra.size,2), y[p][0][k]);
					
					addedConstraints.add( cplex.addLe(r1, powerset.get(k).size() - 1) );


					IloLinearNumExpr r2 = cplex.linearNumExpr();
					for(int i = 0; i < powerset.get(k).size(); i++) {
						ArrayList<Integer> notInQ = notInQ(powerset.get(k));
						for(int j = 0; j < notInQ.size(); j++) {
							if(this.infra.graph[powerset.get(k).get(i)][notInQ.get(j)] == 1) {
								r2.addTerm(1.0, x[p][powerset.get(k).get(i)][notInQ.get(j)]);
							}
						}
					}
					r2.addTerm(1.0, y[p][1][k]);
					addedConstraints.add( cplex.addGe(r2, 1) );


					IloLinearNumExpr r3 = cplex.linearNumExpr();
					r3.addTerm(1.0, y[p][0][k]);
					r3.addTerm(1.0, y[p][1][k]);
					addedConstraints.add( cplex.addLe(r3, 1) );

				}

			}

		}
		
		return addedConstraints;
				 
		
	}

	public void removeSubTourElimiation(IloCplex cplex, ArrayList<IloConstraint> addedConstraints) throws IloException {
		
		IloConstraint[] cons1 = new IloConstraint[addedConstraints.size()];
		
		if(addedConstraints != null) {
			for(int k = 0; k < addedConstraints.size(); k++) {
				cons1[k] = addedConstraints.get(k);
			}
		}
		
		cplex.remove(cons1);
		
		/*for(int i = 0; i < addedConstraints.size(); i++) {
			cplex.remove(addedConstraints.get(i));
		}*/
		
	}
	
	public void buildPintModel(int maxProbes, int capacityProbe, ArrayList<Integer> sinks, 
			IloNumVar[][][]x, IloNumVar[][][] z, IloCplex cplex) throws IloException{
		
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

		//constraint (5.1)
		/*for(int p = 0; p < maxProbes; p++) {

			IloLinearNumExpr r1 = cplex.linearNumExpr();

			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1) r1.addTerm(1.0, x[p][i][j]);
				}
			}

			cplex.addLe(r1, capacityProbe);

		}*/

		
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
					
			System.out.println("k" + k);
			
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
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		
		//cplex.setOut(null);
		//cplex.setParam(IloCplex.DoubleParam.TiLim, 7200);
		
		//cplex.setParam(IloCplex.DoubleParam.EpGap, 0.1);
		
		
		if(cplex.solve()) {
			
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < infra.size; j++) {
					for(int k = 0; k < infra.size; k++) {
						if(cplex.getValue(x[i][j][k]) > 0) {
							//System.out.println("x[" + i + "][" + j + "][" + k + "]=" + cplex.getValue(x[i][j][k]) );
						}
						//this.xMetrics[i][j][k] = cplex.getValue(x[i][j][k]);
						//System.out.println("xMetrics[" + i + "][" + j + "][" + k + "]=" + xMetrics[i][j][k]);
					}
				}
			} 
			
			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < infra.size; k++) {
						if(cplex.getValue(z[i][j][k]) > 0) {
							//System.out.println("z[" + i + "][" + j + "][" + k + "]=" + cplex.getValue(z[i][j][k]) );
							
						}
						//this.zMetrics[i][j][k] = cplex.getValue(z[i][j][k]);
					}
				}
			}
			
			int usedProbes = 0;
			for(int p = 0; p < maxProbes; p++) {
				if(cplex.getValue(np[p]) == 1.0) {
					//System.out.println(cplex.getValue(np[p])); 
					usedProbes++;
					
				}
			
			}
		}else {
			System.out.println("-1" + ";" + (System.nanoTime() - startTime)*0.000000001 + ";" 
					+ this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter 
					+ ";" + this.infra.seed);
			//System.out.println("Infeasible");
		}
		
	}
	
	public void buildPintModelBKP2505(int maxProbes, int capacityProbe, IloNumVar[][][] x, 
			IloNumVar[][][] z, IloCplex cplex) throws IloException {

		long t = System.currentTimeMillis();
		
		int[] nodes = new int[this.infra.size];
		for(int i = 0; i < this.infra.size; i++) {
			nodes[i] = i;
		}
		
		List<List<Integer>> powerset = subsets(nodes);

		
		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
		}

		IloNumVar[][][] y = new IloNumVar[maxProbes][2][powerset.size()];
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < 2; i++) {
				y[p][i] = cplex.boolVarArray(powerset.size());
			}
		}

		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < 2; i++) {
				for(int k = 0; k < powerset.size(); k++) {
					cplex.add(y[p][i][k]);
				}
			}
		}
		
		//System.out.println("Building Model");

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


		int aux[][] = new int[this.infra.size][this.infra.size]	;

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
		
		
		
		
		powerset.sort(new ComparatorPermSize());
		int infraSizeAux = this.infra.size * this.infra.size;
		
		//Constraint (6) subtour elimination
		IloNumVar[][] auxVar = new IloNumVar[maxProbes][powerset.size()];
		IloNumVar[][] auxVarB = new IloNumVar[maxProbes][powerset.size()];
		for(int p = 0; p < maxProbes; p++) {
			auxVar[p] = cplex.numVarArray(powerset.size(), 0, infraSizeAux);
			auxVarB[p] = cplex.boolVarArray(powerset.size());
		}
		
		for(int p = 0; p < maxProbes; p++) {
		//for(int p = 6; p < 7/*maxProbes*/; p++) {

			for(int k = 0; k < powerset.size(); k++) {
			//for(int k = 16; k < 17 /*powerset.size()*/; k++) {
				if(powerset.get(k).size() >= 2 && powerset.get(k).size() < this.infra.size) {
					boolean isToAdd = false;
					System.out.println(powerset.get(k));
					IloLinearNumExpr r1 = cplex.linearNumExpr();
					for(int i = 0; i < powerset.get(k).size(); i++) {
						for(int j = 0; j < powerset.get(k).size(); j++) {
							if(this.infra.graph[powerset.get(k).get(i)][powerset.get(k).get(j)] == 1) {
								r1.addTerm(1.0, x[p][powerset.get(k).get(i)][powerset.get(k).get(j)]);
								System.out.println("   --->" + p + " " + powerset.get(k).get(i) + " " + powerset.get(k).get(j));
								isToAdd = true;
							}
						}
					}
					if(isToAdd == true) {
						
						r1.addTerm(-1.0*infraSizeAux, y[p][0][k]);
						cplex.addLe(r1, powerset.get(k).size()-1); //troquei para N (origianl constraint cplex.addLe(r1, powerset.get(k).size()-1);) 
						System.out.println("   -------->");
						
						IloLinearNumExpr r2 = cplex.linearNumExpr();
						
						for(int i = 0; i < powerset.get(k).size(); i++) {
							ArrayList<Integer> notInQ = notInQ(powerset.get(k));
							for(int j = 0; j < notInQ.size(); j++) {
								if(this.infra.graph[powerset.get(k).get(i)][notInQ.get(j)] == 1) {
									r2.addTerm(1.0, x[p][powerset.get(k).get(i)][notInQ.get(j)]);
									System.out.println("   ------------>" +  powerset.get(k).get(i) + " " +notInQ.get(j));
								}
							}
						}
						
						r2.addTerm(1.0, y[p][1][k]);
						cplex.addGe(r2, 1.0);

						IloLinearNumExpr r3 = cplex.linearNumExpr();
						r3.addTerm(1.0, y[p][0][k]);
						r3.addTerm(1.0, y[p][1][k]);
						cplex.addLe(r3, 1);
						
					}
					
				}

			}


		}
 		

		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		for(int p = 0; p < maxProbes; p++) {
			obj.addTerm(1, np[p]);
		}

		cplex.addMinimize(obj);
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		cplex.exportModel("out.lp");
		//cplex.setOut(null);
		
		//cplex.setParam(IloCplex.DoubleParam.EpGap, 0.6);
		
		if(cplex.solve()) {
			
			int usedProbes = 0;
			
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < 2; i++) {
					for(int k = 0; k < powerset.size(); k++) {
						if(cplex.getValue(y[p][i][k]) > 0) {
							System.out.println("y[" +  p + "][" + i + "][" + k + "]" +cplex.getValue(y[p][i][k])); 
						}
						
					}
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
				if(cplex.getValue(np[p]) == 1.0) {
					System.out.println(p + " " +cplex.getValue(np[p])); 
					usedProbes++;
					
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
					
				for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
					for(int i = 0; i < this.infra.size; i++) {
						if (cplex.getValue(z[p][v][i]) > 0 ) {
							System.out.println("z[" +  p + "][" + v + "][" + i + "] - 1");
						}
					}
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[i][j] == 1 && cplex.getValue(x[p][i][j]) == 1) {
							System.out.println("x[" + p + "][" + i + "][" + j + "] = " + cplex.getValue(x[p][i][j]));
						}
					}
				}
			
			}
			System.out.println("UsedProbes: " + usedProbes);
			
		}else {
			System.out.println("infeasible: ");
			
		}


	}
	
	public void buildPintModel1Source(int maxProbes, int capacityProbe, 
			IloNumVar[][][] x, IloNumVar[][][] z, IloCplex cplex) throws IloException {

		long t = System.currentTimeMillis();
		
				
		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
		}
		
		
		/*IloNumVar[][][] x = new IloNumVar[maxProbes][this.infra.size][this.infra.size];
		
		//Allocate memory
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplex.boolVarArray(this.infra.size);
			}
		}*/

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

		//constraint (5.1)
		for(int p = 0; p < maxProbes; p++) {

			IloLinearNumExpr r1 = cplex.linearNumExpr();

			for(int i = 0; i < this.infra.size; i++) {
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1) r1.addTerm(1.0, x[p][i][j]);
				}
			}

			cplex.addLe(r1, capacityProbe);

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
					if(this.infra.graph[j][i] == 1) {
						r1.addTerm(1.0, x[k][i][j]);
						
					}
				}
				
				IloLinearNumExpr r2 = cplex.linearNumExpr();
				r2.addTerm(auxM, b[i][k]);
				cplex.addLe(r1, r2);
				
			}
			
		}
		
		
		//second constraint
		for(int i = 0; i < this.infra.size; i++) {
			if(i != 4) {
				for(int k = 0; k < maxProbes; k++) {
					
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
					
				}
			}
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
		
		/*for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				for(int k = 0; k < maxProbes; k++) {
					obj.addTerm(1.0, x[k][i][j]);
				}
			}
		}*/
		
		for(int p = 0; p < maxProbes; p++) {
			obj.addTerm(1, np[p]);
		}

		cplex.addMinimize(obj);
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		//cplex.exportModel("out.lp");
		//cplex.setOut(null);
		
		//cplex.setParam(IloCplex.DoubleParam.EpGap, 0.3);
		
		if(cplex.solve()) {
			
			int usedProbes = 0;
			
			for(int p = 0; p < maxProbes; p++) {
				if(cplex.getValue(np[p]) == 1.0) {
					usedProbes++;
				}
			}
			
			for(int i = 0; i < this.infra.size; i++) {
				for(int p = 0; p < maxProbes; p++) {
					if(cplex.getValue(b[i][p]) == 1.0) {
						System.out.println("b[" + i + "][" + p +  "] == 1" ); 
					}
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(cplex.getValue(x[p][i][j]) > 0.01) {
							System.out.println("x[" + p + "][" + i + "][" + j + "] = " + cplex.getValue(x[p][i][j]));
						}
					}
				}
			
			}
			System.out.println("  ");
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(cplex.getValue(y[i][j][p]) > 0.01) {
							System.out.println("y[" + i + "][" + j + "][" + p + "] = " + cplex.getValue(y[i][j][p]) + " -->> " + cplex.getValue(x[p][i][j]));
						}
					}
				}
			}
			
					
			
			for(int p = 0; p < maxProbes; p++) {
					
				for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
					for(int i = 0; i < this.infra.size; i++) {
						if (cplex.getValue(z[p][v][i]) > 0 ) {
							System.out.println("z[" +  p + "][" + v + "][" + i + "] - 1");
						}
					}
				}
			}
			
			System.out.println("UsedProbes: " + usedProbes);
			
			
			
			
		}else {
			System.out.println("infeasible: ");
			
		}


	}
	
	public void buildPintModelBKP(int maxProbes, int capacityProbe, IloNumVar[][][] x, 
			IloNumVar[][][] z, IloCplex cplex) throws IloException {

		long t = System.currentTimeMillis();

				//Define as variaves do problema

		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
			/*for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplex.boolVarArray(this.infra.size);
			}

			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {

				z[p][v] = cplex.boolVarArray(this.infra.size);

			}*/
		}



		//System.out.println("Building Model");

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
		//It counts the number of probes being used.
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
					IloLinearNumExpr r1 = cplex.linearNumExpr();
					r1.addTerm(1.0, np[p]);
					cplex.addLe(x[p][i][j], r1);
				}
			}
		}


		//Constraint (5) : x[i][j] = x[j][i
		for(int i = 0; i < this.infra.size; i++) {
			//System.out.println("Node " + i);
			for(int p = 0; p < maxProbes; p++) {

				IloLinearNumExpr r1 = cplex.linearNumExpr();
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[i][j] == 1) {
						r1.addTerm(1.0, x[p][i][j]);
						//System.out.println("	+Node " + i +  " - " + j);
					}
				}
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[j][i] == 1) {
						r1.addTerm(-1.0, x[p][j][i]);
						//System.out.println("	-Node " + j +  " - " + i);
					}
				}

				cplex.addEq(r1, 0);
			}

		}


		int aux[][] = new int[this.infra.size][this.infra.size]	;

		//Contraint (3) : l[i][j] + l[j][i] = 1
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

		////Constraint (6) subtour elimination
		IloNumVar[][][] f = new IloNumVar[this.infra.size][this.infra.size][this.maxProbes];

		for(int i = 0; i < this.infra.size; i++) {
			for(int k = 0; k < this.infra.size; k++) {
				f[i][k] = cplex.numVarArray(this.maxProbes, 0, Float.MAX_VALUE);
			}
		}
		
		for(int i = 0; i < this.infra.size; i++) {
			for(int p = 0; p < this.maxProbes; p++) {
				IloLinearNumExpr r1 = cplex.linearNumExpr();
				for(int k = 0; k < this.infra.size; k++) {
					if(this.infra.graph[i][k] == 1) {
						r1.addTerm(1.0, f[i][k][p]);
						System.out.println("+f[" + i + "][" +  k  + "][" + p +"]" );
					}
				}
				
				for(int k = 0; k < this.infra.size; k++) {
					if(this.infra.graph[k][i] == 1) {
						r1.addTerm(-1.0, f[k][i][p]);
						System.out.println("-f[" + k + "][" +  i  + "][" + p +"]" );
					}
				}
				
				IloLinearNumExpr r2 = cplex.linearNumExpr();
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1) {
						r2.addTerm(1.0, x[p][i][j]);
						System.out.println("x[" + p + "][" +  i  + "][" + j +"]" );
					}
				}
				
				cplex.addEq(r1, r2);
				
			}
			
		}
		
		int sizeInfra = this.infra.size * this.infra.size;
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				if(this.infra.graph[i][j] == 1) {
					for(int p = 0; p < this.maxProbes; p++) {
						IloLinearNumExpr r1 = cplex.linearNumExpr();
						r1.addTerm(1.0, f[i][j][p]);
						
						IloLinearNumExpr r2 = cplex.linearNumExpr();
						r2.addTerm(sizeInfra, x[p][i][j]);
						
						cplex.addLe(r1, r2);
					}
				}
			}
		}
				
		
		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		for(int p = 0; p < maxProbes; p++) {

			obj.addTerm(1, np[p]);
			
		}

		cplex.addMinimize(obj);
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		//cplex.setOut(null);


		if(cplex.solve()) {
			int usedProbes = 0;
			for(int p = 0; p < maxProbes; p++) {
				System.out.println(" ");
				if(cplex.getValue(np[p]) == 1.0) {
					//System.out.println(cplex.getValue(np[p])); 
					usedProbes++;
					
				}
					
				for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
					for(int i = 0; i < this.infra.size; i++) {
						if (cplex.getValue(z[p][v][i]) > 0 ) {
							System.out.println("z[" +  p + "][" + v + "][" + i + "] - 1");
						}
					}
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[i][j] == 1 && cplex.getValue(x[p][i][j]) == 1) {
							System.out.println("x[" + p + "][" + i + "][" + j + "] = " + cplex.getValue(x[p][i][j]));
						}
					}
				}
			
			}
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[i][j] == 1 && cplex.getValue(f[i][j][p]) == 1) {
							System.out.println("f[" + i + "][" + j + "][" + p + "] = " + cplex.getValue(f[i][j][p]));
						}
					}
				}
			
			}
			System.out.println("UsedProbes: " + usedProbes);
		}


	}
	
	public void buildPintModelBkp1003(int maxProbes, int capacityProbe, IloNumVar[][][] x, 
			IloNumVar[][][] z, IloCplex cplex) throws IloException {

		long t = System.currentTimeMillis();

		int[] nodes = new int[this.infra.size];
		for(int i = 0; i < this.infra.size; i++) {
			nodes[i] = i;
		}
		
		List<List<Integer>> powerset = subsets(nodes);


		//Define as variaves do problema

		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
			/*for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplex.boolVarArray(this.infra.size);
			}

			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {

				z[p][v] = cplex.boolVarArray(this.infra.size);

			}*/
		}

		IloNumVar[][][] y = new IloNumVar[maxProbes][2][powerset.size()];

		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < 2; i++) {
				y[p][i] = cplex.boolVarArray(powerset.size());
			}
		}
		
		//adding variables to the model.
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < 2; i++) {
				for(int k = 0; k < powerset.size();k++) {
					cplex.add(y[p][i][k]);
				}
				
			}
		}

		//System.out.println("Building Model");

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
		//It counts the number of probes being used.
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
					IloLinearNumExpr r1 = cplex.linearNumExpr();
					r1.addTerm(1.0, np[p]);
					cplex.addLe(x[p][i][j], r1);
				}
			}
		}


		//Constraint (5) : x[i][j] = x[j][i
		for(int i = 0; i < this.infra.size; i++) {
			//System.out.println("Node " + i);
			for(int p = 0; p < maxProbes; p++) {

				IloLinearNumExpr r1 = cplex.linearNumExpr();
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[i][j] == 1) {
						r1.addTerm(1.0, x[p][i][j]);
						//System.out.println("	+Node " + i +  " - " + j);
					}
				}
				for(int j = 0; j < this.infra.size; j++) {
					if(i != j && this.infra.graph[j][i] == 1) {
						r1.addTerm(-1.0, x[p][j][i]);
						//System.out.println("	-Node " + j +  " - " + i);
					}
				}

				cplex.addEq(r1, 0);
			}

		}


		int aux[][] = new int[this.infra.size][this.infra.size]	;

		//Contraint (3) : l[i][j] + l[j][i] = 1
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

		//subtour elimination test
		IloNumVar[][][] f = new IloNumVar[this.infra.size][this.infra.size][this.maxProbes];

		for(int i = 0; i < this.infra.size; i++) {
			for(int k = 0; k < this.infra.size; k++) {
				f[i][k] = cplex.numVarArray(this.maxProbes, 0, Float.MAX_VALUE);
			}
		}
		
		for(int i = 0; i < this.infra.size; i++) {
			for(int p = 0; p < this.maxProbes; p++) {
				IloLinearNumExpr r1 = cplex.linearNumExpr();
				for(int k = 0; k < this.infra.size; k++) {
					if(this.infra.graph[i][k] == 1) {
						r1.addTerm(1.0, f[i][k][p]);
					}
				}
				
				for(int k = 0; k < this.infra.size; k++) {
					if(this.infra.graph[k][i] == 1) {
						r1.addTerm(1.0, f[k][i][p]);
					}
				}
				
				IloLinearNumExpr r2 = cplex.linearNumExpr();
				for(int j = 0; j < this.infra.size; j++) {
					if(this.infra.graph[i][j] == 1) {
						r2.addTerm(1.0, x[p][i][j]);
					}
				}
				
				cplex.addEq(r1, r2);
				
			}
			
		}
		
		int sizeInfra = this.infra.size * this.infra.size;
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				if(this.infra.graph[i][j] == 1) {
					for(int p = 0; p < this.maxProbes; p++) {
						IloLinearNumExpr r1 = cplex.linearNumExpr();
						r1.addTerm(1.0, f[i][j][p]);
						
						IloLinearNumExpr r2 = cplex.linearNumExpr();
						r2.addTerm(sizeInfra, x[p][i][j]);
						
						cplex.addLe(r1, r2);
					}
				}
			}
		}
				

		//Constraint (6) subtour elimination
		/*
		for(int p = 0; p < maxProbes; p++) {

			for(int k = 0; k < powerset.size(); k++) {

				if(powerset.get(k).size() >= 2 && powerset.size() < nodes.length) {

					IloLinearNumExpr r1 = cplex.linearNumExpr();
					for(int i = 0; i < powerset.get(k).size(); i++) {
						for(int j = 0; j < powerset.get(k).size(); j++) {
							if(this.infra.graph[powerset.get(k).get(i)][powerset.get(k).get(i)] == 1) {
								r1.addTerm(1.0, x[p][powerset.get(k).get(i)][powerset.get(k).get(j)]);
							}
						}

					}
					r1.addTerm(-1.0*Math.pow(this.infra.size,2), y[p][0][k]);
					cplex.addLe(r1, powerset.get(k).size() - 1);


					IloLinearNumExpr r2 = cplex.linearNumExpr();
					for(int i = 0; i < powerset.get(k).size(); i++) {
						ArrayList<Integer> notInQ = notInQ(powerset.get(k));
						for(int j = 0; j < notInQ.size(); j++) {
							if(this.infra.graph[powerset.get(k).get(i)][notInQ.get(j)] == 1) {
								r2.addTerm(1.0, x[p][powerset.get(k).get(i)][notInQ.get(j)]);
							}
						}

					}
					r2.addTerm(1.0, y[p][1][k]);
					cplex.addGe(r2, 1);


					IloLinearNumExpr r3 = cplex.linearNumExpr();
					r3.addTerm(1.0, y[p][0][k]);
					r3.addTerm(1.0, y[p][1][k]);
					cplex.addLe(r3, 1);

				}

			}


		}*/
		
		

		IloLinearNumExpr obj = cplex.linearNumExpr();
		
		

		for(int p = 0; p < maxProbes; p++) {

			obj.addTerm(1, np[p]);
			
		}


		cplex.addMinimize(obj);
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		//cplex.setOut(null);


		if(cplex.solve()) {
			int usedProbes = 0;
			for(int p = 0; p < maxProbes; p++) {
				System.out.println(" ");
				if(cplex.getValue(np[p]) == 1.0) {
					//System.out.println(cplex.getValue(np[p])); 
					usedProbes++;
					for(int k = 0; k < powerset.size(); k++) {
						if(cplex.getValue(y[p][0][k]) > 0) {
							System.out.println(cplex.getValue(y[p][0][k]) + " " + cplex.getValue(y[p][1][k]));
						}
						
					}
					
				}
					
				for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
					for(int i = 0; i < this.infra.size; i++) {
						if (cplex.getValue(z[p][v][i]) > 0 ) {
							System.out.println("z[" +  p + "][" + v + "][" + i + "] - 1");
						}
					}
				}
			}
			
			for(int p = 0; p < maxProbes; p++) {
				for(int i = 0; i < this.infra.size; i++) {
					for(int j = 0; j < this.infra.size; j++) {
						if(this.infra.graph[i][j] == 1 && cplex.getValue(x[p][i][j]) == 1) {
							System.out.println("x[" + p + "][" + i + "][" + j + "] = " + cplex.getValue(x[p][i][j]));
						}
					}
				}
			
			}
			System.out.println("UsedProbes: " + usedProbes);
		}


	}


	public double solveModel(ResultModel model, boolean hasOutput){

		IloCplex cplex = model.modelCplex;

		if (!hasOutput){
			cplex.setOut(null);
		}

		try{
			cplex.setParam(IloCplex.DoubleParam.TiLim, 1000); //this.timeLimit//TiLim
			if (cplex.solve()){

				//readPaths(model);
				return cplex.getObjValue();

			}else{
				return -1;
			}

		}catch(IloException e){
			System.out.println("Exception" + e);
		}

		return -1;

	}


	
	private void initialize2() {
		
		double path1, path2;
		int depot = 0;
		
		int contIdCycle = 0;

		ArrayList<Integer> path = new ArrayList<Integer>();

		ArrayList<Integer> p1 = new ArrayList<Integer>();
		ArrayList<Integer> p2 = new ArrayList<Integer>();

		ArrayList<Integer> paux = null;

		int[][] collected = new int[this.infra.size][this.infra.telemetryItemsRouter];
		for(int k = 0; k < this.infra.size; k++) {
			for(int l = 0; l < this.infra.telemetryItemsRouter; l++) {
				collected[k][l] = 0;
			}
		}

		//(for now, supposing origin is at node 0).
		int size = this.infra.size;

		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {

				if(this.infra.graph[i][j] == 1) {

					Cycle currentCycle = new Cycle();

					p1 = infra.getShortestPath(0, i);
					p2 = infra.getShortestPath(0, j);
					
					if(p1.size() != 0 && p2.size() != 0) {
						for(int k = p2.size()-1; k>=0; k--) {
							p1.add(p2.get(k));
						}
						currentCycle.nodes.addAll(p1);
						
						for(int k = 0; k < p1.size()-1; k++) {
							currentCycle.links.add(new Pair<Integer,Integer>(p1.get(k), p1.get(k+1)));
						}
						
						paux = p1;
					}else if(p1.size() == 0) {
						for(int k = p2.size()-2; k>=0; k--) {
							p2.add(p2.get(k));
						}
						currentCycle.nodes.addAll(p2);
						
						for(int k = 0; k < p2.size()-1; k++) {
							currentCycle.links.add(new Pair<Integer,Integer>(p2.get(k), p2.get(k+1)));
						}
						
						
						paux = p2;
					}else if(p2.size() == 0) {
						for(int k = p1.size()-2; k>=0; k--) {
							p1.add(p1.get(k));
						}
						currentCycle.nodes.addAll(p1);
						
						for(int k = 0; k < p1.size()-1; k++) {
							currentCycle.links.add(new Pair<Integer,Integer>(p1.get(k), p1.get(k+1)));
						}
						
						paux = p1;
						
					}else {
						continue;
					}
					
					int auxCapacity = currentCycle.nodes.size()-1;

					ArrayList<Tuple> itemsInCycle = new ArrayList<Tuple>();


					boolean hasAdd = false;

					//Collect all items

					for(int indexDevice = 0; indexDevice < currentCycle.nodes.size(); indexDevice++) {

						for(int indexItem = 0; indexItem < infra.numTelemetryItemsRouter[currentCycle.nodes.get(indexDevice)]; indexItem++) {


							if(infra.items[currentCycle.nodes.get(indexDevice)][indexItem] == 1 && collected[currentCycle.nodes.get(indexDevice)][indexItem] == 0) {
								if (infra.sizeTelemetryItems[indexItem] + auxCapacity <= this.capacityProbe) {

									//System.out.println(" collected " + currentCycle.nodes.get(indexDevice) + " " +  indexItem);

									collected[currentCycle.nodes.get(indexDevice)][indexItem] = 1;
									//pack item into this cycle
									auxCapacity += infra.sizeTelemetryItems[indexItem];
									itemsInCycle.add(new Tuple(currentCycle.nodes.get(indexDevice), indexItem));
									hasAdd = false;

								}else {
									//add another cycle

									currentCycle.itemPerCycle.addAll(itemsInCycle);
									this.cycles.put(contIdCycle++, currentCycle);

									hasAdd = true;

									auxCapacity = 0;

									ArrayList linksAux = new ArrayList<Tuple>();
									linksAux.addAll(currentCycle.links);
									currentCycle = new Cycle();
									currentCycle.nodes.addAll(paux);
									currentCycle.links.addAll(linksAux);

									itemsInCycle = new ArrayList<Tuple>();

									indexItem--;


								}

							}	

						}

					}

					if (!hasAdd) {
						currentCycle.itemPerCycle.addAll(itemsInCycle);
						
						this.cycles.put(contIdCycle++, currentCycle);
						
						
					}



				}	

			}
		}
		
	}

	private void initialize() {

		double path1, path2;
		int depot = 0;
		
		int contIdCycle = 0;

		ArrayList<Integer> path = new ArrayList<Integer>();

		ArrayList<Integer> p1 = new ArrayList<Integer>();
		ArrayList<Integer> p2 = new ArrayList<Integer>();
		ArrayList<Integer> p3 = new ArrayList<Integer>();
		ArrayList<Integer> paux = null;

		int[][] collected = new int[this.infra.size][this.infra.telemetryItemsRouter];
		for(int k = 0; k < this.infra.size; k++) {
			for(int l = 0; l < this.infra.telemetryItemsRouter; l++) {
				collected[k][l] = 0;
			}
		}
		
		//(for now, supposing origin is at node 0).
		int size = this.infra.size;
		
		HashMap<String, Integer> coveredLinkSet = new HashMap<String, Integer>();
		

		for(int i = 0; i < size; i++) {
			for(int j = i; j < size; j++) {

				if(this.infra.graph[i][j] == 1 && !coveredLinkSet.containsKey(Integer.toString(i) + Integer.toString(j))) {
					
					Cycle currentCycle = new Cycle();

					p1 = infra.getShortestPath(0, i);
					p2 = infra.getShortestPath(0, j);
					
					if(p1.size() != 0 && p2.size() != 0) {
						p1.add(j);
						
						p3 = infra.getShortestPath(j, 0);
						
						if(p3.size() == 0) System.out.println("bug");
						
						for(int k = 1; k < p3.size(); k++) {
							p1.add(p3.get(k));
						}
						
						//verify if we add duplicates
						for(int k = 0; k < p1.size()-1; k++) {
							if(p1.get(k) == p1.get(k+1)) {
								p1.remove(k);
								k = 0;
							}
						}
						
						currentCycle.nodes.addAll(p1);
						
						for(int k = 0; k < p1.size()-1; k++) {
							currentCycle.links.add(new Pair<Integer,Integer>(p1.get(k), p1.get(k+1)));
						}
						
						paux = p1;
					}else if(p1.size() == 0) {
						
						boolean boolHasArc = false;
						for(int k = 0; k < p2.size()-1; k++) {
							if(p2.get(k) == i && p2.get(k+1) == j) {
								boolHasArc = true;
								break;
							}
						}
						
						if(!boolHasArc) System.out.println("Bug 2");
						
						p3 = infra.getShortestPath(j, 0);
						
						if(p3.size() == 0) System.out.println("bug 3");
						
						for(int k = 0; k < p3.size(); k++) {
							p2.add(p3.get(k));
						}
						
						//verify if we add duplicates
						for(int k = 0; k < p2.size()-1; k++) {
							if(p2.get(k) == p2.get(k+1)) {
								p2.remove(k);
								k = 0;
							}
						}
						
						currentCycle.nodes.addAll(p2);
						
						for(int k = 0; k < p2.size()-1; k++) {
							currentCycle.links.add(new Pair<Integer,Integer>(p2.get(k), p2.get(k+1)));
						}
						
						
						paux = p2;
					}else {
						continue;
					}
					
					int auxCapacity = currentCycle.nodes.size()-1;

					ArrayList<Tuple> itemsInCycle = new ArrayList<Tuple>();


					boolean hasAdd = false;

					for(int k = 0; k < currentCycle.links.size(); k++) {
						int aux1 = currentCycle.links.get(k).first;
						int aux2 = currentCycle.links.get(k).second;
						coveredLinkSet.put(Integer.toString(aux1) + Integer.toString(aux2), 1);
						coveredLinkSet.put(Integer.toString(aux2) + Integer.toString(aux1), 1);
						
					}
					
					
					//Collect all items
					for(int indexDevice = 0; indexDevice < currentCycle.nodes.size(); indexDevice++) {

						for(int indexItem = 0; indexItem < infra.numTelemetryItemsRouter[currentCycle.nodes.get(indexDevice)]; indexItem++) {


							if(infra.items[currentCycle.nodes.get(indexDevice)][indexItem] == 1 && collected[currentCycle.nodes.get(indexDevice)][indexItem] == 0) {
								if (infra.sizeTelemetryItems[indexItem] + auxCapacity <= this.capacityProbe) {

									//System.out.println(" collected " + currentCycle.nodes.get(indexDevice) + " " +  indexItem);

									collected[currentCycle.nodes.get(indexDevice)][indexItem] = 1;
									//pack item into this cycle
									auxCapacity += infra.sizeTelemetryItems[indexItem];
									itemsInCycle.add(new Tuple(currentCycle.nodes.get(indexDevice), indexItem));
									hasAdd = false;

								}else {
									//add another cycle

									currentCycle.itemPerCycle.addAll(itemsInCycle);
									this.cycles.put(contIdCycle++, currentCycle);

									hasAdd = true;

									auxCapacity = 0;

									ArrayList linksAux = new ArrayList<Tuple>();
									linksAux.addAll(currentCycle.links);
									currentCycle = new Cycle();
									currentCycle.nodes.addAll(paux);
									currentCycle.links.addAll(linksAux);

									itemsInCycle = new ArrayList<Tuple>();

									indexItem--;


								}

							}	

						}

					}

					if (!hasAdd) {
						currentCycle.itemPerCycle.addAll(itemsInCycle);
						
						this.cycles.put(contIdCycle++, currentCycle);
						
						
					}



				}	

			}
		}
		
		/*
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				if(this.infra.graph[i][j] == 1) {
					coveredLinkSet.put(Integer.toString(i) + Integer.toString(j), 1);
					
				}
			}
		}*/
		
		for(Integer p : this.cycles.keySet()) {
			System.out.println("Cycle " + p);
			System.out.println(this.cycles.get(p).links);
			for(int i = 0; i < this.cycles.get(p).links.size(); i++) {
				int k = this.cycles.get(p).links.get(i).first;
				int l = this.cycles.get(p).links.get(i).second;
				
				if(this.infra.graph[k][l] == 0) {
					System.out.println("Arc used wrongly");
				}
				
				//coveredLinkSet.put(Integer.toString(k) + Integer.toString(l), 1);
				
				
			}
			
		}
				
		System.out.println("ok");

	}
	
	ArrayList<Integer> getOut(int j){
		
		ArrayList<Integer> out = new ArrayList<Integer>();
		for(int i = 0; i < this.infra.size; i++) {
			if(this.infra.graph[j][i] == 1) out.add(i);
		}
		
		return out;
	}
	
	ArrayList<Integer> getIn(int j){
		
		ArrayList<Integer> in = new ArrayList<Integer>();
		for(int i = 0; i < this.infra.size; i++) {
			if(this.infra.graph[i][j] == 1) in.add(i);
		}
		
		return in;
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



}
