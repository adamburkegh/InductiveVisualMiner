package org.processmining.plugins.inductiveVisualMiner.chain2;

import java.util.concurrent.Executor;

import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.InductiveMiner.graphs.Graph;
import org.processmining.plugins.InductiveMiner.graphs.GraphFactory;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;

/**
 * Generic class to perform any chain of computation jobs, which can be ordered
 * as a partially ordered graph.
 * 
 * @author sander
 *
 */
public class Chain2 {
	private final Graph<ChainLink2<?, ?>> graph = GraphFactory.create(ChainLink2.class, 13);
	private final InductiveVisualMinerState state;
	private final ProMCanceller globalCanceller;
	private final Executor executor;
	private final OnException onException;

	public Chain2(InductiveVisualMinerState state, ProMCanceller globalCanceller, Executor executor,
			OnException onException) {
		this.state = state;
		this.globalCanceller = globalCanceller;
		this.executor = executor;
		this.onException = onException;
	}

	public void addConnection(ChainLink2<?, ?> from, ChainLink2<?, ?> to) {
		graph.addEdge(from, to, 1);
	}

	/**
	 * Not thread safe. Only call from the main event thread.
	 * 
	 * @param clazz
	 */
	public synchronized void execute(Class<? extends ChainLink2<?, ?>> clazz) {
		//locate the chain link
		ChainLink2<?, ?> chainLink = getChainLink(clazz);
		if (chainLink == null) {
			return;
		}

		//invalidate all results that depend on this link
		cancelAndInvalidateResultRecursively(chainLink, state);

		//execute the link
		if (canExecute(chainLink)) {
			try {
				chainLink.execute(globalCanceller, executor, state, this);
			} catch (InterruptedException e) {
				onException.onException(e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Thread safe.
	 * 
	 * @param chainLink
	 */
	public synchronized void executeNext(ChainLink2<?, ?> chainLink) {
		for (long edge : graph.getOutgoingEdgesOf(chainLink)) {
			ChainLink2<?, ?> newChainLink = graph.getEdgeTarget(edge);

			//execute the link
			if (canExecute(newChainLink)) {
				try {
					newChainLink.execute(globalCanceller, executor, state, this);
				} catch (InterruptedException e) {
					onException.onException(e);
					e.printStackTrace();
				}
			}
		}
	}

	public boolean canExecute(ChainLink2<?, ?> chainLink) {
		for (long edge : graph.getIncomingEdgesOf(chainLink)) {
			if (!graph.getEdgeSource(edge).isComplete()) {
				return false;
			}
		}
		return true;
	}

	private ChainLink2<?, ?> getChainLink(Class<? extends ChainLink2<?, ?>> clazz) {
		for (ChainLink2<?, ?> chainLink : graph.getVertices()) {
			if (clazz.isInstance(chainLink)) {
				return chainLink;
			}
		}
		//assert (false);
		return null;
	}

	private void cancelAndInvalidateResultRecursively(ChainLink2<?, ?> chainLink, InductiveVisualMinerState state) {
		chainLink.cancelAndInvalidateResult(state);
		for (long edge : graph.getOutgoingEdgesOf(chainLink)) {
			cancelAndInvalidateResultRecursively(graph.getEdgeTarget(edge), state);
		}
	}
}
