package heuristics;

import java.util.ArrayList;
import java.util.HashSet;


import main.NetworkInfrastructure;

public class CARP_Victor {
	int[][] graph;
	int[][] items;
	public NetworkInfrastructure infra;
	public int num_routers;
	public int collector;
	public int probe_capacity;
	public double time;
	public ArrayList<ArrayList<Integer>> result;
	public ArrayList<ArrayList<Pair<Integer, Integer>>> resultItems;
	public ArrayList<Pair<Integer, Integer>> remainingEdges;
	public ArrayList<Cycle> cycles;
	
	
	public CARP_Victor(NetworkInfrastructure infrastructure, int capacityProbe, int collector) {
		this.infra = infrastructure;
		this.num_routers = infra.size;
		this.collector = collector;
		this.result = new ArrayList<ArrayList<Integer>>();
		this.resultItems = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
		this.remainingEdges = new ArrayList<Pair<Integer, Integer>>();
		this.probe_capacity = capacityProbe;
		
		
		this.graph = new int[infra.size][infra.size];
		for(int i = 0; i < infra.size; i++) {
			for(int j = 0; j < infra.size; j++) {
				this.graph[i][j] = infra.graph[i][j];
			}
		}
		this.items = new int[infra.size][infra.telemetryItemsRouter];
		for(int i = 0; i < infra.size; i++) {
			for(int j = 0; j < infra.telemetryItemsRouter; j++) {
				this.items[i][j] = infra.items[i][j];
			}
		}
		this.startRemainingEdges();
	}
	
	private void startRemainingEdges() {	
		//start every node as non visited, that is to be 'visited'(globally)
		Pair<Integer,Integer> edge = Pair.create(-2,-2);
		for(int i = 0; i < this.num_routers; i++) {
			for(int j = 0; j < this.num_routers; j++) {	
				if(this.graph[i][j] == 1) {
					edge = Pair.create(i, j);
					this.remainingEdges.add(edge);
				}		
			}
		}
	}
	
	public boolean hasItems(int router) {
		boolean has_items = false;
		for(int i = 0; i < this.infra.telemetryItemsRouter; i++) {
			if(this.items[router][i] == 1) {
				has_items = true;
			}
		}
		return has_items;
	}
	
	public boolean graphHasItems() {
		boolean hasItems = false;
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				if(this.items[i][j] == 1) {
					hasItems = true;
				}
			}
		}
		return hasItems;
	}
	
	public int findItem() {
		int node = -1;
		boolean found = false;
		for(int i = 0; i < this.num_routers; i++) {
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				if(this.items[i][j] == 1) {
					found = true;
					node = i;
					break;
				}
			}
			if(found) {
				break;
			}
		}
		return node;
	}
	
	public int chooseCycleStart() {
		int bestNode = 0;
		int bestValue = Integer.MIN_VALUE;
		for(int i = 0; i < this.num_routers; i++) {
			int value = Integer.MIN_VALUE;
			for(int j = 0; j < this.infra.telemetryItemsRouter; j++) {
				if(this.items[i][j] == 1) {
					value += 10; 
				}
			}
			for(int j = 0; j < this.num_routers; j++) {
				if(this.graph[i][j] == 1) {
					value++;
				}
			}
			if(value > bestValue) {
				bestValue = value;
				bestNode = i;
			}
		}
		
		return bestNode;
	}
	
	public void run2() {
		boolean flag_regress = false;
		boolean flag_items = false;
		ArrayList<ArrayList<Integer>> paths = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Pair<Integer, Integer>>> itemsPath = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
		boolean[][] visited = new boolean[this.num_routers][this.num_routers]; //starts false
		
		int smallerItem = Integer.MAX_VALUE;
		for(int i = 0; i < this.infra.sizeTelemetryItems.length; i++) {
			if(this.infra.sizeTelemetryItems[i] < smallerItem) {
				smallerItem = this.infra.sizeTelemetryItems[i];
			}
		}
		
		while(true) {
			ArrayList<Integer> pathAtual = new ArrayList<Integer>();
			ArrayList<Pair<Integer, Integer>> itemsPathAtual = new ArrayList<Pair<Integer, Integer>>();
			int w = this.probe_capacity;
			int old_w = w;
			int collector = this.chooseCycleStart();
			int ant_node = this.chooseCycleStart();
			int prev_node = this.chooseCycleStart();
			this.collector = collector;
			int hops = 0;
			while(true) {
				pathAtual.add(ant_node);
				if(hasItems(ant_node)) {
					ArrayList<Integer> shortPath = this.infra.getShortestPath(ant_node, collector);          //calcula a quantidade de hops at?? o coletor
					int hopsAfterShortPath = 0;
					if(shortPath.size() == 0) {
						hopsAfterShortPath = hops + shortPath.size();
					}else {						
						hopsAfterShortPath = hops + shortPath.size() - 1;
					}
					for(int i = 0; i < this.infra.telemetryItemsRouter; i++) {
						if(this.items[ant_node][i] == 1) {
							if(hops == 0) {
								if(w >= this.infra.sizeTelemetryItems[i] + hopsAfterShortPath + 2) {   //apenas vai coletar o item se houver capacidade para colet??-lo e realizar um ciclo simples
									this.items[ant_node][i] = 0;
									w -= this.infra.sizeTelemetryItems[i];                     //apenas considera o peso do item na capacidade do probe
									Pair<Integer, Integer> pair = Pair.create(ant_node, i); 
									itemsPathAtual.add(pair);                               //adiciona que o item i foi coletado do roteador ant_node
								}
							}else {								
								if(w >= this.infra.sizeTelemetryItems[i] + hopsAfterShortPath) {   //apenas vai coletar o item se houver capacidade para colet??-lo e para voltar at?? o coletor
									this.items[ant_node][i] = 0;
									w -= this.infra.sizeTelemetryItems[i];                     //apenas considera o peso do item na capacidade do probe
									Pair<Integer, Integer> pair = Pair.create(ant_node, i); 
									itemsPathAtual.add(pair);                               //adiciona que o item i foi coletado do roteador ant_node
								}
							}
						}
					}
				}
				boolean canCollectItem = false;
				if(w >= smallerItem + ((hops + 1)*2)) {
					canCollectItem = true;
				}
				if(hasItems(ant_node) && hops != 0) {                                                                   //apenas vai entrar nesse if se o probe n??o tiver capacidade para coletar mais itens do nodo
					ArrayList<Integer> caminhoVolta = this.infra.getShortestPath(ant_node, collector);
					for(int j = 0; j < caminhoVolta.size(); j++) {
						pathAtual.add(caminhoVolta.get(j));
						if(j != 0) {
							Pair<Integer, Integer> edge = Pair.create(caminhoVolta.get(j-1), caminhoVolta.get(j));
							Pair<Integer, Integer> edge_reverse = Pair.create(caminhoVolta.get(j), caminhoVolta.get(j-1));
							remainingEdges.remove(edge);
							remainingEdges.remove(edge_reverse);
						}
					}
						
					itemsPath.add(itemsPathAtual);
					paths.add(pathAtual);
					break;                                                                             //termina o ciclo voltando para o coletor
				}else if(!canCollectItem) {                                               //apenas entra nesse caso se o probe s?? tiver capacidade para retornar at?? o coletor(evita itera????o in??til)
					ArrayList<Integer> caminhoVolta = this.infra.getShortestPath(ant_node, collector);
					for(int j = 0; j < caminhoVolta.size(); j++) {
						pathAtual.add(caminhoVolta.get(j));
						if(j != 0) {
							Pair<Integer, Integer> edge = Pair.create(caminhoVolta.get(j-1), caminhoVolta.get(j));
							Pair<Integer, Integer> edge_reverse = Pair.create(caminhoVolta.get(j), caminhoVolta.get(j-1));
							remainingEdges.remove(edge);
							remainingEdges.remove(edge_reverse);
						}
					}
					itemsPath.add(itemsPathAtual);
					paths.add(pathAtual);
					break;                                                                             //termina o ciclo voltando para o coletor
				}else if(flag_items) {
					int next_node = findItem();
					if(next_node == -1) {
						flag_items = false;
						continue;
					}
					ArrayList<Integer> caminhoIda = this.infra.getShortestPath(ant_node, next_node);      //adiciona o caminho at?? o pr??ximo nodo ao caminho atual do probe
					for(int j = 0; j < caminhoIda.size(); j++) {
						pathAtual.add(caminhoIda.get(j));
						if(j != 0) {
							Pair<Integer, Integer> edge = Pair.create(caminhoIda.get(j-1), caminhoIda.get(j));
							Pair<Integer, Integer> edge_reverse = Pair.create(caminhoIda.get(j), caminhoIda.get(j-1));
							remainingEdges.remove(edge);
							remainingEdges.remove(edge_reverse);
						}
					}
					hops += caminhoIda.size() - 1;
					ant_node = next_node;
					continue;
				}else {
					if(!flag_regress) {                                                                //entra nesse caso quando acabar itens de um roteador e o probe ainda tiver capacidade sobrando
						int next_node = -1;
						for(int i = 0; i < this.infra.size; i++) {
							if(graph[ant_node][i] == 1 && visited[ant_node][i] == false && i != prev_node) {
								next_node = i;
								prev_node = ant_node;
								hops++;
								break;
							}
						}
						if(next_node == -1) {                                                      //entra nesse caso quando n??o houver vizinhos n??o visitados
							if(remainingEdges.isEmpty()) {                                     //caso n??o haja mais arestas a serem visitadas, o ciclo foi conclu??do
								ArrayList<Integer> caminhoVolta = this.infra.getShortestPath(ant_node, collector);
								for(int j = 0; j < caminhoVolta.size(); j++) {
									pathAtual.add(caminhoVolta.get(j));
									if(j != 0) {
										Pair<Integer, Integer> edge = Pair.create(caminhoVolta.get(j-1), caminhoVolta.get(j));
										Pair<Integer, Integer> edge_reverse = Pair.create(caminhoVolta.get(j), caminhoVolta.get(j-1));
										remainingEdges.remove(edge);
										remainingEdges.remove(edge_reverse);
									}
								}
								itemsPath.add(itemsPathAtual);
								paths.add(pathAtual);
								break;
							}else if(ant_node == this.collector) {                               //se o probe retornou ao coletor sem utilizar toda a capacidade
								flag_regress = true;
								continue;
							}else {                                                             //se o probe n??o retornou ao coletor, faz o caminho de volta
								ArrayList<Integer> caminhoVolta = this.infra.getShortestPath(ant_node, collector);
								for(int j = 0; j < caminhoVolta.size(); j++) {
									pathAtual.add(caminhoVolta.get(j));
									if(j != 0) {
										Pair<Integer, Integer> edge = Pair.create(caminhoVolta.get(j-1), caminhoVolta.get(j));
										Pair<Integer, Integer> edge_reverse = Pair.create(caminhoVolta.get(j), caminhoVolta.get(j-1));
										remainingEdges.remove(edge);
										remainingEdges.remove(edge_reverse);
									}
								}
								itemsPath.add(itemsPathAtual);
								paths.add(pathAtual);
								break;
							}
						}else {                                                                     //se ainda houver um vizinho n??o visitado
							Pair<Integer, Integer> edge = Pair.create(ant_node, next_node);
							Pair<Integer, Integer> edge_reverse = Pair.create(next_node, ant_node);
							remainingEdges.remove(edge);
							remainingEdges.remove(edge_reverse);
							if(!hasItems(next_node) && !hasItems(ant_node)) { 								
								visited[ant_node][next_node] = true;
								visited[next_node][ant_node] = true;
							}
							ant_node = next_node;
						}
					}else {                                                                             //se o probe voltou ao coletor sem utilizar toda a capacidade
						int prox_node = -1;
						if(remainingEdges.isEmpty()) {                                              //se n??o houver mais arestas n??o visitadas
							itemsPath.add(itemsPathAtual);
							paths.add(pathAtual);
							break;
						}
						for(Pair<Integer, Integer> p: remainingEdges) {                             //escolhe uma aresta n??o visitada como pr??ximo nodo a ser visitado
							prox_node = p.first;
							break;
						}
						ArrayList<Integer> caminhoIda = this.infra.getShortestPath(ant_node, prox_node);      //adiciona o caminho at?? o pr??ximo nodo ao caminho atual do probe
						for(int j = 0; j < caminhoIda.size(); j++) {
							pathAtual.add(caminhoIda.get(j));
							if(j != 0) {
								Pair<Integer, Integer> edge = Pair.create(caminhoIda.get(j-1), caminhoIda.get(j));
								Pair<Integer, Integer> edge_reverse = Pair.create(caminhoIda.get(j), caminhoIda.get(j-1));
								remainingEdges.remove(edge);
								remainingEdges.remove(edge_reverse);
							}
						}
						hops += caminhoIda.size() - 1;
						ant_node = prox_node;
						flag_regress = false;
						continue;
					}
					if(old_w == w && ant_node == collector) {
						flag_regress = true;
					}else {
						old_w = w;
					}
				}
			}
			if(this.remainingEdges.isEmpty() && !graphHasItems()) {
				break;
			}else if(this.remainingEdges.isEmpty() && graphHasItems()) {
				flag_items = true;
			}else{
				/*ArrayList<Integer> caminhoVolta = getShortestPath(ant_node, this.collector);
				for(int j = 0; j < caminhoVolta.size(); j++) {
					pathAtual.add(caminhoVolta.get(j));
				}
					
				itemsPath.add(itemsPathAtual);
				paths.add(pathAtual);*/
			}
		}
		
		this.result = paths;
		this.resultItems = itemsPath;
	}
	
	
	public void convertToCycles() {
		ArrayList<Cycle> cycles_ = new ArrayList<Cycle>();
		
		for(int i = 0; i < result.size(); i++) {
			Cycle cycle = new Cycle();
			ArrayList<Integer> path = new ArrayList<Integer>();
			int last = result.get(i).get(0);
			path.add(last);
			for(int j = 1; j < result.get(i).size(); j++) {
				if(result.get(i).get(j) != last) {
					path.add(result.get(i).get(j));
					last = result.get(i).get(j);
				}else {
					last = result.get(i).get(j);
				}
			}
			
			cycle.nodes = path;
			
			ArrayList<Tuple> deviceItems = new ArrayList<Tuple>();
			
			for(int j = 0; j < resultItems.get(i).size(); j++) {
				Pair<Integer, Integer> p = resultItems.get(i).get(j);
				Tuple t = new Tuple(p.first, p.second);
				deviceItems.add(t);
			}
			
			cycle.itemPerCycle = deviceItems;
			
			int cap_used = 0;
			for(Tuple t: cycle.itemPerCycle) {
				cap_used += this.infra.sizeTelemetryItems[t.item];
			}
			cap_used += cycle.nodes.size() - 1;
			
			cycle.capacity = this.probe_capacity;
			cycle.capacity_used = cap_used;
			
			cycles_.add(cycle);
		}
		
		this.cycles = cycles_;
	}
	
	public HashSet<Integer> depotNodesCycles() {
    	HashSet<Integer> depotNodes = new HashSet<Integer>();
    	int minTransportationCost = Integer.MAX_VALUE;
    	int minNode = 0;
		for(int i = 0; i < this.num_routers; i++) {
			int transportationCost = 0;
			for(Cycle c: this.cycles) {
				int cost = this.infra.getShortestPath(c.nodes.get(0), i).size();
				if(cost < 0) {
					cost = 0;
				}
				transportationCost += cost;
			}
			if(transportationCost < minTransportationCost) {
				minTransportationCost = transportationCost;
				minNode = i;
			}
		}
    	
    	depotNodes.add(minNode);
    	return depotNodes;
    }
	
	public int calculateCapacity(Cycle c) {
		int capacity_used = 0;
		for(Tuple t: c.itemPerCycle) {
			capacity_used += infra.sizeTelemetryItems[t.item];
		}
		capacity_used += c.nodes.size() -1;
		return capacity_used;
	}
	
	public void adaptToLinks() {
		for(Cycle c: this.cycles) {
			for(int i = 1; i < c.nodes.size(); i++) {
				int first = c.nodes.get(i-1);
				int second = c.nodes.get(i);
				Pair<Integer, Integer> p = new Pair<Integer, Integer>(first, second);
				c.links.add(p);
			}
		}
	}
	
	public void improvementMethod() throws CloneNotSupportedException {                                                                
		ArrayList<Cycle> already_checked_cycles = new ArrayList<Cycle>();              //lista que guarda os ciclos que j?? foram checados
		while(true) {			                                                       //loop principal que executa at?? ter checado todos os ciclos
			ArrayList<Cycle> cycles_aux = new ArrayList<Cycle>(this.cycles);                                      //altero ciclos sempre no cycles_aux, se eu perceber que n??o posso excluir um ciclo do cycle_aux eu s?? retorno para o loot, que ir?? utilizar a lista cycles que n??o havia sido alterada
			int value_least_used = Integer.MAX_VALUE;                                  //usado para saber qual foi o ciclo menos utilizado
			Cycle leastUsed = new Cycle();                                             //guarda o ciclo menos utilizado, n??o fa??o altera????es nesse ciclo
			Cycle leastUsed_aux = new Cycle();                                         //guarda o ciclo menos utilizado, pelos mesmos motivos do cycles_aux eu s?? altero essa vari??vel
			boolean find = false;                                                      //?? true se eu encontrei algum ciclo que ainda n??o foi checado e false caso contr??rio
			for(Cycle c : cycles_aux) {                                                //procura por ciclos com o m??nimo de uso
				if(!already_checked_cycles.contains(c)) {					
					if(c.capacity_used < value_least_used) {
						find = true;                                                   //encontrei ao menos um ciclo
						value_least_used = c.capacity_used;
						leastUsed = c.clone();
						leastUsed_aux = c;
					}
				}
			}
			if(find == false || leastUsed.capacity_used + 2 >= leastUsed.capacity) {     //s?? utiliza ciclos que tenham um capacidade n??o usada relevante(tem que achar algum valor que represente isso, por exemplo o tamanho do menor item, mas por enquanto deixa assim)
				break;                                                                  //como eu sei que esse ?? o menos utilizado, todos outros ciclos ter??o mais capacidade utilizada que esse, ent??o eu saio do loop principal, termiando o algoritmo
			}
			
			if(!leastUsed.itemPerCycle.isEmpty()) {                                     //se o ciclo foi usado para coletar algum item
				ArrayList<Tuple> removeTuples = new ArrayList<Tuple>();                 //guarda os items que eu retirei desse ciclo
				for(Tuple t : leastUsed.itemPerCycle) {                                 //itero sobre todos os itens do ciclo
					boolean found = false;                                              //true se eu encontrei um ciclo que passa pelo mesmo nodo e que tem capacidade para coletar o item, falso caso contr??rio 
					for(int i = 0; i < cycles_aux.size(); i++) {                        //itera sobre todos os outros ciclos
						if(cycles_aux.get(i).nodes.contains(t.device)) {				//se for encontrado um ciclo com capacidade sobrando que passe pelo mesmo nodo que o ciclo atual
							if(cycles_aux.get(i).capacity_used + this.infra.sizeTelemetryItems[t.item] <= cycles_aux.get(i).capacity && !cycles_aux.get(i).equals(leastUsed_aux)) {
								found = true;                                           //encontrei um ciclo que pode coletar o item
								removeTuples.add(t);                                    //adiciono esse item na lista de itens removidos do ciclo
								cycles_aux.get(i).itemPerCycle.add(t);                  //adiciono esse item no ciclo
								cycles_aux.get(i).capacity_used += this.infra.sizeTelemetryItems[t.item];   //adiciono na capacidade
								break;                                                  //n??o preciso mais procurar em outros ciclos
							}
						}
					}
					if(!found) {                                                        //procura por um ciclo que passe perto de um nodo utilizado pelo pathAtual, e o utiliza para coletar esse item
						boolean found_dijkstra = false;                                 //true se encontrou um ciclo que pode criar um caminho para "buscar" o item
						int min_cost = Integer.MAX_VALUE;                               //armazena qual o custo m??nimo para buscar o item
						int choosed_cycle = -1;                                         //armazena o ciclo escolhido
						int choosed_device_index = -1;                                  //armazena o nodo mais pr??ximo do item (ciclo [1,2,3,4,5] foi verificado que o menor caminho para buscar o item (6, 0) ?? [2,6,2], por exemplo)
						int choosed_device_next = -1;                                   //armazena qual o proximo nodo, depois do escolhido, para garantir que o ciclo continue de maneira correta
						for(int c = 0; c < cycles_aux.size(); c++) {                    //itera sobre todos os ciclos
							if(cycles_aux.get(c) == leastUsed_aux) {                    //ignora o ciclo least_used, pois ?? dele que est?? sendo retirado o item
								continue;
							}
							for(int i = 0; i < cycles_aux.get(c).nodes.size()-1; i++) { //itera sobre todos os nodos do ciclo
								int cost_dijkstra_ida = infra.getShortestPath(cycles_aux.get(c).nodes.get(i), t.device).size() - 1;   //calcula o tamanho do caminho de ida
								int cost_dijkstra_volta = infra.getShortestPath(t.device, cycles_aux.get(c).nodes.get(i)).size() - 1; //calcula o tamanho do caminho de volta(?? o mesmo do caminho de ida)
								if(cost_dijkstra_ida < 0) {
									cost_dijkstra_ida = 0;
								}
								if(cost_dijkstra_volta < 0) {
									cost_dijkstra_volta = 0;
								}
								int cost_node = cost_dijkstra_ida + cost_dijkstra_volta + infra.sizeTelemetryItems[t.item];           //calcula o caminho total para coletar o item partindo do nodo i
								if(cost_node + cycles_aux.get(c).capacity_used <= cycles_aux.get(c).capacity) {                       //se o ciclo "c" tiver capacidade suficiente para coletar esse item
									if(cost_node < min_cost) {                                                                        //se o custo para coletar o item for menor que eu encontrei 
										found_dijkstra = true;                                                                        //considero que encontrei pelo menos um ciclo que pode coletar o item
										min_cost = cost_node;                                                                         //atualizo o min_cost 
										choosed_cycle = c;                                                                            //atualizo o choosed_cycle
										choosed_device_index = i;                                                                     //atualizo o choosed_device
										choosed_device_next = i+1;                                                                    //atualizo o choosed_device_next
									}
								}
							}
						}
						if(!found_dijkstra) {							                 //se n??o foi encontrado um ciclo que possa coletar esse item
							already_checked_cycles.add(leastUsed);                   //coloco esse ciclo no already_checked, uma vez que n??o consegui coletar um dos itens dele
							continue;
						}else {                                                          //se encontrei um ciclo que pode coletar o item
							removeTuples.add(t);                                         //adiciono o item em quest??o na lista de tuplas a serem removidas
							cycles_aux.get(choosed_cycle).itemPerCycle.add(t);           //adiciono o item no ciclo escolhido
							cycles_aux.get(choosed_cycle).capacity_used += min_cost;     //adiciono o custo de coletar o item
							ArrayList<Integer> caminho_ida = infra.getShortestPath(cycles_aux.get(choosed_cycle).nodes.get(choosed_device_index), t.device);      //armazena o caminho de ida
							for(int j = 1; j < caminho_ida.size();j++) {                                                                                          //adiciona o caminho de ida na lista nodes do ciclo
								cycles_aux.get(choosed_cycle).nodes.add(choosed_device_index+j, caminho_ida.get(j));
							}
							ArrayList<Integer> caminho_volta = infra.getShortestPath(t.device, cycles_aux.get(choosed_cycle).nodes.get(choosed_device_index));    //armazena o caminho de volta
							for(int j = 1; j < caminho_ida.size();j++) {                                                                                          //adiciona o caminho de volta na lista nodes do ciclo
								cycles_aux.get(choosed_cycle).nodes.add(choosed_device_next+caminho_ida.size()-1, caminho_volta.get(j));
							}
						}
					}
				}
				
				for(Tuple t: removeTuples) {                                              //para cada item armazenado em removeTuples
					leastUsed.itemPerCycle.remove(t);                                     //remove o item do ciclo least_used
					leastUsed.capacity_used = this.calculateCapacity(leastUsed);          //recalcula a capacidade utilizada do ciclo least_used
				}
				cycles.remove(leastUsed_aux);                                         //atualiza os ciclos do sistema
				cycles.add((Cycle) leastUsed);
			}else {                                                                       //caso onde um circuito n??o coleta itens, apenas visita arestas
				boolean canDeleteCycle = true;                                            //se uma das arestas n??o puder ser removida, essa vari??vel ser?? falsa
				for(int i = 0; i < leastUsed.nodes.size()-1; i++) {                       //itera sobre todas as arestas do ciclo
					if(!canDeleteCycle) {                                                 //se eu n??o puder remover uma das arestas do ciclo, eu considero que n??o posso deletar esse ciclo da solu????o
						break;
					}
					int node = leastUsed.nodes.get(i);                                    //node armazena o nodo atual
					int prox_node = leastUsed.nodes.get(i+1);                             //prox_node armazena o nodo depois de node
					boolean found = false;                                                //found ser?? true se eu encontrar outro ciclo que passe pela mesma aresta, do contr??rio continuar?? false
					for(Cycle c: cycles_aux) {                                            //itero sobre todos os ciclos
						if(found) {                                                       //se encontrar um ciclo que passe pela mesma aresta n??o h?? mais necessidade de verificar outros ciclos
							break;
						}
						if(c.equals(leastUsed_aux)) {                                         //desconsidero o ciclo least_used, j?? que ?? dele que estamos tentando retirar a aresta
							continue;
						}
						for(int k = 0; k < c.nodes.size()-1; k++) {                       //itero sobre os nodos do ciclo
							if(c.nodes.get(k) == node && c.nodes.get(k+1) == prox_node) { //se o ciclo tiver essa aresta
								found = true;                                             //marco como encontrado
								break;                                                    //n??o preciso mais procurar nesse ciclo
							}
							if(c.nodes.get(k) == prox_node && c.nodes.get(k+1) == node) { //se o ciclo tiver a aresta ivertida
								found = true;                                             //marco como encontrado
								break;                                                    //n??o preciso mais procurar nesse ciclo
							}
						}
					}
					if(found) {                                                               //se j?? encontrei outro ciclo que passe pela mesma aresta
						canDeleteCycle = true;                                                //marco esse ciclo como um que pode ser deleta (para essa aresta, se alguma aresta marcar essa flag como false n??o tem como reverter e o ciclo n??o ser?? deletado)
					}else {                                                                   //se eu n??o encontro um ciclo que j?? passe por essa aresta, preciso fazer um ciclo passar por ela
						for(Cycle c: cycles_aux) {                                            //itero sobre todos os ciclos
							if(c.capacity - c.capacity_used <= 2 || c.equals(leastUsed_aux)){ //se o ciclo possui uma capacidade m??nima sobrando eu desconsidero
								canDeleteCycle = false;
								continue;
							}
							int c_prev_node = -1;                                         //c_prev_node armazena o node de onde vai partir o caminho que passar?? pela aresta
							int c_capacity_used = c.capacity_used;                        //c_capacity_used armazena a capacidade utilizada pelo ciclo
							for(int k = 0; k < c.nodes.size()-1; k++) {                   //itera sobre os nodes do ciclo
								if(c.nodes.get(k) == node) {                              //caso o ciclo passe pela exata aresta que est?? sendo procurada
									c_prev_node = c.nodes.get(k);                         //define esse nodo como ponto de partida
									break;                                                //para de procurar
								}
								if(c.nodes.get(k) == prox_node) {                         //se a aresta for (5,3), ele pode n??o achar o nodo 5, mas encontrar o nodo 3, que tamb??m serve para o mesmo prop??sito 
									c_prev_node = c.nodes.get(k);                         //define esse nodo como ponto de partida
									int aux = node;                                       //como a aresta (3,5) e (5,3) s??o a mesma para o prop??sito do algorimto, ele faz um swap dos valores de node e prox_node
									node = prox_node;
									prox_node = aux;
									break;                                                //para de procurar
								}
							}
							if(c_prev_node != -1) {                                                              // caso ele tenha encontrado o ponto inicial
								ArrayList<Integer> caminhoIda = infra.getShortestPath(node, prox_node);          //define o caminho de ida at?? o prox_node
								ArrayList<Integer> caminhoVolta = infra.getShortestPath(prox_node, node);        //define o caminho de volta do prox_node at?? o node
								for(int j = 1; j < caminhoVolta.size(); j++) {                                   //junta os dois caminhos
									caminhoIda.add(caminhoVolta.get(j));
								}
								int aditional_cost = caminhoIda.size() - 1;                                      //contabiliza ida e volta (agora caminhoIda tem todo o caminho, ida e volta)
								if(c_capacity_used + aditional_cost <= c.capacity) {                             //verifica se o ciclo possui a capacidade 
									int k = c.nodes.indexOf(node);                                               
									for(int l = 1; l < caminhoIda.size(); l++) {
										cycles_aux.get(cycles_aux.indexOf(c)).nodes.add(k+l, caminhoIda.get(l)); //conta louca que funciona pra colocar o caminho gerado na posi????o correta dentro da lista nodes do ciclo
									}
									c.capacity_used += aditional_cost;                                           //adiciona o custo no ciclo
									canDeleteCycle = true;                                                       //ainda posso deletar esse ciclo
									break;                                                                       //n??o preciso continuar verificando em outros ciclos
								}else {                                                                          //caso n??o possua a capacidade sobrando
									continue;                                                                    //continuo procurando em outros ciclos
								}
							}else {                                                                              //caso n??o tenha um ciclo que passe por um dos nodos da aresta
								canDeleteCycle = false;                                                          //n??o implementei nada para criar esse caminho, pode ser algo que melhore o algoritmo
								continue;                                                                        //simplesmente digo que n??o posso deletar esse ciclo, o continue levar?? para um "if" que vai parar a execu????o
								 
							}
						}
					}
				}
				if(canDeleteCycle) {                                    //se eu consegui atrelar todas as arestas do leastUsed em outros ciclos
					cycles_aux.remove(leastUsed_aux);                       //removo o leastUsed do cycles_aux
					cycles = cycles_aux;
				}else {
					already_checked_cycles.add(leastUsed_aux);          //adiciono o leastUsed_aux (leastUsed sem nenhuma modifica????o) na lista de ciclos j?? checados
					continue;                                           //n??o atualizo o cycles, pois o cycles_aux possui altera????es que n??o foram feitas de verdade, apenas passo para a pr??xima
					
				}
			}
		}
	}

	private boolean verifyCycle(Cycle c, ArrayList<Cycle> already_checked_cycles) {
		boolean different = true;
		for(Cycle checked: already_checked_cycles) {
			if(!different) {
				break;
			}
			if(c.capacity_used != checked.capacity_used) {
				continue;
			}else {
				if(c.itemPerCycle.isEmpty() && checked.itemPerCycle.isEmpty()) {
					if(!c.nodes.equals(checked.nodes)) {
						different = false;
						break;
					}
				}else {
					//verifico os itens
					if(c.itemPerCycle.size() != checked.itemPerCycle.size()) {
						continue;
					}else {
						for(int i = 0; i < c.itemPerCycle.size(); i++) {
							if(c.itemPerCycle.get(i).device == checked.itemPerCycle.get(i).device) {
								if(c.itemPerCycle.get(i).item == checked.itemPerCycle.get(i).item) {
									different = false;
								}else {
									break;
								}
							}else {
								break;
							}
						}
					}
				}
			}
		}
		return different;
	}
}
