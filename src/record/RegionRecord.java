package record;

import java.util.*;
import base.Region;
import base.TileSet;

public class RegionRecord extends Record
{
	final static int numSectors = 0x1e;

	static ArrayList<RegionRecord> regionRecords = new ArrayList<RegionRecord>();
	// This function gets a regionRecord.
	public static RegionRecord get(int _tblAddr, RomPointer ptr) {
		for (int i=0; i<regionRecords.size(); i++) {
			RegionRecord r = regionRecords.get(i);
			if (r.addr == _tblAddr && r.addr != -1) {
				r.addPtr(ptr);
				return r;
			}
		}
		RegionRecord r = new RegionRecord(_tblAddr);
		r.addPtr(ptr);
		regionRecords.add(r);
		return r;
	}
	// This function makes a copy of a regionRecord.
	public static RegionRecord getCopy(RegionRecord copy, RomPointer ptr) {
		RegionRecord r = new RegionRecord(copy);
		r.ptrs = new ArrayList<RomPointer>(); // Remove old pointers
		r.addPtr(ptr);
		regionRecords.add(r);
		return r;
	}

	public static void reloadRecords() {
		regionRecords = new ArrayList<RegionRecord>();
	}

	RomReader rom;
	// Here, addr is warpDataTbl.
	// It *probably* won't change.
	int warpDataBank;

	public ArrayList<Region> regions;
	// tableRecord - the table containing pointers to each warpRecord.
	MoveableDataRecord tableRecord;
	// warpRecords - 0xa*0x3 entries, one for each warp.
	MoveableDataRecord[] warpRecords;
	// sectorDestinations - warp destination for each sector.
	public int[] sectorDestinations;
	// null warps - sectors which don't warp to anywhere.
	// To save space, most levels will share their "null warps" with each other.
	// For this reason and others, all levels should probably be loaded right as the rom is opened.
	boolean[] nullWarp;
	MoveableDataRecord nullWarpDataRecord;

	RegionRecord(int _tblAddr) {
		rom = RomReader.rom;

		modified = false;
		addr = _tblAddr;
		warpDataBank = addr/0x4000;
		// The pointer to tableRecord would be the same as the pointer to this record.
		// See the save() function.
		tableRecord = rom.getMoveableDataRecord(addr, null, false, 0xa*3*2);
		// Generate regions
		regions = new ArrayList<Region>();
		warpRecords = new MoveableDataRecord[0xa*0x3];
		sectorDestinations = new int[0xa*0x3];
		nullWarp = new boolean[0xa*0x3];
		for (int x=0; x<0xa; x++)
		{
			for (int y=0; y<0x3; y++)
			{
				int i = x+y*0xa;
				
				int warpDataAddr = tableRecord.read16(i*2, warpDataBank);
				RomPointer warpDataPointer = new RomPointer(tableRecord, i*2);
				warpRecords[i] = rom.getMoveableDataRecord(warpDataAddr, warpDataPointer, false, 8);
				warpRecords[i].deleteWithNoPtr = true;
				
				Region r = new Region(warpRecords[i].toArray());
				int b1 = warpRecords[i].read(0)&0xff;
				sectorDestinations[i] = (b1>>4)*0xa + (b1&0xf);
				if (sectorDestinations[i] >= numSectors)
					sectorDestinations[i] = 0xff;
				
				// Here, all other regions are checked to make sure no others 
				// contain the same sectors.
				// This happens commonly when there are 2 warp points to the 
				// same region.
				// However when the warp points describe that region 
				// differently, only one is selected.
				// Theoretically 2 regions could contain the same sector, 
				// but... that would be a real hassle.
				boolean contains = false;
				for (int rx=r.firstHSector; rx<r.lastHSector; rx++)
				{
					for (int ry=r.firstVSector; ry<r.lastVSector; ry++)
					{
						if (getRegion(rx*16, ry*16) != null)
						{
							contains = true;
							break;
						}
					}
				}
				nullWarp[i] = false;
				if (!contains && r.isValid() && sectorDestinations[i] < numSectors)
				{
					r.tileSet = TileSet.getTileSet(r.tileSetId);
					regions.add(r);
				}
				else if (!r.isValid() || sectorDestinations[i] >= numSectors)
				{
					if (nullWarpDataRecord == null)
						nullWarpDataRecord = (MoveableDataRecord)warpRecords[i];
					nullWarp[i] = true;
				}
			}
		}
		Collections.sort(regions);

		// I see a potential pitfall about all this:
		// Suppose multiple levels use the same warpdata for one warp. The only one they share is the nullWarp.
		// If I make nullWarpDataRecord one that is not the shared warp, the nullwarp used by other levels may be freed,
		// and classified as free space!
		// Note, this could only happen if not all levels were loaded. Otherwise the other nullwarp pointers would be loaded.
		if (nullWarpDataRecord == null)
		{
		//	System.out.println("Madenull");
			nullWarpDataRecord = rom.getMoveableDataRecord(rom.findFreeSpace(8, warpDataBank, true), null, false, 8);
			nullWarpDataRecord.deleteWithNoPtr = false;
		}
	}

	RegionRecord(RegionRecord r) {
		rom = RomReader.rom;

		modified = false;
		addr = -1;
		warpDataBank = r.warpDataBank;

		byte[] data = r.tableRecord.toArray();
		tableRecord = rom.getMoveableDataRecord(data, null, warpDataBank, false);
		// Generate regions
		regions = new ArrayList<Region>();
		warpRecords = new MoveableDataRecord[0xa*0x3];
		sectorDestinations = new int[0xa*0x3];
		nullWarp = new boolean[0xa*0x3];

		for (int i=0; i<r.regions.size(); i++) {
			regions.add(new Region(r.regions.get(i)));
		}
		for (int i=0; i<0xa*0x3; i++) {
			sectorDestinations[i] = r.sectorDestinations[i];
			nullWarp[i] = r.nullWarp[i];

			data = r.warpRecords[i].toArray();
			warpRecords[i] = rom.getMoveableDataRecord(data, new RomPointer(tableRecord, i*2), warpDataBank, false);
		}
		nullWarpDataRecord = r.nullWarpDataRecord;
	}

	public Region getRegion(int x, int y) {
		// The region returned may well be changed.
		for (int i=0; i<regions.size(); i++)
		{
			Region r = regions.get(i);
			int startX = r.firstHSector*16;
			int endX = r.lastHSector*16;
			int startY = r.firstVSector*16;
			int endY = r.lastVSector*16;
			
			if (x >= startX && x < endX && y >= startY && y < endY)
				return r;
		}
		return null;
	}

	public void save() {
		// The "modified" variable isn't checked for. Member fields can be directly accessed making it difficult to update it.
		// It's not very important anyway, it won't (shouldn't) allocate new memory if nothing changed.

		for (int x=0; x<0xa; x++)
		{
			for (int y=0; y<3; y++)
			{
				int i = y*0xa + x;
				RomPointer warpDataPointer = new RomPointer(tableRecord, i*2);
				
				int b0 = sectorDestinations[i]%0xa;
				b0 |= (sectorDestinations[i]/0xa)<<4;
				if (sectorDestinations[i] >= numSectors)
					b0 = 0xff;
				
				int nextX = b0&0xf;
				int nextY = b0>>4;
				Region r = getRegion(nextX*16, nextY*16);
				
				
				if (r == null)
				{
					if (nullWarp[i] == false)
					{
						nullWarp[i] = true;
						warpRecords[i].removePtr(warpDataPointer);

						warpRecords[i] = nullWarpDataRecord;
						warpRecords[i].addPtr(warpDataPointer);
					}
				}
				else
				{
					if (nullWarp[i] == true)
					{
						nullWarp[i] = false;
						warpRecords[i].removePtr(warpDataPointer);

						warpRecords[i] = rom.getMoveableDataRecord(rom.findFreeSpace(8, warpDataBank, true),
								warpDataPointer, false, 8);
					}
					warpRecords[i].write(0, (byte)b0);
					byte[] bytes = r.toArray();
					for (int j=1; j<8; j++)
						warpRecords[i].write(j, bytes[j]);
				}
	//			warpRecords[i].save();
			}
		}

		if (addr < 0) {
			addr = rom.findFreeSpace(0xa*0x3*2, warpDataBank, true);
		}
		tableRecord.moveAddr(addr);

		savePtrs();
		
		if (ptrs.size() == 0) {
			regionRecords.remove(this);
			// Okay, so the tableRecord never actually HAS pointers... instead, its parent (this) has them.
			// This is when it is told to be deleted.
			// The warpRecords, which depend on this tableRecord, should be deleted as well.
			tableRecord.deleteWithNoPtr = true;
			return;
		}

		modified = false;
		ptrsOutOfDate = false;
	}
}
