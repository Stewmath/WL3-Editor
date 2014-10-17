package base;

import javax.swing.JOptionPane;
import java.io.*;
import record.RomReader;

public class MetadataFileParser extends ValueFileParser {
	final static int METADATA_VERSION = 1;

	boolean opened;

	public MetadataFileParser(String fn) {
		opened = true;
		filename = fn;
		File f = new File(filename);

		try {
			parseFile(f);
			checkMetadata();
		}
		catch (FileNotFoundException e) {
			badMetadata("The metadata file, '" + filename + "', was not found. Should it be created?\n" +
				"If you have a metadata file for this rom, you should click 'No' and find it!");
		}
	}

	void createNewMetadata(File f) {
		clearEntries();
		setValue("romchecksum", ""+RomReader.toHexString(RomReader.rom.getRomChecksum()));
		try {
			f.delete();
			f.createNewFile();

			JOptionPane.showMessageDialog(null,
					"The metadata file is tied to this rom. Don't lose it!",
					"Metadata",
					JOptionPane.PLAIN_MESSAGE,
					null);
		}
		catch(IOException e2){}

		save();
	}

	void checkMetadata() {
		try {
			int checksum = getIntValue("romchecksum");

			if (checksum != RomReader.rom.getRomChecksum()) {
				int option = JOptionPane.showOptionDialog(null,
						"The rom checksum does not match the checksum in the metadata file, '" + filename + "'." +
						"\n\nIf you have edited the rom outside the editor, this is to be expected; click \"continue\"." +
						"\n\nOtherwise, either find the matching metadata file or create a new one.",
						"Checksum mismatch",
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						new String[] {"Continue", "Create new metadata", "Cancel"},
						null);
				if (option == 0) {
					save();
					// Continue
				}
				else if (option == 1) {
					// Create new metadata
					createNewMetadata(new File(filename));
					save();
					return;
				}
				else if (option == 2) {
					// Cancel
					opened = false;
					return;
				}
			}
		}
		catch (NumberFormatException e) {
			badMetadata("The metadata file, '" + filename + "', Appears to be corrupt. Create a new one?");
		}
	}

	void badMetadata(String message) {
		int option = JOptionPane.showOptionDialog(null,
				message + "\n" +
				"If you click 'No' the rom won't be loaded.",
				"Create Metadata file?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				null,
				0);
		if (option == JOptionPane.NO_OPTION) {
			opened = false;
			return;
		}
		else {
			createNewMetadata(new File(filename));
			return;
		}
	}

	public boolean isOpened() {
		return opened;
	}

	public void save() {
		setValue("romchecksum", ""+RomReader.toHexString(RomReader.rom.getRomChecksum()));
		try {
			File f = new File(filename);
			f.delete();
			f.createNewFile();
			PrintWriter out = new PrintWriter(f);

			String fileSection = "default";
			String section = "default";
			for (int i=0; i<getNumEntries(); i++) {
				if (!fileSection.equals(fileSections.get(i))) {
					fileSection = fileSections.get(i);
					out.println("{" + fileSection + "}");
					section = "default";
				}
				if (!section.equals(sections.get(i))) {
					section = sections.get(i);
					out.println("[" + section + "]");
				}

				out.print(names.get(i));
				for (int j=0; j<getNumValues(i); j++) {
					out.print("=" + values.get(i).get(j));
				}
				out.println();
			}

			out.close();
		}
		catch(FileNotFoundException e) {}
		catch(IOException e) {}
	}
}
