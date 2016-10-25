package pkg.SortPageRanks;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import com.google.common.collect.Multiset.Entry;

import org.apache.hadoop.mapreduce.Mapper.Context;

import pkg.DataParser.DATA_PROCESSOR_COUNTER;

/*
 * An MR job that is responsible for rankingtop k pages based on their page rank values. 
 */
public class SortPageRanks {
	static int noOfRecordsToBeShown = 100;

	// Map task/
	public static class MapTask extends Mapper<Object, Text, Text, Text> {
		Map<String, Double> nodeToPageRankMap = new HashMap<>();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			// pageNameX PRValue outlink1 outlink2 outlink3 outlink4 tab
			String alineFromParsedFile = value.toString();
			String[] valuesArray = alineFromParsedFile.split("\t");
			int arrayLength = valuesArray.length;

			// Ignore adjacency list with no PageRank value-
			// condition to be on safer side.
			if (arrayLength < 2) {
				return;
			}

			String currentNode = valuesArray[0].toString();
			Double PageRankOfCurrentNode = Double.parseDouble(valuesArray[1]);
			nodeToPageRankMap.put(currentNode, PageRankOfCurrentNode);
		}

		public void cleanup(Context context) throws IOException, InterruptedException {
			Map<String, Double> result = sortByValue(nodeToPageRankMap);
			int counter = 0;
			for (Map.Entry<String, Double> entry : result.entrySet()) {
				counter++;
				if (counter <= noOfRecordsToBeShown) {
					DoubleWritable value = new DoubleWritable();
					value.set(entry.getValue());
					context.write(new Text("default"), new Text(entry.getKey() + "\t" + entry.getValue()));
				}
			}
		}
	}

	// Reduce task Emit (<SerialNumber><.>, <pagename><\t><pagerank>)
	public static class ReduceTask extends Reducer<Text, Text, IntWritable, Text> {
		Map<String, Double> nodeToPageRankMap = new HashMap<>();

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text nodeNameTabPageRankValue : values) {
				String[] items = nodeNameTabPageRankValue.toString().split("\t");
				String nodeName = items[0];
				String pageRank = items[1];
				nodeToPageRankMap.put(nodeName, (Double.parseDouble(pageRank)));
			}

		}

		public void cleanup(Context context) throws IOException, InterruptedException {
			Map<String, Double> result = sortByValue(nodeToPageRankMap);
			int counter = 0;
			IntWritable key = new IntWritable();

			for (Map.Entry<String, Double> entry : result.entrySet()) {
				counter++;
				key.set(counter);
				if (counter <= noOfRecordsToBeShown) {
					Text value = new Text(entry.getKey() + " " + entry.getValue());
					context.write(key, value);
				}
			}
		}
	}

	// sort input map based on decreasing order of PageRank values
	private static Map<String, Double> sortByValue(Map<String, Double> unsortMap) {

		// 1. Convert Map to List of Map
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());

		// 2. Sort list with Collections.sort(), provide a custom Comparator
		// Try switch the o1 o2 position for a different order
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		// 3. Loop the sorted list and put it into a new insertion order Map
		// LinkedHashMap
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Map.Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		/*
		 * //classic iterator example for (Iterator<Map.Entry<String, Integer>>
		 * it = list.iterator(); it.hasNext(); ) { Map.Entry<String, Integer>
		 * entry = it.next(); sortedMap.put(entry.getKey(), entry.getValue()); }
		 */

		return sortedMap;
	}

}
