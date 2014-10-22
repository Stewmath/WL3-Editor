package record;

import java.io.IOException;
import java.util.*;

public abstract class Record {
	int addr;
	// originalAddr should ONLY be used as a reference...
	// don't use it for checking free space at its original position or stuff like that
	int originalAddr;

	// Note: even if modified is false, remember to check the pointers!
	public boolean modified = false;

	// List of pointers pointing to this data.
	ArrayList<RomPointer> ptrs = new ArrayList<RomPointer>();

	String description = "";

	public abstract void save();

	public int getAddr() {
		return addr;
	}
	public void addPtr(RomPointer ptr) {
		if (ptr == null)
			return;
		for (int i=0; i<ptrs.size(); i++)
		{
			if (ptrs.get(i).equals(ptr))
				return;
		}
		ptrs.add(ptr);
	}
	public void removePtr(RomPointer ptr) {
		for (int i=0; i<ptrs.size(); i++)
		{
			if (ptrs.get(i).equals(ptr))
			{
				ptrs.remove(i--);
				//System.out.println("Pointer removed.");
				// Might as well return here, there shouldn't be pointer duplicates.
				// But lets be safe.
			}
		}
	}
	public void trimPtrs() {
		for (int i=0; i<ptrs.size(); i++) {
			if (ptrs.get(i).isNull()) {
				ptrs.remove(i--);
			}
		}
	}
	public void savePtrs() {
		// Write to all pointers.
		for (int i=0; i<ptrs.size(); i++)
		{
			if (ptrs.get(i).isNull())
				ptrs.remove(i--);
			else {
				RomPointer ptr = ptrs.get(i);
				// If the pointer isn't pointing to the correct address, it must be saved.
				boolean save = false;
				if (ptr.getType() == RomPointer.TYPE_METADATA) {
					// It's unnecessary to save to metadata unless the address changes.
					save = originalAddr != addr;
				}
				else {
					save = true;
					if (!(ptr.getPointedAddr() == addr ||
								(ptr.hasBankAddr() == false && ptr.getPointedAddr()%0x4000 == addr%0x4000))) {
						save = true;
					}
				}
				if (save) {
					ptr.write(addr, addr/0x4000);
					// Some pointers point to other records.
					// All records are saved when the save button is clicked.
					// But if a record that depends on this one is saved first?
					// That happens sometimes, if it does, it's re-saved right here.
					ptr.save();
				}
			}
		}
	}

	public int getNumPtrs() {
		return ptrs.size();
	}

	public void setDescription(String desc) {
		description = desc;
	}
	public String getDescription() {
		return description;
	}
}

