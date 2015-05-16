package org.processmining.plugins.inductiveVisualMiner.Chain;

import java.util.UUID;
import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

import nl.tue.astar.AStarThread.Canceller;

import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;

public abstract class ChainLink<I, O> {

	private Chain chain;
	private Executor executor;
	private Runnable onStart;
	private Runnable onComplete;
	protected ResettableCanceller canceller = new ResettableCanceller();
	
	public static class ResettableCanceller implements Canceller {

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

	/**
	 * 
	 * @return
	 * 
	 *         Gathers all inputs required for the computation
	 */
	protected abstract I generateInput(InductiveVisualMinerState state);

	/**
	 * 
	 * @param input
	 * @return
	 * 
	 *         Performs the computation, given the input. Side-effects not
	 *         allowed; should be thread-safe and static
	 */
	protected abstract O executeLink(I input);

	/**
	 * 
	 * @param result
	 * 
	 *            Processes the result of the computation. Guarantee: if
	 *            executed, then all inputs are still relevant and have not been
	 *            replaced.
	 */
	protected abstract void processResult(O result, InductiveVisualMinerState state);

	public void execute(final UUID execution, final int indexInChain, final InductiveVisualMinerState state) {
		final I input = generateInput(state);

		executor.execute(new Runnable() {
			public void run() {
				if (onStart != null) {
					SwingUtilities.invokeLater(onStart);
				}
				canceller.reset();
				final O result = executeLink(input);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (chain.getCurrentExecution().equals(execution)) {
							processResult(result, state);
							if (onComplete != null) {
								onComplete.run();
							}
							chain.executeNext(execution, indexInChain);
						}
					}
				});
			}
		});
	}

	public void cancel() {
		canceller.cancel();
	}

	/**
	 * Sets a callback that is executed on start of execution. Will be executed
	 * in the main (gui) thread.
	 * 
	 * @param onStart
	 */
	public void setOnStart(Runnable onStart) {
		this.onStart = onStart;
	}

	/**
	 * Sets a callback that is executed on completion of execution. Will be
	 * executed in the main (gui) thread.
	 * 
	 * @param onStart
	 */
	public void setOnComplete(Runnable onComplete) {
		this.onComplete = onComplete;
	}

	public void setExecutor(Executor executor, Chain chain) {
		this.executor = executor;
		this.chain = chain;
	}
}