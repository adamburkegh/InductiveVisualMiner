package org.processmining.plugins.inductiveVisualMiner.popup;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;

public class PopupItemInputLogMove implements PopupItemInput<PopupItemInputLogMove> {

	private final LogMovePosition position;
	private final MultiSet<XEventClass> logMoves;

	public PopupItemInputLogMove(LogMovePosition position, MultiSet<XEventClass> logMoves) {
		this.position = position;
		this.logMoves = logMoves;
	}

	public PopupItemInputLogMove get() {
		return this;
	}

	public LogMovePosition getPosition() {
		return position;
	}

	public MultiSet<XEventClass> getLogMoves() {
		return logMoves;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PopupItemInputLogMove other = (PopupItemInputLogMove) obj;
		if (position == null) {
			if (other.position != null) {
				return false;
			}
		} else if (!position.equals(other.position)) {
			return false;
		}
		return true;
	}

}