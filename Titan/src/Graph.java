import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import com.sun.corba.se.impl.orbutil.ObjectWriter;
import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.attribute.Decimal;
import com.thinkaurelius.titan.core.attribute.Geo;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.core.util.TitanCleanup;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Query.Compare;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

@SuppressWarnings("deprecation")
public class Graph {
	
	public static Map<String, Integer> countMap;
	public static Map<String,Integer> typeFrequency;
	public static Map<String, Double> edgesMap;
	public static HashMap<String, String> enCoding;
	public static HashMap<String, String> deCoding;
		
	public static TitanGraph clearGraph(Database db, TitanGraph graph){
		/**
		 * Delete all the vertices and edges from graph database.
		 * This closes the connection to graph database as well and reopens a database connection and return graph instance
		 */
	
		db.closeTitanGraph(graph);
		TitanCleanup.clear(graph);
		System.out.println("Cleared the graph");
		
		TitanGraph clear_graph = db.getTitanGraph();
		return clear_graph;
	}
	
	public static void build_schema(TitanGraph graph){
		
		long time_1 = System.currentTimeMillis();
		TitanManagement mgmt = graph.getManagementSystem();
		
		PropertyKey typeKey = mgmt.makePropertyKey("type").dataType(String.class).make();
		PropertyKey placeKey = mgmt.makePropertyKey("place").dataType(Geoshape.class).make();
//		PropertyKey placeKeyTSI = mgmt.makePropertyKey("place").dataType(Geoshape.class).make();
		PropertyKey timeKey = mgmt.makePropertyKey("time").dataType(Long.class).make();
		PropertyKey distanceKey = mgmt.makePropertyKey("distance").dataType(Decimal.class).make();
		PropertyKey visibleKey = mgmt.makePropertyKey("visible").dataType(Integer.class).make();
//		PropertyKey edgetypeKey = mgmt.make
		PropertyKey instanceid = mgmt.makePropertyKey("instanceid").dataType(Long.class).make();
		// Making all possible edge labels
		Iterator iterator = enCoding.entrySet().iterator();
		while(iterator.hasNext()){
			Entry x = (Entry) iterator.next();
			String type1 = (String) x.getValue();
			Iterator iterator2 = enCoding.entrySet().iterator();
			while(iterator2.hasNext()){
				Entry y = (Entry)iterator2.next();
				String type2 = (String) y.getValue();
				EdgeLabel label = mgmt.makeEdgeLabel(type1+"-"+type2).multiplicity(Multiplicity.MULTI).make();
				mgmt.buildEdgeIndex(label, type1+"-"+type2, Direction.BOTH,distanceKey);
			}			
		}		
		
		mgmt.buildIndex("type", Vertex.class).addKey(typeKey).buildCompositeIndex();
//		mgmt.buildIndex("type", Vertex.class).addKey(typeKey).buildMixedIndex("search");
//		mgmt.buildIndex("place", Vertex.class).addKey(placeKeyTSI).buildCompositeIndex();
//		mgmt.buildIndex("place", Vertex.class).addKey(placeKey).buildMixedIndex("search");
//		mgmt.buildIndex("node",Vertex.class).addKey(typeKey).addKey(placeKey).addKey(timeKey).addKey(instanceid).buildMixedIndex("search");
		mgmt.buildIndex("node",Vertex.class).addKey(typeKey).addKey(placeKey).addKey(timeKey).buildMixedIndex("search");
//		mgmt.buildIndex("time",Vertex.class).addKey(timeKey).buildMixedIndex("search");
		mgmt.buildIndex("distance", Edge.class).addKey(distanceKey).buildMixedIndex("search");
//		mgmt.buildIndex("distance", Vertex.class).addKey(distanceKey).buildMixedIndex("search");
		mgmt.buildIndex("visible", Vertex.class).addKey(visibleKey).buildCompositeIndex();
		mgmt.commit();
		long time_2 = System.currentTimeMillis();
		
		System.out.println("Schema built in "+(time_2-time_1)+" ms.");
	}
	
	@SuppressWarnings("unused")
	public static void InitializeGraph(TitanGraph graph, int limit) throws Exception
	{
		System.out.println("InitializeGraph method called.\n");
		int START = 5393857;
//		int START = 0;
		int MAX_LIMIT = limit;
		
		Statement stmt;
		
		Connection connection  = (Connection) new MySql().getConnection();
		
//		MySql mySql = new MySql();
//		Connection conn = (Connection) mySql.connect();
		
		if (connection == null) {
			System.out.println("Connection Error!!");
		}else{
//			System.out.println("Creating statement...");
		      
			stmt = (Statement) connection.createStatement();

//		      String sql = "SELECT * FROM dataset WHERE primary_type = \""+typeString+"\" ORDER BY date ASC LIMIT "+START+","+MAX_LIMIT;
			  String sql = "SELECT * FROM dataset ORDER BY date ASC LIMIT "+START+","+MAX_LIMIT;
//		      System.out.println(sql);
		      ResultSet rs = stmt.executeQuery(sql);
//		      System.out.println("query printed");
		      long id;
	 		  double latitude, longitude, time = 0;
	 		  String type = null;
	 		  long time_1 = System.currentTimeMillis();
	 		  int count = 0;
	 		  long time_3 = System.currentTimeMillis();
	 		  long time_4;
	 		  long tym;
	 		  TitanTransaction graph1 = graph.newTransaction();
	 		  
		      while(rs.next()){

		    	 id  = rs.getInt("id");
		         latitude = rs.getDouble("latitude");
		         longitude = rs.getDouble("longitude");
		         type = rs.getString("primary_type");
		         tym = rs.getInt("date");
//		         System.out.println(id+" "+latitude+" "+longitude+" "+type);
		         if(latitude==0.0 || longitude==0.0)
		        	 continue;
		         Geoshape place = Geoshape.point(latitude, longitude);
	 		     Geoshape approxplace = Geoshape.point((double)Math.round(latitude*100)/100, (double)Math.round(longitude*100)/100);  
	 		     Vertex node = graph1.addVertex(id);
	 		     // Note here that new type i.e, encoded type is added instead of original type
	 		     node.setProperty("type", enCoding.get(type));
	 		     node.setProperty("place", place);
	 		     node.setProperty("visible", 1);
	 		     node.setProperty("time", tym);
	 		     node.setProperty("instanceid", id);
//	 		     node.setProperty("distance", 2.345);
	 		     count++;
//	 		     node.getProperty("place");
	 		     
	 		     if(count%200000 == 0)
	 		     {
	 		    	 graph1.commit();
	 		    	 time_4 = System.currentTimeMillis();
	 		    	 System.out.println("Total vertices added till now = "+count+" in "+(time_4-time_3)+" ms.");
	 		    	 time_3 = time_4;
	 		    	 
	 		    	 graph1 = graph.newTransaction();
	 		     }
		      }                                   
		     graph1.commit();
		    long time_2 = System.currentTimeMillis();
	 		time += time_2-time_1; 
	 		System.out.println("Total vertices added till now = "+count+" in "+(time_2-time_1)+" ms.");
		    rs.close();
		    
		    try{
		         if(stmt!=null)
		            connection.close();
		      }catch(SQLException se){
		      }// do nothing
		      try{
		         if(connection!=null)
		            connection.close();
		      }catch(SQLException se){
		         se.printStackTrace();
		      }
		}
	}
	
	public static void stats(TitanGraph graph){
		/*
		 * 1. Distribution of total instances across different crime types.
		 */
		
		Map<String, Integer> typeMap = new HashMap<String, Integer>();
		int counter_type = 0;
		
		for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator
				.hasNext();) {
			Vertex vertex = iterator.next();
			String value = vertex.getProperty("type");
			if(!typeMap.containsKey(value)){
				typeMap.put(value, 1);
				counter_type++;
			}else{
				int temp = typeMap.get(value);
				typeMap.put(value, ++temp);
			}			
		}
		
		System.out.println("Distribution of instances across "+counter_type+" different types : ");
		
		Iterator it = typeMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	    }
	    Graph.typeFrequency = typeMap;
	    
	}
	
	public static double addEdges(TitanGraph graph, double distance) {
		/*
		 * Method to compute vertices in the nearby region as specified by 'distance' argument and add edges between them. 
		 */
		
		int counter = 0;
		double time_1 = System.currentTimeMillis();
		for (Iterator<Vertex> iterator = graph.query().vertices().iterator(); iterator.hasNext();) {
			Vertex vertex = iterator.next();
			vertex.setProperty("visible", 0);
			Geoshape pointGeoshape = vertex.getProperty("place");
			double latitude = pointGeoshape.getPoint().getLatitude();
			double longitude = pointGeoshape.getPoint().getLongitude();
			// counter1 variable stores total no. of vertices satisfying Geo.WITHIN or its no. of edges
			int counter1 = 0;
			String type1 = vertex.getProperty("type");
			for (Iterator <Vertex> iterator2 = graph.query().has("visible",Compare.EQUAL,1).has("place", Geo.WITHIN, Geoshape.circle(latitude, longitude, distance)).has("type",Compare.NOT_EQUAL, type1).vertices().iterator(); iterator2.hasNext();) 
			{
				Vertex vertex2 = iterator2.next();
				Geoshape pointGeoshape2 = vertex2.getProperty("place");
							
				String type2 = vertex2.getProperty("type");
				String edgeLabel = "";
				
				if(Integer.parseInt(type1) < Integer.parseInt(type2)){
					edgeLabel = type1+"-"+type2;					
				}else{
					edgeLabel = type2+"-"+type1;
				}
				
				Edge edge = vertex.addEdge(edgeLabel, vertex2);
				edge.setProperty("distance",pointGeoshape.getPoint().distance(pointGeoshape2.getPoint()));
				
				
				/*
				System.out.println(edge.getProperty("distance"));
				double latitude2 = pointGeoshape2.getPoint().getLatitude();
				double longitude2 = pointGeoshape2.getPoint().getLongitude();
				System.out.println(latitude);
				System.out.println(longitude);
				System.out.println(latitude2);
				System.out.println(longitude2);
				System.out.println("\n");
				*/
				counter1++;
			}
			counter += counter1;			
		}
		double time_2 = System.currentTimeMillis();
		System.out.println("Total no. of edges added are = "+counter+" in "+(time_2-time_1)+ "ms.\n");
		return (time_2-time_1);
	}
	
	@SuppressWarnings("unchecked")
	public static double addEdges(TitanGraph graph, String type, double distance) {
		/*
		 * Method to compute vertices in the nearby region as specified by 'distance' argument with respect to vertices of type 
		 * specified by 'type' argument and add edges between them. 
		 */
		
		int counter = 0;
		double time_1 = System.currentTimeMillis();
		for (Iterator<Vertex> iterator = graph.query().has("type",Compare.EQUAL, type).vertices().iterator(); iterator
				.hasNext();) {
			Vertex vertex = iterator.next();
			vertex.setProperty("visible", 0);
			Geoshape pointGeoshape = vertex.getProperty("place");
			double latitude = pointGeoshape.getPoint().getLatitude();
			double longitude = pointGeoshape.getPoint().getLongitude();
			/*
			System.out.println(counter+" : "+"Id = "+vertex.getId()+" Place = "+vertex.getProperty("place")+" Type = "+
								vertex.getProperty("type")+" Latitude = "+latitude+ " Longitude = "+longitude+"\n");
			
			System.out.println("Vertices in "+distance+" km of locality are : ");
			*/
			// counter1 variable stores total no. of vertices satisfying Geo.WITHIN or its no. of edges
			int counter1 = 0;
			
			for (Iterator <Vertex> iterator2 = graph.query().has("type",Compare.NOT_EQUAL, type).has("visible",Compare.EQUAL,1).has("place", Geo.WITHIN, Geoshape.circle(latitude, longitude, distance)).vertices().iterator();
					iterator2.hasNext();) {
				Vertex vertex2 = iterator2.next();
//				System.out.println(counter+" : "+"Id = "+vertex2.getId()+" Place = "+vertex2.getProperty("place")+" Type = "+vertex2.getProperty("type"));
				
				//Get other point
				Geoshape pointGeoshape2 = vertex2.getProperty("place");
//				String labelString = vertex2.getProperty("type")+"-"+type;
				
//				Add edge between instances of two different types with label as type1-type2 eg:BATTERY-NARCOTICS only if NARCOTICS-BATTERY edge is not present
//			    vertex.query().has("id", vertex2.getId()).direction(Direction.BOTH).has(labelString, vertex2).vertices();
				Edge edge = vertex.addEdge(type+"-"+vertex2.getProperty("type"), vertex2);
					
//				Set property for edge as distance between two vertices
				edge.setProperty("distance",pointGeoshape.getPoint().distance(pointGeoshape2.getPoint()));
				System.out.println(edge.getProperty("distance"));
				System.out.println("shagun");
				counter1++;
			}
//			System.out.println("Vertices in nearby locality are : "+counter1+"\n");
//			System.out.println("No. of edges added are = "+counter1);			
			counter += counter1;			
		}
		double time_2 = System.currentTimeMillis();
		System.out.println("Total no. of edges added for type = "+type+" are = "+counter+" in "+(time_2-time_1)+ "ms.\n");
		return (time_2-time_1);
	}
	
	public static void addEdgesMultiThread(TitanGraph graph, double distance1, double distance2) throws InterruptedException{
		
		double time1 = System.currentTimeMillis();	
		Iterator<Vertex> iterator = graph.getVertices().iterator();
		List<Long> ids = new ArrayList<Long>();
		while(iterator.hasNext()){
			Vertex vertex = (Vertex) iterator.next();
			ids.add((Long) vertex.getId());
		}
		long time_3 = System.currentTimeMillis();
		long time_4;
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		int i = 0;
		TitanTransaction graph1 = graph.newTransaction();
		
		for(;i < ids.size();i++){
			
//			Vertex vertex = iterator.next();
//			TitanTransaction graph2 = graph.newTransaction();
			EdgeInsertion edgeInsertion = new EdgeInsertion(graph1,ids.get(i),distance1, distance2);
//			System.out.println(i);
			executorService.execute(edgeInsertion);
			
			if((i+1)%1001 == 0){
				executorService.shutdown();
				while(!executorService.isTerminated()){
					;
				}
				
//				System.out.println("All the threads terminated successfully");
				
				graph1.commit();
				time_4 = System.currentTimeMillis();
				System.out.println(i+" : Time required is = "+(time_4-time_3));
				time_3 = time_4;
				executorService = Executors.newFixedThreadPool(100);
				graph1 = graph.newTransaction();
				
			}
		
		}
		executorService.shutdown();
//		executorService.awaitTermination(120, TimeUnit.SECONDS);
		while(!executorService.isTerminated()){
			;
		}
		System.out.println("All the threads terminated successfully");
		graph1.commit();
		/*
		Iterator it = edgesMap.entrySet().iterator();
		int count = 0;
		
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        String key = (String) pairs.getKey();
	        double distance1 = (double) pairs.getValue();
	        count++;
	        
	        String[] vertex_ids = key.split("-");
	        Vertex vertex1 = graph1.getVertex(vertex_ids[0]);
	        Vertex vertex2 = graph1.getVertex(vertex_ids[1]);
	        vertex1.addEdge("knows", vertex2);
	        
//	        System.out.println("Edge added between = "+vertex_ids[0]+" and "+vertex_ids[1]);
	    }*/
		
		double time2 = System.currentTimeMillis();	
//		System.out.println("Total time required = "+(time2-time1)+ " for "+count+" edges");
		System.out.println("Total time required = "+(time2-time1));
	}
		
	public static void iterateVertices(TitanGraph graph){
		/*
		 * Iterates over all vertices of a graph and displays total no. of vertices.
		 */
		
		System.out.println("iterateVertices function called.\n");
		int counter = 0;
        	
		for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator
				.hasNext();) {
			Vertex vertex = iterator.next();
//			System.out.println(counter+" : "+"Id = "+vertex.getId()+" Place = "+vertex.getProperty("place")+" Type = "+vertex.getProperty("type")+" Visible = "+vertex.getProperty("visible"));
			counter++;
		}
		System.out.println("Total Vertices = "+counter+"\n");
	}

	public static void iterateEdges(TitanGraph graph) {
		/*
		 * Iterates over all edges of a graph and displays total no. of edges
		 */
		HashMap<String, Integer> count_edges_distribution = new HashMap<String, Integer>();
		System.out.println("iterateEdges function called.\n");
		int counter = 0;
		int temp_count = 0;
		for (Iterator<Edge> iterator = graph.getEdges().iterator(); iterator.hasNext();) {
			Edge edge = iterator.next();
//			Vertex v1 = edge.getVertex(Direction.IN);
//			Vertex v2 = edge.getVertex(Direction.OUT);
//			
//			if(Integer.parseInt((String)v1.getProperty("type")) < Integer.parseInt((String)v2.getProperty("type"))){
//				temp_count ++;
//			}
//			
//			if(count_edges_distribution.get(edge.getLabel()) == null){
//				count_edges_distribution.put(edge.getLabel(), 1);
//			}else{
//				int temp = count_edges_distribution.get(edge.getLabel());
//				count_edges_distribution.put(edge.getLabel(), ++temp);
//			}
			counter++;
		}
		System.out.println("Total no. of edges are = "+counter+ " and temp_count "+temp_count);
//		System.out.println("Distribution of Edges = ");
//		Iterator<String> it = count_edges_distribution.keySet().iterator();
//		while(it.hasNext()){
//			String label = it.next();
//			System.out.println(label+" = "+count_edges_distribution.get(label));
//		}
	}

	public static void removeVertices(TitanGraph graph, String type) {
		/*
		 * Removes vertices of a particular type and also edges incident on it.
		 */
		for (Iterator<Vertex> iterator = graph.query().has("type", Compare.EQUAL, type).vertices().iterator();iterator.hasNext();) {
			Vertex vertex = iterator.next();
			for (Iterator<Edge> iterator2 = vertex.query().edges().iterator();iterator2.hasNext();){
				Edge edge = iterator2.next();
				edge.remove();
			}
			vertex.remove();
		}
		System.out.println("Vertices of type = "+type+" removed.");
	}

	public static void removeEdges(TitanGraph graph){
		/*
		 * Removes all edges of the form type1-type2 and type2-type1
		 */
		System.out.println("removeEdges function called.\n");
		int counter = 0;
					
			for (Iterator<Edge> iterator = graph.getEdges().iterator(); iterator.hasNext();) {
				Edge edge = iterator.next();
//				edge.getVertex(Direction.IN).setProperty("visible", 1);
//				edge.getVertex(Direction.OUT).setProperty("visible", 1);
				edge.remove();
				counter++;
			}
			/*
			for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator.hasNext();) {
				Vertex vertex = iterator.next();
				vertex.setProperty("visible", 1);
			}
			*/
				
		System.out.println("Total no. of edges removed were = "+counter);		
	}
	
	public static void removeEdges(TitanTransaction graph, Long instanceid1, Long instanceid2) {
		
		Vertex vertex1 = (Vertex) graph.query().has("instanceid", com.tinkerpop.blueprints.Compare.EQUAL, instanceid1).vertices().iterator().next();
		Vertex vertex2 = (Vertex) graph.query().has("instanceid", com.tinkerpop.blueprints.Compare.EQUAL, instanceid2).vertices().iterator().next();
		String type1 = vertex1.getProperty("type");
		String type2 = vertex2.getProperty("type");
		if (Integer.parseInt(type1) < Integer.parseInt(type2)){
//			System.out.println(type1+":"+type2);
//			System.out.println(instanceid1+":"+instanceid2);
			Iterator<Edge> it = vertex1.query().edges().iterator();
//			Iterator<Edge> it = vertex1.getEdges(Direction.IN, type1+"-"+type2).iterator();
			while(it.hasNext()){
				Edge edge = it.next();
				Vertex vertex3 = edge.getVertex(Direction.OUT);
//				System.out.println(""+vertex3.getProperty("type"));
				if ((long)vertex3.getProperty("instanceid") == instanceid2){
					edge.remove();
					System.out.println("Edge removed");
				}
			}
		}else{
//			System.out.println(type2+":"+type1);
//			System.out.println(instanceid2+":"+instanceid1);
			Iterator<Edge> it = vertex2.query().edges().iterator();
//			Iterator<Edge> it = vertex2.getEdges(Direction.IN, type2+"-"+type1).iterator();
			while(it.hasNext()){
				Edge edge = it.next();
				Vertex vertex3 = edge.getVertex(Direction.OUT);
//				System.out.println(""+vertex3.getProperty("type"));
				if ((long)vertex3.getProperty("instanceid") == instanceid1){
					edge.remove();
					System.out.println("Edge removed");
				}
			}
		}
		System.out.println("----------");
	}

	public static TitanGraph exp1(Database db, TitanGraph graph, double distance) throws Exception{
		/*
		 * Need to return Titangraph instance because clearGraph functions clears the graph and returns new instance
		 * on which further processing needs to be done.
		 */
//		graph = Graph.clearGraph(db, graph);
//		Graph.InitializeGraph(graph);
		
		
//		Map<String, Integer> typeMap = Socrata.statistics(graph);
//		Graph.iterateVertices(graph);
//		Graph.removeVertices(graph, "PUBLIC INDECENCY");
//		Graph.removeVertices(graph, "NON - CRIMINAL");
		
//		Graph.iterateVertices(graph);
//		Graph.iterateEdges(graph);
//		Graph.removeEdges(graph);
		
		Graph.addEdges(graph, distance);
		/*
		double timeGeo = 0,timeEdge = 0;
		int counter = 0;
		Iterator it = typeMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();k
	        System.out.println(counter+" : Exploring Neighbours for type = " + pairs.getKey());
	        timeGeo += Graph.exploreNeighboursGeo(graph, (String) pairs.getKey(), distance);
	        timeEdge += Graph.exploreNeighboursEdge(graph, (String) pairs.getKey(), distance);
	        counter++;
	    }
	    System.out.println(counter+" : Total time required for exploring neighbours by Geo.WITHIN = "+timeGeo+" : Avg. Time = "+(timeGeo/counter));
	    System.out.println(counter+" : Total time required for exploring neighbours by Edge traversal ="+timeEdge+" : Avg. Time = "+(timeEdge/counter));
		*/
	    return graph;
	}

	public static double exploreNeighboursGeo(TitanGraph graph, String type, double distance) {
		
		System.out.println("Exploring neighbours using Geoshape and Geo.WITHIN i.e, using elasticsearch");
		int counter = 0;
		double time = 0;
		double time_1 = System.currentTimeMillis();
		
		for (Iterator<Vertex> iterator = graph.query().has("type",Compare.EQUAL, type).vertices().iterator(); iterator
				.hasNext();) {
			double time_3 = System.currentTimeMillis();
			Vertex vertex = iterator.next();
			int count = 0;
			Geoshape pointGeoshape = vertex.getProperty("place");
			double latitude = pointGeoshape.getPoint().getLatitude();
			double longitude = pointGeoshape.getPoint().getLongitude();
			
			for (Iterator<Vertex> iterator2  = graph.query().has("place", Geo.WITHIN, Geoshape.circle(latitude, longitude, distance)).has("type",Compare.NOT_EQUAL, type).vertices().iterator();
					iterator2.hasNext();){
				Vertex vertex2 = iterator2.next();
				count++;
			}
			double time_4 = System.currentTimeMillis();
//			System.out.println("Total vertices explored = "+count);
//			System.out.println("time required for "+vertex.getId()+" = "+(time_4-time_3));
			time += time_4-time_3;
			counter+=count;
		}
		double time_2 = System.currentTimeMillis();
		
		System.out.println("Total time = "+(time_2-time_1)+" for "+counter+" nodes");
		System.out.print("Time excluding initial query "+time +" for "+counter+" nodes and avg. time is "+(time/counter)+"\n");
		return (time_2-time_1);
		
	}
	
	public static void exploreNeighboursGeo(TitanGraph graph, double distance) {
		
		System.out.println("Exploring neighbours using Geoshape and Geo.WITHIN i.e, using Standard Indexing or Elastic Search");
		int counter = 0;
		double time = 0;
		double time_1 = System.currentTimeMillis();
		
		for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator
				.hasNext();) {
			double time_3 = System.currentTimeMillis();
			Vertex vertex = iterator.next();
			
			Geoshape pointGeoshape = vertex.getProperty("place");
			double latitude = pointGeoshape.getPoint().getLatitude();
			double longitude = pointGeoshape.getPoint().getLongitude();
			String type = vertex.getProperty("type");
//			System.out.println(type);
			

			for(Iterator<Vertex>iterator2  = graph.query().has("place", Geo.WITHIN, Geoshape.circle(latitude, longitude, distance)).has("type",Compare.NOT_EQUAL, type).vertices().iterator()
					;	iterator2.hasNext();){
				Vertex vertex2 = iterator2.next();
				counter++;
			}
//			counter  += graph.query().has("place", Geo.WITHIN, Geoshape.circle(latitude, longitude, distance)).has("type",Compare.NOT_EQUAL, type).
			double time_4 = System.currentTimeMillis();
			time += time_4-time_3;
//			counter++;
		}
		double time_2 = System.currentTimeMillis();
		
		System.out.println("Total time = "+(time_2-time_1)+" for "+counter+" nodes");
		System.out.print("Time excluding initial query "+time +" for "+counter+" nodes and avg. time is "+(time/counter)+"\n");
		
	}
	
	public static void exploreNeighboursGeoMultiThread(TitanGraph graph, double distance1, double distance2) {

		
		double time1 = System.currentTimeMillis();
//		Thread[] threads = new Thread[10000];
		
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		Iterator<Vertex> iterator = graph.getVertices().iterator();
		int i;

		for(i = 0;iterator.hasNext();i++){
			
			Vertex vertex = iterator.next();
			ExploreNeighbours exploreNeighbours = new ExploreNeighbours(graph,vertex,distance1,distance2);
			executorService.execute(exploreNeighbours);
//			threads[i] = new Thread(exploreNeighbours);
//			threads[i].start();			
		}
		executorService.shutdown();
		int count = 0;
//		System.out.println("Total vertices = "+i);
//		i--;
//		for(int j=0;j<=i;j++){
//			threads[j].join();
//		}
		
		while(!executorService.isTerminated()){
			;
		}
		
		Iterator it = countMap.entrySet().iterator();
		
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
//	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        count += (Integer)pairs.getValue();
	    }
		double time2 = System.currentTimeMillis();	
		System.out.println("Total time required = "+(time2-time1)+ " for "+count+" nodes");
	}
	
	public static double exploreNeighboursEdge(TitanGraph graph, String type, double distance) {
		
		System.out.println("Exploring neighbours using Edge Traversal");
		double time = 0;
		double time_1 = System.currentTimeMillis();
		int counter = 0;
		
		for (Iterator<Vertex> iterator = graph.query().has("type", Compare.EQUAL, type).vertices().iterator();iterator.hasNext();) {
//			double time_3 = System.currentTimeMillis();
		    Vertex vertex = iterator.next();
		    Iterator<Vertex> iterator2 = vertex.query().vertices().iterator();
		    /*
		     * First Method - Returns a list of vertices connected to a vertex under consideration irrespective of its distance
		     * from that vertex. 
		     */

		    /*
		    for(Iterator<Vertex> iterator2 = vertex.query().vertices().iterator();iterator2.hasNext();){
		    	Vertex vertex2 = iterator2.next();
		    	System.out.println("Vertex : "+vertex.getId()+" Vertex_In : "+vertex2.getId());
		    }
		    */
		    
		    /*
		     * Second Method - Returns a list of edges(both - OUT and IN) which can be used to filter neighbouring vertices
		     * based on distance threshold. Filtering need to be implemented. 
		     */
//		     
		    /*
		    for (Iterator<Edge> iterator2 = vertex.query().edges().iterator();iterator2.hasNext();) {
				Edge edge = iterator2.next();
				Vertex vertex2 = edge.getVertex(Direction.IN);
				System.out.println("Vertex : "+vertex.getId()+" Vertex_In : "+vertex2.getId());
			}
			*/
//		    double time_4 = System.currentTimeMillis();
//		    System.out.println("time required for "+vertex.getId()+" = "+(time_4-time_3));
//			time += time_4-time_3;
			counter++;
		    
		}
		double time_2 = System.currentTimeMillis();
		System.out.println("Total time = "+(time_2-time_1)+" for "+counter+" nodes");
//		System.out.print("Time excluding I/O "+time +" for "+counter+" nodes and avg. time is "+(time/counter)+"\n");
		return (time_2-time_1);
	}
		
	public static void exploreNeighboursEdge(TitanGraph graph, double distance) {
		
		System.out.println("Exploring neighbours using Edge Traversal");
		double time = 0,time_3,time_4;
		double time_1 = System.currentTimeMillis();
		int counter = 0;
		
		for (Iterator<Vertex> iterator = graph.getVertices().iterator();iterator.hasNext();) {
			time_3 = System.currentTimeMillis();
		    Vertex vertex = iterator.next();
		    Iterator<Vertex> iterator2 = vertex.query().vertices().iterator();
		    time_4 = System.currentTimeMillis(); 
		    time += time_4-time_3;
		    for( ;	iterator2.hasNext();){
				Vertex vertex2 = iterator2.next();
				counter++;
			}		    
		}
		
		double time_2 = System.currentTimeMillis();
		System.out.println("Total time = "+(time_2-time_1)+" for "+counter+" nodes");
		System.out.print("Time excluding initial query "+time +" for "+counter+" nodes and avg. time is "+(time/counter)+"\n");
	}
			
	public static void main(String[] args) throws Exception {
		
		Graph.countMap = new ConcurrentHashMap<String,Integer>();
		Graph.edgesMap = new ConcurrentHashMap<String,Double>();
		
		// Mapper to map crime types to simple strings
		Mapper mapper = new Mapper();
		Graph.enCoding = mapper.getEncoding();
		Graph.deCoding = mapper.getDecoding();
		
		// Step 0 : Open Graph Database Connection
		Database db = Database.getInstance();
		TitanGraph graph = db.getTitanGraph();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
//		boolean clean = true, initialize = true, addEdges = false;
		boolean clean = false, initialize = false, addEdges = true;
//		boolean clean = false, initialize = false, addEdges = false;
//		boolean clean = true, initialize = true, addEdges = true;
//		boolean clean = false, initialize = true, addEdges = false;
//		boolean clean = true, initialize = false, addEdges = false;
//		removeEdges(graph);
		
		int no_of_instances = 30000;
		double distance_threshold = 0.3;
		
//		for(int i = 10000; i<=100000;i+=10000){
//			if(clean){
//				// Step 1 : Clear initial graph
//				graph = clearGraph(db, graph);
//				// Step 2 : Build Schema
//				build_schema(graph);
//			}
//			if(initialize){
//				// Step 3 : Initialize Graph Database
//				InitializeGraph(graph, i);
//				System.out.println("Graph initialized for i = "+i);
//			}			
//		}
		
		if(clean){
			// Step 1 : Clear initial graph
			graph = clearGraph(db, graph);
			// Step 2 : Build Schema
			build_schema(graph);
		}
		if(initialize){
			// Step 3 : Initialize Graph Database
			InitializeGraph(graph, no_of_instances);
			System.out.println("Graph initialized");
		}
		if(addEdges){
			// Step 4 : Build edges for some distance thresholdtrue
			addEdgesMultiThread(graph, distance_threshold, distance_threshold);
			iterateEdges(graph);
		}
		
		// Step 4 : Generate stats
//		stats(graph1);
//		iterateEdges(graph);
//		exploreNeighboursGeoMultiThread(graph, distance_threshold,0.5);
		date = new Date();
		System.out.println(dateFormat.format(date));
		
		// Step 6 : Explore neighbors for distance threshold = 0.2 using Edge Traversal
//		exploreNeighboursEdge(graph, 0.2);
		
		// Step 7 : Explore neighbors for distance threshold = 0.2 using Geo.WITHIN
		
//		exploreNeighboursGeo(graph,"LIQUOR LAW VIOLATION", 0.2);
//		Graph.countMap = new ConcurrentHashMap<String,Integer>();

//		exploreNeighboursGeo(graph, 0.2);

//		Iterator<Vertex> it = graph.query().has("type", com.tinkerpop.blueprints.Compare.EQUAL,"3").vertices().iterator();
//		while (it.hasNext()) {
//			Vertex vertex = (Vertex) it.next();true
//			GremlinPipeline<String, Vertex> g = new GremlinPipeline<String, Vertex>(graph.getVertex(vertex.getId())).in().as("l1").in().as("l2");
////			Iterator<Pipe<String, Vertex>> a = g.getPipes().iterator().next();
////			
////			while(a.hasNext()){
////				System.out.println(a.next());
////			}
//			
//			System.out.println(vertex.getId()+" "+vertex.getProperty("type"));
//			Iterator<Vertex> it1 = g.iterator();
//			while (it1.hasNext()) {
//				Vertex vertex2 = (Vertex) it1.next();
//				System.out.println(vertex2.getId()+"-----"+vertex2.getProperty("type"));
//			}
//			System.out.println("--------------------");
//		}
//		long time1 = System.currentTimeMillis();
//		int counter = 0;
//		Iterator<Vertex> it = graph.getVertices("type", "3").iterator(); 
//		while(it.hasNext()){
//			final Vertex vertex = it.next();
//			GremlinPipeline pipe = new GremlinPipeline();
//			pipe.start(vertex).in("3-7").in("7-32").out("3-32").filter(new PipeFunction<Vertex,Boolean>() {
//				  public Boolean compute(Vertex argument) {
//					  if((Long)argument.getId() == vertex.getId()){
//						  return true;
//					  }else{
//						  return false;
//					  }
//					  }
//					}).path(new PipeFunction<Vertex, Long>(){
//				public Long compute(Vertex argument) {
//					return (Long) argument.getId();
//				}
//			});
//			counter += pipe.count();
//		}
//		long time2 = System.currentTimeMillis();
//		System.out.println("Total = "+counter+ " ");
//		final String x = null;
		
		
		
		
//		final List<Vertex> temp = new ArrayList<Vertex>();
////		String 'x';
////		final long id;
//		pipe.start(graph.getVertices("type","30")).sideEffect(new PipeFunction<Vertex, Vertex>(){
//			public Vertex compute(Vertex argument){
//				if(temp.size()>0){
//					temp.remove(temp.size()-1);
//				}
//				temp.add(argument);
//				return argument;
//			}
//			}).in("30-31").out("30-31").filter(new PipeFunction<Vertex,Boolean>() {
//			  public Boolean compute(Vertex argument) {
////				  if(argument== x){
////					  return true;
////				  }else{
////					  return false;
////				  }
////				  System.out.println(x);
//				  if(temp.contains(argument)){
//					  return true;
//				  }
//				  else{
//					  return false;
//				  }				  
//				  }
//				}).path(new PipeFunction<Vertex, Long>(){
//			public Long compute(Vertex argument) {
//				return (Long) argument.getId();
//			}
//		}).enablePath();
		
		
		
		
		
//		pipe.start(graph.getVertices()).filter(new PipeFunction<Vertex,Boolean>() 
//				{
//					  public Boolean compute(Vertex argument) 
//					  {
//						  if(argument.getProperty("type").equals("3"))
//						      {
//							      return true;
//						  	  }
//						  else
//						  	  {
//							      return false;
//						  	  }
//					   }
//				}).as('x').in("3-7").in("7-32").out("3-32").filter(new PipeFunction<Vertex,Boolean>() {
//			  public Boolean compute(Vertex argument) {
//				  if(argument== x){
//					  return true;
//				  }else{
//					  return false;
//				  }
//				  }
//				}).path(new PipeFunction<Vertex, Long>(){
//			public Long compute(Vertex argument) {
//				return (Long) argument.getId();
//			}
//		});
			
//		Iterator it = pipe.iterator();
//		while(it.hasNext()){
//			System.out.println(pipe.next());
//		}
		
//		GremlinPipeline<String, Vertex> g = new GremlinPipeline<String, Vertex>(graph.getVertex(2095104)).out("7-32");
//		Iterator<Vertex> it = g.iterator();
//		while(it.hasNext()){
//			Vertex v = it.next();
//			System.out.println(v.getProperty("type")+" ---- "+v.getId());
//			GremlinPipeline<String, Vertex> g1 = new GremlinPipeline<String, Vertex>(graph.getVertex(v.getId())).out("3-7");
//			Iterator<Vertex> it1 = g1.iterator();
//			while(it1.hasNext()){
//				Vertex v1 = it1.next();
//				System.out.println(v.getProperty("type")+" ---- "+v1.getId());
//			}
//		}
		
//		Iterator<Vertex> it = graph.query().vertices().iterator();
//		while(it.hasNext()){
//			Vertex x = it.next();
//			System.out.println(x.getId()+" "+x.getProperty("type")+" => ");
//			Iterator<Vertex> it1 = x.getVertices(Direction.IN).iterator();
//			while (it1.hasNext()) {
//				Vertex vertex = (Vertex) it1.next();
//				System.out.print(vertex.getId()+" "+vertex.getProperty("type")+" ");
//			}
//			System.out.println("");
//		}
		
//		Iterator<Vertex> it = graph.query().has("type", com.tinkerpop.blueprints.Compare.EQUAL,"32").vertices().iterator();
//		while(it.hasNext()){
////			System.out.println("Hi");
//			Vertex x = it.next();
//			Iterator<Vertex> it1 = x.getVertices(Direction.OUT).iterator();
////			Iterator<Vertex> it1 = x.query().has("type", "3").direction(Direction.OUT).has("type", "29").direction(Direction.OUT).vertices().iterator();
//			while(it1.hasNext()){
//				System.out.print(x.getId()+" - "+it1.next().getProperty("type")+" ");
//			}
//			System.out.println("-----");			
//		}
		
		
		// Step 8 : Close Graph Database Connection
		db.closeTitanGraph(graph);
	}
}
