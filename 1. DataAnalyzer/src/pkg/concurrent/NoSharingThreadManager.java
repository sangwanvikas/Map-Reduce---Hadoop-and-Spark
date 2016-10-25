package pkg.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Consts.Consts;
import pkg.temperature.aggregation.AverageTmaxCalculator;

public class NoSharingThreadManager implements Runnable {	
	
	List<String> lines = new ArrayList();
	int startLineNumber=0;
	int endLineNumber = 0;
	List<HashMap<String, List<Integer>>> idToTemperatureValuesMapList = new ArrayList();
	
	public NoSharingThreadManager(List<String> lines, int startLineNumber, int endLineNumber, List<HashMap<String, List<Integer>>> idToTemperatureValuesMapList){
		this.lines = lines;
		this.startLineNumber = startLineNumber;
		this.endLineNumber = endLineNumber;
		this.idToTemperatureValuesMapList = idToTemperatureValuesMapList;
	}
	
    public void run() {
    	// Load Accumulation data structure.
    	HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();
 		for(int i=startLineNumber;i < endLineNumber;i++){
 			String line = lines.get(i);
 			String[] columns = new String[4];
 			columns = line.split(",");

 			String id = columns[0];
 			String type = columns[2];
 			
 			// Ignore records which are not TMAX
 			if(! type.toLowerCase().equals(Consts.TMAX.toLowerCase())){
 				continue;
 			}
 			
 			Integer value = Integer.parseInt(columns[3]);
 						
 			if(! idToTemperatureValuesMap.containsKey(id))
 				idToTemperatureValuesMap.put(id, new ArrayList<Integer>());
 			
 			AverageTmaxCalculator.FindFibbonacci(17);
 			idToTemperatureValuesMap.get(id).add(value);
 			
 		}
 		
 		idToTemperatureValuesMapList.add(idToTemperatureValuesMap); 		
    }
}
