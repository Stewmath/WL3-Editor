package base;

import java.util.Comparator;

import record.RomReader;

public class Region implements Comparable {

	public int firstVSector, lastVSector;
	public int firstHSector, lastHSector;
	public int scrollMode;
	public int objectSetId;
	public int b5,b6;
	public int tileSetId;
	public TileSet tileSet;
	public ObjectSet objectSet;
	
	public Region(byte[] warpData)
	{
		firstVSector = (warpData[1]>>4)&0xf;
		lastVSector = warpData[1]&0xf;
		firstHSector = (warpData[2]>>4)&0xf;
		lastHSector = warpData[2]&0xf;
		scrollMode = warpData[3]&0xff;
		objectSetId = warpData[4]&0xff;
		b5 = warpData[5]&0xff;
		b6 = warpData[6]&0xff;
		tileSetId = warpData[7]&0xff;
		objectSet = ObjectSet.getObjectSet(objectSetId);

	}
	public Region(Region r) {
		firstVSector = r.firstVSector;
		lastVSector = r.lastVSector;
		firstHSector = r.firstHSector;
		lastHSector = r.lastHSector;
		scrollMode = r.scrollMode;
		objectSetId = r.objectSetId;
		b5 = r.b5;
		b6 = r.b6;
		tileSetId = r.tileSetId;
		tileSet = r.tileSet;
		objectSet = r.objectSet;
	}
	
	public byte[] toArray()
	{
		byte[] ret = new byte[8];
		ret[1] = (byte)((firstVSector<<4) | lastVSector);
		ret[2] = (byte)((firstHSector<<4) | lastHSector);
		ret[3] = (byte)scrollMode;
		ret[4] = (byte)objectSetId;
		ret[5] = (byte)b5;
		ret[6] = (byte)b6;
		ret[7] = (byte)tileSetId;
		return ret;
	}
	
	public boolean isValid()
	{
		if (tileSetId != 0 && firstHSector < lastHSector && firstVSector < lastVSector)
			return true;
		return false;
	}
	
	public void setObjectSet(int set)
	{
		if (set < 256)
		{
			objectSetId = set;
			objectSet = ObjectSet.getObjectSet(objectSetId);
		}
	}
	public void setTileSet(int set)
	{
		if (set < 256 && set != 0)
		{
			tileSetId = set;
			tileSet = TileSet.getTileSet(tileSetId);
		}
	}
	
	public void setCropLeft(boolean set)
	{
		if (set)
			scrollMode |= 0x20;
		else
			scrollMode &= ~0x20;
	}
	public void setCropRight(boolean set)
	{
		if (set)
			scrollMode |= 0x10;
		else
			scrollMode &= ~0x10;
	}
	public void setCropTop(boolean set)
	{
		if (set)
			scrollMode |= 0x40;
		else
			scrollMode &= ~0x40;
	}
	public void setCropBottom(boolean set)
	{
		if (set)
			scrollMode |= 0x80;
		else
			scrollMode &= ~0x80;
	}
	public boolean getCropLeft()
	{
		return (scrollMode&0x20)!=0;
	}
	public boolean getCropRight()
	{
		return (scrollMode&0x10)!=0;
	}
	public boolean getCropTop()
	{
		return (scrollMode&0x40)!=0;
	}
	public boolean getCropBottom()
	{
		return (scrollMode&0x80)!=0;
	}
	
	public int getFirstSector()
	{
		return firstVSector*0xa+firstHSector;
	}
	public int getWidth()
	{
		return lastHSector-firstHSector;
	}
	public int getHeight()
	{
		return lastVSector-firstVSector;
	}
	
	public boolean equals(Object o)
	{
		if (o.getClass() != getClass())
			return false;
		Region r = (Region)o;
		
		if (r.firstVSector == firstVSector && r.lastVSector == lastVSector && r.firstHSector == firstHSector && r.lastHSector == lastHSector &&
				scrollMode == r.scrollMode && objectSetId == r.objectSetId && b5 == r.b5 && b6 == r.b6 &&
				tileSetId == r.tileSetId)
			return true;
		return false;
	}

	@Override
	public int compareTo(Object o) {
		Region r = (Region)o;
		
		if (firstVSector < r.firstVSector)
			return -1;
		else if (firstVSector > r.firstVSector)
			return 1;
		if (firstHSector < r.firstHSector)
			return -1;
		else if (firstHSector > r.firstHSector)
			return 1;
		return 0;
	}
	
	public String toString() {
		String ret = "";
		byte[] data = toArray();
		for (int i=0; i<8; i++) {
			ret += RomReader.toHexString(data[i]&0xff, 2) + " ";
		}
		return ret;
	}
}
