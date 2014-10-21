package base;

import graphics.Drawing;

import java.awt.image.*;
import java.awt.Color;
import java.util.*;

import record.*;



public class TileSet {

	public static RomReader rom;
	static TileSet[] loadedTileSets = new TileSet[256];
	
	public static TileSet getTileSet(int id)
	{
		if (loadedTileSets[id] == null)
			loadedTileSets[id] = new TileSet(id);
		return loadedTileSets[id];
	}

	// It's not necessary to have all tilesets loaded.
	// The only thing that is compressed is the flags, and all pointers to the flags are
	// in the flag table. So they can be moved around safely.
	public static void reloadTileSets() {
		for (int i=0; i<loadedTileSets.length; i++) {
			loadedTileSets[i] = null;
		}
	}
	
	final static int tileSetDataTbl = RomReader.BANK(0x44c5, 0x30);
	
	final static int effectTbl =		RomReader.BANK(0x4000, 0x32);
	final static int metaTileTbl = 		RomReader.BANK(0x490d, 0x30);
	final static int flagTbl = 			RomReader.BANK(0x49d1, 0x30);
	final static int gfxData0Tbl = 		RomReader.BANK(0x4a95, 0x30);
	final static int gfxData1Tbl =		RomReader.BANK(0x4af7, 0x30);
	final static int paletteDataTbl =	RomReader.BANK(0x4b1b, 0x30);

	// I set pointers for all of these records, but it's kind of pointless except for flagRecord
	public static MoveableDataRecord getMetaTileRecord(int metaTileIndex) {
		RomPointer metaTilePointer = new RomPointer(metaTileTbl+2*metaTileIndex);
		int metaTileAddr = RomReader.BANK(metaTilePointer.getPointedAddr(), 0x38+metaTileIndex/6);
		return rom.getMoveableDataRecord(metaTileAddr, metaTilePointer,
				false, 128*4, 0x38+metaTileIndex/6);
	}
	public static MoveableDataRecord getEffectRecord(int metaTileIndex) {
		// effectRecord uses metaTileIndex, which is why there is no effectIndex.
		int effectBank;
		if (metaTileIndex >= 0x3f)
			effectBank = 0x50;
		else
			effectBank = 0x32;

		RomPointer effectPointer = new RomPointer(effectTbl+2*metaTileIndex);
		int effectAddr = RomReader.BANK(effectPointer.getPointedAddr(), effectBank);
		return rom.getMoveableDataRecord(effectAddr, effectPointer, false, 0x80*2, effectBank);
	}
	public static MoveableDataRecord getFlagRecord(int flagIndex) {
		RomPointer flagPointer = new RomPointer(flagTbl+2*flagIndex);
		int flagAddr = RomReader.BANK(flagPointer.getPointedAddr(), 0x38+(flagIndex/6));
		MoveableDataRecord flagRecord = rom.getMoveableDataRecord(flagAddr, flagPointer,
				true, 0, 0x38+flagIndex/6);
		flagRecord.setRequiredBank(0x38+flagIndex/6); // Pretty redundant =P
		// The game actually uses bank 0x38+metaTileIndex/6.
		// I think metaTileIndex and flagIndex should always be the same.

		flagRecord.setDescription("Tileset flags " + RomReader.toHexString(flagIndex, 2));
		return flagRecord;
	}
	public static MoveableDataRecord getGfxData0Record(int gfxData0Index) {
		RomPointer gfxData0Pointer = new RomPointer(gfxData0Tbl+2*gfxData0Index);
		int gfxData0Addr = RomReader.BANK(gfxData0Pointer.getPointedAddr(), 0x51+(gfxData0Index/8));
		return rom.getMoveableDataRecord(gfxData0Addr, gfxData0Pointer,
				false, 0x800, 0x51+gfxData0Index/8);
	}
	public static MoveableDataRecord getGfxData1Record(int gfxData1Index) {
		RomPointer gfxData1Pointer = new RomPointer(gfxData1Tbl+2*gfxData1Index);
		int gfxData1Addr = RomReader.BANK(gfxData1Pointer.getPointedAddr(), 0x4e+(gfxData1Index/8));
		return rom.getMoveableDataRecord(gfxData1Addr, gfxData1Pointer,
				false, 0x800, 0x4e+gfxData1Index/8);
	}
	public static MoveableDataRecord getPaletteDataRecord(int paletteDataIndex) {
		RomPointer paletteDataPointer = new RomPointer(paletteDataTbl+2*paletteDataIndex);
		int paletteDataAddr = RomReader.BANK(paletteDataPointer.getPointedAddr(), 0x33);
		return rom.getMoveableDataRecord(paletteDataAddr, paletteDataPointer,
				false, 2*4*8, 0x33);
	}


	
	// It looks like metaTileIndex and flagIndex should always be the same.
	// I don't think the game can really handle them independently.
	private int tileSetDataIndex;
	private int metaTileIndex;
	private int flagIndex;
	private int gfxData0Index;
	private int gfxData1Index;
	private int paletteDataIndex;

	// The only one of these records that may be moved around is flagRecord, since it's compressed.
	MoveableDataRecord tileSetDataRecord;
	MoveableDataRecord metaTileRecord, flagRecord, gfxData0Record, gfxData1Record, paletteDataRecord, effectRecord;

	BufferedImage[] tileImages = new BufferedImage[128];
	int[][] paletteColors = new int[8][4];
	
	public TileSet(int setId)
	{
		int tileSetDataAddr;
	
		tileSetDataIndex = setId;
		tileSetDataAddr = rom.read16FromTable(tileSetDataTbl, tileSetDataIndex, 0x30);
		tileSetDataRecord = rom.getMoveableDataRecord(tileSetDataAddr, null, false, 5);
		
		setMetaTileIndex(tileSetDataRecord.read(0));
		setFlagIndex(tileSetDataRecord.read(1));
		setGfxData0Index(tileSetDataRecord.read(2));
		setGfxData1Index(tileSetDataRecord.read(3));
		setPaletteDataIndex(tileSetDataRecord.read(4));

		
		
		generateTiles();
	}

	public int getId() {
		return tileSetDataIndex;
	}


	public void setMetaTileIndex(int val) {
		metaTileIndex = val;

		metaTileRecord = getMetaTileRecord(metaTileIndex);
		effectRecord = getEffectRecord(metaTileIndex);

		tileSetDataRecord.write(0, (byte)metaTileIndex);
	}
	public void setFlagIndex(int val) {
		flagIndex = val;

		flagRecord = getFlagRecord(flagIndex);

		tileSetDataRecord.write(1, (byte)flagIndex);
	}
	public void setGfxData0Index(int val) {
		gfxData0Index = val;

		gfxData0Record = getGfxData0Record(gfxData0Index);

		tileSetDataRecord.write(2, (byte)gfxData0Index);
	}
	public void setGfxData1Index(int val) {
		gfxData1Index = val;

		gfxData1Record = getGfxData1Record(gfxData1Index);

		tileSetDataRecord.write(3, (byte)gfxData1Index);
	}
	public void setPaletteDataIndex(int val) {
		paletteDataIndex = val;

		paletteDataRecord = getPaletteDataRecord(paletteDataIndex);

		tileSetDataRecord.write(4, (byte)paletteDataIndex);
	}


	public int[] getPalette(int p) {
		int[] colors = new int[4];

		for (int i=0; i<4; i++) {
			colors[i] = paletteColors[p][i];
		}
		return colors;
	}

	public int[][] getPalettes() {
		int[][] colors = new int[8][4];

		for (int j=0; j<8; j++) {
			for (int i=0; i<4; i++) {
				colors[j][i] = paletteColors[j][i];
			}
		}
		return colors;
	}

	public void setPaletteColor(int p, int i, int c) {
		Color color = new Color(c);
		paletteColors[p][i] = c;
		int r = color.getRed()/8;
		int g = color.getGreen()/8;
		int b = color.getBlue()/8;


		int data = r | (g<<5) | (b<<10);
		//	paletteDataRecord.write(p*8+c*2+1, (byte)(data&0xff));
		//	paletteDataRecord.write(p*8+c*2, (byte)(data>>8));
		paletteDataRecord.write16(p*8+i*2, data);
	}

	public void setPalettes(int[][] palettes) {
		for (int i=0; i<8; i++) {
			for (int j=0; j<4; j++) {
				setPaletteColor(i, j, palettes[i][j]);
			}
		}
	}

	public BufferedImage getTileImage(int tileIndex)
	{
		return tileImages[tileIndex];
	}

	public BufferedImage getSubTileImage(int tile) {
		if (tile > 0x7f)
			return getSubTileImage1(tile&0x7f);
		else
			return getSubTileImage0(tile);
	}
	public BufferedImage getSubTileImage0(int tile) {
		return helpGetSubTileImage(tile, gfxData0Record);
	}
	public BufferedImage getSubTileImage1(int tile) {
		return helpGetSubTileImage(tile, gfxData1Record);
	}
	BufferedImage helpGetSubTileImage(int tile, MoveableDataRecord gfxDataRecord) {
		BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_USHORT_555_RGB);
		int[] palette = {
			Drawing.rgbToInt(0,0,0),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(255,255,255)
		};

		int index = tile*16;
		for (int y=0; y<8; y++) {
			int b1 = gfxDataRecord.read(index++);
			int b2 = gfxDataRecord.read(index++);

			for (int x=0; x<8; x++) {
				int c = (b1>>(7-x))&1;
				c |= ((b2>>(7-x))&1)<<1;
				image.setRGB(x, y, palette[3-c]);
			}
		}

		return image;
	}

	public byte[] getSubTileData() {
		byte[] retArray = Arrays.copyOf(gfxData0Record.toArray(), gfxData0Record.getDataSize()+gfxData1Record.getDataSize());
		System.arraycopy(gfxData1Record.toArray(), 0, retArray, gfxData0Record.getDataSize(), gfxData1Record.getDataSize());

		return retArray;
	}

	public void setSubTileData(byte[] data) {
		gfxData0Record.setData(Arrays.copyOfRange(data, 0, gfxData0Record.getDataSize()));
		gfxData1Record.setData(Arrays.copyOfRange(data, gfxData0Record.getDataSize(), data.length));
	}

	public final BufferedImage[] getTileImages() {
		return tileImages;
	}

	public void generateTiles()
	{
		// Generate palettes first
		/*
		for (int i=0; i<8; i++)
		{
			for (int c=0; c<4; c++)
			{
				int index = i*8 + c*2;
				int b1 = paletteDataRecord.read(index);
				int b2 = paletteDataRecord.read(index+1);
				int by = b1 | b2<<8;

				int r = by&0x1f;
				int g = (by>>5)&0x1f;
				int b = (by>>10)&0x1f;

				paletteColors[i][c] = Drawing.rgbToInt(r*8, g*8, b*8);
			}
		}
		*/
		paletteColors = RomReader.binToPalettes(paletteDataRecord.toArray());

		// Generate the actual tiles now.
		for (int i=0; i<0x80; i++)
		{
			tileImages[i] = new BufferedImage(16, 16, BufferedImage.TYPE_USHORT_555_RGB);

			for (int ty=0; ty<2; ty++)
			{
				for (int tx=0; tx<2; tx++)
				{
					int tile = metaTileRecord.read(i*4+ty*2+tx);
					int flags = flagRecord.read(i*4+ty*2+tx);
					int bank = (flags>>3)&1;
					int palette = flags&7;
					boolean flipX = (flags&0x20) != 0;
					boolean flipY = (flags&0x40) != 0;

					for (int y=0; y<8; y++)
					{
						MoveableDataRecord gfxDataRecord;
						if (bank == 0)
							gfxDataRecord = gfxData0Record;
						else
							gfxDataRecord = gfxData1Record;
						for (int x=0; x<8; x++)
						{
							int c;
							if (!flipX)
							{
								c = (gfxDataRecord.read(tile*16+y*2)>>(7-x))&1;
								c |= ((gfxDataRecord.read(tile*16+y*2+1)>>(7-x))&1)<<1;
							}
							else
							{
								c = (gfxDataRecord.read(tile*16+y*2)>>(x))&1;
								c |= ((gfxDataRecord.read(tile*16+y*2+1)>>(x))&1)<<1;
							}

							if (!flipY)
								tileImages[i].setRGB(x+tx*8, y+ty*8, paletteColors[palette][c]);
							else
								tileImages[i].setRGB(x+tx*8, (7-y)+ty*8, paletteColors[palette][c]);
						}
					}

				}
			}
		}

	}

	public int getFlagIndex() {
		return flagIndex;
	}

	public int getGfxData0Index() {
		return gfxData0Index;
	}

	public int getGfxData1Index() {
		return gfxData1Index;
	}

	public int getMetaTileIndex() {
		return metaTileIndex;
	}

	public int getPaletteDataIndex() {
		return paletteDataIndex;
	}

	public int getTileSetDataIndex() {
		return tileSetDataIndex;
	}
	public int getIndex() {
		return tileSetDataIndex;
	}

	public MoveableDataRecord getMetaTileRecord() {
		return metaTileRecord;
	}
	public MoveableDataRecord getFlagRecord() {
		return flagRecord;
	}
	public MoveableDataRecord getGfxData0Record() {
		return gfxData0Record;
	}
	public MoveableDataRecord getGfxData1Record() {
		return gfxData1Record;
	}
	public MoveableDataRecord paletteDataRecord() {
		return paletteDataRecord;
	}
	public MoveableDataRecord getEffectRecord() {
		return effectRecord;
	}
	public MoveableDataRecord getTileSetDataRecord() {
		return tileSetDataRecord;
	}
}
