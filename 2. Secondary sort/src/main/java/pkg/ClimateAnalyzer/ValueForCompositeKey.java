package pkg.ClimateAnalyzer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

// This class represents value for the composite key.
public class ValueForCompositeKey implements Writable, WritableComparable<ValueForCompositeKey> {
	private IntWritable year;
	private Text temperatureType;
	private IntWritable temperatureValue;

	public ValueForCompositeKey() {
		year = new IntWritable();
		temperatureType = new Text();
		temperatureValue = new IntWritable();
	}

	public ValueForCompositeKey(IntWritable year, Text temperatureType, IntWritable temperatureValue) {
		this.year = year;
		this.temperatureType = temperatureType;
		this.temperatureValue = temperatureValue;
	}

	public int compareTo(ValueForCompositeKey value) {
		return 0;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		year.write(out);
		temperatureType.write(out);
		temperatureValue.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		year.readFields(in);
		temperatureType.readFields(in);
		temperatureValue.readFields(in);
	}

	// Getters, Settes and ToString() methods below-
	public IntWritable getYear() {
		return year;
	}

	public void setYear(IntWritable year) {
		this.year = year;
	}

	public Text getTemperatureType() {
		return temperatureType;
	}

	public void setTemperatureType(Text temperatureType) {
		this.temperatureType = temperatureType;
	}

	public IntWritable getTemperatureValue() {
		return temperatureValue;
	}

	public void setTemperatureValue(IntWritable temperatureValue) {
		this.temperatureValue = temperatureValue;
	}

	@Override
	public String toString() {
		return "ValueForCompositeKey [year=" + year + ", temperatureType=" + temperatureType + ", temperatureValue="
				+ temperatureValue + "]";
	}

}
