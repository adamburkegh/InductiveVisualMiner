package org.processmining.plugins.inductiveVisualMiner.dataanalysis;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecorator;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecorator.IvMPanel;

public class OnOffPanel<X extends JComponent> extends IvMPanel {

	private static final long serialVersionUID = -2730521457101145974L;

	private JPanel offPanel = new JPanel();
	private JLabel offMessage = new JLabel("off", SwingConstants.CENTER);
	private X onPanel;

	public OnOffPanel(X onPanel) {
		setLayout(new CardLayout());
		this.onPanel = onPanel;

		offPanel.setLayout(new BorderLayout());
		offPanel.setOpaque(false);
		IvMDecorator.decorate(offMessage);
		offMessage.setOpaque(false);
		offPanel.add(offMessage, BorderLayout.CENTER);
		add(offPanel, "off");

		add(onPanel, "on");

		on();
	}

	public void on() {
		((CardLayout) getLayout()).show(this, "on");
	}

	public void off() {
		((CardLayout) getLayout()).show(this, "off");
	}

	public String getOffMessage() {
		return offMessage.getText();
	}

	public void setOffMessage(String message) {
		offMessage.setText(message);
	}

	public X getOnPanel() {
		return onPanel;
	}

	public JPanel getOffPanel() {
		return offPanel;
	}
}