package org.processmining.plugins.inductiveVisualMiner.dataanalysis.eventattributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.math.plot.utils.Array;
import org.processmining.plugins.inductiveVisualMiner.attributes.IvMAttributesInfo;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMCanceller;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObject;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectValues;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRow;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRowBlockComputer;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType.Type;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.traceattributes.Correlation;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IteratorWithPosition;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMMove;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;
import org.processmining.plugins.inductiveminer2.attributes.Attribute;
import org.processmining.plugins.inductiveminer2.attributes.AttributeUtils;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;

public class EventDataRowBlock<C, P> extends DataRowBlockComputer<C, P> {

	public String getName() {
		return "event-att";
	}

	public IvMObject<?>[] createInputObjects() {
		return new IvMObject<?>[] { IvMObject.ivm_attributes_info, IvMObject.aligned_log_filtered };
	}

	public List<DataRow> compute(C configuration, IvMObjectValues inputs, IvMCanceller canceller) throws Exception {
		IvMLogFiltered logFiltered = inputs.get(IvMObject.aligned_log_filtered);
		IvMAttributesInfo attributes = inputs.get(IvMObject.ivm_attributes_info);

		List<DataRow> result = new ArrayList<>();

		if (logFiltered.isSomethingFiltered()) {
			IvMLogFilteredImpl negativeLog = logFiltered.clone();
			negativeLog.invert();

			for (Attribute attribute : attributes.getEventAttributes()) {
				result.addAll(merge(createAttributeData(logFiltered, attribute, canceller),
						createAttributeData(negativeLog, attribute, canceller), canceller));
			}
		} else {
			for (Attribute attribute : attributes.getEventAttributes()) {
				result.addAll(createAttributeData(logFiltered, attribute, canceller));
			}
		}

		return result;
	}

	/**
	 * Merges a datarow from each if their names match. Assumption: each input
	 * datarow has only one value. In-place.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private List<DataRow> merge(List<DataRow> a, List<DataRow> b, IvMCanceller canceller) {
		for (DataRow dataRowA : a) {

			if (canceller.isCancelled()) {
				return a;
			}

			DataRow dataRowB = null;
			for (DataRow drB : b) {
				if (Arrays.equals(dataRowA.getNames(), drB.getNames())) {
					dataRowB = drB;
					break;
				}
			}

			if (dataRowB == null) {
				dataRowA.setValues(dataRowA.getValue(0), DisplayType.NA());
			} else {
				dataRowA.setValues(dataRowA.getValue(0), dataRowB.getValue(0));
			}
		}
		return a;
	}

	private List<DataRow> createAttributeData(IvMLogFiltered logFiltered, Attribute attribute, IvMCanceller canceller) {
		if (attribute.isNumeric()) {
			return createAttributeDataNumeric(logFiltered, attribute, canceller);
		} else if (attribute.isTime()) {
			return createAttributeDataTime(logFiltered, attribute, canceller);
		} else if (attribute.isLiteral()) {
			return createAttributeDataLiteral(logFiltered, attribute, canceller);
		} else if (attribute.isDuration()) {
			return createAttributeDataDuration(logFiltered, attribute, canceller);
		}

		List<DataRow> result = new ArrayList<>();
		result.add(new DataRow(DisplayType.literal("[not supported]"), attribute.getName(), ""));
		return result;
	}

	private List<DataRow> createAttributeDataNumeric(IvMLogFiltered logFiltered, Attribute attribute,
			IvMCanceller canceller) {
		Type attributeType = DisplayType.fromAttribute(attribute);

		List<DataRow> result = new ArrayList<>();

		//gather values
		double[] valuesFiltered;
		int numberOfTracesWithEventWithAttribute = 0;
		int numberOfEventsWithoutAttribute = 0;
		int numberOfTracesWithoutEventWithAttribute = 0;
		{
			TDoubleArrayList values = new TDoubleArrayList();
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;
				boolean traceHasEventWithout = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						double value = AttributeUtils.valueDouble(attribute, move);
						if (value != -Double.MAX_VALUE) {
							values.add(value);
							traceHasEvent = true;
						} else {
							traceHasEventWithout = true;
							numberOfEventsWithoutAttribute++;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
				if (traceHasEventWithout) {
					numberOfTracesWithoutEventWithAttribute++;
				}
			}

			if (canceller.isCancelled()) {
				return result;
			}

			valuesFiltered = values.toArray();
		}

		if (canceller.isCancelled()) {
			return result;
		}

		result.add(c(attribute, Field.numberOfEventsWithAttribute, DisplayType.numeric(valuesFiltered.length)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute)));

		result.add(c(attribute, Field.numberOfEventsWithoutAttribute,
				DisplayType.numeric(numberOfEventsWithoutAttribute)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithoutAttribute,
				DisplayType.numeric(numberOfTracesWithoutEventWithAttribute)));

		//if the list is empty, better fail now and do not attempt the rest
		if (valuesFiltered.length == 0) {
			result.add(c(attribute, Field.min, DisplayType.NA()));
			result.add(c(attribute, Field.average, DisplayType.NA()));
			result.add(c(attribute, Field.median, DisplayType.NA()));
			result.add(c(attribute, Field.max, DisplayType.NA()));
			result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
		} else {

			double min = Array.min(valuesFiltered);
			result.add(c(attribute, Field.min, DisplayType.create(attributeType, min)));

			if (canceller.isCancelled()) {
				return result;
			}

			BigDecimal valuesAverage = Correlation.mean(valuesFiltered);
			assert valuesAverage != null;
			result.add(c(attribute, Field.average, DisplayType.create(attributeType, valuesAverage.doubleValue())));

			if (canceller.isCancelled()) {
				return result;
			}

			result.add(
					c(attribute, Field.median, DisplayType.create(attributeType, Correlation.median(valuesFiltered))));

			if (canceller.isCancelled()) {
				return result;
			}

			double max = Array.max(valuesFiltered);
			result.add(c(attribute, Field.max, DisplayType.create(attributeType, max)));

			if (canceller.isCancelled()) {
				return result;
			}

			if (min != max) {
				double standardDeviation = Correlation.standardDeviation(valuesFiltered, valuesAverage);
				result.add(c(attribute, Field.standardDeviation, DisplayType.create(attributeType, standardDeviation)));
			} else {
				result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
			}
		}

		return result;
	}

	private List<DataRow> createAttributeDataTime(IvMLogFiltered logFiltered, Attribute attribute,
			IvMCanceller canceller) {
		Type attributeType = Type.time;

		List<DataRow> result = new ArrayList<>();

		//gather values
		long[] valuesFiltered;
		int numberOfTracesWithEventWithAttribute = 0;
		int numberOfEventsWithoutAttribute = 0;
		int numberOfTracesWithoutEventWithAttribute = 0;
		{
			TLongArrayList values = new TLongArrayList();
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;
				boolean traceHasEventWithout = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						long value = AttributeUtils.valueLong(attribute, move);
						if (value != Long.MIN_VALUE) {
							values.add(value);
							traceHasEvent = true;
						} else {
							traceHasEventWithout = true;
							numberOfEventsWithoutAttribute++;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
				if (traceHasEventWithout) {
					numberOfTracesWithoutEventWithAttribute++;
				}
			}

			if (canceller.isCancelled()) {
				return result;
			}

			valuesFiltered = values.toArray();
		}

		if (canceller.isCancelled()) {
			return result;
		}

		result.add(c(attribute, Field.numberOfEventsWithAttribute, DisplayType.numeric(valuesFiltered.length)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute)));

		result.add(c(attribute, Field.numberOfEventsWithoutAttribute,
				DisplayType.numeric(numberOfEventsWithoutAttribute)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithoutAttribute,
				DisplayType.numeric(numberOfTracesWithoutEventWithAttribute)));

		//if the list is empty, better fail now and do not attempt the rest
		if (valuesFiltered.length == 0) {
			result.add(c(attribute, Field.min, DisplayType.NA()));
			result.add(c(attribute, Field.average, DisplayType.NA()));
			result.add(c(attribute, Field.median, DisplayType.NA()));
			result.add(c(attribute, Field.max, DisplayType.NA()));
			result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
		} else {
			long min = NumberUtils.min(valuesFiltered);
			result.add(c(attribute, Field.min, DisplayType.create(attributeType, min)));

			if (canceller.isCancelled()) {
				return result;
			}

			BigDecimal valuesAverage = Correlation.mean(valuesFiltered);
			result.add(c(attribute, Field.average,
					DisplayType.create(attributeType, Math.round(valuesAverage.doubleValue()))));

			if (canceller.isCancelled()) {
				return result;
			}

			result.add(c(attribute, Field.median,
					DisplayType.create(attributeType, Math.round(Correlation.median(valuesFiltered)))));

			if (canceller.isCancelled()) {
				return result;
			}

			long max = NumberUtils.max(valuesFiltered);
			result.add(c(attribute, Field.max, DisplayType.create(attributeType, max)));

			if (canceller.isCancelled()) {
				return result;
			}

			if (min != max) {
				double standardDeviation = Correlation.standardDeviation(valuesFiltered, valuesAverage);
				result.add(c(attribute, Field.standardDeviation, DisplayType.duration(Math.round(standardDeviation))));
			} else {
				result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
			}
		}

		return result;
	}

	private List<DataRow> createAttributeDataLiteral(IvMLogFiltered logFiltered, Attribute attribute,
			IvMCanceller canceller) {
		assert !attribute.isVirtual();

		List<DataRow> result = new ArrayList<>();

		int numberOfEventsWithAttribute = 0;
		int numberOfTracesWithEventWithAttribute = 0;
		int numberOfEventsWithoutAttribute = 0;
		int numberOfTracesWithoutEventWithAttribute = 0;
		{
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;
				boolean traceHasEventWithout = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						if (move.getAttributes().containsKey(attribute.getName())) {
							traceHasEvent = true;
							numberOfEventsWithAttribute++;
						} else {
							traceHasEventWithout = true;
							numberOfEventsWithoutAttribute++;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
				if (traceHasEventWithout) {
					numberOfTracesWithoutEventWithAttribute++;
				}
			}
		}

		if (canceller.isCancelled()) {
			return result;
		}

		result.add(c(attribute, Field.numberOfEventsWithAttribute, DisplayType.numeric(numberOfEventsWithAttribute)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute)));

		result.add(c(attribute, Field.numberOfEventsWithoutAttribute,
				DisplayType.numeric(numberOfEventsWithoutAttribute)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithoutAttribute,
				DisplayType.numeric(numberOfTracesWithoutEventWithAttribute)));

		ArrayList<String> valueSet = new ArrayList<>(attribute.getStringValues());
		result.add(c(attribute, Field.numberOfDifferentValues, DisplayType.numeric(valueSet.size())));

		if (valueSet.isEmpty()) {
			result.add(c(attribute, Field.first, DisplayType.NA()));
			result.add(c(attribute, Field.last, DisplayType.NA()));
		} else {
			int first = 0;
			int last = 0;
			for (int i = 1; i < valueSet.size(); i++) {
				if (valueSet.get(first).toLowerCase().compareTo(valueSet.get(i).toLowerCase()) > 0) {
					first = i;
				} else if (valueSet.get(last).toLowerCase().compareTo(valueSet.get(i).toLowerCase()) < 0) {
					last = i;
				}
			}
			result.add(c(attribute, Field.first, DisplayType.literal(valueSet.get(first))));
			result.add(c(attribute, Field.last, DisplayType.literal(valueSet.get(last))));
		}

		return result;
	}

	private List<DataRow> createAttributeDataDuration(IvMLogFiltered logFiltered, Attribute attribute,
			IvMCanceller canceller) {
		Type attributeType = Type.duration;

		List<DataRow> result = new ArrayList<>();

		//gather values
		long[] valuesFiltered;
		int numberOfTracesWithEventWithAttribute = 0;
		int numberOfEventsWithoutAttribute = 0;
		int numberOfTracesWithoutEventWithAttribute = 0;
		{
			TLongArrayList values = new TLongArrayList();
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;
				boolean traceHasEventWithout = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						long value = AttributeUtils.valueLong(attribute, move);
						if (value != Long.MIN_VALUE) {
							values.add(value);
							traceHasEvent = true;
						} else {
							traceHasEventWithout = true;
							numberOfEventsWithoutAttribute++;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
				if (traceHasEventWithout) {
					numberOfTracesWithoutEventWithAttribute++;
				}
			}

			if (canceller.isCancelled()) {
				return result;
			}

			valuesFiltered = values.toArray();
		}

		if (canceller.isCancelled()) {
			return result;
		}

		result.add(c(attribute, Field.numberOfEventsWithAttribute, DisplayType.numeric(valuesFiltered.length)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute)));

		result.add(c(attribute, Field.numberOfEventsWithoutAttribute,
				DisplayType.numeric(numberOfEventsWithoutAttribute)));

		result.add(c(attribute, Field.numberOfTracesWithEventWithoutAttribute,
				DisplayType.numeric(numberOfTracesWithoutEventWithAttribute)));

		//if the list is empty, better fail now and do not attempt the rest
		if (valuesFiltered.length == 0) {
			result.add(c(attribute, Field.min, DisplayType.NA()));
			result.add(c(attribute, Field.average, DisplayType.NA()));
			result.add(c(attribute, Field.median, DisplayType.NA()));
			result.add(c(attribute, Field.max, DisplayType.NA()));
			result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
		} else {
			long min = NumberUtils.min(valuesFiltered);
			result.add(c(attribute, Field.min, DisplayType.create(attributeType, min)));

			if (canceller.isCancelled()) {
				return result;
			}

			BigDecimal valuesAverage = Correlation.mean(valuesFiltered);
			result.add(c(attribute, Field.average,
					DisplayType.create(attributeType, Math.round(valuesAverage.doubleValue()))));

			if (canceller.isCancelled()) {
				return result;
			}

			result.add(c(attribute, Field.median,
					DisplayType.create(attributeType, Math.round(Correlation.median(valuesFiltered)))));

			if (canceller.isCancelled()) {
				return result;
			}

			long max = NumberUtils.max(valuesFiltered);
			result.add(c(attribute, Field.max, DisplayType.create(attributeType, max)));

			if (canceller.isCancelled()) {
				return result;
			}

			if (min != max) {
				double standardDeviation = Correlation.standardDeviation(valuesFiltered, valuesAverage);
				result.add(c(attribute, Field.standardDeviation,
						DisplayType.create(attributeType, Math.round(standardDeviation))));
			} else {
				result.add(c(attribute, Field.standardDeviation, DisplayType.NA()));
			}
		}

		return result;
	}

	private static DataRow c(Attribute attribute, Field field, DisplayType value) {
		return new DataRow(value, attribute.getName(), field.toString());
	}

	public static enum Field {
		first {
			public String toString() {
				return "first (alphabetically)";
			}
		},
		last {
			public String toString() {
				return "last (alphabetically)";
			}
		},
		min {
			public String toString() {
				return "minimum";
			}
		},
		average {
			public String toString() {
				return "average";
			}
		},
		median {
			public String toString() {
				return "median";
			}
		},
		max {
			public String toString() {
				return "maximum";
			}
		},
		standardDeviation {
			public String toString() {
				return "standard deviation";
			}
		},
		numberOfDifferentValues {
			public String toString() {
				return "number of distinct values";
			}
		},
		numberOfEventsWithAttribute {
			public String toString() {
				return "events with attribute";
			}
		},
		numberOfTracesWithEventWithAttribute {
			public String toString() {
				return "traces having event with attribute";
			}
		},
		numberOfEventsWithoutAttribute {
			public String toString() {
				return "events without attribute";
			}
		},
		numberOfTracesWithEventWithoutAttribute {
			public String toString() {
				return "traces having event without attribute";
			}
		},
	}

}
