package graphics;

import java.awt.BorderLayout;

import javax.swing.*;
import java.awt.event.*;
import java.awt.Color;

public class PaletteEditorPanel extends JPanel {
	public static int[] paletteBuffer = new int[4];

	PaletteEditorPanel itself = this;

	JPanel palettePanel, palettePanel1, palettePanel2;
	JButton[][] paletteButtons;
	JButton[] copyButtons;
	JButton[] pasteButtons;
	int[][] palettes;
	int numPalettes;
	PaletteEditorClient client;

	public PaletteEditorPanel(int[][] _palettes, boolean oneColumn, PaletteEditorClient _client) {
		palettes = _palettes;
		client = _client;

		numPalettes = palettes.length;

		palettePanel = new JPanel();
		palettePanel1 = new JPanel();
		palettePanel2 = new JPanel();
		palettePanel.setBorder(BorderFactory.createTitledBorder("Palettes"));
		palettePanel.setLayout(new BorderLayout());

		palettePanel1.setLayout(new BoxLayout(palettePanel1, BoxLayout.Y_AXIS));
		palettePanel2.setLayout(new BoxLayout(palettePanel2, BoxLayout.Y_AXIS));

		paletteButtons = new JButton[numPalettes][4];
		copyButtons = new JButton[numPalettes];
		pasteButtons = new JButton[numPalettes];
		ActionListener paletteButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton)e.getSource();
				Color newColor = JColorChooser.showDialog(itself, "Choose a color", button.getBackground());
				if (newColor != null) {
					button.setBackground(newColor);
					// We need to figure out which palette this button is for, so search...
					for (int i=0; i<numPalettes; i++) {
						for (int j=0; j<4; j++) {
							if (paletteButtons[i][j] == button) {
								palettes[i][j] = newColor.getRGB();
								break;
							}
						}
					}
					if (client != null)
						client.setPalettes(palettes);
					refreshPaletteButtons();
				}
			}
		};
		ActionListener copyListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton)e.getSource();
				int i;
				for (i=0; i<numPalettes; i++) {
					if (copyButtons[i] == button)
						break;
				}
				for (int j=0; j<4; j++) {
					paletteBuffer[j] = palettes[i][j];
				}
			}
		};
		ActionListener pasteListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton)e.getSource();
				int i;
				for (i=0; i<numPalettes; i++) {
					if (pasteButtons[i] == button)
						break;
				}
				for (int j=0; j<4; j++) {
					palettes[i][j] = paletteBuffer[j];
				}
				refreshPaletteButtons();
				if (client != null)
					client.setPalettes(palettes);
			}
		};
		for (int i=0; i<numPalettes; i++) {
			JPanel superPanel = new JPanel();
			superPanel.setLayout(new BoxLayout(superPanel, BoxLayout.X_AXIS));
			JPanel subPanel = new JPanel();
			subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
			subPanel.setBorder(BorderFactory.createTitledBorder("" + i));

			for (int j=0; j<4; j++) {
				paletteButtons[i][j] = new JButton();
				paletteButtons[i][j].addActionListener(paletteButtonActionListener);
				paletteButtons[i][j].setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
				//paletteButton.setPreferredSize(new Dimension(32, 32));
				subPanel.add(paletteButtons[i][j]);
			}

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
			copyButtons[i] = new JButton("Copy");
			copyButtons[i].addActionListener(copyListener);
			pasteButtons[i] = new JButton("Paste");
			pasteButtons[i].addActionListener(pasteListener);
			buttonPanel.add(copyButtons[i]);
			buttonPanel.add(pasteButtons[i]);

			superPanel.add(subPanel);
			superPanel.add(buttonPanel);

			if (oneColumn || i < 4)
				palettePanel1.add(superPanel);
			else
				palettePanel2.add(superPanel);
		}
		refreshPaletteButtons();

		palettePanel.add(palettePanel1, BorderLayout.WEST);
		palettePanel.add(palettePanel2, BorderLayout.EAST);
		add(palettePanel);
		//palettePanel.setPreferredSize(new java.awt.Dimension(75, palettePanel.getPreferredSize().height));
	}

	public void setOrientation(boolean horizontal) {
		if (horizontal) {
			palettePanel.setLayout(new BoxLayout(palettePanel, BoxLayout.Y_AXIS));
			palettePanel1.setLayout(new BoxLayout(palettePanel1, BoxLayout.X_AXIS));
			palettePanel2.setLayout(new BoxLayout(palettePanel2, BoxLayout.X_AXIS));
		}
		else {
			palettePanel.setLayout(new BoxLayout(palettePanel, BoxLayout.X_AXIS));
			palettePanel1.setLayout(new BoxLayout(palettePanel1, BoxLayout.Y_AXIS));
			palettePanel2.setLayout(new BoxLayout(palettePanel2, BoxLayout.Y_AXIS));
		}
	}
	public int[][] getPalettes() {
		return palettes;
	}
	public void setPalettes(int[][] _palettes) {
		palettes = _palettes;
		refreshPaletteButtons();
	}
	void refreshPaletteButtons() {
		for (int i=0; i<numPalettes; i++) {
			for (int j=0; j<4; j++) {
				paletteButtons[i][j].setBackground(new Color(palettes[i][j]));
			}
		}
	}
}
