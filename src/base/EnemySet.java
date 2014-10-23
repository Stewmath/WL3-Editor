package base;

import java.util.ArrayList;

import record.*;

public class EnemySet {

	final static int GFX_DATA_SIZE = 0x400;
	// Some sprites have a small size for whatever reason
	final static int GFX_ALTERNATE_DATA_SIZE = 0x300;

	static ArrayList<EnemySet> enemySets = new ArrayList<EnemySet>();

	public static EnemySet getEnemySet(int ptr) {
		int addr = RomReader.BANK(ptr, 0x19);
		for (int i=0; i<enemySets.size(); i++) {
			if (enemySets.get(i).enemySetRecord.getAddr() == addr)
				return enemySets.get(i);
		}
		// TODO: handling enemySets in unexpected places? Probably necessary once I allow them to be moved.
		return null;
	}
	public static EnemySet getEnemySet(String name) {
		for (EnemySet e : enemySets) {
			if (e.getName().equals(name))
				return e;
		}
		return null;
	}

	// All enemy sets should be loaded at once, so enemy graphics can be moved freely.
	public static void reloadEnemySets() {
		enemySets = new ArrayList<EnemySet>();

		ValueFileParser file = ValueFileParser.getEnemySetFile();
		int entries = file.getNumEntries();
		for (int i=0; i<entries; i++) {
			int addr = file.indexToIntValue(i);
			String name = file.indexToName(i);

			EnemySet enemySet = new EnemySet(addr, name);
			enemySets.add(enemySet);
		}
	}


	RomReader rom;

	MoveableDataRecord enemySetRecord;
	MoveableDataRecord[] gfxDataRecords = new MoveableDataRecord[4];

	String name;


	EnemySet(int addr, String name) {
		rom = RomReader.rom;

		int recordStart=RomReader.BANK(addr, 0x19);
		int recordEnd=recordStart+1+8+1;

		while (rom.read(recordEnd) != 0xff)
			recordEnd+=2;

		enemySetRecord = rom.getMoveableDataRecord(recordStart, null, false, recordEnd-recordStart+1+0x20);
		enemySetRecord.setDescription("Enemy set '" + ValueFileParser.getEnemySetFile().getName(enemySetRecord.getAddr()) + "'");

		loadGfxRecords();
	}

	public String getName() {
		return name;
	}

	public void addPtr(RomPointer ptr) {
		enemySetRecord.addPtr(ptr);
	}
	public void removePtr(RomPointer ptr) {
		enemySetRecord.removePtr(ptr);
	}
	public int getAddr() {
		return enemySetRecord.getAddr();
	}

	public int getBaseGfxBank() {
		return enemySetRecord.read(0)+0x68;
	}

	public void setBaseGfxBank(int n) {
		enemySetRecord.write(0, (byte)(n-0x68));
		loadGfxRecords();
	}


	public int getNumEnemies() {
		return (enemySetRecord.getDataSize()-9-0x20)/2 - 1;
	}
	public int getEnemy(int i) {
		if (i >= getNumEnemies())
			return -1;
		return enemySetRecord.read16(9+i*2);
	}
	public void setEnemy(int i, int value) {
		if (i >= getNumEnemies())
			return;
		enemySetRecord.writePtr(9+i*2, value);
	}
	public void addEnemy() {
		if (getNumEnemies() >= 0xf-4)
			return;
		byte[] paletteData = enemySetRecord.toArray(9+getNumEnemies()*2);
		enemySetRecord.setDataSize(enemySetRecord.getDataSize()+2);
		// shift paletteData
		enemySetRecord.write(getPaletteOffset(), paletteData);

		enemySetRecord.write16(9+(getNumEnemies()-1)*2, 0x4000);
		enemySetRecord.write16(9+(getNumEnemies()-0)*2, 0xffff);
	}


	public int getPaletteOffset() {
		return 9+getNumEnemies()*2+2;
	}
	public int[][] getPalette(int i) {
		int start = getPaletteOffset()+i*8;
		return RomReader.binToPalettes(enemySetRecord.toArray(start, start+8));
	}
	public int[][] getPalettes() {
		int start = getPaletteOffset();
		return RomReader.binToPalettes(enemySetRecord.toArray(start, start+0x20));
	}
	public void setPaletteData(byte[] data) {
		int start = getPaletteOffset();
		for (int i=0; i<0x20; i++) {
			enemySetRecord.write(i+start, data[i]);
		}
	}
	public void setPalettes(int[][] palettes) {
		byte[] data = RomReader.palettesToBin(palettes);
		setPaletteData(data);
	}

	// Don't convert to absolute address, returns gameboy-style pointer
	public int getGfxPtr(int i) {
		if (i >= 4)
			return -1;
		return enemySetRecord.read16(1+i*2);
	}
	// Convert to absolute address
	public int getGfxAddr(int i) {
		if (i >= 4)
			return -1;
		return RomReader.BANK(enemySetRecord.read16(1+i*2), getBaseGfxBank()+i);
	}
	// Can be an absolute address, or not, as long as the caller understands which bank it must be in.
	public void setGfxAddr(int i, int addr) {
		enemySetRecord.writePtr(i*2+1, addr);
		loadGfxRecords();
	}

	public byte[] getGfxData(int i) {
		if (gfxDataRecords[i] == null)
			return null;
		return gfxDataRecords[i].toArray();
	}
	public void setGfxData(int i, byte[] data) {
		gfxDataRecords[i].setData(data);
	}

	void loadGfxRecords() {
		for (int i=0; i<4; i++) {
			if (gfxDataRecords[i] == null || gfxDataRecords[i].getAddr() != getGfxAddr(i)) {
				RomPointer pointer = new RomPointer(enemySetRecord, i*2+1);
				if (gfxDataRecords[i] != null)
					// Detach pointer from the previous record, since the gfx in use has apparently changed.
					gfxDataRecords[i].removePtr(pointer);

				if (getBaseGfxBank()+i < rom.getRomSize()/0x4000) {
					gfxDataRecords[i] = rom.getMoveableDataRecord(enemySetRecord.read16(i*2+1, getBaseGfxBank()+i),
							pointer, true, 0);

					// Integrity check
					if ((gfxDataRecords[i].getDataSize() != GFX_DATA_SIZE) &&
							gfxDataRecords[i].getDataSize() != GFX_ALTERNATE_DATA_SIZE) {
						gfxDataRecords[i].removePtr(pointer);
						rom.deleteMoveableDataRecord(gfxDataRecords[i]);
						gfxDataRecords[i] = null;
						return;
					}

					gfxDataRecords[i].setRequiredBank(getBaseGfxBank()+i);
					gfxDataRecords[i].setMoveable(true);

					String name = ValueFileParser.getEnemyGfxFile().getSection(""+Integer.toHexString(getBaseGfxBank()+i)).getName(RomReader.toGbPtr(gfxDataRecords[i].getAddr()));

					gfxDataRecords[i].setDescription("Gfx for enemy '" + name + "'");

					gfxDataRecords[i].setDataSize(GFX_DATA_SIZE);

					// Spearhead, Hammer-bot, and Doughnuteer graphics have hard-coded pointers
					// for the credit sequence. Those pointers are added here.
					if (name.equalsIgnoreCase("Spearhead")) {
						RomPointer creditsPointer = new RomPointer(
								RomReader.BANK(0x6f00, 0x2b),
								RomReader.BANK(0x6efb, 0x2b));
						gfxDataRecords[i].addPtr(creditsPointer);
					}
					else if (name.equalsIgnoreCase("Hammer-bot")) {
						RomPointer creditsPointer = new RomPointer(
								RomReader.BANK(0x6f22, 0x2b),
								RomReader.BANK(0x6f1d, 0x2b));
						gfxDataRecords[i].addPtr(creditsPointer);
					}
					else if (name.equalsIgnoreCase("Doughnuteer")) {
						RomPointer creditsPointer = new RomPointer(
								RomReader.BANK(0x6f44, 0x2b),
								RomReader.BANK(0x6f3f, 0x2b));
						gfxDataRecords[i].addPtr(creditsPointer);
					}

					// Add a pointer for the metadata, so the editor can keep track of moved graphics.
					MetaRomPointer metaPointer = new MetaRomPointer(name, "enemyGfx.txt",
							RomReader.toHexString(getBaseGfxBank()+i), MetaRomPointer.FORMAT_GB_PTR);
					gfxDataRecords[i].addPtr(metaPointer);
				}
				else {
					gfxDataRecords[i] = null;
				}
			}
		}
	}
}
