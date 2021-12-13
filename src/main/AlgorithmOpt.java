package main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.*;

public class AlgorithmOpt {

	NetworkInfrastructure infra;

	IloCplex cplex = null;
	int[][][] xMetrics;
	int[][][] zMetrics;
	public double time;
	public int maxProbes;

	public AlgorithmOpt(NetworkInfrastructure infra, int maxProbes) throws IloException {	
		this.cplex = new IloCplex();
		this.infra = infra;
		this.maxProbes = maxProbes;
		this.xMetrics = new int[maxProbes][this.infra.size][this.infra.size];
		this.zMetrics = new int[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];
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



	void buildCPPTelemetry(int maxProbes, int capacityProbe, ArrayList<Integer> sinks) throws IloException {

		long startTime = System.nanoTime();

		//Cplex variables
		IloNumVar[][][] z = new IloNumVar[maxProbes][this.infra.telemetryItemsRouter][this.infra.size];

		//Allocate memory
		for(int p = 0; p < maxProbes; p++) {

			for(int v = 0; v < this.infra.telemetryItemsRouter; v++) {
				z[p][v] = cplex.boolVarArray(this.infra.size);
			}

		}

		IloNumVar[] np = new IloNumVar[maxProbes];

		for(int p = 0; p < maxProbes; p++) {
			np = cplex.boolVarArray(maxProbes);
		}


		IloNumVar[][][] x = new IloNumVar[maxProbes][this.infra.size][this.infra.size];

		//Allocate memory
		for(int p = 0; p < maxProbes; p++) {
			for(int i = 0; i < this.infra.size; i++) {
				x[p][i] = cplex.boolVarArray(this.infra.size);
			}
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
		cplex.setParam(IloCplex.IntParam.Threads, 4);
		//cplex.setOut(null);
		//cplex.setParam(IloCplex.DoubleParam.TiLim, 3600);

		//cplex.setParam(IloCplex.DoubleParam.EpGap, 0.2);


		if(cplex.solve()) {

			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < infra.size; j++) {
					for(int k = 0; k < infra.size; k++) {
						this.xMetrics[i][j][k] = (int)cplex.getValue(x[i][j][k]);
						//System.out.println("xMetrics[" + i + "][" + j + "][" + k + "]=" + xMetrics[i][j][k]);
					}
				}
			} 

			for(int i = 0; i < maxProbes; i++) {
				for(int j = 0; j < infra.telemetryItemsRouter; j++) {
					for(int k = 0; k < infra.size; k++) {
						this.zMetrics[i][j][k] = (int)cplex.getValue(z[i][j][k]);
						//System.out.println("zMetrics[" + i + "][" + j + "][" + k + "]=" + zMetrics[i][j][k]);
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

			/*System.out.println("Opt" + ";" + this.cplex.getObjValue() + ";" + (System.nanoTime() - startTime)*0.000000001 
					+ ";" + this.infra.size + ";" + this.infra.telemetryItemsRouter
					+ ";" + this.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe);*/

		}else {
			System.out.println("-1" + ";" + (System.nanoTime() - startTime)*0.000000001 + ";" 
					+ this.infra.size + ";" + this.infra.telemetryItemsRouter + ";" + this.infra.maxSizeTelemetryItemsRouter 
					+ ";" + this.infra.seed);
			//System.out.println("Infeasible");
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

	public void printSolution() {
		for(int i = 0; i < this.maxProbes; i++) {
			for(int j = 0; j < infra.size; j++) {
				for(int k = 0; k < infra.size; k++) {
					if(xMetrics[i][j][k] == 1) {
						System.out.println("x[" + i + "][" + j + "][" + k + "] = " + xMetrics[i][j][k]);
					}
				}
			}

			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				for(int k = 0; k < infra.size; k++) {
					if(zMetrics[i][j][k] == 1) {
						System.out.println("z[" + i + "][" + j + "][" + k + "] = " + zMetrics[i][j][k]);
					}
				}
			}
		}
	}



}

