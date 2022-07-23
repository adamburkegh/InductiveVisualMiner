package org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.inductiveVisualMiner.causal.CausalAnalysisResult;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObject;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectValues;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRow;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRowBlockAbstract;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;

public class DataRowBlockCausalGraph<C, P> extends DataRowBlockAbstract<Object, C, P> {

	@Override
	public String getName() {
		return "causal graph";
	}

	@Override
	public IvMObject<?>[] createInputObjects() {
		return new IvMObject<?>[] { IvMObject.model, IvMObject.data_analysis_causal };
	}

	@Override
	public List<DataRow<Object>> gather(IvMObjectValues inputs) {
		IvMModel model = inputs.get(IvMObject.model);
		CausalAnalysisResult causalAnalysisResult = inputs.get(IvMObject.data_analysis_causal);

		List<DataRow<Object>> result = new ArrayList<>();

		//		//output the graph in Dot format for export
		//		result.add(new DataRow<>("upper bound causal graph (DOT)",
		//				DisplayType.literal(causalAnalysisResult.toDot().toString())));
		//
		//		//show the upper bound causal graph
		//		for (Pair<Choice, Choice> edge : causalAnalysisResult.getEdges()) {
		//			DataRow<Object> row = new DataRow<Object>("potential causal edge",
		//					DisplayType.literal(edge.getA().toString(model) + "(" + edge.getA().toString(model) + ")"),
		//					DisplayType.literal(edge.getB().toString(model) + "(" + edge.getB().toString(model) + ")"));
		//			result.add(row);
		//		}

		return result;
	}
}
