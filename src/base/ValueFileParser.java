package base;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import record.RomReader;

// ValueFileParser: parses the metadata file and files in the "ref" folder.
// The "values" are the values on the leftmost column. The "associates" are the other strings on that line.
// Usually values are numbers, and some functions treat them as such.
// But for the metadata files, some "values" are strings.
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
		ValueFileParser ret = new ValueFileParser(enemyGfxFile);
		ret.merge(metadataFile.getFileSection("enemyGfx.txt"));
		return ret;
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
		enemyGfxFile = new ValueFileParser("ref/enemyGfx.txt");
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
	

	// List of all values
	ArrayList<Integer> values = new ArrayList<Integer>();
	// List of all values in string form (though usually values are numbers)
	ArrayList<String> valueStrings = new ArrayList<String>();
	// List of all "associates" for the entry (usually just a string, but there can be multiple other associates)
	ArrayList<ArrayList<String>> associates = new ArrayList<ArrayList<String>>();

	// List of which entries belong to which "sections", denoted by square brackets.
	// Same length as previous 2 variables
	ArrayList<String> sections = new ArrayList<String>();
	// List of which entries belong to which "file sections", denoted by curly brackets.
	// Used in the metadata file to add to entries in existing files.
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
		merge(parser);
		filename = parser.filename;
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
			String[] s = line.split("\t");
			if (s[0].charAt(0) == '[') {
				section = s[0].trim().substring(1, s[0].length()-1);
			}
			else if (s[0].charAt(0) == '{') {
				fileSection = s[0].trim().substring(1, s[0].length()-1);
				section = "default";
			}
			else {
				int val = 0;
				try {
					val = Integer.parseInt(s[0], 16);
				}
				catch(NumberFormatException e) {
					val = -1;
				}

				values.add(val);
				valueStrings.add(s[0]);
				int numAssociates = s.length-1;
				ArrayList<String> associateList = new ArrayList<String>();
				for (int i=1; i<=numAssociates; i++) {
					String name = s[i];
					associateList.add(name);
				}
				associates.add(associateList);
				sections.add(section);
				fileSections.add(fileSection);
			}
		}
		in.close();
	}

	public String getFileName() {
		return filename;
	}

	public int getValueIndex(int val) {
		return values.indexOf(val);
	}

	public int getValueIndex(String val) {
		return valueStrings.indexOf(val);
	}

	public int getAssociateIndex(int i, String s) {
		for (int j=0; j<associates.size(); j++) {
			int n = associates.get(j).indexOf(s);
			if (n == i)
				return j;
		}
		return -1;
	}

	// Returns the value of the i'th entry (the leftmost number)
	public int indexToValue(int index) {
		return values.get(index);
	}
	public String indexToValueString(int index) {
		return valueStrings.get(index);
	}

	// Gets an associate for the i'th entry
	public String indexToAssociate(int i, int associate) {
		return associates.get(i).get(associate);
	}
	public String indexToAssociate(int index) {
		return indexToAssociate(index, 0);
	}
	public int getNumEntries() {
		return values.size();
	}


	// Get associate number 'i' for the value 'val'
	public String getAssociate(int i, int val) {
		int n = values.indexOf(val);
		if (n == -1)
			return "";
		int pos = i;
		if (i == -1)
			pos = associates.get(n).size()-1;
		return associates.get(n).get(pos);
	}
	// Get the last associate for the value 'val' (usually there's only 1 associate)
	public String getAssociate(int val) {
		return getAssociate(-1, val);
	}
	public String getAssociate(int i, String val) {
		int n = valueStrings.indexOf(val);
		if (n == -1)
			return "";
		int pos = i;
		if (i == -1)
			pos = associates.get(n).size()-1;
		return associates.get(n).get(pos);
	}
	public String getAssociate(String val) {
		return getAssociate(-1, val);
	}

	public int getAssociateInt(int i, String val) throws NumberFormatException {
		return Integer.parseInt(getAssociate(i, val), 16);
	}
	public int getAssociateInt(String val) throws NumberFormatException {
		return getAssociateInt(0, val);
	}

	// This is the opposite; get the value from an associate equal to 's'
	public int getValue(int i, String s) {
		int j;
		for (j=0; j<associates.size(); j++) {
			System.out.println("Value " + valueStrings.get(j));
			System.out.println("Associate " + associates.get(j));
			int pos = i;
			if (i == -1)
				pos = 0;
			if (associates.get(j).get(pos).equals(s))
				break;
		}
		if (j == associates.size())
			return -1;
		return values.get(j);
	}
	public int getValue(String s) {
		return getValue(-1, s);
	}
	public String getValueString(int i, String s) {
		int j;
		for (j=0; j<associates.size(); j++) {
			int pos = i;
			if (i == -1)
				pos = associates.get(j).size()-1;
			if (associates.get(j).get(pos).equals(s))
				break;
		}
		if (j == associates.size())
			return null;
		return valueStrings.get(j);
	}
	public String getValueString(String s) {
		return getValueString(-1, s);
	}

	// Get the number of associates for entry 'i' (usually 1)
	public int getNumAssociates(int i) {
		return associates.get(i).size();
	}

	public ArrayList<Integer> getAllValues() {
		return new ArrayList<Integer>(values);
	}
	public ArrayList<String> getAllValueStrings() {
		return new ArrayList<String>(valueStrings);
	}


	public void setAssociate(String value, String asc) {
		int index = valueStrings.indexOf(value);
		if (index == -1)
			return;
		associates.get(index).set(0, asc);
	}
	public void setValue(String value, String asc) {
		for (int i=0; i<associates.size(); i++) {
			if (associates.get(i).get(0).equals(asc)) {
				valueStrings.set(i, value);
				int val=0;
				try {
					val = RomReader.parseInt(value);
				}
				catch (NumberFormatException e) {
					val = -1;
				}
				values.set(i, val);
				return;
			}
		}
	}


	public void addEntry(String value, String associate, String fileSection, String section) {
		ArrayList<String> associateList = new ArrayList<String>();
		associateList.add(associate);

		try {
			values.add(RomReader.parseInt(value));
		}
		catch (NumberFormatException e) {
			values.add(-1);
		}
		valueStrings.add(value);
		associates.add(associateList);
		sections.add(section);
		fileSections.add(fileSection);
	}
	public void addEntry(String value, String associate) {
		addEntry(value, associate, "default", "default");
	}

	public void clearEntries() {
		values = new ArrayList<Integer>();
		valueStrings = new ArrayList<String>();
		associates = new ArrayList<ArrayList<String>>();
		sections = new ArrayList<String>();
		fileSections = new ArrayList<String>();
	}

	// Returns a new ValueFileParser containing entries for the specified section.
	public ValueFileParser getSection(String section) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<sections.size(); i++) {
			if (sections.get(i).equalsIgnoreCase(section)) {
				ret.values.add(values.get(i));
				ret.valueStrings.add(valueStrings.get(i));
				ret.associates.add(associates.get(i));
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
				ret.values.add(values.get(i));
				ret.valueStrings.add(valueStrings.get(i));
				ret.associates.add(associates.get(i));
				ret.sections.add(sections.get(i));
				ret.fileSections.add(fileSections.get(i));
			}
		}

		return ret;
	}

	public void merge(ValueFileParser parser) {
		if (parser == null)
			return;
		for (int i=0; i<parser.valueStrings.size(); i++) {
			int j=0;
			for (j=0; j<valueStrings.size(); j++) {
				if (associates.get(j).get(0).equals(parser.associates.get(i).get(0))) {
					values.set(j, parser.values.get(i));
					valueStrings.set(j, parser.valueStrings.get(i));
					sections.set(j, parser.sections.get(i));
					associates.set(j, parser.associates.get(i));
					break;
				}
			}
			// Value is not in 
			if (j == valueStrings.size()) {
				values.add(new Integer(parser.values.get(i)));
				valueStrings.add(new String(parser.valueStrings.get(i)));
				associates.add(new ArrayList<String>(parser.associates.get(i)));
				sections.add(new String(parser.sections.get(i)));
				fileSections.add(new String(parser.fileSections.get(i)));
			}
		}
	}

}
