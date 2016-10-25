package pkg.sequential;

import Consts.Consts;
import pkg.file.manager.*;
import pkg.temperature.aggregation.*;

import java.util.*;
import java.util.Map.Entry;
import com.sun.javafx.scene.layout.region.Margins.Converter;

// GLOBAL HISTORICAL CLIMATOLOGY NETWORK (GHCN-DAILY)
public class SequentialAvgTempCalculator {

	public HashMap<String, List<Integer>> GetIdToTemperatureValuesMap(List<String> lines) {		
		// Accumulation data structure
		HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();

		// Load Accumulation data structure.
		for (String line : lines) {
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
		
		return idToTemperatureValuesMap;
	}


	
}
