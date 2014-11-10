package record;

import base.MetadataFileParser;
import base.ValueFileParser;

// MetaRomPointer: a RomPointer which will write data to the metadata file.
public class MetaRomPointer extends RomPointer {
	// GB_PTR: a number between 0x4000 and 0x7fff, reflecting the way pointers are stored.
	public static final int FORMAT_GB_PTR = 0;
	// ABSOLUTE: the address in the ROM.
	public static final int FORMAT_ABSOLUTE = 1;


	int format;
	String name;
	String fileSection;
	String section;

	// Absolute format (if possible)
	int addr = -1;


	public MetaRomPointer(String name, String fileSection, String section, int format) {
		this.format = format;
		this.name = name;
		this.fileSection = fileSection;
		if (section == null)
			this.section = "default";
		else
			this.section = section;
	}

	public void write(int newAddr, int newBank) {
		if (newBank >= 0)
			addr = RomReader.BANK(newAddr, newBank);
		else
			addr = newAddr;
	}

	public int getPointedAddr() {
		try {
			return ValueFileParser.getMetadataFile().
				getFileSection(fileSection).getSection(section).getIntValue(name);
		}
		catch(NumberFormatException e) {
			return -1;
		}
	}

	public boolean hasBankAddr() {
		return format != FORMAT_GB_PTR;
	}

	public int getType() {
		return RomPointer.TYPE_METADATA;
	}

	public void save() {
		String output = "";
		switch(format) {
			case FORMAT_GB_PTR:
				output = RomReader.toHexString(RomReader.toGbPtr(addr));
				break;
			case FORMAT_ABSOLUTE:
				output = RomReader.toHexString(addr);
				break;
		}
		ValueFileParser.getMetadataFile().setValue(name, output, fileSection, section);
	}

	public boolean isNull() {
		return false;
	}

	public boolean equals(Object o) {
		if (o.getClass() != this.getClass())
			return false;
		MetaRomPointer p = (MetaRomPointer)o;

		if (p.name.equals(name) && p.fileSection.equals(fileSection) && p.section.equals(section))
			return true;
		return false;
	}
}
