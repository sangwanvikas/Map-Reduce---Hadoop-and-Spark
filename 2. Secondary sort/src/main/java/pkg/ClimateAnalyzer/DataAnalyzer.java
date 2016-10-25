package pkg.ClimateAnalyzer;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.net.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;
/*	
 * Secondary sort design pattern - MapReduce task to create time series of temperature data,
 * which calculates mean minimum temperature and mean maximum temperature, by station, by year.
*/

public class DataAnalyzer {

	// Map task
	public static class MapTask extends Mapper<Object, Text, StationIdAndYearPair, ValueForCompositeKey> {

		// Key: offset
		// value: line
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			// Parse a line to get stationsId, TempType(TMIN/TMAX), and
			// tempvalue
			StringTokenizer itr = new StringTokenizer(value.toString(), ",");
			Text stationId = new Text(itr.nextToken());
			IntWritable year = new IntWritable();
			year.set(Integer.parseInt(itr.nextToken().toString().substring(0, 4)));
			String tempType = itr.nextToken();
			Text temperatureType = new Text(tempType);

			// Do not emit if TempTYpe is neither TMIN nor TMAX
			if (tempType.equals("TMIN") || tempType.equals("TMAX")) {
				IntWritable temperatureValue = new IntWritable();
				temperatureValue.set(Integer.valueOf(itr.nextToken()));

				// define composite key
				StationIdAndYearPair compostieKey = new StationIdAndYearPair();
				compostieKey.setStationId(stationId);
				compostieKey.setYear(year);

				// define value for the compostie key
				ValueForCompositeKey valueForCompositeKey = new ValueForCompositeKey();
				valueForCompositeKey.setTemperatureType(temperatureType);
				valueForCompositeKey.setTemperatureValue(temperatureValue);
				valueForCompositeKey.setYear(year);

				context.write(compostieKey, valueForCompositeKey);
			}
		}
	}

	// This class performs sorting based on the second key present in the
	// composite key. It controls the sort order of the year.
	public static class CustomKeySortComparator extends WritableComparator {

		protected CustomKeySortComparator() {
			super(StationIdAndYearPair.class, true);
		}

		// Return compared value of two stationId's, when stationIds of two keys
		// are different.
		// Returns compared value of two years, when stationIds of two keys are
		// equal.
		@Override
		public int compare(WritableComparable w1, WritableComparable w2) {

			StationIdAndYearPair key1 = (StationIdAndYearPair) w1;
			StationIdAndYearPair key2 = (StationIdAndYearPair) w2;

			int stationCompareResult = key1.getStationId().compareTo(key2.getStationId());

			// when two station ids are equal
			if (stationCompareResult == 0) {
				int yearCompareResult = key1.getYear().compareTo(key2.getYear());
				return yearCompareResult;
			}

			return stationCompareResult;
		}
	}

	// This class controls 'stationId' keys are grouped together into a single
	// call to the reduce() method
	public static class CustomGroupingComparator extends WritableComparator {
		protected CustomGroupingComparator() {
			super(StationIdAndYearPair.class, true);
		}

		// Return compared value of two stationId's.
		@Override
		public int compare(WritableComparable w1, WritableComparable w2) {
			StationIdAndYearPair key1 = (StationIdAndYearPair) w1;
			StationIdAndYearPair key2 = (StationIdAndYearPair) w2;

			int stationIdCompareResult = key1.getStationId().compareTo(key2.getStationId());

			return stationIdCompareResult;
		}
	}

	// This class partition keys while passing to the reducer, and helps in
	// deciding what mapper’s output goes to which reducer based on the
	// mapper’s output key.
	public static class CustomPartitioner extends Partitioner<StationIdAndYearPair, ValueForCompositeKey> {

		@Override
		public int getPartition(StationIdAndYearPair key, ValueForCompositeKey value, int numReduceTasks) {
			return ((key.getStationId().hashCode() & Integer.MAX_VALUE) % numReduceTasks);
		}
	}

	// Reduce Task
	public static class ReduceTask extends Reducer<StationIdAndYearPair, ValueForCompositeKey, Text, Text> {
		private IntWritable result = new IntWritable();

		// key: an object of type StationIdAndYearPair,
		// values: an object of type Iterable< ValueForCompositeKey >)
		public void reduce(StationIdAndYearPair key, Iterable<ValueForCompositeKey> values, Context context)
				throws IOException, InterruptedException {

			// Variables used while calculating MeanTemperature for TMIN and
			// TMAX for single year for a stationId.
			Float sumForTmin = 0f;
			int minCount = 0;
			Float sumForTmax = 0f;
			int maxCount = 0;

			HashMap<String, List<String>> map = new HashMap();
			int previousYr = 0;

			String stationId = key.getStationId().toString();

			if (!map.containsKey(stationId)) {
				List<String> defaultList = new ArrayList<String>();
				map.put(stationId, defaultList);
			}

			// For a stationId, Iterate over over objects of same year in
			// values to aggregate their temperature values.
			// STEP 1: Fetch from current 'val' object.
			// STEP 2: Fetch from H[stationId] i.e. last element from
			// the List<string> returned by H[stationId]
			// STEP 3: STEP 1 + STEP 2
			for (ValueForCompositeKey val : values) {

				// STEP 1: Parse object to get year, tempType, and tempValue.
				int currentYr = Integer.parseInt(val.getYear().toString());
				String tempType = val.getTemperatureType().toString();
				Float tempValue = Float.parseFloat(val.getTemperatureValue().toString());

				// For a new year discovered in the values, ddd a string to
				// the end of List<String>, returned by H[stationId]
				List<String> allValuesForStationId = map.get(stationId);
				// Determines if object in the list contains a year which has
				// not been yet seen i.e. 1880, 1880, 1880,......., 1881
				if (previousYr != currentYr) {
					// set to "Year TminAggSum TminCount TmaxAggSum TmaxCount"
					String defaultValue = "0 0f 0 0f 0";
					allValuesForStationId.add(defaultValue);

					// Reset variables, required to calculate mean for single
					// year for a stationId. Hence, reset them when new year
					// appears in the list.
					sumForTmin = 0f;
					minCount = 0;
					sumForTmax = 0f;
					maxCount = 0;
				}

				// STEP 2 : Split, by space, last element from the List<string>,
				// returned by H[stationId]
				String[] valuesForCurrentYr = allValuesForStationId.get(allValuesForStationId.size() - 1).split(" ");

				// STEP 3: Aggregate values obtained from STEP 1 and STEP 2.
				if (tempType.equals("TMIN")) {
					// update sum and count for TMIN
					sumForTmin = tempValue + Float.parseFloat(valuesForCurrentYr[1]);
					minCount = Integer.parseInt(valuesForCurrentYr[2]) + 1;

					// copy to variable.
					sumForTmax = Float.parseFloat(valuesForCurrentYr[3]);
					maxCount = Integer.parseInt(valuesForCurrentYr[4]);
				}

				if (tempType.equals("TMAX")) {
					// copy to variable.
					sumForTmin = Float.parseFloat(valuesForCurrentYr[1]);
					minCount = Integer.parseInt(valuesForCurrentYr[2]);

					// update sum and count for TMAX
					sumForTmax = tempValue + Float.parseFloat(valuesForCurrentYr[3]);
					maxCount = Integer.parseInt(valuesForCurrentYr[4]) + 1;
				}

				// Remove last element from List<String>, returned by
				// H[stationId], and add updated string to the last.
				previousYr = currentYr;
				allValuesForStationId.remove(allValuesForStationId.size() - 1);
				allValuesForStationId
						.add(currentYr + " " + sumForTmin + " " + minCount + " " + sumForTmax + " " + maxCount);
			}

			// For a stationId, Iterate over each element to calculate mean and
			// give output as [(1880, MeanMina0, MeanMaxa0),
			// (1881, MeanMina1, MeanMaxa1) … (1889 …)
			Iterator<Entry<String, List<String>>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry pair = (Entry) it.next();
				Text stationIdAsKey = new Text(pair.getKey().toString());
				List<String> yearSpaceValuesForMean = (List<String>) pair.getValue();
				String result = "[";

				int counter = 0;
				for (String value : yearSpaceValuesForMean) {
					String[] valueArray = value.split(" ");
					String year = valueArray[0];
					String tminAvg = ((Float) (Float.parseFloat(valueArray[1]) / Float.parseFloat(valueArray[2])))
							.toString();
					String tmaxAvg = ((Float) (Float.parseFloat(valueArray[3]) / Float.parseFloat(valueArray[4])))
							.toString();
					result = result + "(" + year + ", " + tminAvg + ", " + tmaxAvg + ")";

					// append comma after a pair of (Year, meanTMin,meanTMax)
					if (counter < yearSpaceValuesForMean.size() - 1) {
						result = result + ", ";
					}
					counter++;
				}
				result = result + "]";

				context.write(stationIdAsKey, new Text(result));
			}

		}
	}
}
