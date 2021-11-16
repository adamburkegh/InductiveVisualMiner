package org.processmining.plugins.inductiveVisualMiner.chain;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.CausalAnalysis;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.CausalAnalysisResult;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.CausalDataTable;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.CausalGraph;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.DirectlyFollowsModel2CausalGraph;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal.EfficientTree2CausalGraph;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFiltered;

public class Cl22AdvancedAnalysisCausal<C> extends DataChainLinkComputationAbstract<C> {

	public String getName() {
		return "Cl22 causal";
	}

	public String getStatusBusyMessage() {
		return "Computing causalities..";
	}

	public IvMObject<?>[] createInputObjects() {
		return new IvMObject<?>[] { IvMObject.model, IvMObject.aligned_log_filtered, IvMObject.data_analyses_delay,
				IvMObject.selected_causal_enabled };
	}

	public IvMObject<?>[] createOutputObjects() {
		return new IvMObject<?>[] { IvMObject.data_analysis_causal };
	}

	public IvMObjectValues execute(C configuration, IvMObjectValues inputs, IvMCanceller canceller) throws Exception {
		if (inputs.get(IvMObject.selected_causal_enabled)) {
			IvMModel model = inputs.get(IvMObject.model);
			IvMLogFiltered logFiltered = inputs.get(IvMObject.aligned_log_filtered);

			//compute causal objects
			Pair<CausalGraph, CausalDataTable> p;
			if (model.isTree()) {
				p = EfficientTree2CausalGraph.convert(model.getTree(), logFiltered);
			} else {
				p = DirectlyFollowsModel2CausalGraph.convert(model.getDfg(), logFiltered);
			}

			//		System.out.println(p);
			//
//			try {
//				String name = "bpic12-a-dfm";
//				FileUtils.writeStringToFile(
//						new File("/home/sander/Documents/svn/49 - causality in process mining - niek/experiments/"
//								+ name + ".dot"),
//						p.getA().toDot().toString());
//				FileUtils.writeStringToFile(
//						new File("/home/sander/Documents/svn/49 - causality in process mining - niek/experiments/"
//								+ name + ".csv"),
//						p.getB().toString(-1));
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}

			//perform the analysis
			CausalAnalysisResult analysisResult = CausalAnalysis.analyse(p);

			return new IvMObjectValues().//
					s(IvMObject.data_analysis_causal, analysisResult);
		} else {
			return null;
		}
	}
}