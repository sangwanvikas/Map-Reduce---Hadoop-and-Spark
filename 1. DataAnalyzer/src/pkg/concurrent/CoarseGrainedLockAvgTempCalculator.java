package pkg.concurrent;

import java.util.HashMap;
import java.util.List;

import Consts.Consts;
import pkg.file.manager.FileManager;
import pkg.temperature.aggregation.AverageTmaxCalculator;

public class CoarseGrainedLockAvgTempCalculator {

	// Accumulation data structure
	HashMap<String, List<Integer>> idToTemperatureValuesMap = new HashMap();

	public HashMap<String, List<Integer>> GetIdToTemperatureValuesMap(List<String> lines) throws InterruptedException {
		int totalProcessorsAvailable = GetAvailableProcessors();
		int totalLines = lines.size();
		int numberofLinesForAThread = totalLines / totalProcessorsAvailable;
		int startLineNumber = 0;
		int endLineNumber = 0;

		for (int i = 0; i < totalProcessorsAvailable; i++) {
			startLineNumber = i * numberofLinesForAThread;

			if (i > totalProcessorsAvailable - 2)
				numberofLinesForAThread = totalLines - startLineNumber;

			endLineNumber = startLineNumber + numberofLinesForAThread;

			// Spawn new Thread
			CoarseGrainedLockThreadManager obj = new CoarseGrainedLockThreadManager(lines, startLineNumber,
					endLineNumber, idToTemperatureValuesMap);
			Thread t = new Thread(obj);
			t.start();
			t.join();
		}

		return idToTemperatureValuesMap;
	}

	public int GetAvailableProcessors() {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		return availableProcessors;
	}

}
