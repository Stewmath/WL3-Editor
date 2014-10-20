package dialogs;

import java.awt.Dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.nio.charset.*;

import javax.swing.*;

import javax.swing.filechooser.FileNameExtensionFilter;

import base.*;
import record.*;

import java.util.*;

import graphics.ComboBoxFromFile;

import record.RomReader;

// TODO: enforce data sizes when exporting

public class ExportDialog extends JDialog {

	public static final int EXPORT_VERSION = 1;
	public static final int HEADER_SIZE = 0x40;


	abstract class ExportableData {
		String name;
		abstract int exportData(ByteArrayOutputStream out) throws IOException;
		abstract void importData(ByteArrayInputStream in) throws IOException;
	}

	ExportableData[] exportableData = new ExportableData[] {
		new ExportableData() {
			String name = "Level Data";

			int exportData(ByteArrayOutputStream out) throws IOException {
				// Keeps track of which leveldata has been written already
				HashMap<MoveableDataRecord, Integer> writtenLevelData = new HashMap<MoveableDataRecord, Integer>();
				// Same for warpdata
				HashMap<MoveableDataRecord, Integer> writtenWarpData = new HashMap<MoveableDataRecord, Integer>();

				ByteArrayOutputStream dataOutput = new ByteArrayOutputStream();

				// 8 levels plus index -1 as the terminator
				// NOTE: THIS IS WRONG FOR LEVEL C8
				int dataPos = HEADER_SIZE + out.size() + (3*4)*8 + 4;

				for (int i=levelIndex/8*8; i<(levelIndex/8+1)*8; i++) {
					if (i > 0xc8)
						break;
					Level level = Level.getLevel(i);
					// Check if the level's tile & object data has a pointer already
					Integer tileObjectPointer = writtenLevelData.get(level.getLevelDataRecord());

					if (tileObjectPointer == null) {
						// Write a new set of tile data and object data to the export
						tileObjectPointer = dataPos;
						dataPos += 0xa0*0x30 + 0xa0*0x30/2;
						writtenLevelData.put(level.getLevelDataRecord(), tileObjectPointer);

						dataOutput.write(level.getTileDataRecord().toArray());
						dataOutput.write(level.getObjectDataRecord().toArray());
					}

					// Same for warp data
					Integer warpDataPointer = writtenWarpData.get(level.getLevelDataRecord());

					if (warpDataPointer == null) {
						warpDataPointer = dataPos;
						dataPos += 0xa*0x3*8;
						writtenWarpData.put(level.getLevelDataRecord(), warpDataPointer);

						dataOutput.write(level.getTileDataRecord().toArray());
						dataOutput.write(level.getObjectDataRecord().toArray());
					}

					writeInt(out, i);
					writeInt(out, tileObjectPointer);
					writeInt(out, warpDataPointer);
				}
				writeInt(out, -1);
				out.write(dataOutput.toByteArray());
				return 0;
			}
			void importData(ByteArrayInputStream in) {
			}
		},
		new ExportableData() {
			String name = "Tileset Data";

			int exportData(ByteArrayOutputStream out) throws IOException {
				return 0;
			}
			void importData(ByteArrayInputStream in) throws IOException {
			}
		},
	};



	ComboBoxFromFile comboBox;
	int levelIndex = -1;

	public ExportDialog() {
		super(null, "Export Level", Dialog.ModalityType.APPLICATION_MODAL);

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		comboBox = new ComboBoxFromFile(this, ValueFileParser.getLevelFile());
		contentPanel.add(comboBox);

		JButton exportButton = new JButton("Export");
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				levelIndex = comboBox.getAddr();

				ByteArrayOutputStream data = new ByteArrayOutputStream();

				try {
					writeHeader(data);

					RomReader.exportData(data.toByteArray(),
							"Export Level",
							new FileNameExtensionFilter("WL3 Archive File (.wl3)", "wl3"));
				}
				catch (IOException ex) {
				}

				setVisible(false);
			}
		});
		contentPanel.add(exportButton);

		add(contentPanel);
		pack();
	}

	int writeInt(ByteArrayOutputStream out, int val) {
		out.write(val&0xff);
		out.write((val>>8)&0xff);
		out.write((val>>16)&0xff);
		out.write((val>>24)&0xff);
		return 4;
	}

	void writeHeader(ByteArrayOutputStream out) throws IOException {
		if (levelIndex == -1)
			return;

		// Magic (0x00)
		out.write("WL3E".getBytes(StandardCharsets.US_ASCII));

		// Version (0x04)
		writeInt(out, EXPORT_VERSION);

		// Everything else
		ByteArrayOutputStream dataOutput = new ByteArrayOutputStream();

		for (ExportableData data : exportableData) {
			writeInt(out, dataOutput.size() + HEADER_SIZE);
			data.exportData(dataOutput);
		}

		while (out.size() < HEADER_SIZE)
			out.write(0);

		out.write(dataOutput.toByteArray());
	}
}
