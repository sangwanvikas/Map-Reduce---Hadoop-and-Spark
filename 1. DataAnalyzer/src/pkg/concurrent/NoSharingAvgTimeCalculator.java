package pkg.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Consts.Consts;
import pkg.file.manager.FileManager;
import pkg.temperature.aggregation.AverageTmaxCalculator;

public class NoSharingAvgTimeCalculator {	
	
	public HashMap<String, List<Integer>> GetIdToTemperatureValues(List<String> lines) throws InterruptedException{		
		int totalProcessorsAvailable = GetAvailableProcessors();
		int totalLines = lines.size();
		int numberofLinesForAThread = totalLines/totalProcessorsAvailable;
		int startLineNumber = 0;
		int endLineNumber = 0;
		
		List<HashMap<String, List<Integer>>> hashMaps = new ArrayList();
		for(int i=0; i < totalProcessorsAvailable; i++) {
			  startLineNumber = i * numberofLinesForAThread;
			  
			  if(i > totalProcessorsAvailable - 2)
				  numberofLinesForAThread = totalLines - startLineNumber;
			    
			  endLineNumber = startLineNumber + numberofLinesForAThread;
			  // Spawn new Thread
			  NoSharingThreadManager obj = new NoSharingThreadManager(lines, startLineNumber, endLineNumber, hashMaps);
			  Thread t = new Thread(obj);
			  t.start();
			  t.join();
			  }	
		
		HashMap<String, List<Integer>> idToTemperatureValuesMap = GetCombinedHashMap(hashMaps);
			
		return idToTemperatureValuesMap;
	}
	
	public int GetAvailableProcessors(){
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		return availableProcessors;		
	}
	
	public HashMap<String, List<Integer>> GetCombinedHashMap(List<HashMap<String, List<Integer>>> hashMaps){
		HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();
		
		for(HashMap<String, List<Integer>> hashMap: hashMaps){
			Iterator<Entry<String, List<Integer>>> it = hashMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry pair = (Map.Entry)it.next();
				String id = pair.getKey().toString();
				List<Integer> temperatureValues = (List<Integer>)pair.getValue();
				
				if (!idToTemperatureValuesMap.containsKey(id))
					idToTemperatureValuesMap.put(id, new ArrayList<Integer>());
				
				idToTemperatureValuesMap.get(id).addAll(temperatureValues);
			}
		}
		
		return idToTemperatureValuesMap;
	}
}
