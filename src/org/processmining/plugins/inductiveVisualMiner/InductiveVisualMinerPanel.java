package org.processmining.plugins.inductiveVisualMiner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughDirectlyFollowsGraph;
import org.processmining.plugins.graphviz.colourMaps.ColourMapBlue;
import org.processmining.plugins.graphviz.colourMaps.ColourMapFixed;
import org.processmining.plugins.graphviz.colourMaps.ColourMapLightBlue;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotElement;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState.ColourMode;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisation;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationParameters;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.LocalDotNode;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.InputFunction;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.sizeMaps.SizeMapFixed;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.sizeMaps.SizeMapLinear;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import com.kitfox.svg.Group;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;

public class InductiveVisualMinerPanel extends JPanel {

	private static final long serialVersionUID = -1078786029763735572L;

	//gui elements
	private final DotPanel graphPanel;
	private final JComboBox<?> colourSelection;
	private final JLabel colourLabel;
	private final JLabel statusLabel;
	private final JTextArea selectionLabel;
	private final JCheckBox showDirectlyFollowsGraphs;
	private final NiceDoubleSlider activitiesSlider;
	private final NiceDoubleSlider noiseSlider;
	private final JLabel classifierLabel;
	private JComboBox<?> classifiersCombobox;
	private JButton exitButton;

	private final AlignedLogVisualisation visualiser;

	private InputFunction<Set<UnfoldedNode>> onSelectionChanged = null;

	public InductiveVisualMinerPanel(final PluginContext context, InductiveVisualMinerState state,
			Collection<XEventClassifier> classifiers) throws IOException {
		visualiser = new AlignedLogVisualisation();
		initVisualisationParameters();

		int gridy = 0;

		setLayout(new GridBagLayout());

		{
			activitiesSlider = SlickerFactory.instance().createNiceDoubleSlider("activities", 0, 1.0, 1.0,
					Orientation.VERTICAL);
			GridBagConstraints cActivitiesSlider = new GridBagConstraints();
			cActivitiesSlider.gridx = 1;
			cActivitiesSlider.gridy = gridy;
			cActivitiesSlider.fill = GridBagConstraints.VERTICAL;
			cActivitiesSlider.anchor = GridBagConstraints.EAST;
			add(getActivitiesSlider(), cActivitiesSlider);
		}

		{
			noiseSlider = SlickerFactory.instance().createNiceDoubleSlider("paths", 0, 1.0,
					1 - state.getMiningParameters().getNoiseThreshold(), Orientation.VERTICAL);
			GridBagConstraints cNoiseSlider = new GridBagConstraints();
			cNoiseSlider.gridx = 2;
			cNoiseSlider.gridy = gridy;
			cNoiseSlider.weighty = 1;
			cNoiseSlider.fill = GridBagConstraints.VERTICAL;
			cNoiseSlider.anchor = GridBagConstraints.WEST;
			add(getNoiseSlider(), cNoiseSlider);
		}

		gridy++;

		{
			boolean dfg = false;
			for (FallThrough f : state.getMiningParameters().getFallThroughs()) {
				dfg = dfg || f instanceof FallThroughDirectlyFollowsGraph;
			}
			showDirectlyFollowsGraphs = SlickerFactory.instance().createCheckBox(
					"Fall back to directly-follows graphs", dfg);
			showDirectlyFollowsGraphs.setEnabled(false);
			//			GridBagConstraints cShowDirectlyFollowsGraphs = new GridBagConstraints();
			//			cShowDirectlyFollowsGraphs.gridx = 1;
			//			cShowDirectlyFollowsGraphs.gridy = gridy++;
			//			cShowDirectlyFollowsGraphs.gridwidth = 2;
			//			cShowDirectlyFollowsGraphs.anchor = GridBagConstraints.NORTHWEST;
			//			add(showDirectlyFollowsGraphs, cShowDirectlyFollowsGraphs);
		}

		{
			classifierLabel = SlickerFactory.instance().createLabel("Classifier");
			GridBagConstraints cClassifierLabel = new GridBagConstraints();
			cClassifierLabel.gridx = 1;
			cClassifierLabel.gridy = gridy;
			cClassifierLabel.gridwidth = 1;
			cClassifierLabel.anchor = GridBagConstraints.WEST;
			add(classifierLabel, cClassifierLabel);

			classifiersCombobox = SlickerFactory.instance().createComboBox(classifiers.toArray());
			GridBagConstraints cClassifiers = new GridBagConstraints();
			cClassifiers.gridx = 2;
			cClassifiers.gridy = gridy++;
			cClassifiers.gridwidth = 1;
			cClassifiers.fill = GridBagConstraints.HORIZONTAL;
			add(classifiersCombobox, cClassifiers);
			classifiersCombobox.setSelectedItem(state.getMiningParameters().getClassifier());
		}

		{
			colourLabel = SlickerFactory.instance().createLabel("Show");
			GridBagConstraints cColourLabel = new GridBagConstraints();
			cColourLabel.gridx = 1;
			cColourLabel.gridy = gridy;
			cColourLabel.gridwidth = 1;
			cColourLabel.anchor = GridBagConstraints.WEST;
			add(colourLabel, cColourLabel);

			colourSelection = SlickerFactory.instance().createComboBox(ColourMode.values());
			GridBagConstraints ccolourSelection = new GridBagConstraints();
			ccolourSelection.gridx = 2;
			ccolourSelection.gridy = gridy++;
			ccolourSelection.gridwidth = 1;
			ccolourSelection.fill = GridBagConstraints.HORIZONTAL;
			add(colourSelection, ccolourSelection);
		}
		
		{
			JLabel saveLabel = SlickerFactory.instance().createLabel("Save");
			GridBagConstraints cExitButton = new GridBagConstraints();
			cExitButton.gridx = 1;
			cExitButton.gridy = gridy;
			cExitButton.gridwidth = 1;
			cExitButton.fill = GridBagConstraints.HORIZONTAL;
			add(saveLabel, cExitButton);
		}

		{
			exitButton = SlickerFactory.instance().createButton("model");
			GridBagConstraints cExitButton = new GridBagConstraints();
			cExitButton.gridx = 2;
			cExitButton.gridy = gridy++;
			cExitButton.gridwidth = 1;
			cExitButton.fill = GridBagConstraints.HORIZONTAL;
			add(exitButton, cExitButton);
			
			final JPanel panel = this;
			exitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//Custom button text
					Object[] options = {"Petri net",
					                    "Process tree"};
					int n = JOptionPane.showOptionDialog(panel,
					    "As what would you like to save the model?\nIt will become available in ProM.",
					    "Save",
					    JOptionPane.YES_NO_CANCEL_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,
					    options,
					    options[0]);
					
					System.out.println(n);
				}
			});
		}
		
		{
			JButton saveImage = SlickerFactory.instance().createButton("image/animation");
			GridBagConstraints cExitButton = new GridBagConstraints();
			cExitButton.gridx = 2;
			cExitButton.gridy = gridy++;
			cExitButton.gridwidth = 1;
			cExitButton.fill = GridBagConstraints.HORIZONTAL;
			add(saveImage, cExitButton);
			
			final JPanel panel = this;
			saveImage.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//Custom button text
					Object[] options = {"Model",
					                    "Image",
					                    "Animation"};
					int n = JOptionPane.showOptionDialog(panel,
					    "What would you like to save?",
					    "Save",
					    JOptionPane.YES_NO_CANCEL_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,
					    options,
					    null);
					
					System.out.println(n);
				}
			});
		}

		{
			selectionLabel = new JTextArea(" \n ");
			selectionLabel.setWrapStyleWord(true);
			selectionLabel.setLineWrap(true);
			selectionLabel.setEditable(false);
			selectionLabel.setOpaque(false);
			GridBagConstraints cSelectionLabel = new GridBagConstraints();
			cSelectionLabel.gridx = 1;
			cSelectionLabel.gridy = gridy++;
			cSelectionLabel.gridwidth = 2;
			cSelectionLabel.anchor = GridBagConstraints.NORTH;
			cSelectionLabel.fill = GridBagConstraints.HORIZONTAL;
			add(selectionLabel, cSelectionLabel);
		}

		{
			statusLabel = SlickerFactory.instance().createLabel(" ");
			GridBagConstraints cStatus = new GridBagConstraints();
			cStatus.gridx = 1;
			cStatus.gridy = gridy++;
			cStatus.gridwidth = 2;
			cStatus.anchor = GridBagConstraints.SOUTH;
			add(statusLabel, cStatus);
		}

		{
			Dot dot = new Dot();
			dot.addNode("Inductive Visual Miner", "");
			dot.addNode("Mining model...", "");
			graphPanel = new DotPanel(dot) {
				private static final long serialVersionUID = -3112819390640390685L;

				public void selectionChanged() {
					//selection of nodes changed; keep track of them

					Set<UnfoldedNode> result = new HashSet<UnfoldedNode>();
					for (DotElement dotElement : graphPanel.getSelectedElements()) {
						result.add(((LocalDotNode) dotElement).getUnode());
					}

					if (onSelectionChanged != null) {
						try {
							onSelectionChanged.call(result);
						} catch (Exception e) {
						}
					}
				}
			};
			GridBagConstraints cGraphPanel = new GridBagConstraints();
			cGraphPanel.gridx = 0;
			cGraphPanel.gridy = 0;
			cGraphPanel.gridheight = gridy;
			cGraphPanel.gridwidth = 1;
			cGraphPanel.weightx = 1;
			cGraphPanel.weighty = 1;
			cGraphPanel.fill = GridBagConstraints.BOTH;
			add(graphPanel, cGraphPanel);
		}
	}

	public synchronized Pair<Dot, AlignedLogVisualisationInfo> updateModel(InductiveVisualMinerState state)
			throws IOException {
		AlignedLogVisualisationParameters parameters = getViewParameters(state);
		Pair<Dot, AlignedLogVisualisationInfo> p = visualiser.fancy(state.getTree(), state.getAlignedFilteredLogInfo(),
				state.getDfgFilteredLogInfos(), parameters);
		graphPanel.changeDot(p.getLeft(), true);
		return p;
	}

	//==visualisation parameters==

	private static AlignedLogVisualisationParameters both = new AlignedLogVisualisationParameters();
	private static AlignedLogVisualisationParameters moves = new AlignedLogVisualisationParameters();
	private static AlignedLogVisualisationParameters paths = new AlignedLogVisualisationParameters();
	private static AlignedLogVisualisationParameters withoutAlignment = new AlignedLogVisualisationParameters();

	private static void initVisualisationParameters() {
		withoutAlignment.setColourModelEdges(null);
		withoutAlignment.setShowFrequenciesOnModelEdges(false);
		withoutAlignment.setShowFrequenciesOnNodes(false);
		withoutAlignment.setModelEdgesWidth(new SizeMapFixed(1));

		paths.setShowFrequenciesOnModelEdges(true);
		paths.setColourModelEdges(new ColourMapBlue());
		paths.setModelEdgesWidth(new SizeMapLinear(1, 3));
		paths.setShowFrequenciesOnMoveEdges(false);
		paths.setShowLogMoves(false);
		paths.setShowModelMoves(false);

		moves.setColourModelEdges(new ColourMapFixed("#BBBBFF"));
		moves.setColourNodes(new ColourMapLightBlue());

		both.setShowFrequenciesOnModelEdges(true);
		both.setShowFrequenciesOnMoveEdges(true);
		both.setColourModelEdges(new ColourMapFixed("#9999FF"));
		both.setColourMoves(new ColourMapFixed("#FF0000"));
		both.setRepairLogMoves(false);
	}

	public static AlignedLogVisualisationParameters getViewParameters(InductiveVisualMinerState state) {
		if (!state.isAlignmentReady()) {
			return withoutAlignment;
		}
		switch (state.getColourMode()) {
			case both :
				return both;
			case deviations :
				return moves;
			default :
				return paths;
		}
	}

	private class Selected {
		public String stroke;
		public String strokeWidth;
		public String strokeDashArray;
	}

	public void makeNodeSelectable(final SVGDiagram svg, final LocalDotNode dotNode, boolean select) {
		final Selected oldSelected = new Selected();
		dotNode.setSelectable(true);
		dotNode.addSelectionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Group svgGroup = DotPanel.getSVGElementOf(svg, dotNode);
				SVGElement shape = svgGroup.getChild(1);

				oldSelected.stroke = DotPanel.setCSSAttributeOf(shape, "stroke", "red");
				oldSelected.strokeWidth = DotPanel.setCSSAttributeOf(shape, "stroke-width", "3");
				oldSelected.strokeDashArray = DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", "5,5");

				DotPanel panel = (DotPanel) e.getSource();
				panel.repaint();
			}
		});
		dotNode.addDeselectionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Group svgGroup = DotPanel.getSVGElementOf(svg, dotNode);
				SVGElement shape = svgGroup.getChild(1);

				DotPanel.setCSSAttributeOf(shape, "stroke", oldSelected.stroke);
				DotPanel.setCSSAttributeOf(shape, "stroke-width", oldSelected.strokeWidth);
				DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", oldSelected.strokeDashArray);

				DotPanel panel = (DotPanel) e.getSource();
				panel.repaint();
			}
		});
		if (select) {
			graphPanel.select(dotNode);
		}
	}

	public DotPanel getGraph() {
		return graphPanel;
	}

	public JComboBox<?> getColourModeSelection() {
		return colourSelection;
	}

	public JLabel getStatusLabel() {
		return statusLabel;
	}

	public JComboBox<?> getClassifiers() {
		return classifiersCombobox;
	}

	public JComboBox<?> getColourSelection() {
		return colourSelection;
	}

	public JTextArea getSelectionLabel() {
		return selectionLabel;
	}

	public JCheckBox getShowDirectlyFollowsGraphs() {
		return showDirectlyFollowsGraphs;
	}

	public NiceDoubleSlider getNoiseSlider() {
		return noiseSlider;
	}

	public NiceDoubleSlider getActivitiesSlider() {
		return activitiesSlider;
	}

	public JButton getExitButton() {
		return exitButton;
	}

	public void setOnSelectionChanged(InputFunction<Set<UnfoldedNode>> onSelectionChanged) {
		this.onSelectionChanged = onSelectionChanged;
	}
}
