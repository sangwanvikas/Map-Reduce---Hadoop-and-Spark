package pkg.PageRankCalculator;

import java.awt.AlphaComposite;
import java.io.IOException;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import pkg.DataParser.DATA_PROCESSOR_COUNTER;


/*
 * An MR job which implements page rank algorithm having taken care of page rank loss
 * die to dangling nodes.
 */
public class PageRankCalculator {
	static Float alpha = .15f;
	static Float beta = 1 - alpha;

	// Map task to emit outlinks and respective contribution to page rank.
	public static class MapTask extends Mapper<Object, Text, Text, Text> {

		Double danglingPRSum = 0d;

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

			// For the 1st iteration, set default PageRank value to
			// (1/TotalNoOfNodes)
			String totalNodes = context.getConfiguration().get("TOTAL_NODES");
			double defaultPageRank = (1.0 / Integer.parseInt(totalNodes));
			if (valuesArray[1].equals("TBD")) {

				value = new Text(UpdateAdjacencyList(value.toString(), defaultPageRank));
				valuesArray[1] = Double.toString(defaultPageRank);
			}

			// Emit a line from adjacency list. This line has a page rank value,
			// which will be updated with new page rank during next iteration.
			Text currentNode = new Text(valuesArray[0].toString());
			context.write(new Text(currentNode), value);

			// value to be emitted: Calculate P(currNode)/K
			Double PageRankOfCurrentNode = Double.parseDouble(valuesArray[1]);

			// Emit (outlinki,contributonValue) only if currentNode will
			// contribute to page rank i.e. current node has at least one
			// outlink.
			if (arrayLength > 2) {
				int numberOfOutlinks = valuesArray.length - 2;
				DoubleWritable ValueContributionToPR = new DoubleWritable();
				ValueContributionToPR.set((double) PageRankOfCurrentNode / numberOfOutlinks);

				// Loop to emit contribution for each outlink node.
				for (int i = 2; i < arrayLength; i++) {
					Text outlinkNodeAsKey = new Text(valuesArray[i]);
					Text valueContributionAsString = new Text(ValueContributionToPR.toString());

					// emit (key, contribution)
					context.write(outlinkNodeAsKey, valueContributionAsString);
				}

			} else {
				// Emit ("defaultKey", pageRankForDanglingNodes)
				long PRofCurrentNode = PageRankOfCurrentNode.longValue();
				danglingPRSum += PageRankOfCurrentNode;				
			}

		}

		public void cleanup(Context context) {			
			long deltaSum = (long) (danglingPRSum * 1000000000);
			context.getCounter(DATA_PROCESSOR_COUNTER.DANGLING_NODES_DELTA_SUM).increment(deltaSum);
		}

	}

	// Reduce task
	public static class ReduceTask extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			String adjacencyListForKey = "[EMPTY]\t0.0";

			// Calculate Page rank for node 'key'
			Double newPageRankValue = 0d;
			Double partAOfPageRankFormula = 0d;
			Double partBOfPageRankFormula = 0d;

			// First calculate part A of PageRank formula
			String totalNodes = context.getConfiguration().get("TOTAL_NODES");
			partAOfPageRankFormula = 1.0 / Integer.parseInt(totalNodes);

			// Secondly, calculate Part B of PageRank formula, by performing
			// summation on all contributions (iterate over values) for node
			// 'key'
			for (Text contributionValue : values) {
				try {
					partBOfPageRankFormula += Double.parseDouble(contributionValue.toString());

				} catch (Exception e) {
					adjacencyListForKey = contributionValue.toString();

				}
			}

			// Third and last thing left to be gotten to calculate PageRank is
			// (DELTA/# dangling nodes)
			//int totalNodes = Integer.parseInt(context.getConfiguration().get("TOTAL_NODES"));
			Double danglingNodesDeltaSum = Double
					.parseDouble(context.getConfiguration().get("DANGLING_NODES_DELTA_SUM"));
			Double deltaByTotalDanglingNodes = beta*((double) danglingNodesDeltaSum / Integer.parseInt(totalNodes));

			newPageRankValue = (alpha * partAOfPageRankFormula) + (beta * partBOfPageRankFormula) + deltaByTotalDanglingNodes;

			// Assign totalNodes to counter in order to calculate Part A of
			// PageRank formula during next iteration
			context.getCounter(DATA_PROCESSOR_COUNTER.TOTAL_NODES).increment(1);

			// Update and emit adjacency list for key with new PageRank value.
			String updatedAdjacencyList = UpdateAdjacencyList(adjacencyListForKey, newPageRankValue);
			context.write(key, new Text(updatedAdjacencyList));
		}
	}

	// Update Adjacency list with new page rank value.
	public static String UpdateAdjacencyList(String oldAdjacencyList, Double newPageRankValue) {
		String updatedAdjacencyList = "";

		// Split AdjacencyList
		String[] valuesArray = oldAdjacencyList.split("\t");

		// Update newPageRankValue
		valuesArray[1] = newPageRankValue.toString();

		// Combine arrays to create updated Adjacency list
		for (int i = 1; i < valuesArray.length; i++) {
			if (i == 1) {
				updatedAdjacencyList += valuesArray[i];
			} else {
				updatedAdjacencyList += "\t" + valuesArray[i];
			}
		}

		return updatedAdjacencyList;
	}
}