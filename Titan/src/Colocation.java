import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cassandra.cli.CliParser.newColumnFamily_return;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class Colocation {
	
	public static Database db;
	public static TitanGraph graph;
	public static HashMap<String, Long> total_count; 
	public static double PI_threshold;
	public static boolean verbose;
	public static ConcurrentHashMap<Integer,ConcurrentHashMap<String,ConcurrentHashMap<String, Double>>> colocations;
	
	public Colocation(){
		this.db = new Database();
		this.graph = this.db.connect();
		this.total_count = new HashMap<String, Long>();
		this.PI_threshold = 0.2;
		this.verbose = false;
		this.colocations = new ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>>();
	}	
	
	public static void print_Frequent(HashMap<String, HashMap<String, Double>> Lk, int k){
		
		System.out.println("Frequent Colocations of Size "+k+" with their participation index");

		Iterator it,it1;
		it = Lk.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry pair = (Map.Entry)it.next();
			String type1 = (String) pair.getKey();
			it1 = ((HashMap<String, Double>) pair.getValue()).entrySet().iterator();
			
			while(it1.hasNext()){
				
				Map.Entry pair1 = (Map.Entry)it1.next();
				String type2 = (String) pair1.getKey();
				double pi = (Double) pair1.getValue();
				
				System.out.println(type1+":"+type2+" = "+pi);
			}
		}
		
	}
	
	public static void print_Candidate(HashSet<List<String>> Ck, int k){
		
		System.out.println("Candidate Colocations of Size "+k);
		Iterator it,it1;
		it = Ck.iterator();
		while(it.hasNext()){
			List<String> tempList = ((List<String>)it.next());
			String candidate = "";
			for(int i= 0;i<tempList.size();i++){
				candidate = candidate+tempList.get(i)+":";				
			}
			candidate = candidate.substring(0, candidate.length()-1);
			System.out.println(candidate);
		}		
	}
	
	public static HashSet<List<String>> join_and_prune(HashMap<String, HashMap<String, Double>> Lk, int k){
		
		long time1 = System.currentTimeMillis();
		System.out.println("Candidate Colocations of Size "+(k+1));
		HashSet<List<String>> Ckplus1 = new HashSet<List<String>>();
		
		Iterator it,it1,it2;
		it = Lk.entrySet().iterator();
		while(it.hasNext()){
			List<String> items = new ArrayList<String>();
			String[] itemskplus1 = new String[k+1];
			
			Map.Entry pair = (Map.Entry)it.next();
			String type1 = (String) pair.getKey();
//			System.out.println("Type = "+type1);
			
			for(int i =0; i<k-1; i++){
				itemskplus1[i] = type1.split(":")[i];
//				System.out.println(type1.split(":")[i]);
			}
			
			it1 = ((HashMap<String, Double>) pair.getValue()).entrySet().iterator();
			
			while(it1.hasNext()){
				
				Map.Entry pair1 = (Map.Entry)it1.next();
				String type2 = (String) pair1.getKey();
				items.add(type2);
//				System.out.println(type2);
			}
			
			for(int i=0;i<items.size()-1;i++){
				
				for(int j=i+1; j<items.size(); j++){
					if(Integer.parseInt(items.get(i)) > Integer.parseInt(items.get(j))){
						itemskplus1[k-1] = items.get(j);
						itemskplus1[k] = items.get(i);
					}
					else{
						itemskplus1[k-1] = items.get(i);
						itemskplus1[k] = items.get(j);
					}
					
//					for(int z = 0; z < k+1;z++){
//						System.out.print(itemskplus1[z]+",");
//					}
//					System.out.println("");
					
					
					boolean flag = true;
					for(int x = 0 ; x < k+1;x++){
						
						int counter = 0;
						String[] itemsk = new String[k];
						
						for(int y = 0; y < k+1;y++){
							if(y==x){
								continue;
							}
							else {
								itemsk[counter] = itemskplus1[y];
								counter++;
							}
						}
						
						String key = "", value = "";
						int y;
						for(y = 0 ; y < k-1;y++){
							key = key+itemsk[y]+",";
						}
						value = itemsk[y];
						key = key.substring(0, key.length()-1);
//						System.out.println(key+"---"+value);
					
						if(Lk.containsKey(key)){
							HashMap<String, Double> temp = Lk.get(key);
							if(temp.containsKey(value)){
								continue;
							}else{
								flag = false;
								break;
							}
						}
						else{
							flag = false;
							break;
						}						
					
					}
					
					if(flag){
						List<String> temp_List = new ArrayList<String>();
						for(int x = 0; x<k+1;x++){
							temp_List.add(itemskplus1[x]);
						}
						Ckplus1.add(temp_List);
					}					
				}
			}
			
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Total time required for joining and pruning for candidate colocations of size "+(k+1)+" is "+(time2-time1));
		return Ckplus1;
	}	
	
	public static void L1(){
		/*
		 * Generate colocations of size 1
		 * Iterate over all vertices of the graph
		 */
		System.out.println("Generating colocations of size 1.\n");
		
		long time1 = System.currentTimeMillis();
		if(verbose){
			System.out.println("Generating colocations of size 1.\n");
		}
		int counter = 0;
	    //could lead to buffer-overflow

		for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator.hasNext();) {
				Vertex vertex = iterator.next();
				String type = vertex.getProperty("type");
				if (total_count.containsKey(type))
				{
					total_count.put(type, total_count.get(type)+1);
				}
				else {
					total_count.put(type, (long) 1);
					
				}
				if(verbose){
					System.out.println(counter+" : "+"Id = "+vertex.getId()+" Place = "+vertex.getProperty("place")+" Type = "+vertex.getProperty("type")+" Visible = "+vertex.getProperty("visible"));
				}
				counter++;
			}
		if(verbose){
			System.out.println("Total number of colocations of size 1 = "+counter+"\n");
		}

		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size 1 : "+(time2-time1));
	}
	
	public static HashMap<String, HashMap<String, Double>> L2(){
//		HashSet<List<String>> C2
		
		/*
		 * Generate colocations of size 2
		 * Iterate over all edges of the graph
		 */
//		ConcurrentHashMap<String, ConcurrentHashMap<String,Double>> freq_C2 = new ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>();
				
		//global_count 
		HashMap<String, HashMap<Long, Boolean>> global_count = new HashMap<String, HashMap<Long, Boolean>>();
		HashMap<String, HashSet<String>> temp_C3 = new HashMap<String, HashSet<String>>();
		
		HashMap<String, HashMap<String, Double>> L2 = new HashMap<String, HashMap<String,Double>>();
		
		long time1 = System.currentTimeMillis();
		
		if(verbose){
			System.out.println("Generating colocations of size 2.\n");
		
		}
		int counter = 0;
		//could lead to buffer-overflow
		
		for (Iterator<Edge> iterator = graph.getEdges().iterator(); iterator.hasNext();) {
			Edge edge = iterator.next();
			if (verbose){
				System.out.println(counter+" : "+" Edge Label = "+edge.getLabel()+" Distance = "+edge.getProperty("distance"));
			}
			Vertex vertex1 = edge.getVertex(Direction.IN);
			String type1 = vertex1.getProperty("type");
			Long Id1 = (Long) vertex1.getId();
			Vertex vertex2 = edge.getVertex(Direction.OUT);
			String type2 = vertex2.getProperty("type");
			Long Id2 = (Long) vertex2.getId();
			//could lead to buffer-overflow	
			
			if(type1.equals(type2))
			{
				continue;
			}
			
			if(global_count.containsKey(type1+":"+type2)==false){
				HashMap<Long, Boolean> default_hashmap = new HashMap<Long, Boolean>();
				global_count.put(type1+":"+type2, default_hashmap);
			}
			if(global_count.containsKey(type2+":"+type1)==false){
				HashMap<Long, Boolean> default_hashmap = new HashMap<Long, Boolean>();
				global_count.put(type2+":"+type1, default_hashmap);
			}
			if(global_count.get(type1+":"+type2).containsKey(Id1) == false ){
				global_count.get(type1+":"+type2).put(Id1, true);
			}
			if(global_count.get(type2+":"+type1).containsKey(Id2) == false ){
				global_count.get(type2+":"+type1).put(Id2, true);
			}
			
			if(verbose){
				System.out.println("Type1 : "+type1+" Type2 : "+type2+" ID1 : "+Id1+" ID2 : "+Id2);
			}
		}
		
		Iterator it1 = global_count.entrySet().iterator();
		
		while(it1.hasNext()){
			
			Map.Entry pair1 = (Map.Entry)it1.next();
			String type = (String) pair1.getKey();
			String type1 = type.split(":")[0];
			String type2 = type.split(":")[1];
			
			if(Integer.parseInt(type1)< Integer.parseInt(type2)){
				Double x1 = (double) global_count.get(type1+":"+type2).size();
				Double x2 = (double) global_count.get(type2+":"+type1).size();
				Double a = x1/total_count.get(type1);
				Double b = x2/total_count.get(type2);
				Double PI = java.lang.Math.min(a, b);
				if (verbose)
				{
					System.out.println(type1);
					System.out.println(type2);
					System.out.println(x1+" / "+total_count.get(type1)+" "+type1);
					System.out.println(x2+" / "+total_count.get(type2)+" "+type2);
					System.out.println(PI);
					System.out.println("\n\n");
				}
				if(PI >= PI_threshold)
				{
					if(verbose){
						System.out.println(type1+":"+type2);
						System.out.println(PI);
						System.out.println("\n");
					}
					counter+=1;
					
					if(L2.containsKey(type1)==false){
						HashMap<String, Double> tempHashMap = new HashMap<String, Double>();
						tempHashMap.put(type2, PI);
						L2.put(type1, tempHashMap);
					}
					else{
						L2.get(type1).put(type2, PI);
					}
					
//					if(temp_C3.containsKey(type1)==false){
//						HashSet<String> tempset = new HashSet<String>();
//						tempset.add(type2);
//						temp_C3.put(type1, tempset);
//					}
//					else{
//						temp_C3.get(type1).add(type2);
//					}
					
				}
			}
			
		}
		
		if (verbose){
			System.out.println("Total no. of colocations of size 2  are = "+counter);
		}
		
		/*
		HashSet<List<String>> C3 = new HashSet<List<String>>();
		
//		HashMap<String, HashSet<String>> temp_C3 = new HashMap<String, HashSet<String>>();
		
		it1 = temp_C3.entrySet().iterator();
		Iterator it2, it3;
		
		String string1, string2, string3;
		
		it1 = temp_C3.entrySet().iterator();
		while(it1.hasNext()){
			Map.Entry pair1 = (Map.Entry)it1.next();
			string1 = (String) pair1.getKey(); 
			it2 = ((Iterable) pair1.getValue()).iterator();
			while(it2.hasNext()){
				string2 = (String) it2.next();
				
				if (Integer.parseInt(string1) >= Integer.parseInt(string2)  )
				{
					continue;
				}
				it3 = temp_C3.get(string2).iterator();
				while(it3.hasNext()){
					string3 = (String) it3.next();
					if (Integer.parseInt(string2) >= Integer.parseInt(string3)  )
					{
						continue;
					}
					if(temp_C3.get(string1).contains(string3))
					{
						if (verbose){
							System.out.println("\n");
							System.out.println(string1);
							System.out.println(string2);
							System.out.println(string3);
							System.out.println("\n");
						}
						List<String> temp_list = new ArrayList();
						temp_list.add(string1);
						temp_list.add(string2);
						temp_list.add(string3);
						C3.add(temp_list);
					}
				}
			}
		}
		*/
		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size 2 : "+(time2-time1));
//		return C3;
		return L2;
		
	}
	
//	public static void L3(){
//		/*
//		 * Generate colocation of size 3
//		 */
//		HashSet<List<String>> C3 = L2();	
//		Iterator iterator = C3.iterator();
//		while(iterator.hasNext())
//			System.out.println(iterator.next());
//		
//	}
	
	public static void main(String[] args) {
		Colocation colocation = new Colocation();
		// Total count of all size-1 colocations
		L1();
		
		// Frequent Colocations of size 2
		HashMap<String, HashMap<String, Double>> L2 = L2();
		print_Frequent(L2, 2);
		
		// Candidate Colocations of size 3
		HashSet<List<String>> C3 = join_and_prune(L2, 2);
		print_Candidate(C3, 3);
		
		db.close(graph);
		
	}
}
