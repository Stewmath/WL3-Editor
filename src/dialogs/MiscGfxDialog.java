package dialogs;

import graphics.ComboBoxFromFile;
import base.ValueFileParser;

import viewers.TileEditor;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.*;

import javax.swing.*;

import record.*;

public class MiscGfxDialog extends JDialog {

	RomReader rom;
	
	JComboBox<String> comboBox;
	ArrayList<MoveableDataRecord> gfxRecords = new ArrayList<MoveableDataRecord>();

	ValueFileParser parser;
	
	public MiscGfxDialog(JFrame parent)
	{
		super(null, "Edit Misc. Graphics", Dialog.ModalityType.APPLICATION_MODAL);
		
		rom = RomReader.rom;
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		comboBox = new JComboBox<String>();
		parser = ValueFileParser.getMiscGfxFile();

		for (int i=0; i<parser.getNumSections(); i++) {
			ValueFileParser subParser = parser.getSection(i);

			MoveableDataRecord gfxRecord = null;

			try {
				String addressType = subParser.getValue("addresstype");
				boolean compressed = subParser.getValue("compressed").equals("true");

				if (addressType.equals("pointer")) {
					int addr;
					int requiredBank = -1;
					int ptrAddress = -1;
					int bankAddress = -1;

					if (subParser.hasEntry("ptrAddress"))
						ptrAddress = subParser.getIntValue("ptrAddress");
					else
						ptrAddress = subParser.getIntValue("ptrAddress1");
					if (subParser.hasEntry("bankAddress"))
						bankAddress = subParser.getIntValue("bankAddress");
					else if (subParser.hasEntry("bankAddress1"))
						bankAddress = subParser.getIntValue("bankAddress1");

					RomPointer ptr;
					if (bankAddress != -1) {
						ptr = new RomPointer(ptrAddress, bankAddress);
						addr = ptr.getPointedAddr();
					}
					else {
						ptr = new RomPointer(ptrAddress);
						requiredBank = subParser.getIntValue("bank");
						addr = RomReader.BANK(ptr.getPointedAddr(), requiredBank);
					}

					gfxRecord = rom.getMoveableDataRecord(addr, ptr, compressed, 0);

					// Get extra pointers if there are any
					for (int j=2;; j++) {
						ptrAddress = -1;
						bankAddress = -1;
						if (subParser.hasEntry("ptrAddress" + RomReader.toHexString(j))) {
							ptrAddress = subParser.getIntValue("ptrAddress" + RomReader.toHexString(j));
							if (subParser.hasEntry("bankAddress" + RomReader.toHexString(j)))
								bankAddress = subParser.getIntValue("bankAddress" + RomReader.toHexString(j));
							if (bankAddress != -1)
								ptr = new RomPointer(ptrAddress, bankAddress);
							else
								ptr = new RomPointer(ptrAddress);
							gfxRecord.addPtr(ptr);
						}
						else
							break;
					}

					gfxRecord.setDescription("Graphics: " + subParser.getSectionName(0));
					if (requiredBank >= 0)
						gfxRecord.setRequiredBank(requiredBank);
				}
			}
			catch(NumberFormatException ex) {
				gfxRecord = null;
			}

			if (gfxRecord != null) {
				comboBox.addItem(parser.getSectionName(i));
				gfxRecords.add(gfxRecord);
			}
		}

		contentPane.add(comboBox);
		contentPane.add(Box.createRigidArea(new Dimension(0, 8)));
		
		JButton okButton = new JButton("OK");
		okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i = comboBox.getSelectedIndex();

				TileEditor eddie = new TileEditor(gfxRecords.get(i).toArray(), null);

				eddie.setVisible(true);
				if (eddie.clickedOk()) {
					gfxRecords.get(i).setData(eddie.getTileData());
				}

				setVisible(false);
			}
		});
		contentPane.add(okButton);

		add(contentPane);
		pack();
		setVisible(true);
	}
}
