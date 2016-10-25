package pkg.Combiner;

import java.io.IOException;
import java.util.StringTokenizer;

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
 * A MapReduce task WITH Combiner - defined to  calculate the mean minimum temperature and the mean
 * maximum temperature, by station, for a single year of data.
 */
public class Combiner {
	// Map task
	public static class Map extends Mapper<Object, Text, Text, Text> {

		// key: Offset
		// value: line
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

			// Parse a line to get stationsId, TempType(TMIN/TMAX), and
			// tempvalue
			StringTokenizer itr = new StringTokenizer(value.toString(), ",");
			Text stationIdAsKey = new Text();
			stationIdAsKey.set(itr.nextToken());
			itr.nextToken();
			Text temperatureTypeAndValue = new Text();
			String tempType = itr.nextToken();

			// Do not emit if TempType is not TMIN or TMAX
			if ((tempType.equals("TMIN") || tempType.equals("TMAX"))) {
				String tempValue = itr.nextToken();

				if (tempType.equals("TMIN")) {
					// set to "tempValueForTMIN 1 0f 0". E.g "-50 1 0f 0"
					temperatureTypeAndValue.set(tempValue + " " + 1 + " " + 0f + " " + 0);

					context.write(stationIdAsKey, temperatureTypeAndValue);
				}
				if (tempType.equals("TMAX")) {
					// set to "0f 0 tempValueForTMAX 1". E.g "0f 0 90 1"
					temperatureTypeAndValue.set(0f + " " + 0 + " " + tempValue + " " + 1);

					context.write(stationIdAsKey, temperatureTypeAndValue);
				}
			}
		}
	}

	// Combiner Task
	public static class Combine extends Reducer<Text, Text, Text, Text> {

		private Text minSumSpaceCountSpaceMaxSumSpaceCount = new Text();

		// key : StationId
		// values: [("-25 1 0f 0"), ("10 1 0f 0"), ("0f 0 50 1"),
		// ("-90 1 0f 0").........]
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// defined to aggregate all temperature value by respective
			// tempTypes.
			Float sumForTmin = 0f;
			int minCount = 0;
			Float sumForTmax = 0f;
			int maxCount = 0;

			// Iterate over values to aggregate temperature values.
			for (Text temperatureTypeAndValue : values) {
				String[] tempAndValue = temperatureTypeAndValue.toString().split(" ");
				if (tempAndValue.length == 4) {

					sumForTmin += Float.parseFloat(tempAndValue[0]);
					minCount += Integer.parseInt(tempAndValue[1]);

					sumForTmax += Float.parseFloat(tempAndValue[2]);
					maxCount += Integer.parseInt(tempAndValue[3]);
				}
			}
			// set to aggregated values "-105 3 50 1"
			minSumSpaceCountSpaceMaxSumSpaceCount.set(
					Float.toString(sumForTmin) + " " + minCount + " " + Float.toString(sumForTmax) + " " + maxCount);

			context.write(key, minSumSpaceCountSpaceMaxSumSpaceCount);
		}
	}

	// Reduce Task
	public static class Reduce extends Reducer<Text, Text, Text, Text> {

		private Text meansSeparatedByComma = new Text();

		// key : StationId
		// values: [("-25 1 0f 0"), ("10 1 0f 0"), ("0f 0 50 1"),
		// ("-90 1 0f 0").........]
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// Variables used while calculating MeanTemperature for TMIN and
			// TMAX
			Float sumForTmin = 0f;
			int minCount = 0;
			Float sumForTmax = 0f;
			int maxCount = 0;

			// Iterate over values to find summation of all temperature values
			// for TMIN and TMAX, and respective counts.
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

			// set to string "MeanMinTemp0, MeanMaxTemp0".
			meansSeparatedByComma.set(Float.toString(meanTmin) + ", " + Float.toString(meanTmax));

			context.write(key, meansSeparatedByComma);
		}
	}
}
