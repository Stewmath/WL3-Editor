package record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.*;

import java.util.logging.Logger;
import base.Region;
import base.TileSet;

public class RegionRecord extends Record
{
	final static Logger log = Logger.getLogger(RegionRecord.class.getName());

	public final static int NUM_SECTORS = 0x1e;

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
		RegionRecord r = new RegionRecord(_tblAddr, ptr);
		regionRecords.add(r);
		return r;
	}
	// This function makes a copy of a regionRecord.
	public static RegionRecord getCopy(RegionRecord copy, RomPointer ptr, int bank) {
		byte[] warpData = copy.getRawWarpData();
		return getNew(warpData, ptr, bank);
	}
	// This function makes a new regionRecord
	public static RegionRecord getNew(byte[] warpData, RomPointer ptr, int bank) {
		RegionRecord r = new RegionRecord(warpData, ptr, bank);
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

	ArrayList<Region> regions = new ArrayList<Region>();

	// Used for checking if the regions have bee changed
	ArrayList<Region> originalRegions = new ArrayList<Region>();

	// tableRecord - the table containing pointers to each warpRecord.
	MoveableDataRecord tableRecord;
	// warpRecords - 0xa*0x3 entries, one for each warp.
	// If null, 0xffff is written to the table record.
	MoveableDataRecord[] warpRecords = new MoveableDataRecord[NUM_SECTORS];
	// sectorDestinations - warp destination for each sector.
	int[] sectorDestinations = new int[NUM_SECTORS];

	RegionRecord(int _tblAddr, RomPointer ptr) {
		rom = RomReader.rom;

		modified = false;
		addr = _tblAddr;
		warpDataBank = addr/0x4000;
		// The pointer to tableRecord would be the same as the pointer to this record.
		// See the save() function.
		tableRecord = rom.getMoveableDataRecord(addr, ptr, false, 0xa*3*2);
		tableRecord.deleteWithNoPtr = true;
		tableRecord.setDescription("Warp table");
		// Generate regions
		warpRecords = new MoveableDataRecord[0xa*0x3];
		sectorDestinations = new int[0xa*0x3];
		for (int x=0; x<0xa; x++)
		{
			for (int y=0; y<0x3; y++)
			{
				int i = x+y*0xa;
				
				int warpDataAddr = tableRecord.read16(i*2);
				if (warpDataAddr == 0xffff) {
					sectorDestinations[i] = 0xff;
					warpRecords[i] = null;
				}
				else {
					// parseWarpData will give warpRecords[i] its pointer and other settings
					warpRecords[i] = rom.getMoveableDataRecord(RomReader.BANK(warpDataAddr, warpDataBank),
							null, false, 8);
					parseWarpData(i);
				}
			}
		}
		Collections.sort(regions);

		for (Region r : regions) {
			originalRegions.add(new Region(r));
		}
	}

	RegionRecord(byte[] warpData, RomPointer ptr, int bank) {
		rom = RomReader.rom;

		addr = -1;
		modified = true;
		warpDataBank = bank;

		tableRecord = rom.getMoveableDataRecord(new byte[NUM_SECTORS*2], ptr, warpDataBank, false);
		tableRecord.deleteWithNoPtr = true;
		tableRecord.setDescription("Warp Table");
		for (int i=0; i<NUM_SECTORS; i++) {
			if (warpData[i*8] != 0xff) {
				warpRecords[i] = rom.getMoveableDataRecord(
						Arrays.copyOfRange(warpData, i*8, i*8+8), null, warpDataBank, false);
				parseWarpData(i);
			}
			else
				sectorDestinations[i] = 0xff;
		}

		Collections.sort(regions);

		// There's no real point to this, the record is automatically marked as modified
		for (Region reg : regions) {
			originalRegions.add(new Region(reg));
		}
	}

	void parseWarpData(int i) {
		RomPointer warpDataPointer = new RomPointer(tableRecord, i*2);
		warpRecords[i].addPtr(warpDataPointer);
		warpRecords[i].deleteWithNoPtr = true;
		warpRecords[i].setDescription("Warp data");

		Region r = new Region(warpRecords[i].toArray());
		int b1 = warpRecords[i].read(0)&0xff;
		sectorDestinations[i] = (b1>>4)*0xa + (b1&0xf);

		if (!r.isValid() || sectorDestinations[i] >= NUM_SECTORS) {
			sectorDestinations[i] = 0xff;
			warpRecords[i].removePtr(warpDataPointer);
			warpRecords[i] = null;
			modified = true;
		}
		else {
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
					Region r2;
					if ((r2 = getRegion(rx*16, ry*16)) != null)
					{
						contains = true;
						if (!r.equals(r2)) {
							// There are a few cases where this will occur on unmodified roms
							// The editor will need to pick one region to use
							log.finer("Region conflict: addr " + RomReader.toHexString(addr, 4) + "\n" +
									"Region 1: " + r2.toString() + "\n" +
									"Region 2: " + r.toString());

							// Fix to prevent beneath the waves from using a screwed up region
							// which they left in for some reason
							if (addr == 0xc316f || addr == 0xc325b || addr == 0xc3347 ||
									addr == 0xc3433 || addr == 0xc351f || addr == 0xc360b) {
								regions.remove(r2);
								contains = false;
								continue;
									}
						}
						break;
					}
				}
			}
			if (!contains) {
				r.tileSet = TileSet.getTileSet(r.tileSetId);
				regions.add(r);
			}
		}
	}

	public int getNumRegions() {
		return regions.size();
	}

	public Region getRegion(int index) {
		return regions.get(index);
	}

	public Region getRegion(int x, int y) {
		// The region returned may be changed by the caller.
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

	public void addRegion(Region r) {
		regions.add(r);
		modified = true;
	}

	public int getSectorDestination(int i) {
		return sectorDestinations[i];
	}
	public void setSectorDestination(int i, int dest) {
		if (sectorDestinations[i] != dest) {
			sectorDestinations[i] = dest;
			modified = true;
		}
	}

	public void save() {
		// Check if the regions have changed
		if (regions.size() != originalRegions.size())
			modified = true;
		else {
			for (int i=0; i<regions.size(); i++) {
				if (!regions.get(i).equals(originalRegions.get(i))) {
					modified = true;
					break;
				}
			}
		}

		if (tableRecord.isNull()) {
			log.info("Deleting region data");
			regionRecords.remove(this);
			return;
		}


		if (modified) {
			for (int i=0; i<NUM_SECTORS; i++) {
				// Clear everything, then write back pointers for the records which still exist
				tableRecord.write16(i*2, 0xffff);
			}

			for (int x=0; x<0xa; x++)
			{
				for (int y=0; y<3; y++)
				{
					int i = y*0xa + x;
					RomPointer warpDataPointer = new RomPointer(tableRecord, i*2);

					int b0 = sectorDestinations[i]%0xa;
					b0 |= (sectorDestinations[i]/0xa)<<4;

					int nextX = b0&0xf;
					int nextY = b0>>4;
					Region r = getRegion(nextX*16, nextY*16);


					if (sectorDestinations[i] >= NUM_SECTORS || r == null || !r.isValid()) {
						// Invalid warp
						sectorDestinations[i] = 0xff;
						if (warpRecords[i] != null) {
							warpRecords[i].removePtr(warpDataPointer);
							warpRecords[i] = null;
						}
					}
					else {
						byte[] data = r.toArray();
						data[0] = (byte)b0;
						if (warpRecords[i] == null) {
							warpRecords[i] = rom.getMoveableDataRecord(data, warpDataPointer, warpDataBank, false);
							warpRecords[i].deleteWithNoPtr = true;
							warpRecords[i].setDescription("Warp data");
						}
						warpRecords[i].setData(data);
					}
				}
			}

			originalRegions = new ArrayList<Region>();
			for (Region r : regions) {
				originalRegions.add(new Region(r));
			}
			modified = false;
		}
	}

	byte[] getWarpData(int sector) {
		int x = sectorDestinations[sector]%0xa;
		int y = sectorDestinations[sector]/0xa;
		Region r = getRegion(x*16, y*16);

		if (r != null && r.isValid()) {
			byte[] data = r.toArray();
			data[0] = (byte)(x | y<<4);
			return data;
		}

		byte[] data = new byte[8];
		for (int i=0; i<8; i++)
			data[i] = (byte)0xff;
		return data;
	}

	// Returns a byte array of size NUM_SECTORS*8 containing warp data for each sector.
	// If a sector has no warp data it's all 0xff's.
	public byte[] getRawWarpData() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();

		try {
			for (int i=0; i<NUM_SECTORS; i++) {
				data.write(getWarpData(i));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return data.toByteArray();
	}

	void setWarpRecordProperties(int i) {
	}

	@Override
	public int getAddr() {
		return tableRecord.getAddr();
	}

	@Override
	public void addPtr(RomPointer p) {
		tableRecord.addPtr(p);
	}

	@Override
	public void removePtr(RomPointer p) {
		tableRecord.removePtr(p);
	}

	// Kinda redundant but eh
	// tableRecord.savePtrs() will be called from romreader.java anyway
	@Override
	public void savePtrs() {
		tableRecord.savePtrs();
	}

	@Override
	public int getNumPtrs() {
		return tableRecord.getNumPtrs();
	}
}
