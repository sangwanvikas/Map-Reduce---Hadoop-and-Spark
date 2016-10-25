package pkg.ClimateAnalyzer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

// This class represents custom composite key writable
public class StationIdAndYearPair implements Writable, WritableComparable<StationIdAndYearPair> {

	private Text stationId = new Text();; // natural key
	private IntWritable year = new IntWritable(); // secondary key

	public StationIdAndYearPair() {

	}

	public StationIdAndYearPair(Text stationId, IntWritable year) {
		this.stationId = stationId;
		this.year = year;
	}

	/**
	 * This comparator controls the sort order of the keys.
	 */
	public int compareTo(StationIdAndYearPair pair) {
		return 0;
	}

	public void write(DataOutput out) throws IOException {
		stationId.write(out);
		year.write(out);
	}

	public void readFields(DataInput in) throws IOException {
		stationId.readFields(in);
		year.readFields(in);
	}

	// Getters, Settes and ToString() methods below-
	public Text getStationId() {
		return stationId;
	}

	public void setStationId(Text stationId) {
		this.stationId = stationId;
	}

	public IntWritable getYear() {
		return year;
	}

	public void setYear(IntWritable year) {
		this.year = year;
	}

	@Override
	public String toString() {
		return "StationIdAndYearPair [stationId=" + stationId + ", year=" + year + "]";
	}

}
