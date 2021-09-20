package org.processmining.plugins.inductiveVisualMiner.performance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PerformanceLevel {
	Level[] value();

	public static enum Level {
		activity, process
	}
}
