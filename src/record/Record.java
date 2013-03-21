package record;

import java.io.IOException;
import java.util.*;

public abstract class Record {
	int addr;
	public boolean modified;
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
		if (!(ptr.getPointedAddr() == addr || (ptr.hasBankAddr() == false && ptr.getPointedAddr()%0x4000 == addr%0x4000)))
			modified = true;
		ptrs.add(ptr);
	}
	public void removePtr(RomPointer ptr) {
		for (int i=0; i<ptrs.size(); i++)
		{
			if (ptrs.get(i).equals(ptr))
			{
				modified = true;
				ptrs.remove(i--);
//				System.out.println("Pointer removed.");
				// Might as well return here, there shouldn't be pointer duplicates.
				// But lets be safe.
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
				ptrs.get(i).write(addr, addr/0x4000);
				// Some pointers point to other records.
				// All records are saved when the save button is clicked.
				// But if a record that depends on this one is saved first?
				// That happens sometimes, if it does, it's re-saved right here.
				ptrs.get(i).save();
			}
		}
	}

	public void setDescription(String desc) {
		description = desc;
	}
	public String getDescription() {
		return description;
	}
}

