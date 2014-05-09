package org.processmining.plugins.inductiveVisualMiner.helperClasses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * @author sleemans
 * 
 */
public class Chain {

	private final List<ChainLink<?, ?>> chain = new ArrayList<ChainLink<?, ?>>();
	private final Executor executor;
	private UUID currentExecutionId;
	private int currentExecutionLinkNumber;
	private ChainLink currentExecutionLink;

	public Chain(Executor executor) {
		this.executor = executor;
		currentExecutionId = null;
		currentExecutionLinkNumber = Integer.MAX_VALUE;
		currentExecutionLink = null;
	}

	public void add(ChainLink<?, ?> link) {
		link.setExecutor(executor, this);
		chain.add(link);
	}

	public synchronized void executeNext(UUID execution, final int indexInChain) {
		//execute next link in the chain
		if (currentExecutionId.equals(execution)) {
			if (indexInChain + 1 < chain.size()) {
				currentExecutionLinkNumber = indexInChain + 1;
				currentExecutionLink = chain.get(indexInChain + 1);
				chain.get(indexInChain + 1).execute(currentExecutionId, indexInChain + 1);
			} else {
				currentExecutionLink = null;
			}
		}
	}

	public synchronized void execute(Class<? extends ChainLink<?, ?>> c) {
		for (int i = 0; i < chain.size(); i++) {
			ChainLink<?, ?> cl = chain.get(i);
			if (c.isInstance(cl)) {
				//see if this chain execution should overwrite (= starts earlier in the chain) than the next one
				//if we drop in ahead of an existing execution, our work will have to be redone again anyway 
				if (i <= currentExecutionLinkNumber) {

					//cancel current execution
					if (currentExecutionLink != null) {
						currentExecutionLink.cancel();
					}

					//replace execution
					currentExecutionId = UUID.randomUUID();
					currentExecutionLinkNumber = i;
					currentExecutionLink = cl;

					cl.execute(currentExecutionId, currentExecutionLinkNumber);
				}
				return;
			}
		}
	}

	public UUID getCurrentExecution() {
		return currentExecutionId;
	}
}