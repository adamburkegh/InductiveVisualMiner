package org.processmining.plugins.inductiveVisualMiner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nl.tue.astar.AStarThread.Canceller;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.Classifiers.ClassifierWrapper;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Quadruple;
import org.processmining.plugins.InductiveMiner.Quintuple;
import org.processmining.plugins.InductiveMiner.Septuple;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.dfgOnly.log2logInfo.IMLog2IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.Dot2Image;
import org.processmining.plugins.graphviz.dot.Dot2Image.Type;
import org.processmining.plugins.graphviz.visualisation.AnimatableSVGPanel.Callback;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState.ColourMode;
import org.processmining.plugins.inductiveVisualMiner.TraceView.TraceViewColourMap;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisation;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationParameters;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.LocalDotEdge;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.LocalDotNode;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedLog;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedLogInfo;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedTrace;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentETM;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentResult;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.alignment.Move;
import org.processmining.plugins.inductiveVisualMiner.animation.ComputeAnimation;
import org.processmining.plugins.inductiveVisualMiner.animation.ComputeTimedLog;
import org.processmining.plugins.inductiveVisualMiner.animation.TimedLog;
import org.processmining.plugins.inductiveVisualMiner.animation.TimedMove;
import org.processmining.plugins.inductiveVisualMiner.animation.TimedMove.Scaler;
import org.processmining.plugins.inductiveVisualMiner.animation.TimedTrace;
import org.processmining.plugins.inductiveVisualMiner.animation.TimestampsAdder;
import org.processmining.plugins.inductiveVisualMiner.colouringFilter.ColouringFilter;
import org.processmining.plugins.inductiveVisualMiner.colouringFilter.ColouringFilterPluginFinder;
import org.processmining.plugins.inductiveVisualMiner.colouringFilter.ColouringFiltersView;
import org.processmining.plugins.inductiveVisualMiner.colouringFilter.ComputeColouringFilter;
import org.processmining.plugins.inductiveVisualMiner.export.ExportAnimation;
import org.processmining.plugins.inductiveVisualMiner.export.ExportModel;
import org.processmining.plugins.inductiveVisualMiner.export.SaveAsDialog;
import org.processmining.plugins.inductiveVisualMiner.export.SaveAsDialog.FileType;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.Chain;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.ChainLink;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.InputFunction;
import org.processmining.plugins.inductiveVisualMiner.logFiltering.FilterLeastOccurringActivities;
import org.processmining.plugins.inductiveVisualMiner.performance.XEventPerformanceClassifier;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerParameters;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerWrapper;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

import com.kitfox.svg.SVGDiagram;

public class InductiveVisualMinerController {

	private final InductiveVisualMinerPanel panel;
	private final InductiveVisualMinerState state;
	private static final int maxAnimatedTraces = 50;

	public class ResettableCanceller implements Canceller {

		private boolean cancelled = false;

		public void cancel() {
			this.cancelled = true;
		}

		public void reset() {
			this.cancelled = false;
		}

		public boolean isCancelled() {
			return cancelled;
		}

	}

	//make an IMlog out of an XLog
	private class MakeLog
			extends
			ChainLink<Triple<XLog, XEventPerformanceClassifier, IMLog2IMLogInfo>, Quadruple<XLogInfo, XLogInfo, IMLog, IMLogInfo>> {

		protected Triple<XLog, XEventPerformanceClassifier, IMLog2IMLogInfo> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveModelButton().setEnabled(false);
			panel.getSaveImageButton().setEnabled(false);
			panel.getSaveImageButton().setText("image");
			return Triple.of(state.getXLog(), state.getPerformanceClassifier(), state.getLog2logInfo());
		}

		protected Quadruple<XLogInfo, XLogInfo, IMLog, IMLogInfo> executeLink(
				Triple<XLog, XEventPerformanceClassifier, IMLog2IMLogInfo> input) {
			setStatus("Making log..");

			IMLog imLog = new IMLog(input.getA(), input.getB().getActivityClassifier());
			IMLogInfo imLogInfo = input.getC().createLogInfo(imLog);
			XLogInfo xLogInfo = XLogInfoFactory.createLogInfo(input.getA(), input.getB().getActivityClassifier());
			XLogInfo xLogInfoPerformance = XLogInfoFactory.createLogInfo(input.getA(), input.getB());

			return Quadruple.of(xLogInfo, xLogInfoPerformance, imLog, imLogInfo);
		}

		protected void processResult(Quadruple<XLogInfo, XLogInfo, IMLog, IMLogInfo> result) {
			state.setLog(result.getA(), result.getB(), result.getC(), result.getD());
			panel.getTraceView().set(result.getC());
		}

		public void cancel() {

		}
	}

	//filter the log using activities threshold
	private class FilterLogOnActivities extends
			ChainLink<Quadruple<IMLog, IMLogInfo, Double, IMLog2IMLogInfo>, Triple<IMLog, IMLogInfo, Set<XEventClass>>> {

		protected Quadruple<IMLog, IMLogInfo, Double, IMLog2IMLogInfo> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveModelButton().setEnabled(false);
			panel.getSaveImageButton().setEnabled(false);
			panel.getSaveImageButton().setText("image");
			return new Quadruple<IMLog, IMLogInfo, Double, IMLog2IMLogInfo>(state.getLog(), state.getLogInfo(),
					state.getActivitiesThreshold(), state.getLog2logInfo());
		}

		protected Triple<IMLog, IMLogInfo, Set<XEventClass>> executeLink(
				Quadruple<IMLog, IMLogInfo, Double, IMLog2IMLogInfo> input) {
			if (input.getC() < 1.0) {
				return FilterLeastOccurringActivities.filter(input.getA(), input.getB(), input.getC(), input.getD());
			} else {
				return new Triple<IMLog, IMLogInfo, Set<XEventClass>>(input.getA(), input.getB(),
						new HashSet<XEventClass>());
			}
		}

		protected void processResult(Triple<IMLog, IMLogInfo, Set<XEventClass>> result) {
			state.setActivityFilteredIMLog(result.getA(), result.getB(), result.getC());
		}

		public void cancel() {

		}
	}

	//mine a model
	private class Mine extends
			ChainLink<Quadruple<ProcessTree, IMLog, VisualMinerWrapper, VisualMinerParameters>, ProcessTree> {

		private ResettableCanceller canceller = new ResettableCanceller();

		protected Quadruple<ProcessTree, IMLog, VisualMinerWrapper, VisualMinerParameters> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			panel.getTraceView().set(state.getLog());
			VisualMinerParameters minerParameters = new VisualMinerParameters(state.getPaths());
			return Quadruple.of(state.getPreMinedTree(), state.getActivityFilteredIMLog(), state.getMiner(),
					minerParameters);
		}

		protected ProcessTree executeLink(Quadruple<ProcessTree, IMLog, VisualMinerWrapper, VisualMinerParameters> input) {
			setStatus("Mining..");
			canceller.reset();
			if (input.getA() == null) {
				//mine a new tree
				return input.getC().mine(input.getB(), input.getD(), canceller);
			} else {
				//use the existing tree
				return input.getA();
			}
		}

		protected void processResult(ProcessTree result) {
			state.setTree(result);
			state.setSelectedNodes(new HashSet<UnfoldedNode>());
			state.setSelectedLogMoves(new HashSet<LogMovePosition>());
			state.resetAlignment();

			panel.getSaveModelButton().setEnabled(true);
			panel.getSaveImageButton().setEnabled(true);
			setStatus("Layouting model..");
		}

		public void cancel() {
			canceller.cancel();
		}
	}

	//compute alignment
	private class Align
			extends
			ChainLink<Quintuple<ProcessTree, XEventPerformanceClassifier, XLog, XEventClasses, XEventClasses>, AlignmentResult> {

		private ResettableCanceller canceller = new ResettableCanceller();

		protected Quintuple<ProcessTree, XEventPerformanceClassifier, XLog, XEventClasses, XEventClasses> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			return Quintuple.of(state.getTree(), state.getPerformanceClassifier(), state.getXLog(), state.getXLogInfo()
					.getEventClasses(), state.getXLogInfoPerformance().getEventClasses());
		}

		protected AlignmentResult executeLink(
				Quintuple<ProcessTree, XEventPerformanceClassifier, XLog, XEventClasses, XEventClasses> input) {
			setStatus("Computing alignment..");
			canceller.reset();
			return AlignmentETM.alignTree(input.getA(), input.getB(), input.getC(), input.getD(), input.getE(),
					canceller);
		}

		protected void processResult(AlignmentResult result) {
			state.setAlignment(result);
			panel.getTraceView().set(state.getAlignedLog());
		}

		public void cancel() {
			canceller.cancel();
		}

	}

	//perform layout
	private class Layout
			extends
			ChainLink<Triple<ProcessTree, AlignedLogInfo, AlignedLogVisualisationParameters>, Triple<Dot, SVGDiagram, AlignedLogVisualisationInfo>> {

		protected Triple<ProcessTree, AlignedLogInfo, AlignedLogVisualisationParameters> generateInput() {
			setStatus("Layouting graph..");

			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");

			//if the view does not show deviations, do not select any log moves
			if (state.getColourMode() == ColourMode.paths) {
				state.setSelectedLogMoves(new HashSet<LogMovePosition>());
			}

			AlignedLogVisualisationParameters parameters = InductiveVisualMinerPanel.getViewParameters(state);
			return Triple.of(state.getTree(), state.getAlignedFilteredLogInfo(), parameters);
		}

		protected Triple<Dot, SVGDiagram, AlignedLogVisualisationInfo> executeLink(
				Triple<ProcessTree, AlignedLogInfo, AlignedLogVisualisationParameters> input) {
			//compute dot
			AlignedLogVisualisation visualiser = new AlignedLogVisualisation();
			Pair<Dot, AlignedLogVisualisationInfo> p = visualiser.fancy(input.getA(), input.getB(), input.getC());

			//set the graph direction
			p.getA().setDirection(state.getGraphDirection());

			//compute svg from dot
			SVGDiagram diagram = DotPanel.dot2svg(p.getA());

			return Triple.of(p.getA(), diagram, p.getB());
		}

		protected void processResult(Triple<Dot, SVGDiagram, AlignedLogVisualisationInfo> result) {
			panel.getGraph().changeDot(result.getA(), result.getB(), true);

			state.setLayout(result.getC());
			makeNodesSelectable(state.getVisualisationInfo(), panel, state.getSelectedNodes(),
					state.getSelectedLogMoves());
		}

		public void cancel() {

		}
	}

	//filter log for node selection
	private class FilterNodeSelection
			extends
			ChainLink<Septuple<AlignedLog, Set<UnfoldedNode>, Set<LogMovePosition>, AlignedLogInfo, IMLog, XLogInfo, List<ColouringFilter>>, Triple<AlignedLog, AlignedLogInfo, IMLog>> {

		private ResettableCanceller canceller = new ResettableCanceller();

		protected Septuple<AlignedLog, Set<UnfoldedNode>, Set<LogMovePosition>, AlignedLogInfo, IMLog, XLogInfo, List<ColouringFilter>> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			return Septuple.of(state.getAlignedLog(), state.getSelectedNodes(), state.getSelectedLogMoves(),
					state.getAlignedLogInfo(), new IMLog(state.getXLog(), state.getActivityClassifier()),
					state.getXLogInfoPerformance(), state.getColouringFilters());
		}

		protected Triple<AlignedLog, AlignedLogInfo, IMLog> executeLink(
				Septuple<AlignedLog, Set<UnfoldedNode>, Set<LogMovePosition>, AlignedLogInfo, IMLog, XLogInfo, List<ColouringFilter>> input) {
			setStatus("Highlighting selection..");

			canceller.reset();

			//apply colouring filters
			Triple<AlignedLog, AlignedLogInfo, IMLog> colouringFilteredAlignment = ComputeColouringFilter
					.applyColouringFilter(input.getA(), input.getD(), input.getE(), input.getF(), input.getG(),
							canceller);

			//apply node/edge selection filters
			if (!input.getB().isEmpty() || !input.getC().isEmpty()) {
				return filterOnSelection(colouringFilteredAlignment.getA(), input.getB(), input.getC(),
						colouringFilteredAlignment.getC());
			} else {
				return colouringFilteredAlignment;
			}

		}

		protected void processResult(Triple<AlignedLog, AlignedLogInfo, IMLog> result) {
			state.setAlignedFilteredLog(result.getA(), result.getB(), result.getC());
			panel.getTraceView().set(state.getAlignedFilteredLog());
		}

		public void cancel() {
			canceller.cancel();
		}
	}

	//colour the nodes
	private class ApplyHighlighting extends ChainLink<AlignedLogInfo, AlignedLogInfo> {

		protected AlignedLogInfo generateInput() {
			return state.getAlignedFilteredLogInfo();
		}

		protected AlignedLogInfo executeLink(AlignedLogInfo input) {
			return input;
		}

		protected void processResult(AlignedLogInfo result) {

			TraceViewColourMap colourMap = InductiveVisualMinerSelectionColourer.colourHighlighting(panel.getGraph()
					.getSVG(), state.getVisualisationInfo(), state.getTree(), result, InductiveVisualMinerPanel
					.getViewParameters(state));
			ColouringFiltersView.updateSelectionDescription(panel, state.getSelectedNodes(),
					state.getSelectedLogMoves(), state.getColouringFilters(), state.getAlignedFilteredLog().size(),
					maxAnimatedTraces);

			//tell trace view the colour map and the selection
			panel.getTraceView().setColourMap(colourMap);
			colourMap.setSelectedNodes(state.getSelectedNodes(), state.getSelectedLogMoves());

			setStatus(" ");
			panel.repaint();
		}

		public void cancel() {

		}

	}

	private class TimeLog extends ChainLink<Triple<AlignedLog, IMLog, XLogInfo>, TimedLog> {

		private ResettableCanceller canceller = new ResettableCanceller();

		protected Triple<AlignedLog, IMLog, XLogInfo> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			return Triple.of(state.getAlignedFilteredLog(), state.getAlignedFilteredXLog(),
					state.getXLogInfoPerformance());
		}

		protected TimedLog executeLink(Triple<AlignedLog, IMLog, XLogInfo> input) {
			setStatus("Creating timed log..");
			canceller.reset();
			return ComputeTimedLog.computeTimedLog(input.getA(), input.getB(), input.getC(), canceller);
		}

		protected void processResult(TimedLog result) {
			state.setTimedLog(result);

			//update the trace view
			panel.getTraceView().set(result);
		}

		public void cancel() {
			canceller.cancel();
		}
	}

	//mine the resources
	private class MineResources extends ChainLink<TimedLog, Pair<MultiSet<Pair<String, String>>, MultiSet<String>>> {

		protected TimedLog generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			return state.getTimedLog();
		}

		protected Pair<MultiSet<Pair<String, String>>, MultiSet<String>> executeLink(TimedLog input) {
			setStatus("Mining resource model..");
			MultiSet<String> result = new MultiSet<>();
			MultiSet<Pair<String, String>> result2 = new MultiSet<>();
			for (TimedTrace trace : input) {
				String last = null;
				for (TimedMove event : trace) {
					String now = event.getResource();
					if (now == null) {
						continue;
					}
					result.add(now);
					if (last != null) {
						result2.add(Pair.of(last, now));
					}
					last = now;
				}
			}
			return Pair.of(result2, result);
		}

		protected void processResult(Pair<MultiSet<Pair<String, String>>, MultiSet<String>> result) {
			panel.getResourceView().set(result);
		}

		public void cancel() {

		}
	}

	//prepare animation
	private class Animate
			extends
			ChainLink<Quintuple<TimedLog, ColourMode, AlignedLogVisualisationInfo, Dot, SVGDiagram>, Pair<SVGDiagram, Scaler>> {

		private ResettableCanceller canceller = new ResettableCanceller();

		protected Quintuple<TimedLog, ColourMode, AlignedLogVisualisationInfo, Dot, SVGDiagram> generateInput() {
			panel.getGraph().setEnableAnimation(false);
			panel.getSaveImageButton().setText("image");
			return Quintuple.of(state.getTimedLog(), state.getColourMode(), state.getVisualisationInfo(), panel
					.getGraph().getDot(), panel.getGraph().getSVG());
		}

		protected Pair<SVGDiagram, Scaler> executeLink(
				Quintuple<TimedLog, ColourMode, AlignedLogVisualisationInfo, Dot, SVGDiagram> input) {
			setStatus("Creating animation..");
			canceller.reset();

			return ComputeAnimation.computeAnimation(input.getA(), input.getB(), input.getC(), maxAnimatedTraces,
					input.getD(), input.getE(), canceller);
		}

		protected void processResult(Pair<SVGDiagram, Scaler> result) {
			state.setAnimationTimeScaler(result.getB());

			//re-colour the selected nodes (i.e. the dashed red border)
			InductiveVisualMinerSelectionColourer.colourSelection(result.getA(), state.getSelectedNodes(),
					state.getSelectedLogMoves(), state.getVisualisationInfo());

			//re-highlight the model
			TraceViewColourMap colourMap = InductiveVisualMinerSelectionColourer.colourHighlighting(result.getA(),
					state.getVisualisationInfo(), state.getTree(), state.getAlignedFilteredLogInfo(),
					InductiveVisualMinerPanel.getViewParameters(state));

			//tell trace view the colour map and the selection
			panel.getTraceView().setColourMap(colourMap);
			colourMap.setSelectedNodes(state.getSelectedNodes(), state.getSelectedLogMoves());

			//update selection description
			ColouringFiltersView.updateSelectionDescription(panel, state.getSelectedNodes(),
					state.getSelectedLogMoves(), state.getColouringFilters(), state.getAlignedFilteredLog().size(),
					maxAnimatedTraces);

			panel.getSaveImageButton().setText("animation");
			setStatus(" ");
			panel.getGraph().setImage(result.getA(), false);
			panel.getGraph().setEnableAnimation(true);
		}

		public void cancel() {
			canceller.cancel();
		}
	}

	private final Chain chain;
	private final PluginContext context;

	public InductiveVisualMinerController(PluginContext context, InductiveVisualMinerPanel panel,
			InductiveVisualMinerState state) {
		this.panel = panel;
		this.state = state;
		this.context = context;

		//initialise gui handlers
		initGui();

		chain = new Chain(context.getExecutor());
		chain.add(new MakeLog());
		chain.add(new FilterLogOnActivities());
		chain.add(new Mine());
		chain.add(new Layout());
		chain.add(new Align());
		chain.add(new Layout());
		chain.add(new FilterNodeSelection());
		chain.add(new ApplyHighlighting());
		chain.add(new TimeLog());
		chain.add(new MineResources());
		chain.add(new Animate());

		//set up plug-ins
		List<ColouringFilter> colouringFilters = ColouringFilterPluginFinder.findFilteringPlugins(context, panel,
				state.getXLog());
		state.setColouringFilters(colouringFilters);
		panel.getColouringFiltersView().initialise(colouringFilters);
		initialiseColourFilters(state.getXLog(), context.getExecutor());

		//start the chain
		chain.execute(MakeLog.class);
	}

	private void initGui() {

		//noise filter
		panel.getPathsSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!panel.getPathsSlider().getSlider().getValueIsAdjusting()) {
					state.setPaths(panel.getPathsSlider().getValue());
					chain.execute(Mine.class);
				}

				//give the focus back to the graph panel
				panel.getGraph().requestFocus(true);
			}
		});

		//classifier
		panel.getClassifiers().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				state.setClassifier(((ClassifierWrapper) panel.getClassifiers().getSelectedItem()).classifier);
				chain.execute(MakeLog.class);
			}
		});

		//miner
		panel.getMinerSelection().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				state.setMiner(((VisualMinerWrapper) panel.getMinerSelection().getSelectedItem()));
				chain.execute(MakeLog.class);
			}
		});

		//activities filter
		panel.getActivitiesSlider().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!panel.getActivitiesSlider().getSlider().getValueIsAdjusting()) {
					state.setActivitiesThreshold(panel.getActivitiesSlider().getValue());
					chain.execute(FilterLogOnActivities.class);
				}

				//give the focus back to the graph panel
				panel.getGraph().requestFocus(true);
			}
		});

		//colour mode
		panel.getColourModeSelection().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				state.setColourMode((ColourMode) panel.getColourModeSelection().getSelectedItem());
				chain.execute(Layout.class);
			}
		});

		//node selection changed
		panel.setOnSelectionChanged(new InputFunction<Pair<Set<UnfoldedNode>, Set<LogMovePosition>>>() {
			public void call(Pair<Set<UnfoldedNode>, Set<LogMovePosition>> input) throws Exception {
				state.setSelectedNodes(input.getA());
				state.setSelectedLogMoves(input.getB());
				chain.execute(FilterNodeSelection.class);
			}
		});

		//graph direction changed
		panel.setOnGraphDirectionChanged(new InputFunction<Dot.GraphDirection>() {
			public void call(GraphDirection input) throws Exception {
				state.setGraphDirection(input);
				chain.execute(Layout.class);
			}
		});

		//set model export button
		panel.getSaveModelButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

				//store the resulting Process tree or Petri net
				String name = XConceptExtension.instance().extractName(state.getXLog());
				ProcessTree tree = state.getTree();

				Object[] options = { "Petri net", "Process tree" };
				int n = JOptionPane.showOptionDialog(panel,
						"As what would you like to save the model?\nIt will become available in ProM.", "Save",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

				if (n == 0) {
					//store as Petri net
					ExportModel.exportPetrinet(context, tree, name);
				} else if (n == 1) {
					//store as Process tree
					ExportModel.exportProcessTree(context, tree, name);
				}
			}
		});

		//set image/animation export button
		panel.getSaveImageButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SaveAsDialog dialog = new SaveAsDialog(panel.getGraph().isEnableAnimation());
				final Pair<File, FileType> p = dialog.askUser(panel);
				if (p == null) {
					return;
				}
				switch (p.getRight()) {
					case pdfImage :
						//save the file asynchronously
						new Thread(new Runnable() {
							public void run() {
								Dot2Image.dot2image(panel.getGraph().getDot(), p.getLeft(), Type.pdf);
							}
						}).start();
						break;
					case pngImage :
						//save the file asynchronously
						new Thread(new Runnable() {
							public void run() {
								Dot2Image.dot2image(panel.getGraph().getDot(), p.getLeft(), Type.png);
							}
						}).start();
						break;
					case svgImage :
						//save the file asynchronously
						new Thread(new Runnable() {
							public void run() {
								Dot2Image.dot2image(panel.getGraph().getDot(), p.getLeft(), Type.svg);
							}
						}).start();
						break;
					case aviMovie :
					//save avi asynchronously
					{
						final SVGDiagram svg = panel.getGraph().getSVG();
						final ColourMode colourMode = state.getColourMode();
						final Dot dot = panel.getGraph().getDot();
						final TimedLog timedLog = state.getTimedLog();
						final AlignedLogVisualisationInfo info = state.getVisualisationInfo();
						new Thread(new Runnable() {
							public void run() {
								try {
									if (!ExportAnimation.saveAVItoFile(timedLog, info, colourMode, svg, dot, p.getA(),
											panel)) {
										System.out.println("deleted");
										p.getA().delete();
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();
					}
						break;
					case svgMovie :
					//save svg asynchronously
					{
						final SVGDiagram svg = panel.getGraph().getSVG();
						final ColourMode colourMode = state.getColourMode();
						final Dot dot = panel.getGraph().getDot();
						final TimedLog timedLog = state.getTimedLog();
						final AlignedLogVisualisationInfo info = state.getVisualisationInfo();
						new Thread(new Runnable() {
							public void run() {
								try {
									Canceller canceller = new Canceller() {
										public boolean isCancelled() {
											return false;
										}
									};
									ExportAnimation.saveSVGtoFile(timedLog, info, colourMode, svg, canceller, dot,
											p.getA());
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}).start();
					}
						break;
				}
			}
		});

		//set trace view button
		panel.getTraceViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel.getTraceView().swapVisibility();
			}
		});

		//set resource view button
		panel.getResourceViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel.getResourceView().swapVisibility();
			}
		});

		//set colouring filters button
		panel.getColouringFiltersViewButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				panel.getColouringFiltersView().swapVisibility();
			}
		});

		//set animation time updater
		panel.getGraph().setTimeStepCallback(new Callback<Double, Object>() {
			public Object call(Double arg) {
				Scaler scaler = state.getAnimationTimeScaler();
				if (scaler != null && scaler.getMin() != Long.MAX_VALUE && scaler.getMax() != Long.MIN_VALUE
						&& panel.getGraph().isEnableAnimation()) {
					panel.getAnimationTimeLabel().setText(
							TimestampsAdder.toString(state.getAnimationTimeScaler().scaleBack(arg)));
				} else {
					panel.getAnimationTimeLabel().setText(" ");
				}
				return null;
			}
		});
	}

	private static void makeNodesSelectable(AlignedLogVisualisationInfo info, InductiveVisualMinerPanel panel,
			Set<UnfoldedNode> selectedNodes, Set<LogMovePosition> selectedLogMoves) {
		for (LocalDotNode dotNode : info.getAllActivityNodes()) {
			panel.makeNodeSelectable(dotNode, selectedNodes.contains(dotNode.getUnode()));
		}
		for (LocalDotEdge logMoveEdge : info.getAllLogMoveEdges()) {
			panel.makeEdgeSelectable(logMoveEdge, selectedLogMoves.contains(LogMovePosition.of(logMoveEdge)));
		}
	}

	public synchronized void setStatus(String s) {
		panel.getStatusLabel().setText(s);
	}

	private static Triple<AlignedLog, AlignedLogInfo, IMLog> filterOnSelection(AlignedLog alignedLog,
			Set<UnfoldedNode> selectedNodes, Set<LogMovePosition> selectedLogMoves, IMLog xLog) {

		AlignedLog fl = new AlignedLog();
		boolean useNodes = !selectedNodes.isEmpty();
		boolean useLogMoves = !selectedLogMoves.isEmpty();
		for (AlignedTrace trace : alignedLog) {
			for (Move move : trace) {
				if (useNodes && move.isModelSync() && selectedNodes.contains(move.getUnode())) {
					fl.add(trace, alignedLog.getCardinalityOf(trace));
					break;
				}
				if (useLogMoves && move.isLogMove() && selectedLogMoves.contains(LogMovePosition.of(move))) {
					fl.add(trace, alignedLog.getCardinalityOf(trace));
					break;
				}
			}

		}

		AlignedLogInfo fli = new AlignedLogInfo(fl);
		return Triple.of(fl, fli, xLog);
	}

	/**
	 * Call all colouring filters to initialise their guis.
	 * 
	 * @param xLog
	 * @param executor
	 */
	private void initialiseColourFilters(final XLog xLog, Executor executor) {
		final Runnable onUpdate = new Runnable() {
			public void run() {
				chain.execute(FilterNodeSelection.class);
			}
		};
		for (final ColouringFilter colouringFilter : state.getColouringFilters()) {
			executor.execute(new Runnable() {
				public void run() {
					colouringFilter.initialiseFilter(xLog, onUpdate);
					panel.getColouringFiltersView().setPanel(colouringFilter, onUpdate);
				}
			});
		}
	}

	public static void debug(Object s) {
		System.out.println(s);
	}
}
