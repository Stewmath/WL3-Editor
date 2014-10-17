package base;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import record.RomReader;

// ValueFileParser: parses the metadata file and files in the "ref" folder.
// The "names" are the strings on the leftmost column. The "parameters" are the other strings on that line.

public class ValueFileParser {
	private static MetadataFileParser metadataFile;

	private static ValueFileParser musicFile;
	private static ValueFileParser scrollFile;
	private static ValueFileParser levelFile;
	private static ValueFileParser tileEffectFile;
	private static ValueFileParser itemSetFile;
	private static ValueFileParser enemySetFile;
	private static ValueFileParser textLocationFile;
	private static ValueFileParser enemyAiFile;
	private static ValueFileParser enemyGfxFile;

	public static MetadataFileParser getMetadataFile() {
		return metadataFile;
	}
	public static ValueFileParser getEnemyAiFile() {
		return enemyAiFile;
	}
	public static ValueFileParser getEnemyGfxFile() {
		return enemyGfxFile;
	}
	public static ValueFileParser getEnemySetFile() {
		return enemySetFile;
	}
	public static ValueFileParser getItemSetFile() {
		return itemSetFile;
	}
	public static ValueFileParser getLevelFile() {
		return levelFile;
	}
	public static ValueFileParser getMusicFile() {
		return musicFile;
	}
	public static ValueFileParser getScrollFile() {
		return scrollFile;
	}
	public static ValueFileParser getTextLocationFile() {
		return textLocationFile;
	}
	public static ValueFileParser getTileEffectFile() {
		return tileEffectFile;
	}

	public static void reloadValueFiles() {
		musicFile = new ValueFileParser("ref/music.txt");
		scrollFile = new ValueFileParser("ref/scrollModes.txt");
		levelFile = new ValueFileParser("ref/level.txt");
		tileEffectFile = new ValueFileParser("ref/tileEffects.txt");
		itemSetFile = new ValueFileParser("ref/itemSet.txt");
		enemySetFile = new ValueFileParser("ref/enemySet.txt");
		textLocationFile = new ValueFileParser("ref/textLocations.txt");
		enemyAiFile = new ValueFileParser("ref/enemyAis.txt");

		if (metadataFile != null) {
			enemyGfxFile = new ValueFileParser("ref/enemyGfx.txt");
			enemyGfxFile.merge(metadataFile.getFileSection("enemyGfx.txt"));
		}
	}

	public static void reloadMetadataFile(String filename) {
		metadataFile = new MetadataFileParser(filename);
	}

	public static void saveMetadataFile() {
		getMetadataFile().save();
	}

	static String nextLine(Scanner in)
	{
		String s="";
		
		while (in.hasNextLine())
		{
			s = in.nextLine();
			if (!(s.trim().compareTo("") == 0 || s.trim().charAt(0) == ';'))
				break;
			s = "";
		}
		return s;
	}
	

	// List of all names
	ArrayList<String> names = new ArrayList<String>();
	// List of all values for the entry (usually just one string, but there can be multiple others)
	ArrayList<ArrayList<String>> values = new ArrayList<ArrayList<String>>();

	// List of which entries belong to which "sections", denoted by square brackets.
	// Same length as previous 2 variables.
	ArrayList<String> sections = new ArrayList<String>();
	// List of which entries belong to which "file sections", denoted by curly brackets.
	// Used in the metadata file to add to entries for existing files.
	ArrayList<String> fileSections = new ArrayList<String>();

	String filename;

	public ValueFileParser(String fn) {
		filename = fn;
		File f = new File(filename);

		try {
			parseFile(f);
		}
		catch(FileNotFoundException e)
		{
			JOptionPane.showMessageDialog(null, 
					"Error: \"" + f.toString() + "\" could not be opened.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
	}

	ValueFileParser() {
	}

	ValueFileParser(ValueFileParser parser) {
		names = new ArrayList<String>(parser.names);
		values = new ArrayList<ArrayList<String>>(parser.values);
		sections = new ArrayList<String>(parser.sections);
		fileSections = new ArrayList<String>(parser.fileSections);
		filename = new String(parser.filename);
	}

	void parseFile(File f) throws FileNotFoundException {
		Scanner in = new Scanner(f);

		String section = "default";
		String fileSection = "default";
		while (in.hasNextLine())
		{
			String line = nextLine(in);
			if (line.trim().equals(""))
				break;
			String[] s = line.split("[\t=]");
			if (line.trim().charAt(0) == '[' && line.trim().charAt(line.trim().length()-1) == ']') {
				section = s[0].trim().substring(1, s[0].length()-1);
			}
			else if (line.trim().charAt(0) == '{' && line.trim().charAt(line.trim().length()-1) == '}') {
				fileSection = s[0].trim().substring(1, s[0].length()-1);
				section = "default";
			}
			else {
				names.add(s[0]);
				int numValues = s.length-1;
				ArrayList<String> valueList = new ArrayList<String>();
				for (int i=1; i<=numValues; i++) {
					valueList.add(s[i].trim());
				}
				values.add(valueList);
				sections.add(section);
				fileSections.add(fileSection);
			}
		}
		in.close();
	}

	public String getFileName() {
		return filename;
	}

	public int getNameIndex(String name) {
		return names.indexOf(name);
	}

	public int getValueIndex(int valNum, String s) {
		for (int j=0; j<values.size(); j++) {
			if (values.get(j).get(valNum).equals(s))
				return j;
		}
		return -1;
	}
	public int getValueIndex(String s) {
		return getValueIndex(0, s);
	}

	public int getValueIndex(int valNum, int val) {
		for (int j=0; j<values.size(); j++) {
			try {
				int numericVal = RomReader.parseInt(values.get(j).get(valNum));
				if (numericVal == val)
					return j;
			}
			catch(NumberFormatException e) {
				continue;
			}
		}
		return -1;
	}
	public int getValueIndex(int val) {
		return getValueIndex(0, val);
	}

	// Returns the value of the i'th entry (the leftmost number)
	public String indexToName(int index) {
		return names.get(index);
	}

	// Gets a value for the i'th entry
	public String indexToValue(int valNum, int index) {
		return values.get(index).get(valNum);
	}
	public String indexToValue(int index) {
		return indexToValue(0, index);
	}

	public int indexToIntValue(int valNum, int index) throws NumberFormatException {
		return RomReader.parseInt(indexToValue(valNum, index));
	}
	public int indexToIntValue(int index) {
		return indexToIntValue(0, index);
	}


	public int getNumEntries() {
		return names.size();
	}


	public String getValue(int i, String name) {
		int n = names.indexOf(name);
		if (n == -1)
			return "";
		int pos = i;
		if (i == -1)
			pos = 0;
		return values.get(n).get(pos);
	}
	public String getValue(String val) {
		return getValue(-1, val);
	}

	public int getIntValue(int i, String name) throws NumberFormatException {
		return Integer.parseInt(getValue(i, name), 16);
	}
	public int getIntValue(String name) throws NumberFormatException {
		return getIntValue(0, name);
	}

	public String getName(int i, String val) {
		int j;
		for (j=0; j<values.size(); j++) {
			int pos = i;
			if (i == -1)
				pos = 0;
			if (values.get(j).get(pos).equals(val))
				break;
		}
		if (j == values.size())
			return null;
		return names.get(j);
	}
	public String getName(String val) {
		return getName(-1, val);
	}

	public String getName(int valIndex, int val) {
		for (int i=0; i<names.size(); i++) {
			try {
				int numericVal = RomReader.parseInt(values.get(i).get(valIndex));
				if (numericVal == val)
					return names.get(i);
			}
			catch(NumberFormatException e) {}
		}
		return "";
	}
	public String getName(int val) {
		return getName(0, val);
	}

	// Get the number of associates for entry 'i' (usually 1)
	public int getNumValues(int i) {
		return values.get(i).size();
	}

	public ArrayList<String> getAllNames() {
		return new ArrayList<String>(names);
	}


	public void setName(String name, String value) {
		for (int i=0; i<values.size(); i++) {
			if (values.get(i).get(0).equals(value)) {
				names.set(i, name);
				return;
			}
		}
	}
	public void setValue(String name, String value, String fileSection, String section) {
		int index = names.indexOf(name);
		if (index == -1) {
			addEntry(name, value, fileSection, section);
			return;
		}
		values.get(index).set(0, value);
	}
	public void setValue(String name, String value) {
		setValue(name, value, "default", "default");
	}


	void addEntry(String name, String value, String fileSection, String section) {
		ArrayList<String> valueList = new ArrayList<String>();
		valueList.add(value);

		names.add(name);
		values.add(valueList);
		sections.add(section);
		fileSections.add(fileSection);
	}


	public void clearEntries() {
		names = new ArrayList<String>();
		values = new ArrayList<ArrayList<String>>();
		sections = new ArrayList<String>();
		fileSections = new ArrayList<String>();
	}

	// Returns a new ValueFileParser containing entries for the specified section.
	public ValueFileParser getSection(String section) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<sections.size(); i++) {
			if (sections.get(i).equalsIgnoreCase(section)) {
				ret.names.add(names.get(i));
				ret.values.add(values.get(i));
				ret.sections.add(sections.get(i));
				ret.fileSections.add(fileSections.get(i));
			}
		}

		return ret;
	}
	public ValueFileParser getFileSection(String fileSection) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<fileSections.size(); i++) {
			if (fileSections.get(i).equalsIgnoreCase(fileSection)) {
				ret.names.add(names.get(i));
				ret.values.add(values.get(i));
				ret.sections.add(sections.get(i));
				ret.fileSections.add(fileSections.get(i));
			}
		}

		return ret;
	}

	public void merge(ValueFileParser parser) {
		if (parser == null)
			return;
		for (int i=0; i<parser.names.size(); i++) {
			int j=0;
			for (j=0; j<names.size(); j++) {
				if (names.get(j).equals(parser.names.get(i)) &&
						sections.get(j).equals(parser.sections.get(i))) {
					names.set(j, parser.names.get(i));
					values.set(j, parser.values.get(i));
					sections.set(j, parser.sections.get(i));
					fileSections.set(j, parser.fileSections.get(i));
					break;
				}
			}
			// Value is not in 
			if (j == names.size()) {
				names.add(new String(parser.names.get(i)));
				values.add(new ArrayList<String>(parser.values.get(i)));
				sections.add(new String(parser.sections.get(i)));
				fileSections.add(new String(parser.fileSections.get(i)));
			}
		}
	}

}
