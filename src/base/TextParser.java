package base;

import record.*;
import java.io.*;
import javax.swing.JOptionPane;
import java.util.ArrayList;

import viewers.CreditEditor;

public class TextParser {
	public static ValueFileParser normalTableParser = new ValueFileParser("ref/textTable.txt");
	public static ValueFileParser creditTableParser = new ValueFileParser("ref/creditTextTable.txt");

	boolean creditText;

	ValueFileParser tableParser;
	int addr;
	MoveableDataRecord record;
	RomPointer pointer;

	String text;
	String recordDescription;
	ArrayList<Byte> data;

	public TextParser(int _addr, RomPointer _pointer, String desc, boolean creditText) {
		addr = _addr;
		pointer = _pointer;
		recordDescription = desc;

		this.creditText = creditText;

		if (creditText)
			tableParser = creditTableParser;
		else
			tableParser = normalTableParser;
		loadText();
	}

	public MoveableDataRecord getRecord() {
		return record;
	}

	public String getText() {
		return text;
	}

	public int setText(String text) {
		if (creditText)
			text = text.toUpperCase();

		this.text = text;

		data = new ArrayList<Byte>();
		int i=0, pos=0;
		boolean madeLine = false;
		int numLines=0;
		while (pos < text.length()) {
			String s = "";
			if (text.charAt(pos) == '{') {
				int end = text.indexOf('}',pos);
				s = text.substring(pos, end+1);
				pos = end;
			}
			else
				s = ""+text.charAt(pos);
			int val;
			try {
				val = tableParser.getIntValue(s);
			}
			catch(NumberFormatException e) {
				val = -1;
			}

			if (text.charAt(pos) == '\n') {
				numLines++;
				if (creditText) {
					data.add((byte)0x7f);
				}
				else {
					if (!madeLine) {
						int numToAdd = 16-(data.size()%16);
						for (int j=0; j<numToAdd; j++) {
							data.add((byte)0x7f);
						}
					}
				}
				madeLine = false;
			}
			else
			{
				if (val == -1) {
					// if that character doesn't exist, check for {XX} format
					try {
						val = Integer.parseInt(s.substring(s.indexOf('{')+1, s.indexOf('}')), 16);
					}
					catch(NumberFormatException e) {}
					catch(IndexOutOfBoundsException e) {}

					if (val == -1) {
						// No good. There's a problem.
						JOptionPane.showMessageDialog(null,
								"Unmappable character \"" + text.charAt(pos) + "\".",
								"Formatting Error",
								JOptionPane.ERROR_MESSAGE);
						return 1;
					}
				}
				// If 16 characters have been typed, but no newline character found, make an error.
				if (!creditText && madeLine) {
					JOptionPane.showMessageDialog(null,
							"Line " + (data.size()-2)/16 + " has more than 16 characters.",
							"Formatting error",
							JOptionPane.ERROR_MESSAGE);
					return 1;
				}

				data.add((byte)val);
				if (data.size()%16 == 0)
					madeLine = true;
			}
			pos++;
		}
		if (!creditText) {
			while (data.size() < 1024)
				data.add((byte)0x7f);
		}
		else {
			while (numLines < CreditEditor.maxLines) {
				data.add((byte)0x7f);
				numLines++;
			}
		}
		// Note: numLines may not be reliable for non-credit text

		if (!creditText && data.size() > 0x400) {
			JOptionPane.showMessageDialog(null,
					"Only 64 lines are allowed.",
					"Formatting Error",
					JOptionPane.ERROR_MESSAGE);
			return 1;
		}
		else if (creditText && numLines > CreditEditor.maxLines) {
			JOptionPane.showMessageDialog(null,
					"Only " + CreditEditor.maxLines + " lines are allowed.",
					"Formatting Error",
					JOptionPane.ERROR_MESSAGE);
			return 1;
		}

		return 0;
	}

	public void save() {
		record.setData(data);
	}

	public final ArrayList<Byte> getData() {
		return data;
	}


	void loadText() {
		text = "";
		if (creditText) {
			record = RomReader.rom.moveableDataRecordExists(addr);
			if (record != null)
				record.addPtr(pointer);
			else {
				int end = addr;
				int line=0;
				while (line < CreditEditor.maxLines) {
					if (RomReader.rom.read(end) == 0x7f)
						line++;
					end++;
				}
				record = RomReader.rom.getMoveableDataRecord(addr, pointer, false, end-addr);
			}
		}
		else
			record = RomReader.rom.getMoveableDataRecord(addr, pointer, true, 0);
		record.setDescription(recordDescription);

		int i=0;
		int lastChar = -1;
		int numLines=0;
		while (i < record.getDataSize()) {
			String s = tableParser.getName(record.read(i));
			if (record.read(i) == 0x7f) {
				if (creditText)
					s = "\n";
				else
					s = " ";
			}
			if (s.isEmpty())
				s = "{"+Integer.toHexString(record.read(i))+"}";
			if (s.charAt(0) == '\n')
				numLines++;
			text += s;
			lastChar = record.read(i);
			i++;
			if (!creditText && i%16 == 0) {
				// Go backwards, removing extra space
				for (int j=0; j<=16; j++) {
					if (j == 16 || (record.read(i-j-1) != 0x7f)) {
						if (j >= text.length())
							text = "";
						else
							text = text.substring(0, text.length()-j);
						break;
					}
				}
				text += '\n';
				numLines++;
			}
		}

		int end = text.length()-1;
		while (text.charAt(end) == '\n' || text.charAt(end) == ' ')
			end--;
		text = text.substring(0, end+1);

		data = new ArrayList<Byte>(record.toArrayList());
	}
}
