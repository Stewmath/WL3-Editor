package dialogs;

import graphics.ComboBoxFromFile;
import base.ValueFileParser;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import record.*;

public class MiscMusicDialog extends JDialog {

	RomReader rom;
	
	int numEntries=0;
	MoveableDataRecord[] dataRecord1 = new MoveableDataRecord[100];
	MoveableDataRecord[] dataRecord2 = new MoveableDataRecord[100];
	ComboBoxFromFile[] comboBoxes = new ComboBoxFromFile[100];
	
	public MiscMusicDialog(JFrame parent)
	{
		super(parent, "Edit Misc. Music", Dialog.ModalityType.APPLICATION_MODAL);
		
		rom = RomReader.rom;
		
		numEntries=0;
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.add(makeMusicEntry("Overworld (day)", 0x3b9f, 0x3ba5));
		contentPane.add(makeMusicEntry("Overworld (night)", 0x3bac, 0x3bb2));
		contentPane.add(makeMusicEntry("Pause menu", RomReader.BANK(0x40a2, 0x7c), RomReader.BANK(0x40a6, 0x7c)));
		contentPane.add(makeMusicEntry("Titlescreen", RomReader.BANK(0x44f1, 1), RomReader.BANK(0x44f5, 1)));
		// The titlescreen music is started in 2 different places.
		comboBoxes[3].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MoveableDataRecord r1 = rom.getMoveableDataRecord(0x448a, null, false, 1);
				MoveableDataRecord r2 = rom.getMoveableDataRecord(0x448e, null, false, 1);
				r1.write(0, (byte)(comboBoxes[3].getAddr()>>8));
				r2.write(0, (byte)(comboBoxes[3].getAddr()&0xff));
			}
		});
		contentPane.add(makeMusicEntry("View Treasure", RomReader.BANK(0x63d9, 0x26), RomReader.BANK(0x63df, 0x26)));
		contentPane.add(makeMusicEntry("Final Boss", RomReader.BANK(0x168b, 0), RomReader.BANK(0x168a, 0)));
		contentPane.add(makeMusicEntry("Credits", RomReader.BANK(0x40f0, 0x58), RomReader.BANK(0x40f4, 0x58)));
		
		contentPane.add(Box.createRigidArea(new Dimension(0, 8)));
		
		JButton okButton = new JButton("OK");
		okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
			}
		});
		contentPane.add(okButton);
		
		add(contentPane);
		pack();
		setVisible(true);
	}
	
	// addr1 is the high byte,
	// addr2 is the low byte.
	JPanel makeMusicEntry(String name, int addr1, int addr2)
	{
		dataRecord1[numEntries] = rom.getMoveableDataRecord(addr1, null, false, 1);
		dataRecord2[numEntries] = rom.getMoveableDataRecord(addr2, null, false, 1);
		
		JPanel panel = new JPanel();
		comboBoxes[numEntries] = new ComboBoxFromFile(this, ValueFileParser.getMusicFile());
		comboBoxes[numEntries].setSelected(((dataRecord1[numEntries].read(0)&0xff)<<8)|(dataRecord2[numEntries].read(0)&0xff));
		comboBoxes[numEntries].setActionCommand(""+numEntries);
		comboBoxes[numEntries].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				ComboBoxFromFile source = (ComboBoxFromFile)e.getSource();
				int id=0;
				for (int i=0; i<numEntries; i++)
				{
					if (comboBoxes[i] == source)
					{
						id = i;
						break;
					}
				}
				int num = comboBoxes[id].getAddr();
				
				int b1 = (num>>8);
				int b2 = (num&0xff);
				if (num >= 0)
				{
					
					dataRecord1[id].write(0, (byte)b1);
					dataRecord2[id].write(0, (byte)b2);
				}
				
				comboBoxes[id].setSelected((b1<<8)|b2);
			}
		});
		panel.add(comboBoxes[numEntries]);
		panel.setBorder(BorderFactory.createTitledBorder(name));
		
		numEntries++;
		
		return panel;
	}
}
