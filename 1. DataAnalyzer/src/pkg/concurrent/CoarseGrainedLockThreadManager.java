package pkg.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import Consts.Consts;
import pkg.temperature.aggregation.AverageTmaxCalculator;

public class CoarseGrainedLockThreadManager implements Runnable {

	List<String> lines = new ArrayList<String>();
	int startLineNumber = 0;
	int endLineNumber = 0;
	HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();
	Mutex lock = new Mutex();
	Object inputLock = new Object();

	public CoarseGrainedLockThreadManager(List<String> lines, int startLineNumber, int endLineNumber,
			HashMap<String, List<Integer>> idToTemperatureValuesMap) {

		this.lines = lines;
		this.startLineNumber = startLineNumber;
		this.endLineNumber = endLineNumber;
		this.idToTemperatureValuesMap = idToTemperatureValuesMap;
	}

	public void run() {
		//try {
			// COARSE-GRAINED LOCK
			//lock.acquire();
			synchronized(inputLock){
			// Load Accumulation data structure.
			for (int i = startLineNumber; i < endLineNumber; i++) {
				String line = lines.get(i);
				String[] columns = new String[4];
				columns = line.split(",");

				String id = columns[0];
				String type = columns[2];

				// Ignore records which are not TMAX
				if (!type.toLowerCase().equals(Consts.TMAX.toLowerCase())) {
					continue;
				}

				Integer value = Integer.parseInt(columns[3]);

				if (!idToTemperatureValuesMap.containsKey(id))
					idToTemperatureValuesMap.put(id, new ArrayList<Integer>());

				AverageTmaxCalculator.FindFibbonacci(17);
				idToTemperatureValuesMap.get(id).add(value);				
			}
			}
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally {
//			//lock.release();
//		  }
	}
}
