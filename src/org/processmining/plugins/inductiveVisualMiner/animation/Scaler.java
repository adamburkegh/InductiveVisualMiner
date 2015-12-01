package org.processmining.plugins.inductiveVisualMiner.animation;

import nl.tue.astar.AStarThread.Canceller;

import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMMove;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;

public class Scaler {
	private final double animationDuration;
	private final double initialisationInLogTime; //the time a trace spends before reaching the first node (and after finishing the last)
	private final double min;
	private final double max;
	private final boolean correctTime; //denotes whether the time comes from the event log or is randomly generated

	public static Scaler fromLog(final IvMLog log, final double fadeDurationInUserTime,
			final double animationDurationInUserTime, final Canceller canceller) {
		double logMin = Long.MAX_VALUE;
		double logMax = Long.MIN_VALUE;
		for (IvMTrace trace : log) {
			for (IvMMove move : trace) {
				if (move.getLogTimestamp() != null) {
					logMin = Math.min(logMin, move.getLogTimestamp());
					logMax = Math.max(logMax, move.getLogTimestamp());
				}
			}
			if (canceller.isCancelled()) {
				return null;
			}
		}

		if (logMin == Long.MAX_VALUE || logMax == Long.MIN_VALUE) {
			return null;
		}

		//account for the fading time
		double logDurationInLogTime = logMax - logMin;
		double initialisationInLogTime = (logDurationInLogTime * fadeDurationInUserTime)
				/ (animationDurationInUserTime - 2 * fadeDurationInUserTime);

		return new Scaler(animationDurationInUserTime, initialisationInLogTime, logMin - initialisationInLogTime,
				logMax + initialisationInLogTime, true);
	}

	public static Scaler fromValues(final double animationDuration) {
		return new Scaler(animationDuration, 0, 0, animationDuration, false);
	}

	private Scaler(final double animationDuration, double initialisationInLogTime, final double min, final double max,
			boolean correctTime) {
		this.min = min;
		this.max = max;
		this.animationDuration = animationDuration;
		this.initialisationInLogTime = initialisationInLogTime;
		this.correctTime = correctTime;
	}

	public Double logTime2UserTime(Double logTime) {
		if (logTime == null) {
			return null;
		}
		if (max == min) {
			return animationDuration * logTime;
		}
		return animationDuration * (logTime - min) / (max - 1.0 * min);
	}

	public Double logTime2UserTime(Long logTime) {
		if (logTime == null) {
			return null;
		}
		if (max == min) {
			return animationDuration * logTime;
		}
		return animationDuration * (logTime - min) / (max - 1.0 * min);
	}

	public Double userTime2LogTime(Double userTime) {
		if (userTime == null) {
			return null;
		}
		if (max == min) {
			return userTime / animationDuration;
		}
		return (userTime / (1.0 * animationDuration)) * (max - 1.0 * min) + min;
	}
	
	public Double userTime2Fraction(Double userTime) {
		Double logTime = userTime2LogTime(userTime);
		if (logTime == null) {
			return null;
		}
		if (max == min) {
			return animationDuration * logTime;
		}
		return (logTime - min) / (max - 1.0 * min);
	}

	public double getMinInUserTime() {
		return 0;
	}

	public double getMaxInUserTime() {
		return animationDuration;
	}

	public double getMinInLogTime() {
		return min;
	}

	public double getMaxInLogTime() {
		return max;
	}

	public double getInitialisationInLogTime() {
		return initialisationInLogTime;
	}

	/**
	 * 
	 * @return whether the timing is derived from the log (true) or randomly
	 *         generated (false).
	 */
	public boolean isCorrectTime() {
		return correctTime;
	}
}