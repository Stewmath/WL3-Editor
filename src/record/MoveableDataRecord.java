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

	// isMoveable: if false, we won't attempt to move this record around when it's time to save.
	// However, even if false, invoking "moveAddr" will force it to move to the specified address.
	public boolean isMoveable=true;

	RomReader rom;
	int originalAddr=-1;
	
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
		originalAddr = addr;
		this.ptrs = new ArrayList<RomPointer>(pointers);
		if (compressed)
			this.type = RECORD_COMPRESSED;
		else
			this.type = RECORD_NORMAL;
		
		deleteWithNoPtr = false;
		
		// if compressed==true, parameter 'size' is ignored.
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
		originalAddr = -1;
		originalSize = -1;
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
		}
		else
		{
			// Hold data in decompressedData, even though there's no compression.
			decompressedData = new ArrayList<Byte>();
			for (int i=0; i<data.length; i++)
				decompressedData.add(data[i]);
			compressedData = null;
		}
	}

	// Use sparingly... currently this overrides free space checks.
	public void moveAddr(int newAddr)
	{
		addr = newAddr;
		modified = true;
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
		// modified is not set here... hmmm...
		// I do kinda prefer it this way, since certain sprite graphics are resized, and
		// I'd rather not rewrite them unless they're edited...
		while (decompressedData.size() < size)
			decompressedData.add((byte)0);
		while (decompressedData.size() > size)
			decompressedData.remove(size);
	}
	public boolean fitsInOriginalSpace()
	{
		if (originalAddr < 0)
			return false;

		int originalSlotSize = originalSize+rom.getFreeSpaceLength(originalAddr+originalSize);
		if (type != RECORD_COMPRESSED) {
			return decompressedData.size() <= originalSlotSize;
		}
		else
		{
			compressedData = rom.convertToRLE(decompressedData);
			return compressedData.size() <= originalSlotSize;
		}
	}
	// Read u8
	public int read(int i)
	{
		if (i >= decompressedData.size())
			return -1;
		return decompressedData.get(i)&0xff;
	}
	// Read u16
	public int read16(int i)
	{
		if (i >= decompressedData.size())
			return -1;
		else if (i == decompressedData.size()-1)
			return read(i);
		return (decompressedData.get(i)&0xff)+((decompressedData.get(i+1)&0xff)<<8);
	}
	// Read u16 and convert it to a rom address for the given bank
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
		if (i < decompressedData.size()) {
			modified = true;
			decompressedData.set(i, val);
		}
	}
	// No affiliation with member "ptrs", this simply writes
	// a 16-bit gameboy style pointer to index i.
	public void writePtr(int i, int val)
	{
		int newVal = (val%0x4000)+0x4000;
		
		if (i+1 < decompressedData.size())
		{
			modified = true;
			write(i, (byte)(newVal&0xff));
			write(i+1, (byte)(newVal>>8));
		}
	}
	public void write16(int i, int val) {
		if (i+1 < decompressedData.size()) {
			modified = true;
			write(i, (byte)(val&0xff));
			write(i+1, (byte)(val>>8));
		}
	}
	public void write(int i, byte[] data) {
		if (data.length != 0 && i < getDataSize())
			modified = true;
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
		if (!modified) {
			savePtrs();
			return;
		}

		if (type == RECORD_COMPRESSED)
			compressedData = rom.convertToRLE(decompressedData);
		
		// Clear location of original data
		if (originalAddr >= 0) {
			rom.clear(originalAddr, originalSize);
		}

		// Condition for moving data
		// If addr != originalAddr (someone called moveAddr), we assume it's okay to write there?
		if (addr < 0 || (addr == originalAddr && !fitsInOriginalSpace()) ||
				(addr/0x4000 != requiredBank && requiredBank >= 0)) {
			if (!isMoveable) {
				JOptionPane.showMessageDialog(null,
						"Un-moveable data was changed in size and is too big." +
						"Data was originally stored at 0x" + RomReader.toHexString(originalAddr) + ".\n" +
						(description != "" ? "Data description: \"" + description + "\"\n" : "Data has no description.\n") +
						"\nThe rom will not be saved. Try decreasing the data size and try again.",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				rom.saveFail = true;
				return;
			}
			// Find a new spot for the data
			if (requiredBank >= 0)
				addr = rom.findFreeSpace(getSize(), requiredBank, true);
			else
				addr = rom.findFreeSpace(getSize(), true);
			if (addr < 0) {
				JOptionPane.showMessageDialog(null,
						"There was an error allocating space in the rom for data.\n" +
						"Data was originally stored at 0x" + RomReader.toHexString(originalAddr) + ".\n" +
						(description != "" ? "Data description: \"" + description + "\"\n" : "Data has no description.\n") +
						"\nThe rom will not be saved.",
						"Error",
						JOptionPane.ERROR_MESSAGE);
				rom.saveFail = true;
				return;
			}

			String moveInfoString;
			if (originalAddr == -1)
				moveInfoString = "New data will be inserted to address 0x" + RomReader.toHexString(addr) + ".\n";
			else
				moveInfoString = "Data will be moved from 0x" + RomReader.toHexString(originalAddr) + " to 0x" + RomReader.toHexString(addr) + ".\n";
			if (description == null || description == "")
				moveInfoString += "Data has no description.\n";
			else
				moveInfoString += "Data description: \"" + description + "\"\n";
			JOptionPane.showMessageDialog(null,
					moveInfoString,
					"Warning",
					JOptionPane.WARNING_MESSAGE);
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
		originalAddr = addr;
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
