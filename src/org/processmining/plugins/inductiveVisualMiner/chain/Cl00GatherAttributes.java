package org.processmining.plugins.inductiveVisualMiner.chain;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.AttributeClassifiers;
import org.processmining.plugins.InductiveMiner.AttributeClassifiers.AttributeClassifier;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.AttributesInfo;

public class Cl00GatherAttributes extends
		ChainLink<XLog, Triple<AttributesInfo, AttributeClassifier, AttributeClassifier[]>> {

	protected XLog generateInput(InductiveVisualMinerState state) {
		return state.getSortedXLog();
	}

	protected Triple<AttributesInfo, AttributeClassifier, AttributeClassifier[]> executeLink(XLog input,
			IvMCanceller canceller) throws Exception {
		AttributesInfo info = new AttributesInfo(input);
		String[] attributes = info.getEventAttributes();

		Pair<AttributeClassifier[], AttributeClassifier> p = AttributeClassifiers.getAttributeClassifiers(input,
				attributes, true);
		AttributeClassifier[] attributeClassifiers = p.getA();
		AttributeClassifier firstClassifier = p.getB();

		return Triple.of(info, firstClassifier, attributeClassifiers);
	}

	protected void processResult(Triple<AttributesInfo, AttributeClassifier, AttributeClassifier[]> result,
			InductiveVisualMinerState state) {
		state.setAttributesInfo(result.getA(), result.getB(), result.getC());
		state.setClassifier(AttributeClassifiers.constructClassifier(result.getB()));
	}
}
