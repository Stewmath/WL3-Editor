package base;

import graphics.ComboBoxFromFile;
import record.*;

public class ObjectSet {

	// This table is unusual in that each entry has 2 pointers.
	// This means each entry is 4 bytes long.
	final static int objectTbl = RomReader.BANK(0x5009, 0x19);

	public static int lastObjectSet = 0x91;
	static ObjectSet[] objectSets = new ObjectSet[lastObjectSet+1];

	public static ObjectSet getObjectSet(int id) {
		if (id > lastObjectSet)
			return null;
		return objectSets[id];
	}

	// This function is called the first time from MainFrame.java.
	// All object sets should be loaded, so that enemy sets can be safely moved around.
	public static void reloadObjectSets() {
		for (int i=0; i<=lastObjectSet; i++) {
			objectSets[i] = new ObjectSet(i);
		}
	}
	public static void saveObjectSets() {
	}
	



	RomReader rom;
	int id;

	MoveableDataRecord itemSetRecord;

	public EnemySet enemySet;

	private ObjectSet(int _id)
	{
		rom = RomReader.rom;
		
		id = _id;
		setItemSetAddr(rom.read16(objectTbl+id*4));
		setEnemySetAddr(rom.read16(objectTbl+id*4+2));
	}
	
	public int getId()
	{
		return id;
	}

	public int getItemSetAddr()
	{
		return RomReader.toGbPtr(itemSetRecord.getAddr());
	}
	public void setItemSetAddr(int addr)
	{
		RomPointer itemSetPointer = new RomPointer(objectTbl+id*4);
		if (itemSetRecord != null)
			itemSetRecord.removePtr(itemSetPointer);
		itemSetRecord = rom.getMoveableDataRecord(RomReader.BANK(addr, 0x19), itemSetPointer, false, 4);
		itemSetRecord.setDescription("'" + ComboBoxFromFile.itemSetFile.getAssociate(itemSetRecord.getAddr()) + "' item set");
//		itemSetRecord.deleteWithNoPtr = false;
	}
	
	public void setEnemySetAddr(int addr)
	{
		RomPointer enemySetPointer = new RomPointer(objectTbl+id*4+2);
		if (enemySet != null)
			enemySet.removePtr(enemySetPointer);
		enemySet = EnemySet.getEnemySet(addr);
		enemySet.addPtr(enemySetPointer);
	}
	
	public EnemySet getEnemySet() {
		return enemySet;
	}
}
