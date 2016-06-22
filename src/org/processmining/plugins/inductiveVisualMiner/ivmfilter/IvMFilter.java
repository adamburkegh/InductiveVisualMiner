package org.processmining.plugins.inductiveVisualMiner.ivmfilter;

import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.AttributesInfo;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;

/**
 * Do not extend this class directly. Rather, use PreMiningEventFilter,
 * PreMiningTraceFilter or HighlightingFilter.
 * 
 * @author sleemans
 *
 */
public abstract class IvMFilter {

	/**
	 * Constructor. User is waiting when this function is called.
	 */
	public IvMFilter() {

	}

	/**
	 * Returns the name of this filter.
	 * 
	 * @return
	 */
	public abstract String getName() throws Exception;

	/**
	 * Initialises the JPanel containing the filter settings. Called
	 * asynchronously. changed.
	 * 
	 * @return
	 */
	public abstract IvMFilterGui createGui(AttributesInfo attributesInfo) throws Exception;

	/**
	 * Returns whether this filter is actually filtering something. If this
	 * method returns false, no other filtering function will be called and the
	 * entire log will be used.
	 * 
	 * @return
	 */
	protected abstract boolean isEnabled() throws Exception;

	/**
	 * This function is called when the user updates a filter and the filtering
	 * has to be recomputed.
	 */
	protected final void update() {
		if (onUpdate != null) {
			onUpdate.run();
		}
	}

	//private methods
	private IvMFilterGui panel = null;
	private Runnable onUpdate = null;
	private boolean enabledFilter = false;

	/**
	 * Called asynchronously: do not alter the gui directly. Return whether
	 * something changed in the selection and an update is necessary (do not
	 * call update() yourself).
	 * 
	 * @param log
	 * @param ivmLog
	 * @throws Exception
	 */
	protected abstract boolean fillGuiWithLog(IMLog log, IvMLog ivmLog) throws Exception;

	public final void createFilterGui(Runnable onUpdate, AttributesInfo attributesInfo) {
		this.onUpdate = onUpdate;
		try {
			panel = createGui(attributesInfo);
		} catch (Exception e) {
			panel = null;
			e.printStackTrace();
		}
	}

	public final boolean swapEnabledFilter() {
		enabledFilter = !enabledFilter;
		return enabledFilter;
	}

	public final boolean isEnabledFilter() {
		try {
			return enabledFilter && (panel != null) && isEnabled();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public final String getFilterName() {
		try {
			return getName();
		} catch (Exception e) {
			e.printStackTrace();
			return "misbehaving filter plug-in";
		}
	}

	public final IvMFilterGui getPanel() {
		return panel;
	}
}
