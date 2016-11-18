package org.processmining.plugins.inductiveVisualMiner.animation.renderingthread;

import java.awt.geom.AffineTransform;
import java.util.Random;

import org.processmining.plugins.inductiveVisualMiner.animation.GraphVizTokens;
import org.processmining.plugins.inductiveVisualMiner.animation.GraphVizTokensLazyIterator;
import org.processmining.plugins.inductiveVisualMiner.animation.tracecolouring.TraceColourMap;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;

/**
 * Keeps track of the external settings of the animation. Thread-safe.
 * 
 * @author sleemans
 *
 */
public class ExternalSettingsManager {
	ExternalSettings settings = null;
	private Random random = new Random();

	public class ExternalSettings {
		//resizing variables
		int width = 10;
		int height = 10;

		//rendering variables
		public IvMLogFiltered filteredLog;
		public TraceColourMap trace2colour;
		public GraphVizTokensLazyIterator tokens;
		public AffineTransform transform;

		//traceability
		int id;
	}

	public ExternalSettingsManager() {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = 10;
		newExternalSettings.height = 10;
		newExternalSettings.tokens = null;
		newExternalSettings.filteredLog = null;
		newExternalSettings.trace2colour = null;
		newExternalSettings.transform = null;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
	}

	public ExternalSettings getExternalSettings() {
		return settings;
	}

	public boolean isMostRecentSetting(int settingsId) {
		return settingsId == settings.id;
	}

	public synchronized int setImageTransformation(AffineTransform image2user) {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = settings.width;
		newExternalSettings.height = settings.height;
		newExternalSettings.tokens = settings.tokens;
		newExternalSettings.filteredLog = settings.filteredLog;
		newExternalSettings.trace2colour = settings.trace2colour;
		newExternalSettings.transform = image2user;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
		return newExternalSettings.id;
	}

	public synchronized int setSize(int width, int height) {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = width;
		newExternalSettings.height = height;
		newExternalSettings.tokens = settings.tokens;
		newExternalSettings.filteredLog = settings.filteredLog;
		newExternalSettings.trace2colour = settings.trace2colour;
		newExternalSettings.transform = settings.transform;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
		return newExternalSettings.id;
	}

	public synchronized int setTokens(GraphVizTokens animationGraphVizTokens) {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = settings.width;
		newExternalSettings.height = settings.height;
		newExternalSettings.tokens = new GraphVizTokensLazyIterator(animationGraphVizTokens);
		newExternalSettings.filteredLog = settings.filteredLog;
		newExternalSettings.trace2colour = settings.trace2colour;
		newExternalSettings.transform = settings.transform;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
		return newExternalSettings.id;
	}

	public synchronized int setFilteredLog(IvMLogFilteredImpl ivMLogFiltered) {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = settings.width;
		newExternalSettings.height = settings.height;
		newExternalSettings.tokens = settings.tokens;
		newExternalSettings.filteredLog = ivMLogFiltered;
		newExternalSettings.trace2colour = settings.trace2colour;
		newExternalSettings.transform = settings.transform;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
		return newExternalSettings.id;
	}
	
	public synchronized int setTrace2Colour(TraceColourMap trace2colour) {
		ExternalSettings newExternalSettings = new ExternalSettings();
		newExternalSettings.width = settings.width;
		newExternalSettings.height = settings.height;
		newExternalSettings.tokens = settings.tokens;
		newExternalSettings.filteredLog = settings.filteredLog;
		newExternalSettings.trace2colour = trace2colour;
		newExternalSettings.transform = settings.transform;
		newExternalSettings.id = random.nextInt();

		settings = newExternalSettings;
		return newExternalSettings.id;
	}

}
