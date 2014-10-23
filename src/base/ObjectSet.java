package base;

import record.*;

public class ObjectSet {

	// This table is unusual in that each entry has 2 pointers.
	// This means each entry is 4 bytes long.
	final static int objectTbl = RomReader.BANK(0x5009, 0x19);

	static MoveableDataRecord objectTblRecord;

	public static int NUM_OBJECT_SETS = 0x92;
	static ObjectSet[] objectSets = new ObjectSet[NUM_OBJECT_SETS];

	public static ObjectSet getObjectSet(int id) {
		if (id >= NUM_OBJECT_SETS)
			return null;
		return objectSets[id];
	}

	// This function is called the first time from MainFrame.java.
	// All object sets should be loaded, so that enemy sets can be safely moved around.
	public static void reloadObjectSets() {
		objectTblRecord = RomReader.rom.getMoveableDataRecord(objectTbl, null, false, NUM_OBJECT_SETS*4);
		for (int i=0; i<NUM_OBJECT_SETS; i++) {
			objectSets[i] = new ObjectSet(i);
		}
	}
	



	RomReader rom;
	int id;

	public EnemySet enemySet;

	private ObjectSet(int _id)
	{
		rom = RomReader.rom;
		
		id = _id;
		setItemSetAddr(objectTblRecord.read16(id*4));
		setEnemySetAddr(objectTblRecord.read16(id*4+2));
	}
	
	public int getId()
	{
		return id;
	}

	public int getItemSetAddr()
	{
		return objectTblRecord.read16(id*4);
	}
	public void setItemSetAddr(int addr)
	{
		objectTblRecord.writePtr(id*4, addr);
	}
	
	public void setEnemySetAddr(int addr)
	{
		RomPointer enemySetPointer = new RomPointer(objectTblRecord, id*4+2);
		if (enemySet != null)
			enemySet.removePtr(enemySetPointer);
		enemySet = EnemySet.getEnemySet(addr);
		enemySet.addPtr(enemySetPointer);
	}

	public void setEnemySet(EnemySet e) {
		RomPointer enemySetPointer = new RomPointer(objectTblRecord, id*4+2);
		if (enemySet != null)
			enemySet.removePtr(enemySetPointer);
		enemySet = e;
		enemySet.addPtr(enemySetPointer);
	}
	
	public EnemySet getEnemySet() {
		return enemySet;
	}
}
