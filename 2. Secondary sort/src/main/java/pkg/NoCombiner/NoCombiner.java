package pkg.NoCombiner;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/*
 * A MapReduce task W/O Combiner - defined to  calculate the mean minimum temperature and the mean
 * maximum temperature, by station, for a single year of data.
 */

public class NoCombiner {

	// Map Task
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
			String tempValue = itr.nextToken();

			// Do not emit if TempType is neither TMIN nor TMAX
			if (tempType.equals("TMIN") || tempType.equals("TMAX")) {
				temperatureTypeAndValue.set(tempType + " " + tempValue);

				context.write(stationIdAsKey, temperatureTypeAndValue);
			}
		}
	}

	// Reduce Task
	public static class Reduce extends Reducer<Text, Text, Text, Text> {

		private Text meansSeparatedByComma = new Text();

		// key : StationId
		// values: [("TMIN -25"), ("TMIN 10"), ("TMAX 50"),
		// ("TMIN -90").........]
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// Variables used while calculating MeanTemperature for TMIN and
			// TMAX
			Float sumForTmin = 0f;
			int minCount = 0;
			Float sumForTmax = 0f;
			int maxCount = 0;

			// Iterate over each item in the values to find summation of all
			// temperature values for TMIN and TMAX, and respective counts,
			// where values are:
			// [("TMIN -25"), ("TMIN 10"), ("TMAX 50"), ("TMIN -90").........]
			for (Text temperatureTypeAndValue : values) {
				String[] tempAndValue = temperatureTypeAndValue.toString().split(" ");

				if (tempAndValue.length == 2) {
					String tempType = tempAndValue[0];
					Float tempValue = Float.parseFloat(tempAndValue[1]);

					if (tempType.equals("TMIN")) {
						sumForTmin += tempValue;
						minCount++;
					}

					if (tempType.equals("TMAX")) {
						sumForTmax += tempValue;
						maxCount++;
					}
				}
			}

			// Calculate Average
			float meanTmin = (float) sumForTmin / minCount;
			float meanTmax = (float) sumForTmax / maxCount;

			// set to string "MeanMinTemp0, MeanMaxTemp0"
			meansSeparatedByComma.set(Float.toString(meanTmin) + ", " + Float.toString(meanTmax));

			context.write(key, meansSeparatedByComma);
		}
	}
}
