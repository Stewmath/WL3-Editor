package dialogs;

import java.awt.Dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.nio.charset.*;

import java.util.logging.Logger;

import javax.swing.*;

import javax.swing.filechooser.FileNameExtensionFilter;

import base.*;
import record.*;

import java.util.*;

import graphics.ComboBoxFromFile;

import record.RomReader;

// TODO: enforce data sizes when exporting

public class ExportDialog extends JDialog {
	static Logger logger = Logger.getLogger(ExportDialog.class.getName());

	public static final int EXPORT_VERSION = 1;
	public static final int HEADER_SIZE = 0x40;


	abstract class ExportableData {
		String name;
		abstract int exportData(ByteArrayOutputStream out) throws IOException;
		abstract boolean importData(byte[] data, int offset);
	}

	ExportableData[] exportableData = new ExportableData[] {
		new ExportableData() {
			String name = "Level Data";

			int exportData(ByteArrayOutputStream out) throws IOException {
				// Keeps track of which leveldata has been written already
				HashMap<Object, Integer> writtenLevelData = new HashMap<Object, Integer>();
				// Same for warpdata
				HashMap<Object, Integer> writtenWarpData = new HashMap<Object, Integer>();

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
					Integer warpDataPointer = writtenWarpData.get(level.getRegionDataRecord());

					if (warpDataPointer == null) {
						warpDataPointer = dataPos;
						dataPos += 0xa*0x3*8;
						writtenWarpData.put(level.getRegionDataRecord(), warpDataPointer);

						dataOutput.write(level.getRegionDataRecord().getRawWarpData());
					}

					writeInt(out, i);
					writeInt(out, tileObjectPointer);
					writeInt(out, warpDataPointer);
				}
				writeInt(out, -1);
				out.write(dataOutput.toByteArray());
				return 0;
			}

			boolean importData(byte[] data, int offset) throws ArrayIndexOutOfBoundsException {
				ByteArrayInputStream in = new ByteArrayInputStream(data, offset, data.length-offset);
				HashMap<Integer, Level> readLevelData = new HashMap<Integer,Level>();
				HashMap<Integer, Level> readWarpData = new HashMap<Integer,Level>();

				int index = readInt(in);

				while (index != -1) {
					logger.fine("Importing level " + RomReader.toHexString(index, 2));

					// Offsets of data in file
					int tileObjectDataPointer = readInt(in);
					int warpDataPointer = readInt(in);

					Level level = Level.getLevel(index);

					// Tile, Object data
					Level mergeLevel = readLevelData.get(tileObjectDataPointer);
					if (mergeLevel == null) {
						// Data has not been read yet, create the data and give it to the level
						byte[] tileData = Arrays.copyOfRange(data,
								tileObjectDataPointer,
								tileObjectDataPointer+0xa0*0x30);
						byte[] objectData = Arrays.copyOfRange(data,
								tileObjectDataPointer+0xa0*0x30,
								tileObjectDataPointer+0xa0*0x30+0xa0*0x30/2);

						level.setNewLevelData(tileData, objectData);

						readLevelData.put(tileObjectDataPointer, level);
					}
					else {
						// Data has been read already, merge the current level's data with it
						level.mergeLevelDataWith(mergeLevel);
					}

					// Regions
					mergeLevel = readWarpData.get(warpDataPointer);
					if (mergeLevel == null) {
						byte[] warpData = Arrays.copyOfRange(data,
								warpDataPointer,
								warpDataPointer+RegionRecord.NUM_SECTORS*8);
						level.setNewRegionData(warpData);

						readWarpData.put(warpDataPointer, level);
					}
					else {
						level.setRegionDataRecord(mergeLevel.getRegionDataRecord());
					}

					index = readInt(in);
				}

				return true;
			}
		},
		new ExportableData() {
			String name = "Tileset Data";

			int exportData(ByteArrayOutputStream out) throws IOException {
				return 0;
			}
			boolean importData(byte[] data, int offset) {
				return false;
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

		JButton importButton = new JButton("Import");
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				byte[] data = RomReader.importData("Import Level",
						new FileNameExtensionFilter("WL3 Archive File (.wl3)", "wl3"));

				if (!readHeader(data)) {
					logger.warning("Import error");
				}

				setVisible(false);
			}
		});
		contentPanel.add(importButton);

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

	int readInt(byte[] data, int i) throws ArrayIndexOutOfBoundsException {
		return (data[i]&0xff) | (data[i+1]&0xff)<<8 | (data[i+2]&0xff)<<16 | (data[i+3]&0xff)<<24;
	}
	int readInt(ByteArrayInputStream in) {
		byte[] data = new byte[4];
		in.read(data, 0, 4);
		return (data[0]&0xff) | (data[1]&0xff)<<8 | (data[2]&0xff)<<16 | (data[3]&0xff)<<24;
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

	boolean readHeader(byte[] data) {
		try {
			byte[] magicBytes = Arrays.copyOf(data, 4);
			String magic = new String(magicBytes, StandardCharsets.US_ASCII);
			if (!magic.equals("WL3E")) {
				importErr("Archive is corrupt.");
				return false;
			}

			int version = readInt(data, 4);
			if (version > EXPORT_VERSION) {
				importErr("Archive is from a newer revision.");
				return false;
			}

			int headerPos = 8;
			for (ExportableData importer : exportableData) {
				int pointer = readInt(data, headerPos);
				importer.importData(data, pointer);
				headerPos += 4;
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			importErr("Abrupt end of file.");
		}

		return true;
	}

	void importErr(String message) {
		JOptionPane.showMessageDialog(null,
				"Import Error: " + message,
				"Import Error",
				JOptionPane.ERROR_MESSAGE);
	}
}
