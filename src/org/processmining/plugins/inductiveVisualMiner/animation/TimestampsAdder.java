package org.processmining.plugins.inductiveVisualMiner.animation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.InductiveMiner.mining.logs.IMTrace2;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedLog;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedTrace;
import org.processmining.plugins.inductiveVisualMiner.alignment.Move;

public class TimestampsAdder {

	public static double animationDuration = 20;
	public static double beginEndEdgeDuration = 1;
	private static Random random = new Random(123);

	public static List<XEventClass> getTraceLogProjection(IMTrace2 trace, XLogInfo xLogInfo) {
		List<XEventClass> lTrace = new ArrayList<>();
		for (XEvent event : trace) {
			lTrace.add(xLogInfo.getEventClasses().getClassOf(event));
		}
		return lTrace;
	}

	/*
	 * Make a log-projection hashmap
	 */
	public static HashMap<List<XEventClass>, AlignedTrace> getIMTrace2AlignedTrace(AlignedLog aLog) {
		HashMap<List<XEventClass>, AlignedTrace> result = new HashMap<>();
		for (AlignedTrace aTrace : aLog) {
			List<XEventClass> trace = new ArrayList<>();
			for (Move m : aTrace) {
				if (m.getEventClass() != null) {
					trace.add(m.getEventClass());
				}
			}
			result.put(trace, aTrace);
		}
		return result;
	}

	public static Long getTimestamp(XEvent event) {
		Date date = XTimeExtension.instance().extractTimestamp(event);
		if (date != null) {
			return date.getTime();
		}
		return null;
	}
	
	public static String toString(Long timestamp) {
		if (timestamp != null) {
			Date date = new Date(timestamp);
			if (date.getTime() % 1000 != 0) {
				return (new SimpleDateFormat ("dd-MM-yyyy HH:mm:ss:SSS")).format(date);
			} else if (date.getSeconds() != 0) {
				return (new SimpleDateFormat ("dd-MM-yyyy HH:mm:ss")).format(date);
			} else if (date.getMinutes() != 0) {
				return (new SimpleDateFormat ("dd-MM-yyyy HH:mm")).format(date);
			} else if (date.getHours() != 0) {
				return (new SimpleDateFormat ("dd-MM-yyyy HHh")).format(date);
			} else {
				return (new SimpleDateFormat ("dd-MM-yyyy")).format(date);
			}
		} else {
			return null;
		}
	}
}
