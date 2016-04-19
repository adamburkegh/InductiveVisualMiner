package org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.miners;

import java.util.ArrayList;
import java.util.Arrays;

import org.processmining.plugins.InductiveMiner.dfgOnly.log2logInfo.IMLog2IMLogInfoLifeCycle;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinder;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIM;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIMi;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIMiEmptyLog;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIMiEmptyTrace;
import org.processmining.plugins.InductiveMiner.mining.cuts.CutFinder;
import org.processmining.plugins.InductiveMiner.mining.cuts.IMi.CutFinderIMi;
import org.processmining.plugins.InductiveMiner.mining.cuts.IMlc.CutFinderIMlc;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughFlowerWithEpsilon;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughActivityConcurrent;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughTauLoop;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterCombination;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterLoop;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterMaybeInterleaved;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterParallel;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterSequenceFiltering;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterXor;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessor;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessorInterleaved;

public class LifeCycleMiningParametersIvM extends MiningParameters {

	/*
	 * No other parameter, except mentioned in this file, has influence on the mined model
	 */
	
	public LifeCycleMiningParametersIvM() {
		//determine algorithm
		
		setLogConverter(new IMLog2IMLogInfoLifeCycle());
		
		setBaseCaseFinders(new ArrayList<BaseCaseFinder>(Arrays.asList(
				new BaseCaseFinderIMiEmptyLog(),
				new BaseCaseFinderIMiEmptyTrace(),
				new BaseCaseFinderIMi(),
				new BaseCaseFinderIM()
				)));
		
		setCutFinder(new ArrayList<CutFinder>(Arrays.asList(
				new CutFinderIMlc(),
				new CutFinderIMi()
				)));
		
		setLogSplitter(new LogSplitterCombination(
						new LogSplitterXor(), 
						new LogSplitterSequenceFiltering(), 
						new LogSplitterParallel(),
						new LogSplitterLoop(),
						new LogSplitterMaybeInterleaved(),
						new LogSplitterParallel()
		));
		
		setFallThroughs(new ArrayList<FallThrough>(Arrays.asList(
				new FallThroughActivityConcurrent(),
				new FallThroughTauLoop(true),
				new FallThroughFlowerWithEpsilon()
				)));
		
		setPostProcessors(new ArrayList<PostProcessor>(Arrays.asList(
				new PostProcessorInterleaved()
				)));
		
		//set parameters
		setNoiseThreshold((float) 0.2);
		setRepairLifeCycle(true);
	}
}
