package org.processmining.plugins.inductiveVisualMiner.dataanalysis.causal;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeUtils;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;

public class EfficientTree2CausalGraph {
	public static Dot convert(EfficientTree tree, IvMLogInfo logInfo) {
		//first, figure out how often each node was maximally executed in a trace in the log
		int[] k = new int[tree.getMaxNumberOfNodes()];
		for (int node : EfficientTreeUtils.getAllNodes(tree)) {
			k[node] = 5;
		}

		Dot result = new Dot();

		//create dot nodes
		THashMap<Choice, DotNode> choice2dotNode = new THashMap<>();
		List<Choice> choices = getChoices(tree, tree.getRoot(), new TIntArrayList(), k);
		for (Choice choice : choices) {
			DotNode dotNode = result.addNode(choice.getId());
			choice2dotNode.put(choice, dotNode);
		}

		//create dot edges
		getGraph(result, tree, tree.getRoot(), new TIntArrayList(), k, choice2dotNode);

		System.out.println(result.toString());

		return result;
	}

	public static void getGraph(Dot dot, EfficientTree tree, int node, TIntList ids, int[] k,
			THashMap<Choice, DotNode> choice2dotNode) {
		if (tree.isActivity(node) || tree.isTau(node)) {
			return;
		}

		assert tree.isOperator(node);

		if (tree.isConcurrent(node) || tree.isInterleaved(node) || tree.isOr(node)) {
			//simply recurse
			for (int child : tree.getChildren(node)) {
				getGraph(dot, tree, child, ids, k, choice2dotNode);
			}
		} else if (tree.isXor(node)) {
			//first simply recurse
			for (int child : tree.getChildren(node)) {
				getGraph(dot, tree, child, ids, k, choice2dotNode);
			}

			//for xor, the choices of the children depend on the choice made in the xor (with a 1 causality, but it is a causal relation)
			Choice choiceA = getXorChoice(tree, node, ids);
			DotNode dotNodeA = choice2dotNode.get(choiceA);
			for (int childB : tree.getChildren(node)) {
				List<Choice> choicesChildB = getChoices(tree, childB, ids, k);

				for (Choice choiceB : choicesChildB) {
					DotNode dotNodeB = choice2dotNode.get(choiceB);
					dot.addEdge(dotNodeA, dotNodeB);
				}
			}

		} else if (tree.isSequence(node)) {
			//first simply recurse
			for (int child : tree.getChildren(node)) {
				getGraph(dot, tree, child, ids, k, choice2dotNode);
			}

			//for sequence, every choice depends on all choices before it
			for (int childIndexA = 0; childIndexA < tree.getNumberOfChildren(node); childIndexA++) {
				int childA = tree.getChild(node, childIndexA);
				List<Choice> choicesChildA = getChoices(tree, childA, ids, k);

				if (choicesChildA.size() > 0) {
					for (int childIndexB = childIndexA + 1; childIndexB < tree
							.getNumberOfChildren(node); childIndexB++) {
						int childB = tree.getChild(node, childIndexB);
						List<Choice> choicesChildB = getChoices(tree, childB, ids, k);

						for (Choice choiceA : choicesChildA) {
							for (Choice choiceB : choicesChildB) {
								DotNode dotNodeA = choice2dotNode.get(choiceA);
								DotNode dotNodeB = choice2dotNode.get(choiceB);
								dot.addEdge(dotNodeA, dotNodeB);
							}
						}
					}
				}
			}
		} else if (tree.isLoop(node)) {
			//for loop, we need to recurse on unfoldings up to a certain length k
			for (int j = 0; j < k[node]; j++) {
				//unfold anc recurse the children's choices
				TIntArrayList childIds = new TIntArrayList(ids);
				childIds.add(node);
				childIds.add(j);

				for (int child : tree.getChildren(node)) {
					getGraph(dot, tree, child, childIds, k, choice2dotNode);
				}
			}

			//second, add all dependencies between the choices of children resulting from unfoldings
			for (int jA = 0; jA < k[node] - 1; jA++) {
				for (int childA : tree.getChildren(node)) {

					TIntArrayList childIdsA = new TIntArrayList(ids);
					childIdsA.add(node);
					childIdsA.add(jA);
					List<Choice> choicesChildA = getChoices(tree, childA, childIdsA, k);
					choicesChildA.add(getLoopChoice(tree, node, ids, jA));

					for (int jB = jA + 1; jB < k[node]; jB++) {
						for (int childB : tree.getChildren(node)) {

							TIntArrayList childIdsB = new TIntArrayList(ids);
							childIdsB.add(node);
							childIdsB.add(jB);
							List<Choice> choicesChildB = getChoices(tree, childB, childIdsB, k);
							choicesChildB.add(getLoopChoice(tree, node, ids, jB));

							for (Choice choiceA : choicesChildA) {
								for (Choice choiceB : choicesChildB) {
									dot.addEdge(choice2dotNode.get(choiceA), choice2dotNode.get(choiceB));
								}
							}

						}
					}
				}
			}
		} else {
			assert false;
		}
	}

	public static List<Choice> getChoices(EfficientTree tree, int node, TIntList ids, int[] k) {
		List<Choice> result = new ArrayList<>();
		if (tree.isTau(node) || tree.isActivity(node)) {
			return result;
		}

		assert tree.isOperator(node);
		if (tree.isLoop(node)) {
			//for loop, we need to unfold choices up to a certain length k
			for (int j = 0; j < k[node]; j++) {
				//unfold the children's choices
				{
					TIntArrayList childIds = new TIntArrayList(ids);
					childIds.add(node);
					childIds.add(j);

					for (int child : tree.getChildren(node)) {
						result.addAll(getChoices(tree, child, childIds, k));
					}
				}

				//add a choice for this unfolding
				Choice choice = getLoopChoice(tree, node, ids, j);
				result.add(choice);
			}
		} else {
			for (int child : tree.getChildren(node)) {
				result.addAll(getChoices(tree, child, ids, k));
			}

			if (tree.isXor(node)) {
				Choice choice = getXorChoice(tree, node, ids);
				result.add(choice);
			} else if (tree.isOr(node)) {
				//for the or, there's an individual choice to do every child
				for (int child : tree.getChildren(node)) {
					Choice choice = new Choice();
					choice.nodes.add(child);
					choice.ids.addAll(ids);
					result.add(choice);
				}
			} else {
				//the other operators add no choices

			}
		}

		return result;
	}

	private static Choice getLoopChoice(EfficientTree tree, int node, TIntList ids, int j) {
		TIntArrayList childIds = new TIntArrayList(ids);
		childIds.add(node);
		childIds.add(j);
		Choice choice = new Choice();
		choice.nodes.add(tree.getChild(node, 1));
		choice.nodes.add(tree.getChild(node, 2));
		choice.ids.addAll(childIds);
		return choice;
	}

	private static Choice getXorChoice(EfficientTree tree, int node, TIntList ids) {
		//an xor actually adds a choice
		Choice choice = new Choice();
		for (int child : tree.getChildren(node)) {
			choice.nodes.add(child);
		}
		choice.ids.addAll(ids);
		return choice;
	}
}