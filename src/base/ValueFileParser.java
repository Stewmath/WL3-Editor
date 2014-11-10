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
	private static ValueFileParser miscGfxFile;

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
	public static ValueFileParser getMiscGfxFile() {
		return miscGfxFile;
	}

	public static void reloadValueFiles() {
		musicFile = makeValueFile("music.txt");
		scrollFile = makeValueFile("scrollModes.txt");
		levelFile = makeValueFile("level.txt");
		tileEffectFile = makeValueFile("tileEffects.txt");
		itemSetFile = makeValueFile("itemSet.txt");
		enemySetFile = makeValueFile("enemySet.txt");
		textLocationFile = makeValueFile("textLocations.txt");
		enemyAiFile = makeValueFile("enemyAis.txt");
		miscGfxFile = makeValueFile("miscGfx.txt");
		enemyGfxFile = makeValueFile("enemyGfx.txt");
	}

	static ValueFileParser makeValueFile(String name) {
		ValueFileParser p = new ValueFileParser("ref/" + name);
		if (metadataFile != null)
			p.merge(metadataFile.getFileSection(name));
		return p;
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
	

	// 4 Parallel ArrayLists:
	// List of all names
	ArrayList<String> names = new ArrayList<String>();
	// List of all values for the entry (usually just one string, but there can be multiple others)
	ArrayList<String> values = new ArrayList<String>();

	// List of which entries belong to which "sections", denoted by square brackets.
	// Same length as previous 2 variables.
	ArrayList<String> sections = new ArrayList<String>();
	// List of which entries belong to which "file sections", denoted by curly brackets.
	// Used in the metadata file to add to entries for existing files.
	ArrayList<String> fileSections = new ArrayList<String>();

	// 2 parallel ArrayLists:
	ArrayList<String> sectionList = new ArrayList<String>();
	// sectionIndices isn't properly implemented yet
	ArrayList<Integer> sectionIndices = new ArrayList<Integer>();

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
			String[] s;
			s = line.split("=");

			if (line.trim().charAt(0) == '[' && line.trim().charAt(line.trim().length()-1) == ']') {
				String newSection = s[0].trim().substring(1, s[0].length()-1);
				if (!section.equalsIgnoreCase(newSection)) {
					sectionList.add(newSection);
					sectionIndices.add(getNumEntries());
					section = newSection;
				}
			}
			else if (line.trim().charAt(0) == '{' && line.trim().charAt(line.trim().length()-1) == '}') {
				fileSection = s[0].trim().substring(1, s[0].length()-1);
				section = "default";
			}
			else {
				if (sectionList.size() == 0 && section.equalsIgnoreCase("default")) {
					sectionList.add("default");
					sectionIndices.add(getNumEntries());
				}
				names.add(s[0]);
				int numValues = s.length-1;
				if (s.length >= 2)
					values.add(s[1].trim());
				else
					values.add("");
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

	public int getValueIndex(String s) {
		for (int j=0; j<values.size(); j++) {
			if (values.get(j).equalsIgnoreCase(s))
				return j;
		}
		return -1;
	}

	public int getValueIndex(int val) {
		for (int j=0; j<values.size(); j++) {
			try {
				int numericVal = RomReader.parseInt(values.get(j));
				if (numericVal == val)
					return j;
			}
			catch(NumberFormatException e) {
				continue;
			}
		}
		return -1;
	}

	// Returns the value of the i'th entry (the leftmost number)
	public String indexToName(int index) {
		return names.get(index);
	}

	// Gets a value for the i'th entry
	public String indexToValue(int index) {
		return values.get(index);
	}

	public int indexToIntValue(int index) throws NumberFormatException {
		return RomReader.parseInt(indexToValue(index));
	}


	public int getNumEntries() {
		return names.size();
	}


	public String getValue(String name) {
		int n = names.indexOf(name);
		if (n == -1)
			return "";
		return values.get(n);
	}

	public int getIntValue(String name) throws NumberFormatException {
		return Integer.parseInt(getValue(name), 16);
	}

	public String getName(String val) {
		int j;
		for (j=0; j<values.size(); j++) {
			if (values.get(j).equalsIgnoreCase(val))
				break;
		}
		if (j == values.size())
			return null;
		return names.get(j);
	}

	public String getName(int val) {
		for (int i=0; i<names.size(); i++) {
			try {
				int numericVal = RomReader.parseInt(values.get(i));
				if (numericVal == val)
					return names.get(i);
			}
			catch(NumberFormatException e) {}
		}
		return "";
	}

	public ArrayList<String> getAllNames() {
		return new ArrayList<String>(names);
	}


	public void setName(String name, String value) {
		for (int i=0; i<values.size(); i++) {
			if (values.get(i).equalsIgnoreCase(value)) {
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
		values.set(index, value);
	}
	public void setValue(String name, String value) {
		setValue(name, value, "default", "default");
	}

	public boolean hasEntry(String name) {
		for (int i=0; i<getNumEntries(); i++) {
			if (names.get(i).equalsIgnoreCase(name))
				return true;
		}
		return false;
	}


	void addEntry(String name, String value, String fileSection, String section) {
		int pos;
		// Find the start of the fileSection
		for (pos=0; pos<fileSections.size(); pos++) {
			boolean correctSection = fileSections.get(pos).equalsIgnoreCase(fileSection);
			if (correctSection)
				break;
		}
		// Find the start of the section
		for (; pos<sections.size(); pos++) {
			boolean correctSection = sections.get(pos).equalsIgnoreCase(section);
			if (correctSection)
				break;
		}
		// Find the end of the section
		for (; pos<sections.size(); pos++) {
			boolean correctSection =
				sections.get(pos).equalsIgnoreCase(section) &&
				fileSections.get(pos).equalsIgnoreCase(fileSection);
			if (!correctSection)
				break;
		}

		// pos now points to the end of the section it should be in,
		// if any entries in that section exist already.

		if (sections.isEmpty() || !section.equalsIgnoreCase(sections.get(pos-1))) {
			sectionList.add(section);
			sectionIndices.add(pos);
		}

		names.add(pos, name);
		values.add(pos, value);
		sections.add(pos, section);
		fileSections.add(pos, fileSection);
	}


	public void clearEntries() {
		names = new ArrayList<String>();
		values = new ArrayList<String>();
		sections = new ArrayList<String>();
		fileSections = new ArrayList<String>();
	}

	public int getNumSections() {
		return sectionList.size();
	}

	public String getSectionName(int sectionIndex) {
		return sectionList.get(sectionIndex);
	}

	// Returns a new ValueFileParser containing entries for the specified section.
	public ValueFileParser getSection(String section) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<sections.size(); i++) {
			if (sections.get(i).equalsIgnoreCase(section)) {
				ret.addEntry(names.get(i), values.get(i), fileSections.get(i), sections.get(i));
			}
		}

		return ret;
	}
	public ValueFileParser getSection(int index) {
		return getSection(getSectionName(index));
	}

	public ValueFileParser getFileSection(String fileSection) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<fileSections.size(); i++) {
			if (fileSections.get(i).equalsIgnoreCase(fileSection)) {
				ret.addEntry(names.get(i), values.get(i), fileSections.get(i), sections.get(i));
			}
		}

		return ret;
	}

	public void merge(ValueFileParser parser) {
		if (parser == null)
			return;
		for (int i=0; i<parser.names.size(); i++) {
			setValue(parser.names.get(i), parser.values.get(i), parser.sections.get(i), parser.fileSections.get(i));
		}
	}

}
