package viewers;

import record.*;
import viewerclients.TileSetViewerClient;
import base.*;
import graphics.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;

// This file contains a couple classes:
// MetaTileEditor:
// This class edits the metatiles, which are 2x2 squares made up of 8x8 tiles, making one big 16x16 tile.
//
// SubTileViewer:
// Like TileSetViewer, for the smaller 8x8 tiles.
public class MetaTileEditor extends JDialog implements PaletteEditorClient {
	MetaTileEditor itself = this;
	JFrame parent;

	JTextField tileSetField;
	JTextField metaTileField;
	JTextField flagField;
	JTextField gfxData0Field;
	JTextField gfxData1Field;
	JTextField paletteDataField;

	JPanel metaTilePanel;
	PaletteEditorPanel palettePanel;
	
	JTextField	paletteField;
	JCheckBox	flipXBox, flipYBox;
	JCheckBox	priorityBox;
	JCheckBox	bankBox;
	ComboBoxFromFile metaTileEffectBox;
	JLabel statusLabel;

	boolean effectListenerDisabled = false, subTileListenerDisabled=false;

	TileSet tileSet;

	TileSetViewer tileSetViewer;
	SubTileViewer subTileViewer;
	// The tile being modified
	int metaTile;
	int metaTileScale = 3;
	BufferedImage metaTileImage;
	// The section of the tile which is selected - 0,1,2,3 are top-left, top-right, bottom-left, bottom-right.
	int selectedSubTile=0;

	public MetaTileEditor(JFrame prnt, TileSet t) {
		super(prnt, "TileSet Editor", Dialog.ModalityType.APPLICATION_MODAL);
		parent = prnt;
		this.tileSet = t;

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		tileSetField = new JTextField();
		tileSetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int n = Integer.parseInt(tileSetField.getText(), 16);
					if (n > 0) {
						tileSet = TileSet.getTileSet(n);
						metaTileEffectBox.setSelected(tileSet.effectRecord.read16(metaTile*2));
						refreshPropertyFields();
						refreshFlagFields();
						refreshTileSet();
					}
				} catch(NumberFormatException ex){}

				tileSetField.setText(RomReader.toHexString(tileSet.getId()));
			}
		});
		contentPane.add(new LabelWithComponent(new JLabel("Editing Tileset: "), tileSetField));
		contentPane.add(Box.createVerticalGlue());

		ActionListener propertyActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				writePropertyFields();
			}
		};
		JPanel propertyPanel = new JPanel();
		propertyPanel.setLayout(new GridBagLayout());
		propertyPanel.setBorder(BorderFactory.createTitledBorder("Properties"));
		metaTileField = new JTextField();
		flagField = new JTextField();
		gfxData0Field = new JTextField();
		gfxData1Field = new JTextField();
		paletteDataField = new JTextField();
		metaTileField.addActionListener(propertyActionListener);
		flagField.addActionListener(propertyActionListener);
		gfxData0Field.addActionListener(propertyActionListener);
		gfxData1Field.addActionListener(propertyActionListener);
		paletteDataField.addActionListener(propertyActionListener);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = GridBagConstraints.RELATIVE;
		cons.gridy = 0;
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.weightx = 0.0;
		propertyPanel.add(new JLabel("Metatiles: "), cons);
		cons.weightx = 1.0;
		propertyPanel.add(metaTileField, cons);
		cons.weightx = 0.0;
		propertyPanel.add(new JLabel(" Flags: "), cons);
		cons.weightx = 1.0;
		propertyPanel.add(flagField, cons);
		cons.weightx = 0.0;
		propertyPanel.add(new JLabel(" Bank 0 subtiles: "), cons);
		cons.weightx = 1.0;
		propertyPanel.add(gfxData0Field, cons);
		cons.weightx = 0.0;
		cons.gridy = 1;
		propertyPanel.add(new JLabel("Bank 1 subtiles: "), cons);
		cons.weightx = 1.0;
		propertyPanel.add(gfxData1Field, cons);
		cons.weightx = 0.0;
		propertyPanel.add(new JLabel(" Palette data: "), cons);
		cons.weightx = 1.0;
		propertyPanel.add(paletteDataField, cons);

		JPanel topPanel = new JPanel();

		tileSetViewer = new TileSetViewer(tileSet, new TileGridViewerClient() {
			public void tileSelectionChanged(int selection) {
				requestFocus();
				setMetaTile(selection);
			}
			public void tileHoverChanged(int selection) {
				if (selection == -1)
					statusLabel.setText("Selected metatile: " + RomReader.toHexString(metaTile, 2));
				else
					statusLabel.setText("Viewing metatile:  " + RomReader.toHexString(selection, 2));
			}
		});
		tileSetViewer.setTileSet(tileSet);
		subTileViewer = new SubTileViewer(tileSet, new TileGridViewerClient() {
			public void tileSelectionChanged(int selection) {
				if (!subTileListenerDisabled) {
					requestFocus();
					setSubTile(selection);
				}
			}
			public void tileHoverChanged(int selection) {
			}
		});
		subTileViewer.setSelectionColor(Color.blue);

		JPanel tileSetViewerPanel = new JPanel(), subTileViewerPanel = new JPanel();
		tileSetViewerPanel.add(tileSetViewer);
		subTileViewerPanel.add(subTileViewer);
		tileSetViewerPanel.setBorder(BorderFactory.createTitledBorder("Metatiles"));
		subTileViewerPanel.setBorder(BorderFactory.createTitledBorder("Subtiles (enlarged x2)"));

		topPanel.add(tileSetViewerPanel);
		topPanel.add(Box.createHorizontalGlue());
		topPanel.add(subTileViewerPanel);


		JPanel tileEditPanel = new JPanel();
		tileEditPanel.setLayout(new BorderLayout());

		JPanel metaTileEffectPanel = new JPanel();
		metaTileEffectPanel.setBorder(BorderFactory.createTitledBorder("Tile Effect"));
		metaTileEffectBox = new ComboBoxFromFile(this, ValueFileParser.getTileEffectFile());
		metaTileEffectBox.setMaximumSize(metaTileEffectBox.getPreferredSize());
		metaTileEffectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!effectListenerDisabled) {
					int addr = metaTileEffectBox.getAddr();
					if (addr >= 0) {
						metaTileEffectBox.setSelected(addr);
						tileSet.effectRecord.write16(metaTile*2, addr);
					}
				}
			}
		});
		metaTileEffectPanel.add(metaTileEffectBox);
		
		metaTilePanel = new JPanel() {
			public void paintComponent(Graphics g) {
				int x = getSize().width/2-8*metaTileScale;
				int y = getSize().height/2-8*metaTileScale;
				g.drawImage(metaTileImage, x, y, 16*metaTileScale, 16*metaTileScale, null);

				int cursX = (selectedSubTile%2)*8*metaTileScale;
				int cursY = (selectedSubTile/2)*8*metaTileScale;
				g.setColor(Color.white);
				Drawing.drawSquare(g, 1, x+cursX, y+cursY, 8*metaTileScale);
			}
		};
		metaTilePanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int startX = metaTilePanel.getSize().width/2-8*metaTileScale;
				int startY = metaTilePanel.getSize().height/2-8*metaTileScale;
				int x = (e.getX()-startX)/(8*metaTileScale);
				int y = (e.getY()-startY)/(8*metaTileScale);
				
				setSelectedSubTile(x+y*2);
				itself.requestFocus();
			}
		});
		metaTilePanel.setPreferredSize(new Dimension(16*metaTileScale, 16*metaTileScale));

		JPanel flagPanel = new JPanel();
		flagPanel.setLayout(new BoxLayout(flagPanel, BoxLayout.Y_AXIS));
		flagPanel.setBorder(BorderFactory.createTitledBorder("Flags"));
		paletteField = new JTextField("  ");
		paletteField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = paletteField.getText();
				try {
					int n = Integer.parseInt(s, 16);
					setPalette(n);
				} catch(NumberFormatException ex) {}
				refreshFlagFields();

                selectedSubTile = (selectedSubTile+1)%4;
                metaTilePanel.repaint();
			}
		});
		flipXBox = new JCheckBox("Flip X");
		flipXBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int flagIndex = metaTile*4+selectedSubTile;
				int flags = tileSet.flagRecord.read(flagIndex);
				flags &= ~0x20;
				if (e.getStateChange() == ItemEvent.SELECTED)
					flags |= 0x20;
				tileSet.flagRecord.write(flagIndex, (byte)flags);

				refreshFlagFields();
				refreshTileSet();
			}
		});
		flipYBox = new JCheckBox("Flip Y");
		flipYBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int flagIndex = metaTile*4+selectedSubTile;
				int flags = tileSet.flagRecord.read(flagIndex);
				flags &= ~0x40;
				if (e.getStateChange() == ItemEvent.SELECTED)
					flags |= 0x40;
				tileSet.flagRecord.write(flagIndex, (byte)flags);

				refreshFlagFields();
				refreshTileSet();
			}
		});
		priorityBox = new JCheckBox("Sprite priority");
		priorityBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int flagIndex = metaTile*4+selectedSubTile;
				int flags = tileSet.flagRecord.read(flagIndex);
				flags &= ~0x80;
				if (e.getStateChange() == ItemEvent.SELECTED)
					flags |= 0x80;
				tileSet.flagRecord.write(flagIndex, (byte)flags);

				refreshFlagFields();
			}
		});
		bankBox = new JCheckBox("Bank (0/1)");
		bankBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int flagIndex = metaTile*4+selectedSubTile;
				int flags = tileSet.flagRecord.read(flagIndex);
				flags &= ~0x8;
				if (e.getStateChange() == ItemEvent.SELECTED)
					flags |= 0x8;
				tileSet.flagRecord.write(flagIndex, (byte)flags);

				refreshFlagFields();
				refreshTileSet();
			}
		});
					
		flagPanel.add(new LabelWithComponent(new JLabel("Palette: "), paletteField));
		flagPanel.add(flipXBox);
		flagPanel.add(flipYBox);
		flagPanel.add(priorityBox);
		flagPanel.add(bankBox);
		flagPanel.setMaximumSize(new Dimension(70, flagPanel.getPreferredSize().height));

		JPanel subTileEditPanel = new JPanel();
		subTileEditPanel.setLayout(new BoxLayout(subTileEditPanel, BoxLayout.Y_AXIS));
		subTileEditPanel.add(metaTileEffectPanel);
		subTileEditPanel.add(Box.createVerticalGlue());
		subTileEditPanel.add(metaTilePanel);

		tileEditPanel.add(subTileEditPanel, BorderLayout.CENTER);
		tileEditPanel.add(flagPanel, BorderLayout.EAST);

		palettePanel = new PaletteEditorPanel(tileSet.getPalettes(), true, this);
		
		JButton subTileEditorButton = new JButton("Edit Subtiles");
		subTileEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TileEditor editor = new TileEditor(tileSet.getSubTileData(), tileSet.getPalettes());
				editor.setOffsetText("Bank 0 Tiles: 0x" + RomReader.toHexString(tileSet.gfxData0Record.getAddr()) + "\n" +
					"Bank 1 Tiles: 0x" + RomReader.toHexString(tileSet.gfxData1Record.getAddr()));
				editor.setVisible(true);
				if (editor.clickedOk()) {
					tileSet.setSubTileData(editor.getTileData());
					tileSet.setPalettes(editor.getPalettes());
					refreshTileSet();
				}
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		buttonPanel.add(okButton);

		contentPane.add(propertyPanel);
		contentPane.add(topPanel);
		contentPane.add(subTileEditorButton);
		contentPane.add(tileEditPanel);
		contentPane.add(buttonPanel);

		setLayout(new BorderLayout());
		add(contentPane, BorderLayout.CENTER);

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusLabel = new JLabel();
		statusLabel.setFont(new Font("monospaced", Font.BOLD, 12));
		statusLabel.setPreferredSize(new Dimension(getWidth(), 16));
		statusPanel.add(statusLabel);

		add(palettePanel, BorderLayout.EAST);
		add(statusPanel, BorderLayout.SOUTH);

		// Detect arrow key input
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int newTile = selectedSubTile;
				switch(e.getKeyCode()) {
					case KeyEvent.VK_UP:
						newTile -= 2;
						break;
					case KeyEvent.VK_DOWN:
						newTile += 2;
						break;
					case KeyEvent.VK_RIGHT:
						newTile++;
						break;
					case KeyEvent.VK_LEFT:
						newTile--;
						break;
				}
				setSelectedSubTile(newTile);
			}
		});
		setMetaTile(0);
		refreshPropertyFields();

		setFocusable(true);
		requestFocus();
		pack();
		setVisible(true);
	}

	void setPalette(int n) {
		if (n < 8 && n >= 0) {
			int flagIndex = metaTile*4+selectedSubTile;
			int flags = tileSet.flagRecord.read(flagIndex);
			flags &= ~7;
			flags |= n;
			tileSet.flagRecord.write(flagIndex, (byte)flags);

			refreshFlagFields();
			refreshTileSet();
		}
	}

	// Read the data in each of the property fields and update the tileset.
	void writePropertyFields() {
		try {
			int n = Integer.parseInt(metaTileField.getText(), 16);
			tileSet.setMetaTileIndex(n);
		} catch(NumberFormatException e){}
		try {
			int n = Integer.parseInt(flagField.getText(), 16);
			tileSet.setFlagIndex(n);
		} catch(NumberFormatException e){}
		try {
			int n = Integer.parseInt(gfxData0Field.getText(), 16);
			tileSet.setGfxData0Index(n);
		} catch(NumberFormatException e){}
		try {
			int n = Integer.parseInt(gfxData1Field.getText(), 16);
			tileSet.setGfxData1Index(n);
		} catch(NumberFormatException e){}
		try {
			int n = Integer.parseInt(paletteDataField.getText(), 16);
			tileSet.setPaletteDataIndex(n);
		} catch(NumberFormatException e){}

		refreshPropertyFields();
		refreshTileSet();
	}
	void refreshPropertyFields() {
		tileSetField.setText(Integer.toHexString(tileSet.tileSetDataIndex).toUpperCase());
		metaTileField.setText(Integer.toHexString(tileSet.metaTileIndex).toUpperCase());
		flagField.setText(Integer.toHexString(tileSet.flagIndex).toUpperCase());
		gfxData0Field.setText(Integer.toHexString(tileSet.gfxData0Index).toUpperCase());
		gfxData1Field.setText(Integer.toHexString(tileSet.gfxData1Index).toUpperCase());
		paletteDataField.setText(Integer.toHexString(tileSet.paletteDataIndex).toUpperCase());
	}
	
	// Refresh all flag fields with the tileset's info.
	void refreshFlagFields() {
		int flags = tileSet.flagRecord.read(metaTile*4+selectedSubTile);
		paletteField.setText(""+(flags&7));
		flipXBox.setSelected((flags&0x20) != 0);
		flipYBox.setSelected((flags&0x40) != 0);
		priorityBox.setSelected((flags&0x80) != 0);
		bankBox.setSelected((flags&0x8) != 0);
		subTileListenerDisabled = true;
		subTileViewer.setSelectedTile(tileSet.metaTileRecord.read(metaTile*4+selectedSubTile)+
				((tileSet.flagRecord.read(metaTile*4+selectedSubTile)&0x8) != 0 ? 0x80 : 0));
		subTileListenerDisabled = false;
	}
	void refreshTileSet() {
		tileSet.generateTiles();

		palettePanel.setPalettes(tileSet.getPalettes());
		subTileViewer.setTileSet(tileSet);
		tileSetViewer.setTileSet(tileSet);
		metaTileImage = tileSet.getTileImage(metaTile);
		repaint();
	}
	void setMetaTile(int tile) {
		metaTile = tile;
		metaTileEffectBox.setSelected(tileSet.effectRecord.read16(metaTile*2));
		refreshFlagFields();
		refreshTileSet();
	}

	void setSelectedSubTile(int s) {
		if (s >= 4 || s < 0)
			return;

		selectedSubTile = s;

		refreshFlagFields();
		repaint();
	}
	// Set a subtile to the specified subtile. The subtile in question is selectedSubTile.
	// Bit 7 is set if it's in vram bank 1 rather than 0.
	void setSubTile(int subTile) {
		int bank = subTile>>7;
		subTile &= 0x7f;

		tileSet.metaTileRecord.write(metaTile*4+selectedSubTile, (byte)subTile);

		int flagVal = tileSet.flagRecord.read(metaTile*4+selectedSubTile);
		flagVal &= ~8;
		flagVal |= bank<<3;
		tileSet.flagRecord.write(metaTile*4+selectedSubTile, (byte)flagVal);
		refreshFlagFields();
		refreshTileSet();
	}

	public void setTileData(byte[] data) {
		tileSet.setSubTileData(data);
	}

	// PaletteEditorClient
	public void setPalettes(int[][] palettes) {
		tileSet.setPalettes(palettePanel.getPalettes());
		refreshTileSet();
	}
}

class SubTileViewer extends TileGridViewer {
	TileGridViewerClient client;

	TileSet tileSet;
	BufferedImage image;

	boolean selectable=true;

	int selectedTile=0;

	int scale;

	Point cursorPos = new Point(0,0);

	public SubTileViewer(TileSet tileSet, TileGridViewerClient c) {
		super(RomReader.binToTiles(tileSet.getSubTileData(), 0, Drawing.defaultPalette), 16, 2, c);
		this.tileSet = tileSet;
		client = c;
	}

	public void setTileSet(TileSet t) {
		this.tileSet = t;
		setTiles(RomReader.binToTiles(tileSet.getSubTileData(), 0, Drawing.defaultPalette), 16, 2);
	}
}
