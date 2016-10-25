package pkg.Driver.ClimateAnalyzerWOSecSort;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import pkg.ClimateAnalyzer.*;
import pkg.ClimateAnalyzer.DataAnalyzer.CustomGroupingComparator;
import pkg.ClimateAnalyzer.DataAnalyzer.CustomKeySortComparator;
import pkg.ClimateAnalyzer.DataAnalyzer.CustomPartitioner;
import pkg.ClimateAnalyzer.DataAnalyzer.MapTask;
import pkg.ClimateAnalyzer.DataAnalyzer.ReduceTask;
import pkg.Combiner.Combiner;
import pkg.Combiner.Combiner.Combine;
import pkg.Combiner.Combiner.Map;
import pkg.Combiner.Combiner.Reduce;
import pkg.InMapperCombiner.InMapperCombiner;
import pkg.NoCombiner.NoCombiner;

public class Driver {
	public enum ProgramTypes{
		NOCOMBINER,
		COMBINER,
		INMAPPERCOMBINER,
		SECONDARYSORT		
	}
	
	public static void main(String[] args) throws Exception{
		String type = args[0].toUpperCase();
		System.out.println(type);
		ProgramTypes programType = ProgramTypes.valueOf(type);
		
		switch (programType) {
		case NOCOMBINER:
			RunNoCombiner(args);
			break;
		case COMBINER:
			RunCombiner(args);
			break;
		case INMAPPERCOMBINER:
			RunInMapperCombiner(args);
			break;
		case SECONDARYSORT:
			RunSecondarySort(args);
			break;

		default:
			break;
		} 		
	}
	
	private static void RunNoCombiner(String[] args) throws Exception{
		Configuration conf = new Configuration();
		conf.set("mapred.textoutputformat.separator", ", ");
		Job job = Job.getInstance(conf, "No Combiner");
		job.setJarByClass(NoCombiner.class);
		job.setMapperClass(Map.class);
		// job.setCombinerClass(Reduce.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	private static void RunCombiner(String[] args)throws Exception{
		Configuration conf = new Configuration();
		conf.set("mapred.textoutputformat.separator", ", ");
		Job job = Job.getInstance(conf, "Combiner");
		job.setJarByClass(Combiner.class);
		job.setMapperClass(Map.class);
		job.setCombinerClass(Combine.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	private static void RunInMapperCombiner(String[] args)throws Exception{
		Configuration conf = new Configuration();
		conf.set("mapred.textoutputformat.separator", ", ");
		Job job = Job.getInstance(conf, "In Mapper Combiner");
		job.setJarByClass(InMapperCombiner.class);
		job.setMapperClass(Map.class);
		// job.setCombinerClass(Combine.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	private static void RunSecondarySort(String[] args) throws Exception{
		Configuration conf = new Configuration();
		conf.set("mapred.textoutputformat.separator", ", ");

		Job job = Job.getInstance(conf, "Secondary sort");
		job.setJarByClass(DataAnalyzer.class);
		job.setMapperClass(MapTask.class);

		job.setMapOutputKeyClass(StationIdAndYearPair.class);
		job.setMapOutputValueClass(ValueForCompositeKey.class);

		job.setPartitionerClass(CustomPartitioner.class);

		job.setSortComparatorClass(CustomKeySortComparator.class);
		job.setGroupingComparatorClass(CustomGroupingComparator.class);

		job.setReducerClass(ReduceTask.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(args[1]));
		FileOutputFormat.setOutputPath(job, new Path(args[2]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
