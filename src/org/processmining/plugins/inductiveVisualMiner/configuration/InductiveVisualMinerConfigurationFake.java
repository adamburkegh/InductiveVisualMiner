package org.processmining.plugins.inductiveVisualMiner.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerPanel;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.chain.Chain;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMFilter;
import org.processmining.plugins.inductiveVisualMiner.mode.Mode;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupItemActivity;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupItemLog;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupItemLogMove;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupItemModelMove;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupItemStartEnd;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerWrapper;

public class InductiveVisualMinerConfigurationFake implements InductiveVisualMinerConfiguration {

	private InductiveVisualMinerConfigurationDefault phantom;
	private Chain chain;
	private InductiveVisualMinerState state;
	private InductiveVisualMinerPanel panel;
	private ProMCanceller canceller;
	private Executor executor;
	private List<VisualMinerWrapper> discoveryTechniques;

	public InductiveVisualMinerConfigurationFake(InductiveVisualMinerState state, InductiveVisualMinerPanel panel,
			ProMCanceller canceller, Executor executor) {
		this.executor = executor;
		this.canceller = canceller;
		this.state = state;
		this.panel = panel;
		phantom = new InductiveVisualMinerConfigurationDefault(state.getXLog(), canceller, executor);
		this.discoveryTechniques = phantom.getDiscoveryTechniques();
	}

	public List<VisualMinerWrapper> getDiscoveryTechniques() {
		return discoveryTechniques;
	}

	public VisualMinerWrapper[] getDiscoveryTechniquesArray() {
		VisualMinerWrapper[] result = new VisualMinerWrapper[discoveryTechniques.size()];
		return discoveryTechniques.toArray(result);
	}

	public List<IvMFilter> getPreMiningFilters() {
		return phantom.getPreMiningFilters();
	}

	public List<IvMFilter> getHighlightingFilters() {
		return phantom.getHighlightingFilters();
	}

	public List<Mode> getModes() {
		return phantom.getModes();
	}

	public Mode[] getModesArray() {
		return phantom.getModesArray();
	}

	public InductiveVisualMinerState getState() {
		return state;
	}

	public InductiveVisualMinerPanel getPanel() {
		return panel;
	}

	public Chain getChain() {
		if (chain == null) {
			chain = phantom.createChain(state, panel, canceller, executor, getPreMiningFilters(),
					getHighlightingFilters());
		}
		return chain;
	}

	public void setDiscoveryTechniques(List<VisualMinerWrapper> discoveryTechniques) {
		this.discoveryTechniques = discoveryTechniques;
	}

	public void setDiscoveryTechniques(VisualMinerWrapper[] miners) {
		this.discoveryTechniques = new ArrayList<>(Arrays.asList(miners));
	}

	public List<PopupItemActivity> getPopupItemsActivity() {
		return phantom.getPopupItemsActivity();
	}

	public List<PopupItemStartEnd> getPopupItemsStartEnd() {
		return phantom.getPopupItemsStartEnd();
	}

	public List<PopupItemLogMove> getPopupItemsLogMove() {
		return phantom.getPopupItemsLogMove();
	}

	public List<PopupItemModelMove> getPopupItemsModelMove() {
		return phantom.getPopupItemsModelMove();
	}

	public List<PopupItemLog> getPopupItemsLog() {
		return phantom.getPopupItemsLog();
	}

}