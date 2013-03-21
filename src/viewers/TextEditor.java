package viewers;

import base.TextParser;
import record.RomPointer;
import base.ValueFileParser;
import javax.swing.*;
import java.awt.*;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.*;
import graphics.*;

public class TextEditor extends JDialog {
	ComboBoxFromFile comboBox;
	JScrollPane textPane;
	JTextArea textArea;
	TextParser textParser;

	public TextEditor(JFrame owner) {
		super(owner, "Text Editor", Dialog.ModalityType.APPLICATION_MODAL);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new GridBagLayout());

		comboBox = new ComboBoxFromFile(this, ComboBoxFromFile.textLocationFile, false);
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetTextParser();
			}
		});

		textArea = new JTextArea("", 30, 16) {
			public void paint(Graphics g2) {
				Graphics2D g = (Graphics2D)g2;
				super.paint(g);
				FontMetrics metrics = g.getFontMetrics(textArea.getFont());
				int x = metrics.stringWidth("*")*16+2;
				g.setColor(Color.red);

				/*
				int height=5;
				int gap=13;
				int y=0;
				while (y < textArea.getHeight()) {
					g.drawLine(x, y, x, y+height);
					y = y+height+gap;
				}
				*/
				g.drawLine(x, 0, x, textArea.getHeight());
			}
		};
		textArea.setFont(new Font("monospaced", 0, 12));

		resetTextParser();

		textPane = new JScrollPane(textArea);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = 0;
		cons.gridy = GridBagConstraints.RELATIVE;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0;

		cons.weighty = 0.0;
		contentPane.add(comboBox, cons);
		cons.weighty = 1.0;
		contentPane.add(textPane, cons);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textParser.setText(textArea.getText()) == 0) {
					textParser.save();
					setVisible(false);
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		cons.gridx = 0;
		cons.gridy = 2;
		cons.weightx = 0.0;
		cons.weighty = 0.0;

		contentPane.add(okButton, cons);
		contentPane.add(Box.createRigidArea(new Dimension(10, 0)));
		cons.gridx = 1;
		contentPane.add(cancelButton, cons);

		contentPane.add(Box.createHorizontalGlue());

		add(contentPane);
		pack();
		setVisible(true);
	}

	void resetTextParser() {
		ValueFileParser parser = comboBox.getParser();
		RomPointer p = new RomPointer(parser.indexToValue(comboBox.getSelectedIndex()),
				Integer.parseInt(parser.indexToAssociate(0, comboBox.getSelectedIndex()), 16));
		textParser = new TextParser(p.getPointedAddr(), p, false);

		textArea.setText(textParser.getText());
		textArea.setCaretPosition(0);
	}
}
