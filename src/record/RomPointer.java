package record;

public class RomPointer {
	// TYPE_DIRECT = pointer to data directly in rom.
	final int TYPE_DIRECT=0;
	// TYPE_DIRECT_FRAGMENTED = pointer to data in rom, with bytes of pointer separated
	final int TYPE_DIRECT_FRAGMENTED=2;
	// TYPE_RECORD = pointer to data in a record.
	final int TYPE_RECORD=1;

	int type;

	// -- TYPE_DIRECT --
	// ptrAddr1 = address of the pointer.
	// If it's fragmented, ptrAddr11 points to the high byte, ptrAddr12 to the low byte.
	// Else, ptrAddr11 points to the little-endian address.
	int ptrAddr1;
	int ptrAddr2;
	// bankAddr = address of the corresponding bank. If bankAddr<0, ignore it.
	int bankAddr;

	// -- TYPE_RECORD --
	MoveableDataRecord ptrRecord;
	int ptrIndex;
	MoveableDataRecord bankRecord;
	int bankIndex;

	// Make a TYPE_DIRECT.
	public RomPointer(int ptrAddr1, int bankAddr) {
		type = TYPE_DIRECT;
		this.ptrAddr1 = ptrAddr1;
		this.bankAddr = bankAddr;
	}
	// Make a TYPE_DIRECT. bankAddr is set to -1 to indicate there is no bankAddr.
	public RomPointer(int ptrAddr1) {
		type = TYPE_DIRECT;
		this.ptrAddr1 = ptrAddr1;
		this.bankAddr = -1;
	}
	// Make a TYPE_DIRECT_FRAGMENTED.
	public RomPointer(int ptrAddr1, int ptrAddr2, int bankAddr) {
		type = TYPE_DIRECT_FRAGMENTED;
		this.ptrAddr1 = ptrAddr1;
		this.ptrAddr2 = ptrAddr2;
		this.bankAddr = bankAddr;
	}
	// Make a TYPE_RECORD.
	public RomPointer(MoveableDataRecord ptrRecord, int ptrIndex, MoveableDataRecord bankRecord, int bankIndex) {
		type = TYPE_RECORD;
		this.ptrRecord = ptrRecord;
		this.ptrIndex = ptrIndex;
		this.bankRecord = bankRecord;
		this.bankIndex = bankIndex;
	}
	// Make a TYPE_RECORD with a single record for both ptr and bank.
	public RomPointer(MoveableDataRecord record, int ptrIndex, int bankIndex) {
		type = TYPE_RECORD;
		this.ptrRecord = record;
		this.ptrIndex = ptrIndex;
		this.bankRecord = record;
		this.bankIndex = bankIndex;
	}
	// Make a TYPE_RECORD without a bank.
	public RomPointer(MoveableDataRecord ptrRecord, int ptrIndex) {
		type = TYPE_RECORD;
		this.ptrRecord = ptrRecord;
		this.ptrIndex = ptrIndex;
		this.bankRecord = null;
		this.bankIndex = -1;
	}
 
	public void write(int newAddr, int newBank) {
		int ptr = RomReader.toGbPtr(newAddr);
		switch(type) {
			case TYPE_DIRECT:
				RomReader.rom.writePtr(ptrAddr1, ptr);
				if (bankAddr >= 0)
					RomReader.rom.write(bankAddr, (byte)newBank);
				break;
			case TYPE_DIRECT_FRAGMENTED:
				RomReader.rom.write(ptrAddr1, (byte)((ptr>>8)&0xff));
				RomReader.rom.write(ptrAddr2, (byte)(ptr&0xff));
				if (bankAddr >= 0)
					RomReader.rom.write(bankAddr, (byte)newBank);
				break;
			case TYPE_RECORD:
				ptrRecord.writePtr(ptrIndex, ptr);
				if (bankRecord != null)
					bankRecord.write(bankIndex, (byte)newBank);
				break;
		}
	}

	public int getPointedAddr() {
		switch(type) {
			case TYPE_DIRECT:
				if (bankAddr >= 0)
					return RomReader.rom.read16(ptrAddr1, RomReader.rom.read(bankAddr));
				else
					return RomReader.toGbPtr(RomReader.rom.read16(ptrAddr1));

			case TYPE_DIRECT_FRAGMENTED:
				if (bankAddr >= 0)
					return RomReader.BANK(RomReader.rom.read(ptrAddr2)|(RomReader.rom.read(ptrAddr1)<<8), RomReader.rom.read(bankAddr));
				else
					return RomReader.rom.read(ptrAddr2)|(RomReader.rom.read(ptrAddr1)<<8);
			case TYPE_RECORD:
				if (bankRecord != null)
					return ptrRecord.read16(ptrIndex, bankRecord.read(bankIndex));
				else
					return RomReader.toGbPtr(ptrRecord.read16(ptrIndex));
		}
		// It'll never reach here.
		return -1;
	}

	public boolean hasBankAddr() {
		if (type == TYPE_RECORD) {
			if (bankRecord == null)
				return false;
			return true;
		}
		else {
			if (bankAddr < 0)
				return false;
			return true;
		}
	}

	public void save() {
		/*
		if (type == TYPE_RECORD) {
			try {
				ptrRecord.save();
			} catch(java.lang.Error e) {
				//System.out.println(ptrRecord.getAddr());
			}
			if (bankRecord != null) {
				bankRecord.save();
			}
		}
		*/
	}

	/*
	public int getPtrAddr() {
		if (type == TYPE_DIRECT)
			return ptrAddr1;
		else //if (type == TYPE_RECORD)
			return ptrRecord.getAddr()+ptrR
	}
	public int getBankAddr() {
		if (type == TYPE_DIRECT)
			return bankAddr;
		else {
			if (bankRecord == null)
				return -1;
			return bankRecord.read(bankIndex);
		}
	}
	*/

	// This function checks if this pointer is invalid.
	public boolean isNull() {
		if (type == TYPE_DIRECT || type == TYPE_DIRECT_FRAGMENTED)
			return false;
		if (ptrRecord.isNull() || (bankRecord != null && bankRecord.isNull()))
			return true;
		return false;
	}

	public boolean equals(Object o) {
		if (o.getClass() != this.getClass())
			return false;
		RomPointer p = (RomPointer)o;
		if (type != p.type)
			return false;
		if (type == TYPE_DIRECT)
			return p.ptrAddr1 == ptrAddr1;
		else if (type == TYPE_DIRECT_FRAGMENTED)
            return p.ptrAddr1 == ptrAddr1 && p.ptrAddr2 == ptrAddr2;
        else
			return p.ptrRecord == ptrRecord && p.ptrIndex == ptrIndex;
	}
	
}
