package record;

// The only reason this class exists is to ensure that tiledata and objectdata are
// saved in the same bank...
public class JoinedRecord
{
	RomReader rom;
	MoveableDataRecord r1,r2;
	JoinedRecord(MoveableDataRecord r1, MoveableDataRecord r2)
	{
		rom = RomReader.rom;
		this.r1 = r1;
		this.r2 = r2;
		r1.belongsToJoinedRecord = true;
		r2.belongsToJoinedRecord = true;
	}
	
	public void save()
	{
		if (!(r1.fitsInOriginalSpace() && r2.fitsInOriginalSpace()))
		{
			int dest = rom.findFreeSpace(r1.getSize()+r2.getSize(), false);
			int bank = dest/0x4000;
			r1.setRequiredBank(bank);
			r2.setRequiredBank(bank);
		}
		r1.save();
		r2.save();
	}
}
