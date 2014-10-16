package base;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

public class ValueFileParser {
	static String nextLine(Scanner in)
	{
		String s="";
		
		while (in.hasNext())
		{
			s = in.nextLine();
			if (!(s.trim().compareTo("") == 0 || s.trim().charAt(0) == ';'))
				break;
		}
		return s;
	}
	

	// List of all values
	ArrayList<Integer> values = new ArrayList<Integer>();
	// List of all "associates" for the entry (usually just a string, but there can be multiple other associates)
	ArrayList<ArrayList<String>> associates = new ArrayList<ArrayList<String>>();

	// List of which entries belong to which "sections", denoted by square brackets.
	// Same length as previous 2 variables
	ArrayList<String> sections = new ArrayList<String>();
	// List of which entries belong to which "file sections", denoted by curly brackets.
	// Not sure what this was intended for.
	ArrayList<String> fileSections = new ArrayList<String>();

	String filename;

	public ValueFileParser(String fn) {
		filename = fn;
		File f = new File("ref/" + filename);

		try {
			Scanner in = new Scanner(f);
			
			String section = "default";
			String fileSection = "default";
			try {
				while (in.hasNext())
				{
					String[] s = nextLine(in).split("\t");
					if (s[0].charAt(0) == '[') {
						section = s[0].trim().substring(1, s[0].length()-1);
					}
					else if (s[0].charAt(0) == '{') {
						fileSection = s[0].trim().substring(1, s[0].length()-1);
						section = "default";
					}
					else {
						int val = Integer.parseInt(s[0], 16);

						values.add(val);
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
			} catch(NumberFormatException e) {}
			in.close();
		}
		catch(FileNotFoundException e)
		{
			JOptionPane.showMessageDialog(null, 
					"Error: \"" + f.toString() + "\" could not be opened.",
					"Error",
					JOptionPane.ERROR_MESSAGE
					);
			return;
		}
	}

	ValueFileParser() {
	}

	public String getFileName() {
		return filename;
	}

	public int getValueIndex(int val) {
		return values.indexOf(val);
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

	// Gets an associate for the i'th entry
	public String indexToAssociate(int index, int associate) {
		return associates.get(index).get(associate);
	}
	public String indexToAssociate(int index) {
		return indexToAssociate(index, 0);
	}
	public int getNumEntries() {
		return values.size();
	}


	// Get associate number 'i' for the value 'num'
	public String getAssociate(int i, int num) {
		int n = values.indexOf(num);
		if (n == -1)
			return "";
		int pos = i;
		if (i == -1)
			pos = associates.get(n).size()-1;
		return associates.get(n).get(pos);
	}
	// Get the last associate for the value 'num' (usually there's only 1 associate)
	public String getAssociate(int num) {
		return getAssociate(-1, num);
	}

	// This is the opposite; get the number from an associate equal to 's'
	public int getValue(int i, String s) {
		int j;
		for (j=0; j<associates.size(); j++) {
			int pos = i;
			if (i == -1)
				pos = associates.get(j).size()-1;
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

	// Get the number of associates for entry 'i' (usually 1)
	public int getNumAssociates(int i) {
		return associates.get(i).size();
	}

	public ArrayList<Integer> getAllValues() {
		return new ArrayList<Integer>(values);
	}

	// Returns a new ValueFileParser containing entries for the specified section.
	public ValueFileParser getSection(String section) {
		ValueFileParser ret = new ValueFileParser();

		for (int i=0; i<sections.size(); i++) {
			if (sections.get(i).equalsIgnoreCase(section)) {
				ret.values.add(values.get(i));
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
		for (int i=0; i<parser.values.size(); i++) {
			int val = parser.values.get(i);
			int j=0;
			for (j=0; j<values.size(); j++) {
				if (values.get(j) == val || associates.get(i).get(1) == parser.associates.get(j).get(1)) {
					values.set(j, parser.values.get(i));
					sections.set(j, parser.sections.get(i));
					associates.set(j, parser.associates.get(i));
					break;
				}
			}
			// Value is not in 
			if (j == values.size()) {
				values.add(parser.values.get(i));
				associates.add(parser.associates.get(i));
				sections.add(parser.sections.get(i));
				fileSections.add(parser.fileSections.get(i));
			}
		}
	}
}
