package viewers;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import base.ObjectSet;
import record.*;

import graphics.*;

import java.awt.Graphics;
import java.awt.image.*;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

public class ObjectSetEditor extends JDialog {

	ObjectSet objectSet;
	
	PaletteEditorPanel palettePanel;
	JTextField objectSetField;
	ComboBoxFromFile itemSetBox;
	ComboBoxFromFile enemySetBox;

	JTextField baseBankField;
	ComboBoxFromFile[] gfxSlotFields = new ComboBoxFromFile[4];
	JPanel[] gfxSlotPanels = new JPanel[4];
	JButton[] gfxSlotEditButtons = new JButton[4];
	ComboBoxFromFile[] objectFields = new ComboBoxFromFile[0xc];
	
	boolean disableListeners=false;
	
	public ObjectSetEditor(JFrame owner, ObjectSet o)
	{
		super(owner, "Object Set Editor", Dialog.ModalityType.APPLICATION_MODAL);
		JPanel contentPane = new JPanel();
		
		objectSet = o;
		

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridBagLayout());

		objectSetField = new JTextField();
		objectSetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!disableListeners) {
					objectSet = ObjectSet.getObjectSet(Integer.parseInt(e.getActionCommand(), 16));
					refreshAll();
				}
			}
		});
		
		itemSetBox = new ComboBoxFromFile(this, ComboBoxFromFile.itemSetFile);
		itemSetBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!disableListeners) {
					objectSet.setItemSetPtr(itemSetBox.getAddr());
					refreshItemSet();
				}
			}
		});
		itemSetBox.setPreferredSize(new Dimension(130, 20));
		
		enemySetBox = new ComboBoxFromFile(this, ComboBoxFromFile.enemySetFile);
		enemySetBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!disableListeners) {
					objectSet.setEnemySetPtr(enemySetBox.getAddr());
					refreshEnemySet();
				}
			}
		});

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = GridBagConstraints.RELATIVE;
		cons.gridy = 0;
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.weightx = 0.0;
		topPanel.add(new JLabel("Editing Object Set: "), cons);
		cons.weightx = 1.0;
		topPanel.add(objectSetField, cons);

		cons.gridy++;
		cons.insets = new Insets(5, 0, 5, 0);
		cons.gridwidth = 2;
		topPanel.add(new JSeparator(), cons);

		cons.gridy++;
		cons.insets = new Insets(0, 0, 0, 0);
		cons.gridwidth = 1;
		cons.weightx = 0.0;
		topPanel.add(new JLabel("Item Set: "), cons);
		cons.weightx = 1.0;
		topPanel.add(itemSetBox, cons);

		cons.gridy++;
		cons.weightx = 0.0;
		topPanel.add(new JLabel("Enemy Set: "), cons);
		cons.weightx = 1.0;
		topPanel.add(enemySetBox, cons);

		JPanel enemySetPanel = new JPanel();
		enemySetPanel.setLayout(new BoxLayout(enemySetPanel, BoxLayout.Y_AXIS));

		baseBankField = new JTextField();
		baseBankField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!disableListeners) {
					try {
						int n = Integer.parseInt(baseBankField.getText(), 16);
						if (n > 0xff) {
							JOptionPane.showMessageDialog(null,
								"Error: Base gfx bank must be between 00 and ff.",
								"Error",
								JOptionPane.ERROR_MESSAGE);
						}
						else {
							objectSet.setEnemySetBaseBank(n);
						}
					} catch(NumberFormatException ex){}
					refreshEnemySet();
				}
			}
		});
		JPanel baseSubPanel = new JPanel();
		baseSubPanel.setLayout(new BoxLayout(baseSubPanel, BoxLayout.X_AXIS));
		baseSubPanel.add(new JLabel("Base Gfx Bank: "));
		baseSubPanel.add(Box.createHorizontalGlue());
		baseBankField.setPreferredSize(new Dimension(400, baseBankField.getPreferredSize().height));
		baseBankField.setMaximumSize(baseBankField.getPreferredSize());
		baseSubPanel.add(baseBankField);

		enemySetPanel.add(baseSubPanel);

		JPanel gfxSlotPanel = new JPanel();
		gfxSlotPanel.setLayout(new BoxLayout(gfxSlotPanel, BoxLayout.X_AXIS));

		for (int i=0; i<4; i++) {
			gfxSlotPanels[i] = new JPanel();
			gfxSlotPanel.add(gfxSlotPanels[i]);
		}

		enemySetPanel.add(gfxSlotPanel);

		ActionListener objectListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while (objectFields[i] != e.getSource()) {
					i++;
				}
				objectSet.enemySetRecord.writePtr(9+i*2, objectFields[i].getAddr());
				objectFields[i].setSelected(objectFields[i].getAddr());
			}
		};
		JPanel row = new JPanel();
		for (int i=0; i<0xc; i++) {
			if (i%4 == 0) {
				enemySetPanel.add(row);
				row = new JPanel();
			}
			JPanel objectPanel = new JPanel();
			objectFields[i] = new ComboBoxFromFile(this, ComboBoxFromFile.enemyAiFile, true);
			objectFields[i].addActionListener(objectListener);
			objectFields[i].setPreferredSize(new Dimension(200, objectFields[i].getPreferredSize().height));
			objectFields[i].setMaximumSize(objectFields[i].getPreferredSize());

			objectPanel.add(objectFields[i]);
			objectPanel.setBorder(BorderFactory.createTitledBorder("Object " + RomReader.toHexString(i+4)));
			row.add(objectPanel);
		}
		enemySetPanel.add(row);

		palettePanel = new PaletteEditorPanel(objectSet.getEnemySetPalettes(), false, new PaletteEditorClient() {
			public void setPalettes(int[][] palettes) {
				objectSet.setEnemySetPalettes(palettes);
			}
		});
		palettePanel.setOrientation(true);
		enemySetPanel.add(palettePanel);

		enemySetPanel.setBorder(BorderFactory.createTitledBorder("Enemy Set"));


		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		buttonPanel.add(okButton);

		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.add(topPanel);
		contentPane.add(enemySetPanel);
		contentPane.add(buttonPanel);
		//contentPane.add(new EnemyTileViewer());
		add(contentPane, BorderLayout.NORTH);

		setMinimumSize(new Dimension(630, getMinimumSize().height));
		pack();
		refreshAll();

		setVisible(true);
	}

	void refreshItemSet() {
		itemSetBox.setSelected(objectSet.getItemSetPtr());
	}
	void refreshEnemySet() {
		enemySetBox.setSelected(objectSet.getEnemySetPtr());

		baseBankField.setText(RomReader.toHexString(objectSet.getEnemySetBaseBank()));

		ActionListener gfxSlotListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!disableListeners) {
					int i=0;
					while (gfxSlotFields[i] != e.getSource()) {
						i++;
					}
					objectSet.setEnemySetGfx(i, gfxSlotFields[i].getAddr());
				}
			}
		};
		for (int i=0; i<4; i++) {
			gfxSlotPanels[i].removeAll();
			gfxSlotPanels[i].setLayout(new BoxLayout(gfxSlotPanels[i], BoxLayout.Y_AXIS));

			gfxSlotFields[i] = new ComboBoxFromFile(this, ComboBoxFromFile.enemyGfxFile.getSection(""+Integer.toHexString(objectSet.getEnemySetBaseBank()+i)));
			gfxSlotFields[i].addActionListener(gfxSlotListener);
			gfxSlotFields[i].setSelected(objectSet.enemySetRecord.read16(i*2+1));
			gfxSlotPanels[i].add(gfxSlotFields[i]);

			JPanel buttonPanel = new JPanel();
			gfxSlotEditButtons[i] = new JButton("Edit...");
			gfxSlotEditButtons[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!disableListeners) {
						int i=0;
						while (gfxSlotEditButtons[i] != e.getSource()) {
							i++;
						}

						byte[] tileData = objectSet.enemySetGfxRecords[i].toArray();
						if (objectSet.enemySetGfxRecords[i] == null) {
							JOptionPane.showMessageDialog(null, 
								"Error: Bank number is too high.",
								"Error",
								JOptionPane.ERROR_MESSAGE
								);
						}
						else {
							TileEditor tileEditor = new TileEditor(tileData, objectSet.getEnemySetPalettes());
							tileEditor.setArrangement(TileGridViewer.ARRANGEMENT_SPRITE);
							tileEditor.setSelectedPalette(i);
							tileEditor.setOffsetText("0x"+RomReader.toHexString(objectSet.enemySetGfxRecords[i].getAddr())+" (compressed)");
							tileEditor.setVisible(true);
							if (tileEditor.clickedOk()) {
								objectSet.enemySetGfxRecords[i].setData(tileEditor.getTileData());
								objectSet.setEnemySetPaletteData(tileEditor.getPaletteData());
								palettePanel.setPalettes(objectSet.getEnemySetPalettes());
							}
						}
					}
				}
			});
			buttonPanel.add(gfxSlotEditButtons[i]);
			gfxSlotPanels[i].add(buttonPanel);
			gfxSlotPanels[i].setBorder(BorderFactory.createTitledBorder(
						"Gfx Slot " + i + " (Bank " + RomReader.toHexString(objectSet.getEnemySetBaseBank()+i)+")"));
		}
		for (int i=0; i<0xc; i++) {
			if (i < objectSet.getEnemySetNumEnemies()-1 && i < 0xc) {
				objectFields[i].setSelected(objectSet.enemySetRecord.read16(9+i*2));
				objectFields[i].setEnabled(true);
			}
			else {
				objectFields[i].setEnabled(false);
			}
		}

		palettePanel.setPalettes(objectSet.getEnemySetPalettes());
		pack();
	}
	void refreshAll()
	{
		disableListeners = true;
		objectSetField.setText(Integer.toHexString(objectSet.getId()).toUpperCase());
		refreshItemSet();
		refreshEnemySet();
		disableListeners = false;

		repaint();
	}
}
