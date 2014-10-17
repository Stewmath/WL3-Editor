package base;

import base.TextParser;
import record.*;
import record.RomPointer;

import java.awt.image.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.*;
import java.io.*;




public class Level {

	public final static int levelDataTbl = RomReader.BANK(0x40be, 0x30);
	// This is a table of tables...
	public final static int warpDataTblTbl = RomReader.BANK(0x4319, 0x30);

	public final static int musicDataTbl = RomReader.BANK(0x7e40, 0xf);
	
	public final static int lastLevel = 0xc8;
	
	public RomReader rom;
	
	MoveableDataRecord musicRecord;
	
	int levelIndex;
	
	TileSet defaultTileSet;
	public MoveableDataRecord levelDataRecord;
	public MoveableDataRecord tileDataRecord;
	public MoveableDataRecord objectDataRecord;
	public JoinedRecord layoutRecord;
	RomPointer levelDataPointer, tileDataPointer, objectDataPointer;


	public RegionRecord regionDataRecord;
	RomPointer regionDataPointer;
	
	BufferedImage levelImage = null;
	
	static Level[] levels = new Level[lastLevel+1];
	public static Level getLevel(int index)
	{
		if (index > lastLevel)
			return null;
		if (levels[index] == null) {
			levels[index] = new Level(RomReader.rom, index);
		}
		
		// All different "versions" of the level should be loaded.
		// If they use the same data in some areas, it's useful for it to be loaded into the records.
		for (int i=(index/8)*8; i<(index/8)*8+8 && i <= lastLevel; i++)
			if (levels[i] == null)
				levels[i] = new Level(RomReader.rom, i);
		
		return levels[index];
	}
	public static void reloadLevels()
	{
		for (int i=0; i<=lastLevel; i++)
			levels[i] = new Level(RomReader.rom, i);
	}
	
	Level(RomReader _rom, int index)
	{
		rom = _rom;
		levelIndex = index;
		
		// Be careful here - check getNewLevelData() to make sure any changes made here match it.
		int levelDataAddr = rom.read16FromTable(levelDataTbl, levelIndex, 0x30);
		levelDataPointer = new RomPointer(levelDataTbl+2*levelIndex); 
		levelDataRecord = rom.getMoveableDataRecord(levelDataAddr, levelDataPointer, false, 5); 
		
		int levelDataBank = levelDataRecord.read(2);
		tileDataPointer = new RomPointer(levelDataRecord,0,2);
		objectDataPointer = new RomPointer(levelDataRecord,3,2);
		tileDataRecord = rom.getMoveableDataRecord(levelDataRecord.read16(0, levelDataBank), tileDataPointer, true, 0);
		objectDataRecord = rom.getMoveableDataRecord(levelDataRecord.read16(3, levelDataBank), objectDataPointer, true, 0);

		levelDataRecord.deleteWithNoPtr = true;
		tileDataRecord.deleteWithNoPtr = true;
		objectDataRecord.deleteWithNoPtr = true;
		layoutRecord = rom.getJoinedRecord(tileDataRecord, objectDataRecord);
		
		int warpDataBank;
		if (levelIndex >= 0x64)
			warpDataBank = 0x31;
		else
			warpDataBank = 0x30;
		int warpDataTbl = rom.read16FromTable(warpDataTblTbl, levelIndex, warpDataBank);
		regionDataPointer = new RomPointer(warpDataTblTbl+2*levelIndex);
		regionDataRecord = RegionRecord.get(warpDataTbl, regionDataPointer);
		
		int musicDataAddr = musicDataTbl+2*levelIndex;
		musicRecord = rom.getMoveableDataRecord(musicDataAddr, null, false, 2);
		
		updateRecordDescriptions();
		
	//	generateImage();
		
	}
	
	public int getLevelDataAddr()
	{
		return levelDataRecord.getAddr();
	}

	void updateRecordDescriptions() {
		tileDataRecord.setDescription("Level " + RomReader.toHexString(getId(), 2) + " tile data");
		objectDataRecord.setDescription("Level " + RomReader.toHexString(getId(), 2) + " object data");
		levelDataRecord.setDescription("Level " + RomReader.toHexString(getId(), 2) + " level data");
	}
	public void getNewLevelData()
	{
		// Remove pointers - these records will soon be replaced.
		levelDataRecord.removePtr(levelDataPointer);

		// Don't remove these pointers... they're "attached" to the level data we just abandoned.
		/*
		tileDataRecord.removePtr(tileDataPointer);
		objectDataRecord.removePtr(objectDataPointer);
		*/

		// Make a copy of levelDataRecord.
		byte[] data = levelDataRecord.toArray();
		levelDataRecord = rom.getMoveableDataRecord(data, levelDataPointer, 0x30, false);

		// Update pointers which rely on levelDataRecord.
		tileDataPointer = new RomPointer(levelDataRecord,0,2);
		objectDataPointer = new RomPointer(levelDataRecord,3,2);

		// Make a copy of the tiledata.
		data = tileDataRecord.toArray();
		tileDataRecord = rom.getMoveableDataRecord(data, tileDataPointer, -1, true);

		// Make a copy of the objectdata.
		data = objectDataRecord.toArray();
		objectDataRecord = rom.getMoveableDataRecord(data, objectDataPointer, -1, true);

		// set the JoinedRecord.
		layoutRecord = rom.getJoinedRecord(tileDataRecord,objectDataRecord);

		// deleteWithNoPtr should be set.
		levelDataRecord.deleteWithNoPtr = true;
		tileDataRecord.deleteWithNoPtr = true;
		objectDataRecord.deleteWithNoPtr = true;

		for (int i=getId()/8*8; i<(getId()/8+1)*8; i++) {
			if (i > lastLevel)
				break;
			getLevel(i).updateRecordDescriptions();
		}
	}

	public void copyLevelData(Level l2) {
		// Important, in case the old leveldata is no longer used, it can be deleted.
		levelDataRecord.removePtr(levelDataPointer);

		levelDataRecord = l2.levelDataRecord;
		levelDataRecord.addPtr(levelDataPointer);

		tileDataPointer = new RomPointer(levelDataRecord, 0, 2);
		objectDataPointer = new RomPointer(levelDataRecord, 3, 2);

		tileDataRecord = l2.tileDataRecord;
		objectDataRecord = l2.objectDataRecord;
		layoutRecord = l2.layoutRecord;
	}

	public void setRegionDataRecord(RegionRecord r) {
		regionDataRecord.removePtr(regionDataPointer);
		
		regionDataRecord = r;
		regionDataRecord.addPtr(regionDataPointer);

	}
	public RegionRecord getRegionDataRecord() {
		return regionDataRecord;
	}

	public byte getTile(int x, int y) {
		// High bit indicates if an object is present on that space... so AND it with 0x7f.
		return (byte)(tileDataRecord.read(x+y*0xa0)&0x7f);
	}
	public void setTile(int x, int y, int tile) {
		int i = x+y*0xa0;
		int objectBit = 0;
		if (getObject(x, y) != 0 && getObject(x, y) != 0xf)
			objectBit = 0x80;
		tileDataRecord.write(i, (byte)(tile|objectBit));

		TileSet tileSet = null;
		Region r = getRegion(x, y);
		if (r != null)
			tileSet = r.tileSet;
		if (tileSet != null)
			levelImage.getGraphics().drawImage(tileSet.getTileImage(tile), x*16, y*16, null);
	}
	
	public int getObject(int x, int y) {
		int i = x+y*0xa0;
		
		int b = objectDataRecord.read(i/2)&0xff;
		if (i%2 == 0)
			b >>= 4;
		else
			b &= 0x0f;
		
		if ((tileDataRecord.read(i)&0x80) != 0 || b == 0xf)
			return b;
		return 0;
	}
	public void setObject(int x, int y, int obj) {
		int i = x+y*0xa0;
		
		if (obj == 0 || obj == 0xf)
			tileDataRecord.write(i, (byte)(tileDataRecord.read(i)&0x7f));
		else
			tileDataRecord.write(i, (byte)(tileDataRecord.read(i)|0x80));
		
		if (i%2 == 0)
			objectDataRecord.write(i/2, (byte)((objectDataRecord.read(i/2)&0x0f)|(obj<<4)));
		else
			objectDataRecord.write(i/2, (byte)((objectDataRecord.read(i/2)&0xf0)|(obj)));
	}
	
	public Region getRegion(int x, int y) {
		return regionDataRecord.getRegion(x,y);
	}
	
	public int getId() {
		return levelIndex;
	}
	
	public int getMusicId() {
		return musicRecord.read16(0);
	}
	public void setMusicId(int id) {
		musicRecord.write(0, (byte)(id&0xff));
		musicRecord.write(1, (byte)(id>>8));
	}
	
	public BufferedImage getImage() {
		if (levelImage == null)
			generateImage();
		return levelImage;
	}
	public void freeImage() {
		levelImage = null;
	}
	
	public void generateImage() {
		levelImage = new BufferedImage(0xa0*16, 0x30*16, BufferedImage.TYPE_USHORT_555_RGB);
		Graphics g = levelImage.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, levelImage.getWidth(), levelImage.getHeight());
		
		for (int i=0; i<regionDataRecord.regions.size(); i++)
		{
			Region r = regionDataRecord.regions.get(i);
			int endX = r.lastHSector*16;
			int endY = r.lastVSector*16;
			TileSet tileSet = TileSet.getTileSet(r.tileSetId);
			
			for (int x = r.firstHSector*16; x<endX; x++)
			{
				for (int y = r.firstVSector*16; y<endY; y++)
				{
					int tile = getTile(x, y);
					g.drawImage(tileSet.getTileImage(tile), x*16, y*16, null);
				}
			}
		}

		/*
		for (int y=0; y<0x30; y++)
		{
			for (int x=0; x<0xa0; x++)
			{
				int tile = tileDataRecord.get(x+y*0xa0);
				g.drawImage(tileSet.getTileImage(tile&0x7f), x*16, y*16, null);
			}
		}*/
	}
}
