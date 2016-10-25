package pkg.InMapperCombiner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/*
 * A MapReduce task WITH In Mapper Combiner - defined to  calculate the mean minimum temperature 
 * and the mean maximum temperature, by station, for a single year of data.
 */
public class InMapperCombiner {

	// Map task
	public static class Map extends Mapper<Object, Text, Text, Text> {

		// Hash map defined at the task level of the Map class.
		private HashMap<String, String> map;

		// Initialized H before the first Map() function call in the task.
		public void setup(Context context) throws IOException, InterruptedException {
			map = new HashMap<>();
		}

		// key: Offset
		// value: line
		// This map does not emit, it just add locally aggregated temperature values 
		// for the given stationId key. E.g. a sting "-190 30 400 50" gets assigned to h[stationId]. 
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			
			// Parse each line to get stationsId, TempType(TMIN/TMAX), and temp value
			StringTokenizer itr = new StringTokenizer(value.toString(), ",");
			Text stationIdAsKey = new Text();
			stationIdAsKey.set(itr.nextToken());
			itr.nextToken();
			String tempType = itr.nextToken();			

			// Do not assign to map if TempType is not TMIN or TMAX
			if ((tempType.equals("TMIN") || tempType.equals("TMAX"))) {
				String tempValue = itr.nextToken();

				if (!map.containsKey(stationIdAsKey.toString())) {
					// H[stationId] <- "TminAgg MinCount TMaxAgg MaxCount"
					map.put(stationIdAsKey.toString(), "0f 0 0f 0");
				}

				String[] tempAndValue = map.get(stationIdAsKey.toString()).split(" ");

				// Update existing H[stationId] with values parsed from current line.
				if (tempAndValue.length == 4) {
					Float sumForTmin = 0f;
					int minCount = 0;
					Float sumForTmax = 0f;
					int maxCount = 0;

					if (tempType.equals("TMIN")) {
						// update sum and count for TMIN
						sumForTmin = Float.parseFloat(tempValue) + Float.parseFloat(tempAndValue[0]);
						minCount = Integer.parseInt(tempAndValue[1]) + 1;

						// copy to variable.
						sumForTmax = Float.parseFloat(tempAndValue[2]);
						maxCount = Integer.parseInt(tempAndValue[3]);

						map.put(stationIdAsKey.toString(),
								Float.toString(sumForTmin) + " " + minCount + " " + sumForTmax + " " + maxCount);
					}

					if (tempType.equals("TMAX")) {
						// copy to variable.
						sumForTmin = Float.parseFloat(tempAndValue[0]);
						minCount = Integer.parseInt(tempAndValue[1]);

						// update sum and count for TMAX
						sumForTmax = Float.parseFloat(tempValue) + Float.parseFloat(tempAndValue[2]);
						maxCount = Integer.parseInt(tempAndValue[3]) + 1;

						map.put(stationIdAsKey.toString(),
								Float.toString(sumForTmin) + " " + minCount + " " + sumForTmax + " " + maxCount);
					}
				}

			}
		}

		// This function emits aggregated-tally for the entire task.
		public void cleanup(Context context) throws IOException, InterruptedException {
			Iterator<Entry<String, String>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry pair = (Entry) it.next();
				Text stationIdAsKey = new Text(pair.getKey().toString());
				
				// sets to a string "-4000 34 65564 40"
				Text minAggSpaceMinCountSpaceMaxAggSpaceMaxCount = new Text((String) pair.getValue());

				context.write(stationIdAsKey, minAggSpaceMinCountSpaceMaxAggSpaceMaxCount);
			}
		}
	}

	// Reduce task
	public static class Reduce extends Reducer<Text, Text, Text, Text> {

		private Text meansSeparatedByComma = new Text();

		// key: stationId
		// value: [(-4000 34 65564 40)]
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			
			// Variables used while calculating MeanTemperature for TMIN and TMAX
			Float sumForTmin = 0f;
			int minCount = 0;
			Float sumForTmax = 0f;
			int maxCount = 0;

			for (Text temperatureTypeAndValue : values) {

				String[] tempAndValue = temperatureTypeAndValue.toString().split(" ");

				if (tempAndValue.length == 4) {
					sumForTmin += Float.parseFloat(tempAndValue[0]);
					minCount += Integer.parseInt(tempAndValue[1]);

					sumForTmax += Float.parseFloat(tempAndValue[2]);
					maxCount += Integer.parseInt(tempAndValue[3]);
				}
			}

			// Calculate Average
			Float meanTmin = (float) sumForTmin / minCount;
			Float meanTmax = (float) sumForTmax / maxCount;

			// set to string "MeanMinTemp0, MeanMaxTemp0"
			meansSeparatedByComma.set(Float.toString(meanTmin) + ", " + Float.toString(meanTmax));

			context.write(key, meansSeparatedByComma);
		}
	}
}
