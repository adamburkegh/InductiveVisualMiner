package org.processmining.plugins.inductiveVisualMiner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.processmining.cohortanalysis.cohort.Cohort;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.InductiveMiner.AttributeClassifiers.AttributeClassifier;
import org.processmining.plugins.InductiveMiner.Function;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotElement;
import org.processmining.plugins.graphviz.visualisation.export.Exporter;
import org.processmining.plugins.graphviz.visualisation.listeners.MouseInElementsChangedListener;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignment.InductiveVisualMinerAlignment;
import org.processmining.plugins.inductiveVisualMiner.animation.AnimationEnabledChangedListener;
import org.processmining.plugins.inductiveVisualMiner.animation.AnimationTimeChangedListener;
import org.processmining.plugins.inductiveVisualMiner.animation.GraphVizTokens;
import org.processmining.plugins.inductiveVisualMiner.animation.Scaler;
import org.processmining.plugins.inductiveVisualMiner.animation.renderingthread.RendererFactory;
import org.processmining.plugins.inductiveVisualMiner.attributes.IvMAttributesInfo;
import org.processmining.plugins.inductiveVisualMiner.chain.Cl04FilterLogOnActivities;
import org.processmining.plugins.inductiveVisualMiner.chain.Cl09LayoutAlignment;
import org.processmining.plugins.inductiveVisualMiner.chain.Cl13FilterNodeSelection;
import org.processmining.plugins.inductiveVisualMiner.chain.DataChain;
import org.processmining.plugins.inductiveVisualMiner.chain.DataChainLinkComputation;
import org.processmining.plugins.inductiveVisualMiner.chain.DataChainLinkGui;
import org.processmining.plugins.inductiveVisualMiner.chain.DataState;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObject;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectCarteBlanche;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectValues;
import org.processmining.plugins.inductiveVisualMiner.chain.OnException;
import org.processmining.plugins.inductiveVisualMiner.chain.OnStatus;
import org.processmining.plugins.inductiveVisualMiner.configuration.InductiveVisualMinerConfiguration;
import org.processmining.plugins.inductiveVisualMiner.configuration.InductiveVisualMinerConfigurationDefault;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.cohorts.CohortAnalysis2HighlightingFilterHandler;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.cohorts.CohortAnalysisTableFactory;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.cohorts.HighlightingFilter2CohortAnalysisHandler;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.eventattributes.EventAttributeAnalysisTableFactory;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.logattributes.LogAttributeAnalysis;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.logattributes.LogAttributeAnalysisTableFactory;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.traceattributes.TraceAttributeAnalysisTableFactory;
import org.processmining.plugins.inductiveVisualMiner.export.ExportAlignment;
import org.processmining.plugins.inductiveVisualMiner.export.ExportAlignment.Type;
import org.processmining.plugins.inductiveVisualMiner.export.ExportModel;
import org.processmining.plugins.inductiveVisualMiner.export.ExporterAvi;
import org.processmining.plugins.inductiveVisualMiner.export.ExporterDataAnalyses;
import org.processmining.plugins.inductiveVisualMiner.export.ExporterModelStatistics;
import org.processmining.plugins.inductiveVisualMiner.export.ExporterTraceData;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.InputFunction;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.ResourceTimeUtils;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.UserStatus;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecorator;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMHighlightingFiltersController;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMPreMiningFiltersController;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.highlightingfilter.HighlightingFiltersView;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.mode.Mode;
import org.processmining.plugins.inductiveVisualMiner.mode.ModePaths;
import org.processmining.plugins.inductiveVisualMiner.popup.LogPopupListener;
import org.processmining.plugins.inductiveVisualMiner.popup.PopupPopulator;
import org.processmining.plugins.inductiveVisualMiner.tracecolouring.TraceColourMap;
import org.processmining.plugins.inductiveVisualMiner.tracecolouring.TraceColourMapFixed;
import org.processmining.plugins.inductiveVisualMiner.tracecolouring.TraceColourMapSettings;
import org.processmining.plugins.inductiveVisualMiner.traceview.TraceViewEventColourMap;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerWrapper;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.miners.Miner;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotNode;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

import com.kitfox.svg.SVGDiagram;

public class InductiveVisualMinerController {

	private final InductiveVisualMinerPanel panel;
	private final DataState state;
	private final InductiveVisualMinerConfiguration configuration;
	private final DataChain chain;
	private final PluginContext context;
	private final UserStatus userStatus;

	private DataChainLinkGui updatePopups;
	//these fields are time critical and should not go via the state
	private Scaler animationScaler = null;
	private Mode animationMode = null;
	private AlignedLogVisualisationData animationVisualisationData = null;

	//preferences
	private static final Preferences preferences = Preferences.userRoot()
			.node("org.processmining.inductivevisualminer");
	public static final String playAnimationOnStartupKey = "playanimationonstartup";

	public InductiveVisualMinerController(final PluginContext context,
			final InductiveVisualMinerConfiguration configuration, final ProMCanceller canceller) {
		this.state = configuration.getState();
		state.setConfiguration(configuration);
		this.configuration = configuration;
		this.panel = configuration.getPanel();
		this.userStatus = new UserStatus();
		this.context = context;
		chain = configuration.getChain();

		chain.setObject(IvMObject.carte_blanche, new IvMObjectCarteBlanche(state)); //carte blanche allows requests for objects without blocking execution

		//initialise gui handlers
		initGui(canceller, configuration);

		//set up the controller view
		chain.setOnChange(new Runnable() {
			public void run() {
				panel.getControllerView().pushCompleteChainLinks(chain);
			}
		});

		//set up exception handling
		chain.setOnException(new OnException() {
			public void onException(Exception e) {
				setStatus("- error - aborted -", 0);
			}
		});

		//set up status handling
		chain.setOnStatus(new OnStatus() {
			public void startComputation(DataChainLinkComputation chainLink) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setStatus(chainLink.getStatusBusyMessage(), chainLink.hashCode());
					}
				});
			}

			public void endComputation(DataChainLinkComputation chainLink) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setStatus(null, chainLink.hashCode());
					}
				});
			}
		});

		//start the chain
		chain.setObject(IvMObject.input_log, state.getXLog());
	}

	/**
	 * Given panel and state are ignored.
	 * 
	 * @param context
	 * @param panel
	 * @param state
	 * @param canceller
	 */
	@Deprecated
	public InductiveVisualMinerController(final PluginContext context, final InductiveVisualMinerPanel panel,
			final InductiveVisualMinerState state, ProMCanceller canceller) {
		this(context, new InductiveVisualMinerConfigurationDefault(state.getXLog(), canceller, context.getExecutor()),
				canceller);
	}

	protected void initGui(final ProMCanceller canceller, InductiveVisualMinerConfiguration configuration) {

		initGuiPopups();

		initGuiClassifiers();

		initGuiMiner();

		initGuiAlignment();

		initGuiAnimation();

		initGuiHistogram();

		//model editor
		panel.getEditModelView().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() instanceof IvMModel) {
					chain.setObject(IvMObject.model, (IvMModel) e.getSource());
				}
			}
		});

		//node selection changed
		panel.setOnSelectionChanged(new InputFunction<Selection>() {
			public void call(Selection input) throws Exception {
				chain.setObject(IvMObject.selected_model_selection, input);
			}
		});

		//graph direction changed
		panel.setOnGraphDirectionChanged(new Runnable() {
			public void run() {
				chain.setObject(IvMObject.selected_graph_user_settings, panel.getGraph().getUserSettings());
			}
		});
		panel.getGraph().getUserSettings().setDirection(GraphDirection.leftRight);
		chain.setObject(IvMObject.selected_graph_user_settings, panel.getGraph().getUserSettings());

		//animation enabled/disabled
		panel.setOnAnimationEnabledChanged(new AnimationEnabledChangedListener() {
			public boolean animationEnabledChanged() {
				boolean enable = !state.hasObject(IvMObject.selected_animation_enabled);
				chain.setObject(IvMObject.selected_animation_enabled, enable);
				preferences.putBoolean(playAnimationOnStartupKey, enable);

				if (!enable) {
					//animation gets disabled
					panel.getGraph().setAnimationEnabled(false);
					setAnimationStatus(panel, "animation disabled", true);
					panel.repaint();
					return false;
				} else {
					//animation gets enabled
					return true;
				}
			}
		});
		if (preferences.getBoolean(playAnimationOnStartupKey, true)) {
			state.putObject(IvMObject.selected_animation_enabled, true);
		}

		//set model export button
		panel.getSaveModelButton().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

				//store the resulting Process tree or Petri net
				String name = XConceptExtension.instance().extractName(state.getObject(IvMObject.sorted_log));
				IvMModel model = state.getObject(IvMObject.model);

				Object[] options;
				if (model.isTree()) {
					options = new Object[5];
					options[0] = "Petri net";
					options[1] = "Accepting Petri net";
					options[2] = "Expanded accepting Petri net";
					options[3] = "Process Tree";
					options[4] = "Efficient tree";
				} else {
					options = new Object[4];
					options[0] = "Petri net";
					options[1] = "Accepting Petri net";
					options[2] = "Expanded accepting Petri net";
					options[3] = "Directly follows model";
				}

				int n = JOptionPane.showOptionDialog(panel,
						"As what would you like to save the model?\nIt will become available in ProM.", "Save",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

				switch (n) {
					case 0 :
						//store as Petri net
						ExportModel.exportPetrinet(context, model, name, canceller);
						break;
					case 1 :
						ExportModel.exportAcceptingPetriNet(context, model, name, canceller);
						break;
					case 2 :
						ExportModel.exportExpandedAcceptingPetriNet(context, model, name, canceller);
						break;
					case 3 :
						if (model.isTree()) {
							ExportModel.exportProcessTree(context, model.getTree().getDTree(), name);
						} else {
							ExportModel.exportDirectlyFollowsModel(context, model, name);
						}
						break;
					case 4 :
						ExportModel.exportEfficientTree(context, model.getTree(), name);
						break;
				}
			}
		});
		panel.getSaveModelButton().setEnabled(false);

		//add animation and statistics to export
		panel.getGraph().setGetExporters(new GetExporters() {
			public List<Exporter> getExporters(List<Exporter> exporters) {
				exporters.add(new ExporterDataAnalyses(state));
				if (state.getIvMLogFiltered() != null && state.isAlignmentReady()
						&& state.getIvMAttributesInfo() != null) {
					exporters.add(new ExporterTraceData(state));
				}
				if (state.isPerformanceReady()) {
					exporters.add(new ExporterModelStatistics(state.getConfiguration()));
				}
				if (panel.getGraph().isAnimationEnabled()) {
					exporters.add(new ExporterAvi(state));
				}
				return exporters;
			}
		});

		//set image/animation export button
		panel.getSaveImageButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.getGraph().exportView();
			}
		});

		//set alignment export button
		panel.getSaveLogButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				String name = XConceptExtension.instance().extractName(state.getSortedXLog());
				IvMLog log = state.getIvMLogFiltered();
				IvMModel model = state.getModel();
				XEventClassifier classifier = state.getActivityClassifier();

				Object[] options = { "Just the log (log & sync moves)", "Aligned log (all moves)",
						"Model view (model & sync moves)" };
				int n = JOptionPane.showOptionDialog(panel,
						"Which filtered view of the log would you like to export?\nIt will become available as an event log in ProM.",
						"Export Log", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[0]);

				switch (n) {
					case 0 :
						ExportAlignment.exportAlignment(context, log, model, name, Type.logView);
						break;
					case 1 :
						//ExportAlignment.exportAlignment(context, log, model, name, Type.both);
						InductiveVisualMinerAlignment alignment = ExportAlignment.exportAlignment(log, model,
								classifier);
						context.getProvidedObjectManager().createProvidedObject(name + " (alignment)", alignment,
								InductiveVisualMinerAlignment.class, context);
						if (context instanceof UIPluginContext) {
							((UIPluginContext) context).getGlobalContext().getResourceManager()
									.getResourceForInstance(alignment).setFavorite(true);
						}
						break;
					case 2 :
						ExportAlignment.exportAlignment(context, log, model, name, Type.modelView);
						break;
				}
			}
		});
		panel.getSaveLogButton().setEnabled(false);

		//listen to ctrl c to show the controller view
		{
			panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "showControllerView"); // - key
			panel.getActionMap().put("showControllerView", new AbstractAction() {
				private static final long serialVersionUID = 1727407514105090094L;

				public void actionPerformed(ActionEvent arg0) {
					panel.getControllerView().setVisible(true);
					chain.getOnChange().run();
				}

			});
		}

		//set pre-mining filters button
		initGuiPreMiningFilters();

		//set edit model button
		panel.getEditModelButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.getEditModelView().enableAndShow();
			}
		});

		//set graph handlers
		initGuiGraph();

		//set trace view button
		initGuiTraceView();

		//set data analyses
		{
			initGuiDataAnalyses();

			//link cohort data analysis view and cohort highlighting filter
			panel.getHighlightingFiltersView()
					.setHighlightingFilter2CohortAnalysisHandler(new HighlightingFilter2CohortAnalysisHandler() {
						public void showCohortAnalysis() {
							panel.getDataAnalysesView().enableAndShow();
							panel.getDataAnalysesView().showAnalysis(CohortAnalysisTableFactory.name);
						}

						public void setEnabled(boolean enabled) {
							//do nothing if the user disables the cohort
						}
					});
			panel.getDataAnalysesView()
					.setCohortAnalysis2HighlightingFilterHandler(new CohortAnalysis2HighlightingFilterHandler() {
						public void setSelectedCohort(Cohort cohort, boolean highlightInCohort) {
							panel.getHighlightingFiltersView().setHighlightingFilterSelectedCohort(cohort,
									highlightInCohort);
						}
					});

			//link cohort data analysis view switch and cohort computations
			chain.setObject(IvMObject.selected_cohort_analysis_enabled, false);
			panel.getDataAnalysesView().addSwitcherListener(CohortAnalysisTableFactory.name, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean selected = ((AbstractButton) e.getSource()).getModel().isSelected();
					if (selected) {
						//start the computation
						chain.setObject(IvMObject.selected_cohort_analysis_enabled, false);
						panel.getDataAnalysesView().setSwitcherMessage(CohortAnalysisTableFactory.name,
								"Compute " + CohortAnalysisTableFactory.name + " [computing..]");
					} else {
						//stop the computation
						/*
						 * It seems counter-intuitive, but we already have means
						 * in place to stop running computations. That is, if we
						 * start a new one [which will not compute anything due
						 * the flag set], the old one will be stopped
						 * automatically.
						 */
						chain.setObject(IvMObject.selected_cohort_analysis_enabled, false);
						panel.getDataAnalysesView().setSwitcherMessage(CohortAnalysisTableFactory.name,
								"Compute " + CohortAnalysisTableFactory.name);
					}
				}
			});
		}

		//set trace colouring button
		{
			panel.getTraceColourMapViewButton().addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					panel.getTraceColourMapView().enableAndShow();
				}
			});
			TraceColourMap traceColourMap = new TraceColourMapFixed(RendererFactory.defaultTokenFillColour);
			state.putObject(IvMObject.trace_colour_map, traceColourMap);
			panel.getGraph().setTraceColourMap(traceColourMap);
			panel.getTraceColourMapView().setOnUpdate(new Function<TraceColourMapSettings, Object>() {
				public Object call(TraceColourMapSettings input) throws Exception {
					chain.setObject(IvMObject.trace_colour_map_settings, input);
					return null;
				}
			});
		}

		//set highlighting filters button
		initGuiHighlightingFilters();

	}

	protected void initGuiMiner() {
		//miner
		chain.setObject(IvMObject.selected_miner, new Miner());
		panel.getMinerSelection().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				chain.setObject(IvMObject.selected_miner,
						(VisualMinerWrapper) panel.getMinerSelection().getSelectedItem());
			}
		});

		//noise threshold
		chain.setObject(IvMObject.selected_noise_threshold, 0.8);
		panel.getPathsSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!panel.getPathsSlider().getSlider().getValueIsAdjusting()) {
					chain.setObject(IvMObject.selected_noise_threshold, panel.getPathsSlider().getValue());
				}
			}
		});

		//model-related buttons
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "enable model-related buttons";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.model };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMModel model = inputs.get(IvMObject.model);

				panel.getSaveModelButton().setEnabled(true);
				panel.getEditModelView().setModel(model);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getSaveModelButton().setEnabled(false);
				panel.getEditModelView().setMessage("Mining tree...");
			}
		});
	}

	protected void initGuiGraph() {
		//layout
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "model dot";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.graph_dot, IvMObject.graph_svg };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				Dot dot = inputs.get(IvMObject.graph_dot);
				SVGDiagram svg = inputs.get(IvMObject.graph_svg);

				panel.getGraph().changeDot(dot, svg, true);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//here, we could put the graph on blank, but that is annoying
				//				Dot dot = new Dot();
				//				DotNode dotNode = dot.addNode("...");
				//				dotNode.setOption("shape", "plaintext");
				//				panel.getGraph().changeDot(dot, true);
			}
		});

		final Cl09LayoutAlignment layoutAlignment = new Cl09LayoutAlignment();
		chain.register(layoutAlignment);

		//mode switch
		chain.setObject(IvMObject.selected_visualisation_mode, new ModePaths());
		panel.getColourModeSelection().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				chain.setObject(IvMObject.selected_visualisation_mode,
						(Mode) panel.getColourModeSelection().getSelectedItem());
			}
		});

		//register the requirements of the modes
		registerModeRequests(layoutAlignment);

		//trace view event colour map & model node selection
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "trace view event colour map";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.trace_view_event_colour_map, IvMObject.carte_blanche,
						IvMObject.graph_visualisation_info };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				TraceViewEventColourMap traceViewEventColourMap = inputs.get(IvMObject.trace_view_event_colour_map);

				panel.getTraceView().setEventColourMap(traceViewEventColourMap);

				/**
				 * We don't want to be triggered by a change in selection, thus
				 * we get it from the carte-blanche
				 */
				Selection selection = inputs.get(IvMObject.carte_blanche)
						.getDirectIfPresent(IvMObject.selected_model_selection);
				ProcessTreeVisualisationInfo visualisationInfo = inputs.get(IvMObject.graph_visualisation_info);
				makeElementsSelectable(visualisationInfo, panel, selection);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				// TODO no action taken?
			}
		});
	}

	protected void initGuiTraceView() {
		panel.getTraceViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel.getTraceView().enableAndShow();
			}
		});

		//trace view (IM log)
		DataChainLinkGui traceViewIMLog = new DataChainLinkGui() {

			public String getName() {
				return "trace view (IMLog)";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.imlog, IvMObject.trace_colour_map };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IMLog imLog = inputs.get(IvMObject.imlog);
				TraceColourMap traceColourMap = inputs.get(IvMObject.trace_colour_map);

				panel.getTraceView().set(imLog, traceColourMap);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getTraceView().set(null, null);
			}
		};
		chain.register(traceViewIMLog);

		//chain.register (aligned log)
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "trace view (IvMLog)";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.model, IvMObject.aligned_log_filtered,
						IvMObject.selected_model_selection, IvMObject.trace_colour_map };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMModel model = inputs.get(IvMObject.model);
				IvMLogFilteredImpl aLog = inputs.get(IvMObject.aligned_log_filtered);
				Selection selection = inputs.get(IvMObject.selected_model_selection);
				TraceColourMap traceColourMap = inputs.get(IvMObject.trace_colour_map);

				panel.getTraceView().set(model, aLog, selection, traceColourMap);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//do nothing to prevent the IM log to be overruled
			}
		});
	}

	protected void initGuiDataAnalyses() {
		panel.getDataAnalysisViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.getDataAnalysesView().enableAndShow();
			}
		});

		//log data analysis
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "update log data analysis";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.data_analysis_log };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				panel.getDataAnalysesView().setData(LogAttributeAnalysisTableFactory.name, state);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getDataAnalysesView().invalidate(LogAttributeAnalysisTableFactory.name);
			}
		});

		//log data analysis with virtual attributes
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "update log data analysis (+virtual)";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.data_analysis_log,
						IvMObject.data_analysis_log_virtual_attributes };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				LogAttributeAnalysis attributeAnalysis = inputs.get(IvMObject.data_analysis_log);
				@SuppressWarnings("unchecked")
				List<Pair<String, DisplayType>> virtualAttributes = inputs
						.get(IvMObject.data_analysis_log_virtual_attributes);

				attributeAnalysis.addVirtualAttributes(virtualAttributes);
				panel.getDataAnalysesView().setData(LogAttributeAnalysisTableFactory.name, state);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				LogAttributeAnalysis dataAnalysis = state.getObject(IvMObject.data_analysis_log);
				if (dataAnalysis != null) {
					dataAnalysis.setVirtualAttributesToPlaceholders();
				}
				panel.getDataAnalysesView().setData(LogAttributeAnalysisTableFactory.name, state);
			}
		});

		//data analysis - cohort analysis
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "show cohorts";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.data_analysis_cohort };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				panel.getDataAnalysesView().setData(CohortAnalysisTableFactory.name, state);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getDataAnalysesView().invalidate(CohortAnalysisTableFactory.name);
			}
		});

		//data analysis - event analysis
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "show event data analysis";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.data_analysis_event };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				panel.getDataAnalysesView().setData(EventAttributeAnalysisTableFactory.name, state);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getDataAnalysesView().invalidate(EventAttributeAnalysisTableFactory.name);
			}
		});

		//data analysis - trace analysis
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "show trace data analysis";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.data_analysis_trace };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				panel.getDataAnalysesView().setData(TraceAttributeAnalysisTableFactory.name, state);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getDataAnalysesView().invalidate(TraceAttributeAnalysisTableFactory.name);
			}
		});
	}

	protected void initGuiClassifiers() {
		//update data on classifiers
		panel.getClassifiers().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				chain.setObject(IvMObject.selected_classifier, panel.getClassifiers().getSelectedClassifier());
			}
		});

		//update classifiers on data
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "set classifiers";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.classifiers };
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getClassifiers().setEnabled(false);
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				AttributeClassifier[] classifiers = inputs.get(IvMObject.classifiers);
				panel.getClassifiers().setEnabled(true);
				panel.getClassifiers().replaceClassifiers(classifiers);
			}
		});
	}

	protected void initGuiPopups() {
		updatePopups = new DataChainLinkGui() {

			public String getName() {
				return "update popup (basic)";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.model, IvMObject.graph_visualisation_info,
						IvMObject.aligned_log_info_filtered };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMModel model = inputs.get(IvMObject.model);
				ProcessTreeVisualisationInfo visualisationInfo = inputs.get(IvMObject.graph_visualisation_info);
				IvMLogInfo ivmLogInfoFiltered = inputs.get(IvMObject.aligned_log_info_filtered);

				PopupPopulator.updatePopup(state, configuration, panel, model, visualisationInfo, ivmLogInfoFiltered);
				panel.repaint();
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				PopupPopulator.updatePopup(state, configuration, panel, null, null, null);
				panel.repaint();
			}
		};
		chain.register(updatePopups);

		//set mouse-in-out node updater
		panel.getGraph().addMouseInElementsChangedListener(new MouseInElementsChangedListener<DotElement>() {
			public void mouseInElementsChanged(Set<DotElement> mouseInElements) {
				chain.executeLink(updatePopups);
			}
		});

		//set log popup handler
		if (!configuration.getPopupItemsLog().isEmpty()) {
			panel.getGraph().addLogPopupListener(new LogPopupListener() {
				public void isMouseInButton(boolean isIn) {
					chain.executeLink(updatePopups);
				}
			});
		}
	}

	protected void initGuiPreMiningFilters() {
		chain.setObject(IvMObject.selected_activities_threshold, 1.0);
		panel.getActivitiesSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!panel.getActivitiesSlider().getSlider().getValueIsAdjusting()) {
					chain.setObject(IvMObject.selected_activities_threshold, panel.getActivitiesSlider().getValue());
				}
			}
		});

		chain.setObject(IvMObject.controller_premining_filters, new IvMPreMiningFiltersController(
				configuration.getPreMiningFilters(), panel.getPreMiningFiltersView()));

		panel.getPreMiningFiltersButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.getPreMiningFiltersView().enableAndShow();
			}
		});

		panel.getPreMiningFiltersView().setOnUpdate(new Runnable() {
			public void run() {
				chain.executeLink(Cl04FilterLogOnActivities.class);
			}
		});

		//initialise filters
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "initialise pre-mining filters";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.attributes_info, IvMObject.controller_premining_filters };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				AttributesInfo attributesInfo = inputs.get(IvMObject.attributes_info);
				IvMPreMiningFiltersController controller = inputs.get(IvMObject.controller_premining_filters);

				controller.setAttributesInfo(attributesInfo);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//TODO: no action taken?
			}
		});
	}

	protected void initGuiAlignment() {

		//save log button
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "save aligned log";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.aligned_log_filtered };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				panel.getSaveLogButton().setEnabled(true);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getSaveLogButton().setEnabled(false);
			}
		});
	}

	protected void initGuiHighlightingFilters() {

		panel.getHighlightingFiltersViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel.getHighlightingFiltersView().enableAndShow();
			}
		});
		panel.getHighlightingFiltersView().setOnUpdate(new Runnable() {
			public void run() {
				chain.executeLink(Cl13FilterNodeSelection.class);
			}
		});
		state.putObject(IvMObject.controller_highlighting_filters, new IvMHighlightingFiltersController(
				configuration.getHighlightingFilters(), panel.getHighlightingFiltersView()));

		//initialise filters
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "initialise highlighting filters";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.ivm_attributes_info, IvMObject.controller_highlighting_filters };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMAttributesInfo attributesInfo = inputs.get(IvMObject.ivm_attributes_info);
				IvMHighlightingFiltersController controller = inputs.get(IvMObject.controller_highlighting_filters);

				controller.setAttributesInfo(attributesInfo);

				panel.getTraceColourMapView().setAttributes(attributesInfo);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getTraceColourMapView().invalidateAttributes();
			}
		});

		//filtering description
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "selection description";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.selected_model_selection,
						IvMObject.controller_highlighting_filters, IvMObject.model };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				Selection selection = inputs.get(IvMObject.selected_model_selection);
				IvMHighlightingFiltersController controller = inputs.get(IvMObject.controller_highlighting_filters);
				IvMModel model = inputs.get(IvMObject.model);

				HighlightingFiltersView.updateSelectionDescription(panel, selection, controller, model);
				panel.repaint();
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				HighlightingFiltersView.updateSelectionDescription(panel, null, null, null);
				panel.repaint();
			}
		});

		//tell trace view the colour map and the selection
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "trace colour map";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.model, IvMObject.aligned_log_filtered,
						IvMObject.selected_model_selection, IvMObject.trace_colour_map };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMModel model = inputs.get(IvMObject.model);
				IvMLogFilteredImpl ivmLogFiltered = inputs.get(IvMObject.aligned_log_filtered);
				Selection selection = inputs.get(IvMObject.selected_model_selection);
				TraceColourMap traceColourMap = inputs.get(IvMObject.trace_colour_map);

				panel.getTraceView().set(model, ivmLogFiltered, selection, traceColourMap);
				panel.getTraceView().repaint();
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//TODO: no action necessary?
			}
		});
	}

	protected void initGuiAnimation() {
		//enable animation
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "animation enabled";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.selected_animation_enabled };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				boolean enabled = inputs.get(IvMObject.selected_animation_enabled);
				if (!enabled) {
					System.out.println("animation disabled");
					InductiveVisualMinerController.setAnimationStatus(panel, "animation disabled", true);
					panel.getGraph().setAnimationEnabled(false);
				} else {
					//this is taken care of by the animation handler
				}
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//no action necessary
			}
		});

		//animation to panel
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "update animation";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.animation, IvMObject.animation_scaler };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				GraphVizTokens animation = inputs.get(IvMObject.animation);
				Scaler scaler = inputs.get(IvMObject.animation_scaler);

				panel.getGraph().setTokens(animation);
				panel.getGraph().setAnimationExtremeTimes(scaler.getMinInUserTime(), scaler.getMaxInUserTime());
				panel.getGraph().setAnimationEnabled(true);

				panel.repaint();
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getGraph().setAnimationEnabled(false);
				InductiveVisualMinerController.setAnimationStatus(panel, " ", false);
			}
		});

		//filtered log to animation
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "filtered log to animation";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.aligned_log_filtered };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				IvMLogFilteredImpl ivmLog = inputs.get(IvMObject.aligned_log_filtered);

				panel.getGraph().setFilteredLog(ivmLog);

				panel.getGraph().repaint();
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				panel.getGraph().setFilteredLog(null);
			}
		});

		//
		/**
		 * Set animation time updater. Naturally, this does not go via the
		 * chain, and we cache the scaler
		 */
		chain.register(new DataChainLinkGui() {
			public String getName() {
				return "catch animation objects";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.animation_scaler, IvMObject.selected_visualisation_mode,
						IvMObject.visualisation_data };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				animationScaler = inputs.get(IvMObject.animation_scaler);
				animationMode = inputs.get(IvMObject.selected_visualisation_mode);
				animationVisualisationData = inputs.get(IvMObject.visualisation_data);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				animationScaler = null;
			}
		});

		panel.getGraph().setAnimationTimeChangedListener(new AnimationTimeChangedListener() {
			public void timeStepTaken(double userTime) {
				if (panel.getGraph().isAnimationEnabled()) {
					Scaler scaler = animationScaler;
					if (scaler != null) {
						long logTime = Math.round(scaler.userTime2LogTime(userTime));
						if (scaler.isCorrectTime()) {
							setAnimationStatus(panel, ResourceTimeUtils.timeToString(logTime), true);
						} else {
							setAnimationStatus(panel, "random", true);
						}

						//draw modes that require an update with each time step
						if (animationMode != null && animationVisualisationData != null) {
							if (animationMode.isUpdateWithTimeStep()) {
								animationVisualisationData.setTime(logTime);
								try {
									//TODO: re-enable
									//updateHighlighting(panel, state);
								} catch (UnknownTreeNodeException e) {
									e.printStackTrace();
								}
								panel.getTraceView().repaint();
							}
						}
					}
				}
			}
		});
	}

	protected void initGuiHistogram() {
		//resize handler
		panel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				//on resize, we have to resize the histogram as well
				chain.setObject(IvMObject.histogram_width, (int) panel.getGraph().getControlsProgressLine().getWidth());
			}
		});

		//Update the width once the dot is ready. We cannot initialise the width as long as the window has not been drawn yet. Once the dot is computed, this should be fine. 
		chain.register(new DataChainLinkGui() {

			public String getName() {
				return "histogram width";
			}

			public IvMObject<?>[] getInputObjects() {
				return new IvMObject<?>[] { IvMObject.graph_dot };
			}

			public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs) throws Exception {
				int width = (int) panel.getGraph().getControlsProgressLine().getWidth();
				chain.setObject(IvMObject.histogram_width, width);
			}

			public void invalidate(InductiveVisualMinerPanel panel) {
				//no action necessary				
			}
		});
	}

	public static void makeElementsSelectable(ProcessTreeVisualisationInfo info, InductiveVisualMinerPanel panel,
			Selection selection) {
		for (LocalDotNode dotNode : info.getAllActivityNodes()) {
			panel.makeNodeSelectable(dotNode, selection.isSelected(dotNode));
		}
		for (LocalDotEdge logMoveEdge : info.getAllLogMoveEdges()) {
			panel.makeEdgeSelectable(logMoveEdge, selection.isSelected(logMoveEdge));
		}
		for (LocalDotEdge modelMoveEdge : info.getAllModelMoveEdges()) {
			panel.makeEdgeSelectable(modelMoveEdge, selection.isSelected(modelMoveEdge));
		}
		for (LocalDotEdge edge : info.getAllModelEdges()) {
			panel.makeEdgeSelectable(edge, selection.isSelected(edge));
		}
	}

	/**
	 * Sets the status message of number. The status message stays in view until
	 * it is reset using NULL for that number.
	 * 
	 * @param message
	 * @param number
	 */
	public void setStatus(String message, int number) {
		userStatus.setStatus(message, number);
		panel.getStatusLabel().setText(userStatus.getText());
		panel.getStatusLabel().repaint();
	}

	public static void setAnimationStatus(InductiveVisualMinerPanel panel, String s, boolean isTime) {
		if (isTime) {
			panel.getAnimationTimeLabel().setFont(IvMDecorator.fontMonoSpace);
			panel.getAnimationTimeLabel().setText("time: " + s);
		} else {
			panel.getAnimationTimeLabel().setFont(panel.getStatusLabel().getFont());
			panel.getAnimationTimeLabel().setText(s);
		}
	}

	public void registerModeRequests(final Cl09LayoutAlignment layoutAlignment) {
		for (Mode mode : configuration.getModes()) {
			final Mode mode2 = mode;
			if (mode2.inputsRequested().length > 0) {
				for (IvMObject<?> object : mode2.inputsRequested()) {
					final IvMObject<?> object2 = object;
					chain.register(new DataChainLinkGui() {

						public String getName() {
							return "mode " + mode2 + ": " + object.getName();
						}

						public IvMObject<?>[] getInputObjects() {
							return new IvMObject<?>[] { IvMObject.carte_blanche, object2 };
						}

						public void updateGui(InductiveVisualMinerPanel panel, IvMObjectValues inputs)
								throws Exception {
							Mode mode3 = inputs.get(IvMObject.carte_blanche)
									.getDirectIfPresent(IvMObject.selected_visualisation_mode);
							if (mode3 != null && mode2 == mode3) { //if this is the selected mode
								chain.executeLink(layoutAlignment);
							}
						}

						public void invalidate(InductiveVisualMinerPanel panel) {
							//no action taken
						}
					});
				}
			}
		}
	}

	public static void debug(Object s) {
		System.out.println(s);
	}

	public InductiveVisualMinerPanel getPanel() {
		return panel;
	}

	public InductiveVisualMinerState getState() {
		return state;
	}
}
