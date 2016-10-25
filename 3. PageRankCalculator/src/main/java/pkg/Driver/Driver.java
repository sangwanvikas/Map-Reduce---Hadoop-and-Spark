package pkg.Driver;

import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import pkg.DataParser.DATA_PROCESSOR_COUNTER;
import pkg.DataParser.DataParser;
import pkg.PageRankCalculator.PageRankCalculator;
import pkg.SortPageRanks.SortPageRanks;

/*
 * This class runs series of jobs i.e. Data Parsing MR job, an iterative series of
 *  Page Rank calculation jobs, and then a sorting job to rank top 100 pages
 */
public class Driver {

	static long numberOfNodes = 18318;
	static String fileName = "pr";
	static String currentFileName = fileName + 0;
	static double danglingNodesDeltaSum = 0;

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Provide exactly 3 parameters. " + "First as input location for datadump. "
					+ "Second as output location for adjacency list" + "Third as output location for PageRank");
			return;
		}

		RunDataProcessingMRJob(args[0], args[1]);
		RunPageRankCalculationMRJob(args[1], args[1]);
		RunSortPageRankMRJob(args[1], args[2]);
	}

	// Configure Data Pre-processing job
	public static void RunDataProcessingMRJob(String inputLocationForDataDump, String adjacencyListLocation)
			throws Exception {

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf, "Data preprocessor");
		job.setJarByClass(DataParser.class);
		job.setMapperClass(DataParser.MapTask.class);

		// job.setReducerClass(ReduceTask.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(inputLocationForDataDump));
		FileOutputFormat.setOutputPath(job, new Path(adjacencyListLocation + currentFileName));

		job.waitForCompletion(true);

		Counters counters = job.getCounters();
		long c1 = counters.findCounter(DATA_PROCESSOR_COUNTER.TOTAL_NODES).getValue();
		numberOfNodes = c1;
	}

	// Configure Page Rank calculation job
	public static void RunPageRankCalculationMRJob(String adjacencyListLocation, String pageRankFiles)
			throws Exception {

		for (int i = 1; i < 11; i++) {
			System.out.println("running for the " + i + " time");

			Configuration conf = new Configuration();

			// Pass # of nodes, received during last iteration, to the current
			// PageRank MR job in order to calculate PART A of PageRank formula.
			conf.set("TOTAL_NODES", Long.toString(numberOfNodes));
			conf.set("DANGLING_NODES_DELTA_SUM", Double.toString(danglingNodesDeltaSum));

			Job job = Job.getInstance(conf, "Page Rank algorithm");
			job.setJarByClass(DataParser.class);
			job.setMapperClass(PageRankCalculator.MapTask.class);

			job.setReducerClass(PageRankCalculator.ReduceTask.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

			FileInputFormat.addInputPath(job, new Path(adjacencyListLocation + currentFileName));
			
			currentFileName = fileName + i;
			FileOutputFormat.setOutputPath(job, new Path(pageRankFiles + currentFileName));

			job.waitForCompletion(true);

			// Counter to get hold of total nodes found during this iteration,
			// in order to calculate PART A of PageRank formula during next
			// iteration
			Counters counters = job.getCounters();
			numberOfNodes = counters.findCounter(DATA_PROCESSOR_COUNTER.TOTAL_NODES).getValue();

			// Counters for calculating DeltaSum/TotalNodes
			danglingNodesDeltaSum = ((float) counters.findCounter(DATA_PROCESSOR_COUNTER.DANGLING_NODES_DELTA_SUM)
					.getValue()) / 1000000000;
		}
	}

	// Configure Sorting job, which ranks top 100 ranked pages.
	public static void RunSortPageRankMRJob(String eleventhAdjacencyListLocation, String resultFileLocation)
			throws Exception {

		System.err.println(eleventhAdjacencyListLocation + " " + resultFileLocation);

		Configuration conf = new Configuration();
		conf.set("mapred.textoutputformat.separator", ". ");

		Job job = Job.getInstance(conf, "Sort Page Ranks");
		job.setJarByClass(DataParser.class);
		job.setMapperClass(SortPageRanks.MapTask.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);

		job.setNumReduceTasks(1);
		job.setReducerClass(SortPageRanks.ReduceTask.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(eleventhAdjacencyListLocation + currentFileName));
		FileOutputFormat.setOutputPath(job, new Path(resultFileLocation));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
