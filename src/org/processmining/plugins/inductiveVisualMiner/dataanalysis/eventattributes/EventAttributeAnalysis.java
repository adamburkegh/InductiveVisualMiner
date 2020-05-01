package org.processmining.plugins.inductiveVisualMiner.dataanalysis.eventattributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.math.NumberUtils;
import org.deckfour.xes.model.XAttribute;
import org.math.plot.utils.Array;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMCanceller;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType.Type;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.traceattributes.Correlation;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IteratorWithPosition;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMMove;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;
import org.processmining.plugins.inductiveminer2.attributes.Attribute;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.THashMap;

public class EventAttributeAnalysis {

	public static enum Field {
		first {
			public String toString() {
				return "alphabetically first";
			}
		},
		last {
			public String toString() {
				return "alphabetically last";
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
		numberOfDifferentValues {
			public String toString() {
				return "number of values";
			}
		}
	}

	private Map<Attribute, EnumMap<Field, DisplayType>> attribute2data = new THashMap<>();
	private Map<Attribute, EnumMap<Field, DisplayType>> attribute2dataNegative = new THashMap<>();

	private boolean isSomethingFiltered;

	public EventAttributeAnalysis(final IvMModel model, IvMLogNotFiltered fullLog, final IvMLogFiltered logFiltered,
			AttributesInfo attributes, final IvMCanceller canceller)
			throws CloneNotSupportedException, InterruptedException {
		isSomethingFiltered = logFiltered.isSomethingFiltered();

		final IvMLogFilteredImpl logFilteredNegative = logFiltered.clone();
		logFilteredNegative.invert();

		final ConcurrentMap<Attribute, EnumMap<Field, DisplayType>> attribute2dataC = new ConcurrentHashMap<>();
		final ConcurrentMap<Attribute, EnumMap<Field, DisplayType>> attribute2dataNegativeC = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(
				Math.max(Runtime.getRuntime().availableProcessors() - 1, 1),
				new ThreadFactoryBuilder().setNameFormat("ivm-thread-eventdataanalysis-%d").build());
		try {
			for (Attribute attribute : attributes.getEventAttributes()) {
				if (isSupported(attribute)) {
					final Attribute attribute2 = attribute;
					executor.execute(new Runnable() {
						public void run() {

							if (canceller.isCancelled()) {
								return;
							}

							EnumMap<Field, DisplayType> data = createAttributeData(logFiltered, attribute2, canceller);
							if (data != null) {
								attribute2dataC.put(attribute2, data);
							}
						}
					});

					if (isSomethingFiltered) {
						executor.execute(new Runnable() {
							public void run() {

								if (canceller.isCancelled()) {
									return;
								}

								EnumMap<Field, DisplayType> dataNegative = createAttributeData(logFilteredNegative,
										attribute2, canceller);
								if (dataNegative != null) {
									attribute2dataNegativeC.put(attribute2, dataNegative);
								}
							}
						});
					}
				}
			}

			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} finally {
			executor.shutdownNow();
		}

		for (Entry<Attribute, EnumMap<Field, DisplayType>> e : attribute2dataC.entrySet()) {
			attribute2data.put(e.getKey(), e.getValue());
		}
		for (Entry<Attribute, EnumMap<Field, DisplayType>> e : attribute2dataNegativeC.entrySet()) {
			attribute2dataNegative.put(e.getKey(), e.getValue());
		}
	}

	public boolean isSomethingFiltered() {
		return isSomethingFiltered;
	}

	public Collection<Attribute> getEventAttributes() {
		return attribute2data.keySet();
	}

	public EnumMap<Field, DisplayType> getAttributeData(Attribute attribute) {
		return attribute2data.get(attribute);
	}

	public EnumMap<Field, DisplayType> getAttributeDataNegative(Attribute attribute) {
		return attribute2dataNegative.get(attribute);
	}

	private EnumMap<Field, DisplayType> createAttributeData(IvMLogFiltered logFiltered, Attribute attribute,
			IvMCanceller canceller) {
		EnumMap<Field, DisplayType> result = new EnumMap<Field, DisplayType>(Field.class);
		if (attribute.isNumeric()) {
			createAttributeDataContinuous(result, logFiltered, attribute, canceller);
		} else if (attribute.isTime()) {
			createAttributeDataTime(result, logFiltered, attribute, canceller);
		} else if (attribute.isLiteral()) {
			createAttributeDataLiteral(result, logFiltered, attribute, canceller);
		}

		if (canceller.isCancelled()) {
			return null;
		}

		return result;
	}

	private void createAttributeDataLiteral(EnumMap<Field, DisplayType> result, IvMLogFiltered logFiltered,
			Attribute attribute, IvMCanceller canceller) {
		assert !attribute.isVirtual();

		int numberOfEventsWithAttribute = 0;
		int numberOfTracesWithEventWithAttribute = 0;
		{
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						if (move.getAttributes().containsKey(attribute.getName())) {
							traceHasEvent = true;
							numberOfEventsWithAttribute++;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
			}
		}

		if (canceller.isCancelled()) {
			return;
		}

		result.put(Field.numberOfEventsWithAttribute, DisplayType.numeric(numberOfEventsWithAttribute));

		result.put(Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute));

		ArrayList<String> valueSet = new ArrayList<>(attribute.getStringValues());
		result.put(Field.numberOfDifferentValues, DisplayType.numeric(valueSet.size()));

		if (valueSet.isEmpty()) {
			result.put(Field.first, DisplayType.NA());
			result.put(Field.last, DisplayType.NA());
		} else {
			int first = 0;
			int last = 0;
			for (int i = 1; i < valueSet.size(); i++) {
				if (valueSet.get(first).toLowerCase().compareTo(valueSet.get(i).toLowerCase()) < 0) {
					first = i;
				} else if (valueSet.get(last).toLowerCase().compareTo(valueSet.get(i).toLowerCase()) > 0) {
					last = i;
				}
			}
			result.put(Field.first, DisplayType.literal(valueSet.get(first)));
			result.put(Field.last, DisplayType.literal(valueSet.get(last)));
		}
	}

	private void createAttributeDataTime(EnumMap<Field, DisplayType> result, IvMLogFiltered logFiltered,
			Attribute attribute, IvMCanceller canceller) {
		Type attributeType = Type.time;

		//gather values
		long[] valuesFiltered;
		int numberOfTracesWithEventWithAttribute = 0;
		{
			TLongArrayList values = new TLongArrayList();
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						long value = getLongValue(attribute, move);
						if (value != Long.MIN_VALUE) {
							values.add(value);
							traceHasEvent = true;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
			}

			if (canceller.isCancelled()) {
				return;
			}

			valuesFiltered = values.toArray();
		}

		if (canceller.isCancelled()) {
			return;
		}

		result.put(Field.numberOfEventsWithAttribute, DisplayType.numeric(valuesFiltered.length));

		result.put(Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute));

		//if the list is empty, better fail now and do not attempt the rest
		if (valuesFiltered.length == 0) {
			result.put(Field.min, DisplayType.NA());
			result.put(Field.average, DisplayType.NA());
			result.put(Field.median, DisplayType.NA());
			result.put(Field.max, DisplayType.NA());
			result.put(Field.standardDeviation, DisplayType.NA());
		} else {
			result.put(Field.min, DisplayType.create(attributeType, NumberUtils.min(valuesFiltered)));

			if (canceller.isCancelled()) {
				return;
			}

			BigDecimal valuesAverage = Correlation.mean(valuesFiltered);
			result.put(Field.average, DisplayType.create(attributeType, Math.round(valuesAverage.doubleValue())));

			if (canceller.isCancelled()) {
				return;
			}

			result.put(Field.median, DisplayType.create(attributeType, Math.round(Correlation.median(valuesFiltered))));

			if (canceller.isCancelled()) {
				return;
			}

			result.put(Field.max, DisplayType.create(attributeType, NumberUtils.max(valuesFiltered)));

			if (canceller.isCancelled()) {
				return;
			}

			if (result.get(Field.min).getValue() != result.get(Field.max).getValue()) {
				double standardDeviation = Correlation.standardDeviation(valuesFiltered, valuesAverage);
				result.put(Field.standardDeviation, DisplayType.duration(standardDeviation));
			}
		}
	}

	private void createAttributeDataContinuous(EnumMap<Field, DisplayType> result, IvMLogFiltered logFiltered,
			Attribute attribute, IvMCanceller canceller) {
		Type attributeType = DisplayType.fromAttribute(attribute);

		//gather values
		double[] valuesFiltered;
		int numberOfTracesWithEventWithAttribute = 0;
		{
			TDoubleArrayList values = new TDoubleArrayList();
			for (IteratorWithPosition<IvMTrace> it = logFiltered.iterator(); it.hasNext();) {
				IvMTrace trace = it.next();

				boolean traceHasEvent = false;

				for (IvMMove move : trace) {
					if (move.getAttributes() != null) {
						double value = getDoubleValue(attribute, move);
						if (value != -Double.MAX_VALUE) {
							values.add(value);
							traceHasEvent = true;
						}
					}
				}

				if (traceHasEvent) {
					numberOfTracesWithEventWithAttribute++;
				}
			}

			if (canceller.isCancelled()) {
				return;
			}

			valuesFiltered = values.toArray();
		}

		if (canceller.isCancelled()) {
			return;
		}

		result.put(Field.numberOfEventsWithAttribute, DisplayType.numeric(valuesFiltered.length));

		result.put(Field.numberOfTracesWithEventWithAttribute,
				DisplayType.numeric(numberOfTracesWithEventWithAttribute));

		//if the list is empty, better fail now and do not attempt the rest
		if (valuesFiltered.length == 0) {
			result.put(Field.min, DisplayType.NA());
			result.put(Field.average, DisplayType.NA());
			result.put(Field.median, DisplayType.NA());
			result.put(Field.max, DisplayType.NA());
			result.put(Field.standardDeviation, DisplayType.NA());
		} else {

			result.put(Field.min, DisplayType.create(attributeType, Array.min(valuesFiltered)));

			if (canceller.isCancelled()) {
				return;
			}

			BigDecimal valuesAverage = Correlation.mean(valuesFiltered);
			result.put(Field.average, DisplayType.create(attributeType, valuesAverage.doubleValue()));

			if (canceller.isCancelled()) {
				return;
			}

			result.put(Field.median, DisplayType.create(attributeType, Correlation.median(valuesFiltered)));

			if (canceller.isCancelled()) {
				return;
			}

			result.put(Field.max, DisplayType.create(attributeType, Array.max(valuesFiltered)));

			if (canceller.isCancelled()) {
				return;
			}

			if (result.get(Field.min).getValue() != result.get(Field.max).getValue()) {
				double standardDeviation = Correlation.standardDeviation(valuesFiltered, valuesAverage);
				{
					if (attribute.isTime()) {
						result.put(Field.standardDeviation, DisplayType.time(Math.round(standardDeviation)));
					} else {
						result.put(Field.standardDeviation, DisplayType.create(attributeType, standardDeviation));
					}
				}
			}
		}
	}

	private static boolean isSupported(Attribute attribute) {
		return attribute.isNumeric() || attribute.isTime();
	}

	public static double getDoubleValue(Attribute attribute, IvMMove move) {
		if (attribute.isNumeric() || attribute.isTime()) {
			XAttribute xAttribute = move.getAttributes().get(attribute.getName());
			if (xAttribute == null) {
				return -Double.MAX_VALUE;
			}
			if (attribute.isNumeric()) {
				return Attribute.parseDoubleFast(xAttribute);
			} else if (attribute.isTime()) {
				return Attribute.parseTimeFast(xAttribute);
			}
		}
		return -Double.MAX_VALUE;
	}

	public static long getLongValue(Attribute attribute, IvMMove move) {
		if (attribute.isNumeric() || attribute.isTime()) {
			XAttribute xAttribute = move.getAttributes().get(attribute.getName());
			if (xAttribute == null) {
				return Long.MIN_VALUE;
			}
			if (attribute.isNumeric()) {
				return Attribute.parseLongFast(xAttribute);
			} else if (attribute.isTime()) {
				return Attribute.parseTimeFast(xAttribute);
			}
		}
		return Long.MIN_VALUE;
	}

}