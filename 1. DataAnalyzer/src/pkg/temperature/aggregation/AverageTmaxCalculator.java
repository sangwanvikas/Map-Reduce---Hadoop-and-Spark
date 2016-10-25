package pkg.temperature.aggregation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AverageTmaxCalculator {
	
	public void FindAverage(HashMap<String, List<Integer>> idToTemperaturesMap){
		// Iterate over accumulation data structure to calculate AVERAGE
		Iterator<Entry<String, List<Integer>>> it = idToTemperaturesMap.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry)it.next();
			String id = pair.getKey().toString();
			List<Integer> temperatureValues = (List<Integer>)pair.getValue();
			
			int cumulativeSum = 0;
			int count = temperatureValues.size();
			for(Integer value:temperatureValues){
				cumulativeSum += value;
			}
		    float avg = (float)cumulativeSum/count;
		}		
	}
	
	public static Integer FindFibbonacci(int number){
		if (number == 1 || number == 2) {
			return 1;
		}
 
		return FindFibbonacci(number - 1) + FindFibbonacci(number - 2); // tail recursion
	}
}
