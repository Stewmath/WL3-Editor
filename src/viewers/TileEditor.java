package viewers;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import javax.swing.border.BevelBorder;

import javax.swing.filechooser.FileNameExtensionFilter;

import viewers.TileGridViewer;
import graphics.*;
import record.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class TileEditor extends JDialog implements TileGridViewerClient {
	TileEditor itself = this;
	
	JPanel tileEditPanel;
	JRadioButton[] colorButtons;
	JRadioButton blackWhiteButton;
	JRadioButton[] paletteButtons;
	JRadioButton[] arrangementButtons;
	JTextArea offsetLabel;
	JLabel statusLabel;

	JButton[] paletteEditButtons;

	TileGridViewer tileGridViewer;

	byte[] tileData;
	int[][] palettes;
	int numPalettes;
	BufferedImage[] tileImages;

	int selectedTile;
	int selectedColor;
	int selectedPalette;

	int tileScale = 9;

	boolean disableListeners = false;
	boolean ok=false;

	public TileEditor(byte[] _tileData, int[][] _palettes) {
		super(null, "Tile Editor", Dialog.ModalityType.APPLICATION_MODAL);
		tileData = _tileData;
		if (_palettes == null) {
			numPalettes = 0;
			palettes = Drawing.defaultPalette;
		}
		else {
			numPalettes=_palettes.length;
			palettes = new int[numPalettes][4];
			for (int i=0; i<numPalettes; i++) {
				for (int j=0; j<4; j++)
					palettes[i][j] = _palettes[i][j];
			}
		}

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		JPanel tileViewerPanel = new JPanel();

		tileImages = RomReader.binToTiles(tileData, selectedPalette, palettes);
		tileGridViewer = new TileGridViewer(tileImages, 16, 2, this);
		tileGridViewer.setSelectionColor(Color.blue);
		//tileGridViewer.setPreferredSize(new Dimension(16*16, 16*16+16));
		tileViewerPanel.add(tileGridViewer);
		tileViewerPanel.setBorder(BorderFactory.createTitledBorder("Tiles"));

		JPanel colorPanel = new JPanel();
		colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));
		colorPanel.setBorder(BorderFactory.createTitledBorder("Colours"));
		colorButtons = new JRadioButton[4];
		ButtonGroup colorGroup = new ButtonGroup();
		ActionListener colorActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i=0; i<4; i++) {
					if (e.getSource() == colorButtons[i]) {
						colorButtons[i].getModel().setPressed(true);
						selectedColor = i;
					}
				}
			}
		};
		for (int i=0; i<4; i++) {
			colorButtons[i] = new JRadioButton();
			colorButtons[i].addActionListener(colorActionListener);
			colorGroup.add(colorButtons[i]);
			colorPanel.add(colorButtons[i]);
		}
		colorButtons[0].setSelected(true);
		tileEditPanel = new JPanel() {
			public void paintComponent(Graphics g) {
				g.drawImage(tileImages[selectedTile], 0, 0, 8*tileScale, 8*tileScale, null);
			}
		};
		tileEditPanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int x = e.getX()/tileScale;
				int y = e.getY()/tileScale;

				if (e.getButton() == MouseEvent.BUTTON1) {
					drawOnTile(x, y);
				}
				else if (e.getButton() == MouseEvent.BUTTON3) {
					int b1 = tileData[selectedTile*16+2*y];
					int b2 = tileData[selectedTile*16+2*y+1];
					selectedColor = (b1>>(7-x))&1;
					selectedColor |= ((b2>>(7-x))&1)<<1;
					colorButtons[selectedColor].setSelected(true);
				}
			}
			public void mouseReleased(MouseEvent e) {
				refreshTiles();
			}
		});
		tileEditPanel.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				int x = e.getX()/tileScale;
				int y = e.getY()/tileScale;

				drawOnTile(x, y);
			}
		});
		tileEditPanel.setPreferredSize(new Dimension(8*tileScale, 8*tileScale));
		tileEditPanel.setMaximumSize(new Dimension(8*tileScale, 8*tileScale));

		JPanel tileEditSuperPanel = new JPanel();
		tileEditSuperPanel.add(tileEditPanel);
		tileEditSuperPanel.setBorder(BorderFactory.createTitledBorder("Draw here"));

		// I'm such a creative namer.
		JPanel tileEditSuperSuperPanel = new JPanel();
		tileEditSuperSuperPanel.add(tileEditSuperPanel);

		JPanel offsetPanel = new JPanel();
		offsetPanel.setLayout(new BoxLayout(offsetPanel, BoxLayout.Y_AXIS));
		offsetPanel.setBorder(BorderFactory.createTitledBorder("Offsets (for Tile Layer Pro)"));
		offsetPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		offsetLabel = new JTextArea("Offsets");
		offsetLabel.setEditable(false);
		offsetLabel.setBorder(null);
		offsetLabel.setBackground(getBackground());
		offsetPanel.add(offsetLabel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok = true;
				setVisible(false);
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok = false;
				setVisible(false);
			}
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);


		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusLabel = new JLabel("Selected tile: 00");
		statusLabel.setFont(new Font("monospaced", Font.BOLD, 12));
		statusLabel.setPreferredSize(new Dimension(getWidth(), 16));
		statusPanel.add(statusLabel);

		centerPanel.add(tileViewerPanel);
		centerPanel.add(tileEditSuperSuperPanel);
		centerPanel.add(colorPanel);
		centerPanel.add(offsetPanel);
		centerPanel.add(buttonPanel);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

		JPanel arrangementPanel = new JPanel();
		arrangementPanel.setLayout(new BoxLayout(arrangementPanel, BoxLayout.Y_AXIS));
		ButtonGroup arrangementGroup = new ButtonGroup();
		ActionListener arrangementActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i=0; i<TileGridViewer.NUM_ARRANGEMENTS; i++) {
					if (e.getSource() == arrangementButtons[i]) {
						arrangementButtons[i].getModel().setPressed(true);
						tileGridViewer.setArrangement(i);
					}
				}
			}
		};
		arrangementButtons = new JRadioButton[TileGridViewer.NUM_ARRANGEMENTS];
		arrangementButtons[0] = new JRadioButton("8x8 (Tiles)");
		arrangementButtons[1] = new JRadioButton("8x16 (Sprites)");
		for (int i=0; i<TileGridViewer.NUM_ARRANGEMENTS; i++) {
			arrangementButtons[i].addActionListener(arrangementActionListener);
			arrangementGroup.add(arrangementButtons[i]);
			arrangementPanel.add(arrangementButtons[i]);
		}
		arrangementButtons[tileGridViewer.getArrangement()].setSelected(true);
		arrangementPanel.setBorder(BorderFactory.createTitledBorder("Arrangement"));
		rightPanel.add(arrangementPanel);

		if (numPalettes > 0) {
			JPanel palettePanel = new JPanel();
			palettePanel.setLayout(new BoxLayout(palettePanel, BoxLayout.Y_AXIS));
			paletteButtons = new JRadioButton[numPalettes];
			ButtonGroup paletteGroup = new ButtonGroup();
			ActionListener paletteListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!disableListeners) {
						int i=0;
						while (paletteButtons[i] != e.getSource())
							i++;
						setSelectedPalette(i);
					}
				}
			};
			blackWhiteButton = new JRadioButton("B&W");
			blackWhiteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!disableListeners) {
						setSelectedPalette(-1);
					}
				}
			});
			paletteGroup.add(blackWhiteButton);
			palettePanel.add(blackWhiteButton);
			for (int i=0; i<numPalettes; i++) {
				paletteButtons[i] = new JRadioButton("Palette " + RomReader.toHexString(i));
				paletteButtons[i].addActionListener(paletteListener);
				paletteGroup.add(paletteButtons[i]);
				palettePanel.add(paletteButtons[i]);
			}
			palettePanel.setBorder(BorderFactory.createTitledBorder("View with.."));
			rightPanel.add(palettePanel);

			JPanel paletteEditPanel = new JPanel();
			paletteEditPanel.setLayout(new BoxLayout(paletteEditPanel, BoxLayout.Y_AXIS));
			ActionListener paletteEditListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (numPalettes > 0) {
						JButton button = (JButton)e.getSource();
						Color newColor = JColorChooser.showDialog(itself, "Choose a color", button.getBackground());
						if (newColor != null) {
							button.setBackground(newColor);
							for (int j=0; j<4; j++) {
								if (paletteEditButtons[j] == button) {
									palettes[selectedPalette][j] = newColor.getRGB();
									break;
								}
							}
							refreshPalettes();
						}
					}
				}
			};
			paletteEditButtons = new JButton[4];
			for (int j=0; j<4; j++) {
				paletteEditButtons[j] = new JButton();
				paletteEditButtons[j].addActionListener(paletteEditListener);
				//paletteEditButtons[j].setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
				//paletteButton.setPreferredSize(new Dimension(32, 32));
				paletteEditPanel.add(paletteEditButtons[j]);
			}
			JPanel paletteEditButtonPanel = new JPanel();
			paletteEditButtonPanel.setLayout(new BoxLayout(paletteEditButtonPanel, BoxLayout.Y_AXIS));
			JButton copyButton = new JButton("Copy");
			copyButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (int j=0; j<4; j++) {
						int color = (selectedPalette==-1 ? Drawing.defaultPalette[0][j] : palettes[selectedPalette][j]);
						PaletteEditorPanel.paletteBuffer[j] = color;
					}
				}
			});
			JButton pasteButton = new JButton("Paste");
			pasteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (int j=0; j<4; j++) {
						if (selectedPalette != -1) {
							palettes[selectedPalette][j] = PaletteEditorPanel.paletteBuffer[j];
							refreshPalettes();
						}
					}
				}
			});
			paletteEditButtonPanel.add(copyButton);
			paletteEditButtonPanel.add(pasteButton);

			JPanel paletteEditSuperPanel = new JPanel();
			paletteEditSuperPanel.setLayout(new BoxLayout(paletteEditSuperPanel, BoxLayout.X_AXIS));
			paletteEditSuperPanel.add(paletteEditPanel, BorderLayout.WEST);
			paletteEditSuperPanel.add(paletteEditButtonPanel);
			rightPanel.add(paletteEditSuperPanel);
		}
		setSelectedPalette(-1);

		JButton exportButton = new JButton("Export...");
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RomReader.exportData(tileData,
						"Export Graphics",
						new FileNameExtensionFilter("Binary file", "bin"));
				if (numPalettes > 0)
					RomReader.exportData(RomReader.palettesToRGB24(palettes, numPalettes),
							"Export Palettes",
							new FileNameExtensionFilter("Palette file", "pal"));
			}
		});
		rightPanel.add(exportButton);

		JButton importButton = new JButton("Import...");
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				byte[] data = RomReader.importData("Import Graphics",
						new FileNameExtensionFilter("Binary file", "bin"));
				if (data != null) {
					tileData = Arrays.copyOf(data, tileData.length);
					refreshTiles();
				}
			}
		});
		rightPanel.add(importButton);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(centerPanel, BorderLayout.CENTER);
		contentPane.add(rightPanel, BorderLayout.EAST);

		setLayout(new BorderLayout());
		add(contentPane, BorderLayout.CENTER);
		add(statusPanel, BorderLayout.SOUTH);
		pack();
	}

	public void setOffsetText(String s) {
		offsetLabel.setText(s);
		pack();
	}

	public void setSelectedPalette(int p) {
		disableListeners = true;
		if (p == -1) {
			selectedPalette = p;
			blackWhiteButton.setSelected(true);
		}
		else if (p < numPalettes) {
			selectedPalette = p;
			paletteButtons[p].setSelected(true);
		}
		refreshPalettes();
		disableListeners = false;
	}

	public void setArrangement(int a) {
		arrangementButtons[a].setSelected(true);
		tileGridViewer.setArrangement(a);
	}

	void drawOnTile(int x, int y) {
		if (!(x >= 0 && x < 8 && y >= 0 && y < 8))
			return;
		int index = (selectedTile)*16+y*2;
		int b1 = tileData[index];
		int b2 = tileData[index+1];
		int bit = 1<<(7-x);
		b1 &= ~bit;
		b2 &= ~bit;
		b1 |= (selectedColor&1)<<(7-x);
		b2 |= (selectedColor>>1)<<(7-x);
		tileData[index] =  (byte)b1;
		tileData[index+1] = (byte)b2;

		int color = (selectedPalette==-1 ? Drawing.defaultPalette[0][selectedColor] : palettes[selectedPalette][selectedColor]);
		tileImages[selectedTile].setRGB(x, y, color);
		repaint();
	}

	void refreshTiles() {
		if (selectedPalette == -1)
			tileImages = RomReader.binToTiles(tileData, 0, Drawing.defaultPalette);
		else
			tileImages = RomReader.binToTiles(tileData, selectedPalette, palettes);
		tileGridViewer.setTiles(tileImages, 16, 2);
		repaint();
	}

	void refreshPalettes() {
		refreshTiles();
		for (int i=0; i<4; i++) {
			int color = (selectedPalette==-1 ? Drawing.defaultPalette[0][i] : palettes[selectedPalette][i]);
			colorButtons[i].setBackground(new Color(color));
			if (numPalettes > 0) {
				paletteEditButtons[i].setBackground(new Color(color));
			}
		}
	}

	// TileGridViewerClient
	public void tileSelectionChanged(int selection) {
		selectedTile = selection;
		repaint();
	}

	public void tileHoverChanged(int selection) {
		if (selection == -1)
			statusLabel.setText("Selected tile: " + RomReader.toHexString(selectedTile, 2));
		else
			statusLabel.setText("Viewing tile:  " + RomReader.toHexString(selection, 2));
	}

	public byte[] getTileData() {
		return tileData;
	}

	public byte[] getPaletteData() {
		return RomReader.palettesToBin(palettes, numPalettes);
	}

	public int[][] getPalettes() {
		return palettes;
	}

	public boolean clickedOk() {
		return ok;
	}
}
