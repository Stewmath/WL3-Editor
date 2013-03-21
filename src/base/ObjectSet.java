package base;

import java.util.HashMap;

import graphics.ComboBoxFromFile;
import record.*;

public class ObjectSet {

	public static int lastObjectSet = 0x91;
	static boolean loadAllObjectSets=true;
	static ObjectSet[] objectSets = new ObjectSet[lastObjectSet+1];

	public static ObjectSet getObjectSet(int id) {
		if (id > lastObjectSet)
			return null;
		return objectSets[id];
	}

	// This function is called the first time from MainFrame.java.
	public static void reloadObjectSets() {
		for (int i=0; i<=lastObjectSet; i++) {
			objectSets[i] = new ObjectSet(i);
		}
	}
	public static void saveObjectSets() {
	}
	
	// This table is unusual in that each entry has 2 pointers.
	// This means each entry is 4 bytes long.
	final int objectTbl = RomReader.BANK(0x5009, 0x19);
	RomReader rom;
	int id;
	// Note: I have reservations about moving these records.
	// Without all the levels loaded at once, one can't know 
	// if all of the pointers are there.
	public MoveableDataRecord itemSetRecord, enemySetRecord;
	public MoveableDataRecord[] enemySetGfxRecords = new MoveableDataRecord[4];

	private ObjectSet(int _id)
	{
		rom = RomReader.rom;
		
		id = _id;
		setItemSetPtr(rom.read16(objectTbl+id*4));
		setEnemySetPtr(rom.read16(objectTbl+id*4+2));
//		itemSetRecord.deleteWithNoPtr = false;
//		enemySetRecord.deleteWithNoPtr = false;
	}
	
	public int getItemSetPtr()
	{
		return RomReader.toGbPtr(itemSetRecord.getAddr());
	}
	public void setItemSetPtr(int ptr)
	{
		RomPointer itemSetPointer = new RomPointer(objectTbl+id*4);
		if (itemSetRecord != null)
			itemSetRecord.removePtr(itemSetPointer);
		itemSetRecord = rom.getMoveableDataRecord(RomReader.BANK(ptr, 0x19), itemSetPointer, false, 4);
		itemSetRecord.setDescription("'" + ComboBoxFromFile.itemSetFile.getAssociate(itemSetRecord.getAddr()) + "' item set");
//		itemSetRecord.deleteWithNoPtr = false;
	}
	
	public int getEnemySetPtr()
	{
		return RomReader.toGbPtr(enemySetRecord.getAddr());
	}
	public void setEnemySetPtr(int ptr)
	{
		RomPointer enemySetPointer = new RomPointer(objectTbl+id*4+2);
		if (enemySetRecord != null)
			enemySetRecord.removePtr(enemySetPointer);
		int recordStart=RomReader.BANK(ptr, 0x19);
		int recordEnd=recordStart+1+8+1;

		while (rom.read(recordEnd) != 0xff)
			recordEnd+=2;

		enemySetRecord = rom.getMoveableDataRecord(recordStart, enemySetPointer, false, recordEnd-recordStart+1+0x20);
		enemySetRecord.setDescription("Enemy set '" + ComboBoxFromFile.enemySetFile.getAssociate(enemySetRecord.getAddr()) + "'");
		refreshEnemySetGfx();
//		enemySetRecord.deleteWithNoPtr = false;
	}
	
	public int getId()
	{
		return id;
	}

	public void addEnemy() {
		byte[] paletteData = enemySetRecord.toArray(9+getEnemySetNumEnemies()*2);
		enemySetRecord.setDataSize(enemySetRecord.getSize()+2);
		// shift paletteData
		enemySetRecord.write(9+getEnemySetNumEnemies()*2, paletteData);

		enemySetRecord.write16(9+(getEnemySetNumEnemies()-2)*2, 0x4000);
		enemySetRecord.write16(9+(getEnemySetNumEnemies()-1)*2, 0xffff);
	}

	public int getEnemySetBaseBank() {
		return enemySetRecord.read(0)+0x68;
	}

	public void setEnemySetBaseBank(int n) {
		enemySetRecord.write(0, (byte)(n-0x68));
		setEnemySetPtr(getEnemySetPtr());
	}

	public int getEnemySetNumEnemies() {
		return (enemySetRecord.getDataSize()-9-0x20)/2;
	}

	public int[][] getEnemySetPalette(int i) {
		int start = 9+getEnemySetNumEnemies()*2+i*8;
		return RomReader.binToPalettes(enemySetRecord.toArray(start, start+8));
	}
	public int[][] getEnemySetPalettes() {
		int start = 9+getEnemySetNumEnemies()*2;
		return RomReader.binToPalettes(enemySetRecord.toArray(start, start+0x20));
	}

	public void setEnemySetPaletteData(byte[] data) {
		int start = 9+getEnemySetNumEnemies()*2;
		for (int i=0; i<0x20; i++) {
			enemySetRecord.write(i+start, data[i]);
		}
	}
	public void setEnemySetPalettes(int[][] palettes) {
		byte[] data = RomReader.palettesToBin(palettes);
		int start = 9+getEnemySetNumEnemies()*2;
		for (int i=0; i<0x20; i++) {
			enemySetRecord.write(i+start, data[i]);
		}
	}

	public void setEnemySetGfx(int i, int addr) {
		enemySetRecord.writePtr(i*2+1, addr);
		refreshEnemySetGfx();
	}

	void refreshEnemySetGfx() {
		for (int i=0; i<4; i++) {
			if (getEnemySetBaseBank()+i < rom.getRomSize()/0x4000) {
				RomPointer pointer = new RomPointer(enemySetRecord, i*2+1);
				if (enemySetGfxRecords[i] != null)
					enemySetGfxRecords[i].removePtr(pointer);
				enemySetGfxRecords[i] = rom.getMoveableDataRecord(enemySetRecord.read16(i*2+1, getEnemySetBaseBank()+i), pointer, true, 0);
				enemySetGfxRecords[i].setRequiredBank(getEnemySetBaseBank()+i);
				enemySetGfxRecords[i].setDescription("Gfx for enemy '" +
						ComboBoxFromFile.enemyGfxFile.getSection(""+Integer.toHexString(getEnemySetBaseBank()+i)).getAssociate(RomReader.toGbPtr(enemySetGfxRecords[i].getAddr())) + "'");
				enemySetGfxRecords[i].isMoveable = false;
			}
			else {
				enemySetGfxRecords[i] = null;
			}
		}
	}
}
