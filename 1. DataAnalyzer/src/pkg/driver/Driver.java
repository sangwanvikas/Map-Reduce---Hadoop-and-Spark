package pkg.driver;

import java.lang.reflect.Array;
import java.util.*;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.FieldAccessor_Integer;

import Consts.Consts;
import pkg.concurrent.*;
import pkg.file.manager.FileManager;
import pkg.sequential.*;
import pkg.temperature.aggregation.AverageTmaxCalculator;
import sun.util.logging.resources.logging;

public class Driver {
	private final static int iterationCount = 10;
	private final static String filePath = Consts.fileName1912Path;
	static AverageTmaxCalculator _aggFunction = new AverageTmaxCalculator();

	public static void main(String args[]) {
		List<String> lines = GetLinesFromFile(filePath);
				
		CalculateAverageTemperature(lines, OperationType.SEQUENTIAL);
		CalculateAverageTemperature(lines, OperationType.NOLOCK);
		CalculateAverageTemperature(lines, OperationType.COARSE);
		CalculateAverageTemperature(lines, OperationType.FINE);
		CalculateAverageTemperature(lines, OperationType.NOSHARING);
	}

	public static List<String> GetLinesFromFile(String filePath) {
		System.out.println(String.format("Reading file content from %s ...", filePath));
		
		FileManager fileMgr = new FileManager();
		List<String> lines = fileMgr.GetFileLines(filePath);
		
		System.out.println(String.format("Total # of lines read: %d", lines.size()));
		System.out.println(String.format("%s \n", "File content loaded in Memory successfully !!"));
		
		return lines;
	}

	public static void CalculateAverageTemperature(List<String> lines, OperationType operationType) {
		System.out.println(String.format("Calculating AVG TMAX for %s ... ", operationType.toString()));

		// will store execution time for each 10 iterations. It's used to find Min, Avg, Max
		long[] executionTimes = new long[iterationCount];
		HashMap<String, List<Integer>> idToTemperatureValuesMap;
		
		// Calculate AVGTmax for 10 times
		for (int i = 0; i < iterationCount; i++) {
			idToTemperatureValuesMap = new HashMap();
			
			long startTime = System.currentTimeMillis();
			idToTemperatureValuesMap = GetIdToTemperatureValuesMap(lines, operationType);
			_aggFunction.FindAverage(idToTemperatureValuesMap);
			long finishTime = System.currentTimeMillis();

			long diff = finishTime - startTime;
			executionTimes[i] = diff;
//			System.out.println(String.format("%d. Time taken in milliseconds %s", i, Long.toString(diff)));
		}
		
		// Print Min, Avg, and Max from the list which contains execution times for 10 iterations.
		System.out.println(String.format("Minimum : %s",Long.toString(GetMininimum(executionTimes))));
		System.out.println(String.format("Average : %s",Float.toString(GetAverage(executionTimes))));
		System.out.println(String.format("Maximum : %s",Long.toString(GetMaximum(executionTimes))));
		System.out.println("----------------------------------------------------------");
		
	}

	public static HashMap<String, List<Integer>> GetIdToTemperatureValuesMap(List<String> lines,
			OperationType operationType) {
		HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();
		Object calculator;
		
		switch (operationType) {
		case SEQUENTIAL:
			calculator = new SequentialAvgTempCalculator();
			idToTemperatureValuesMap = ((SequentialAvgTempCalculator) calculator).GetIdToTemperatureValuesMap(lines);
			break;
			
		case NOLOCK:
			calculator = new NoLockAvgTempCalculator();
			try {
				idToTemperatureValuesMap = ((NoLockAvgTempCalculator) calculator).GetIdToTemperatureValuesMap(lines);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case COARSE:
			calculator = new CoarseGrainedLockAvgTempCalculator();
			try {
				idToTemperatureValuesMap = ((CoarseGrainedLockAvgTempCalculator) calculator)
						.GetIdToTemperatureValuesMap(lines);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case FINE:
			calculator = new FineLockAvgTempCalculator();
			try {
				idToTemperatureValuesMap = ((FineLockAvgTempCalculator) calculator).GetIdToTemperatureValuesMap(lines);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case NOSHARING:
			calculator = new NoSharingAvgTimeCalculator();
			try {
				idToTemperatureValuesMap = ((NoSharingAvgTimeCalculator) calculator).GetIdToTemperatureValues(lines);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}

		return idToTemperatureValuesMap;
	}

	public static long GetMininimum(long[] temperatureValues){
		long minValue = temperatureValues[0];
		    for (int i = 0; i < temperatureValues.length; i++) {
		        if (temperatureValues[i] < minValue) {
		        	minValue = temperatureValues[i];
		        }
		    }
		    return minValue;
	}
	
	public static float GetAverage(long[] temperatureValues){
		long summation = 0;
		    for (int i = 0; i < temperatureValues.length; i++) {
		        	summation += temperatureValues[i];
		    }
		    return (float)(summation/temperatureValues.length);
	}
	
	public static long GetMaximum(long[] temperatureValues){
		long maxValue = temperatureValues[0];
		    for (int i = 0; i < temperatureValues.length; i++) {
		        if (temperatureValues[i] > maxValue) {
		            maxValue = temperatureValues[i];
		        }
		    }
		    return maxValue;
	}
		
	public enum OperationType {
		SEQUENTIAL("Sequential"), NOLOCK("Sharing NO-LOCK"), COARSE("Sharing COARSE GRAINED"), FINE(
				"Sharing FINE GRAINED"), NOSHARING("No Sharing");

		private final String name;

		private OperationType(String s) {
			name = s;
		}

		public boolean equalsName(String otherName) {
			return (otherName == null) ? false : name.equals(otherName);
		}

		public String toString() {
			return this.name;
		}

	}
}
