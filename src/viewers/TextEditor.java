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

		comboBox = new ComboBoxFromFile(this, ValueFileParser.getTextLocationFile(), false);
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
				int width = metrics.stringWidth("*");
				int height = metrics.getHeight();

				g.setColor(Color.red);

				int x = width*16+2;
				// Characters per line limit
				g.drawLine(x, 0, x, textArea.getHeight());
				// Number of lines limit
				g.drawLine(0, height*64, x-1, height*64);
				// Line grouping
				g.setColor(Color.blue);
				for (int i=0; i<64/4; i++) {
					int y = i*height*4;
					g.drawLine(0, y, x-1, y);
				}
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
		int index = comboBox.getSelectedIndex();
		String description = "Text: " + comboBox.getParser().indexToName(index);
		ValueFileParser parser = comboBox.getParser();
		RomPointer p = new RomPointer(parser.indexToIntValue(0, index),
				parser.indexToIntValue(1, index));
		textParser = new TextParser(p.getPointedAddr(), p, description, false);

		textArea.setText(textParser.getText());
		textArea.setCaretPosition(0);
	}
}
