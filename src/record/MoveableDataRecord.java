package record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.image.*;
import java.awt.Color;

import javax.swing.JOptionPane;

public class MoveableDataRecord extends Record
{
	final static int RECORD_NORMAL = 0;
	final static int RECORD_COMPRESSED = 1;
	final static int RECORD_TBL = 2;

	// If set, this will delete the data when all its pointers (references) are removed.
	// Sometimes there are no pointers, or all pointers aren't known, so it's false by default.
	public boolean deleteWithNoPtr=false;
	// If this record belongs to a JoinedRecord, it should be accessed through that joined record.
	// At least, for the purposes of saving.
	public boolean belongsToJoinedRecord=false;

	public boolean isMoveable=true;

	RomReader rom;
	int firstAddr=-1;
	boolean deleteFirstAddr=false;
	
	int requiredBank=-1;
	int originalSize;
	int type;
	// At any given time, compressedData is not guaranteed to be accurate.
	// It's really used as a temporary buffer.
	ArrayList<Byte> compressedData;

	ArrayList<Byte> decompressedData;
	
	MoveableDataRecord(int addr, ArrayList<RomPointer> pointers, boolean compressed, int size) {
		modified = false;
		rom = RomReader.rom;
		this.addr = addr;
		firstAddr = addr;
		this.ptrs = new ArrayList<RomPointer>(pointers);
		if (compressed)
			this.type = RECORD_COMPRESSED;
		else
			this.type = RECORD_NORMAL;
		
		deleteWithNoPtr = false;
		
		// if compressed==true, parameter size is ignored.
		if (compressed)
		{
			decompressedData = rom.readRLE(addr);
			compressedData = rom.readRawRLE(addr);
			originalSize = compressedData.size();
		}
		else
		{
			// Hold data in decompressedData, even though there's no compression.
			decompressedData = new ArrayList<Byte>();
			for (int i=0; i<size; i++)
				decompressedData.add((Byte)(byte)rom.read(addr+i));
			compressedData = null;
			originalSize = decompressedData.size();
		}

	}
	MoveableDataRecord(byte[] data, ArrayList<RomPointer> pointers, int b, boolean compressed) {
		// This is a newly created record - previously non-existent in the rom - so it must be saved.
		modified = true;

		rom = RomReader.rom;
		addr = -1;
		firstAddr = addr;
		ptrs = new ArrayList<RomPointer>(pointers);
		requiredBank = b;
		if (compressed)
			this.type = RECORD_COMPRESSED;
		else
			this.type = RECORD_NORMAL;
		
		deleteWithNoPtr = false;
		
		if (compressed)
		{
			decompressedData = new ArrayList<Byte>();
			for (int i=0; i<data.length; i++)
				decompressedData.add(data[i]);
			compressedData = rom.convertToRLE(decompressedData);
			originalSize = compressedData.size();
		}
		else
		{
			// Hold data in decompressedData, even though there's no compression.
			decompressedData = new ArrayList<Byte>();
			for (int i=0; i<data.length; i++)
				decompressedData.add(data[i]);
			compressedData = null;
			originalSize = decompressedData.size();
		}
	}

	public void moveAddr(int newAddr)
	{
		if (firstAddr == addr)
		{
			deleteFirstAddr = true;
		}
		addr = newAddr;
		modified = true;
	}
	public void reload()
	{
		if (addr < 0) {
			return;
		}
		modified = true;
		if (type == RECORD_COMPRESSED)
		{
			decompressedData = rom.readRLE(addr);
			compressedData = rom.readRawRLE(addr);
			originalSize = compressedData.size();
		}
		else
		{
			int size = decompressedData.size();
			decompressedData = new ArrayList<Byte>();
			for (int i=0; i<size; i++)
			{
				if (addr > 0)
					decompressedData.add((byte)rom.read(addr+i));
				else if (firstAddr > 0)
					decompressedData.add((byte)rom.read(firstAddr+i));
			}
			originalSize = decompressedData.size();
		}
	}
	public int getSize() {
		if (type == RECORD_COMPRESSED) {
			compressedData = rom.convertToRLE(decompressedData);
			return compressedData.size();
		}
		else
			return decompressedData.size();
	}
	public int getOriginalSize() {
		return originalSize;
	}
	public int getDataSize() {
		return decompressedData.size();
	}
	public void setDataSize(int size) {
		while (decompressedData.size() < size)
			decompressedData.add((byte)0);
		while (decompressedData.size() > size)
			decompressedData.remove(size);
	}
	public boolean fitsInOriginalSpace()
	{
		if (firstAddr < 0)
			return false;

		int originalSlotSize = originalSize+rom.getFreeSpaceLength(firstAddr+originalSize);
		if (type != RECORD_COMPRESSED) {
			return decompressedData.size() <= originalSlotSize;
		}
		else
		{
			compressedData = rom.convertToRLE(decompressedData);
			return compressedData.size() <= originalSlotSize;
		}
	}
	public int read(int i)
	{
		if (i >= decompressedData.size())
			return -1;
		return decompressedData.get(i)&0xff;
	}
	public int read16(int i)
	{
		if (i >= decompressedData.size())
			return -1;
		else if (i == decompressedData.size()-1)
			return read(i);
		return (decompressedData.get(i)&0xff)+((decompressedData.get(i+1)&0xff)<<8);
	}
	public int read16(int i, int bank) {
		int val = read16(i);
		if (val < 0)
			return val;
		return RomReader.BANK(val, bank);
	}
	// Treat data as a chunk of gfx data and return an image.
	public BufferedImage readTile(int t, int[] palette) {
		BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_USHORT_555_RGB);

		int start = t*16;
		for (int y=0; y<8; y++) {
			for (int x=0; x<8; x++) {
				int c = (read(start+y*2)>>(7-x))&1;
				c |= ((read(start+y*2+1)>>(7-x))&1)<<1;
				image.setRGB(x, y, palette[c]);
			}
		}
		return image;
	}
	public byte[] toArray(int start, int end) {
		byte[] retData = new byte[end-start];
		for (int i=start; i<end; i++)
			retData[i-start] = decompressedData.get(i);
		return retData;
	}
	public byte[] toArray(int start) {
		return toArray(start, decompressedData.size());
	}
	public byte[] toArray()
	{
		return toArray(0, decompressedData.size());
	}
	public ArrayList<Byte> toArrayList()
	{
		return new ArrayList<Byte>(decompressedData);
	}
	public void write(int i, byte val)
	{
		modified = true;
		if (i < decompressedData.size())
			decompressedData.set(i, val);
	}
	// No affiliation with member "ptrs", this simply writes
	// a 16-bit gameboy style pointer to index i.
	public void writePtr(int i, int val)
	{
		modified = true;
		int newVal = (val%0x4000)+0x4000;
		
		if (i+1 < decompressedData.size())
		{
			write(i, (byte)(newVal&0xff));
			write(i+1, (byte)(newVal>>8));
		}
	}
	public void write16(int i, int val) {
		modified = true;
		if (i+1 < decompressedData.size()) {
			write(i, (byte)(val&0xff));
			write(i+1, (byte)(val>>8));
		}
	}
	public void write(int i, byte[] data) {
		for (int j=0; j<data.length && j+i < getDataSize(); j++) {
			decompressedData.set(j+i, data[i]);
		}
	}
	public void write(byte[] data) {
		setData(data);
	}
	public void setData(byte[] data) {
		modified = true;

		decompressedData = new ArrayList<Byte>();
		for (int i=0; i<data.length; i++) {
			decompressedData.add(data[i]);
		}
	}
	public void setData(ArrayList<Byte> data) {
		modified = true;
		decompressedData = new ArrayList<Byte>(data);
	}

	// Sometimes, moveable data is required to be in a specific bank.
	public void setRequiredBank(int b)
	{
		if (!(addr >= 0 && addr/0x4000 == b))
			modified = true;
		requiredBank = b;
	}

	public void save() {
		if (!modified)
			return;

		if (type == RECORD_COMPRESSED)
			compressedData = rom.convertToRLE(decompressedData);
		
		if (addr < 0 || !fitsInOriginalSpace() || (addr/0x4000 != requiredBank && requiredBank >= 0)) {
			if (!isMoveable) {
				JOptionPane.showMessageDialog(null,
						"Un-moveable data was changed in size and is too big." +
						"Data was originally stored at 0x" + RomReader.toHexString(firstAddr) + ".\n" +
						(description != "" ? "Data description: \"" + description + "\"\n" : "Data has no description.\n") +
						"\nThe rom will not be saved. Try decreasing the data size and try again.",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				rom.saveFail = true;
				return;
			}
			if (requiredBank >= 0)
				addr = rom.findFreeSpace(getSize(), requiredBank, true);
			else
				addr = rom.findFreeSpace(getSize(), true);
			if (addr < 0) {
				JOptionPane.showMessageDialog(null,
						"There was an error allocating space in the rom for data.\n" +
						"Data was originally stored at 0x" + RomReader.toHexString(firstAddr) + ".\n" +
						(description != "" ? "Data description: \"" + description + "\"\n" : "Data has no description.\n") +
						"\nThe rom will not be saved.",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				rom.saveFail = true;
				return;
			}
			deleteFirstAddr = true;

			String moveInfoString;
			if (firstAddr == -1)
				moveInfoString = "New data will be inserted to address 0x" + RomReader.toHexString(addr) + ".\n";
			else
				moveInfoString = "Data will be moved from 0x" + RomReader.toHexString(firstAddr) + " to 0x" + RomReader.toHexString(addr) + ".\n";
			if (description == null || description == "")
				moveInfoString += "Data has no description.\n";
			else
				moveInfoString += "Data description: \"" + description + "\"\n";
			JOptionPane.showMessageDialog(null,
					moveInfoString,
					"Warning",
					JOptionPane.WARNING_MESSAGE);
		}
		
		// If data has been moved, its previous location should probably be deleted.
		if (firstAddr != addr && firstAddr > 0 && deleteFirstAddr) {
			rom.clear(firstAddr, originalSize);
		}
		
		savePtrs();
		
		if (isNull()) {
			addr = -1;
		}
		else {
			if (type == RECORD_COMPRESSED)
				rom.write(addr, compressedData);
			else
				rom.write(addr, decompressedData);
		}
		firstAddr = addr;
		deleteFirstAddr = false;
		originalSize = getSize();
		modified = false;
	}

	// This function returns true if this record can and should be deleted.
	public boolean isNull()
	{
		if (ptrs.size() == 0 && deleteWithNoPtr)
			return true;
		return false;
	}
}
