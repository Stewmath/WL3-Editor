package graphics;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import base.ValueFileParser;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ComboBoxFromFile extends JComboBox {

	static String next(Scanner in)
	{
		String s="";
		
		while (in.hasNext())
		{
			s = in.next().trim();
			if (s.charAt(0) == ';' || s.compareTo("") == 0)
				in.nextLine();
			else
				break;
		}
		if (!in.hasNext())
			return "";
		return s;
	}
	
	ValueFileParser parser;
	
	public ComboBoxFromFile(Component parent, ValueFileParser p, boolean displayAddr)
	{
		super();
		setEditable(true);

		parser = p;
		
		ArrayList<Integer> values = parser.getAllValues();
		for (int i=0; i<values.size(); i++) {
			int addr = values.get(i);
			String name = parser.getAssociate(addr);
			if (displayAddr)
				this.addItem(name + " (" + Integer.toHexString(addr).toUpperCase() + ")");
			else
				this.addItem(name);
		}

	}
	public ComboBoxFromFile(Component parent, ValueFileParser p)
	{
		super();
		setEditable(true);

		parser = p;
		
		ArrayList<Integer> values = parser.getAllValues();
		for (int i=0; i<values.size(); i++) {
			int addr = values.get(i);
			String name = parser.getAssociate(addr);
			this.addItem(name + " (" + Integer.toHexString(addr).toUpperCase() + ")");
		}

	}

	public void setSelected(int addr)
	{
		ArrayList<Integer> values = parser.getAllValues();
		int i = values.indexOf(addr);
		if (i == -1)
			this.setSelectedItem(Integer.toHexString(addr).toUpperCase());
		else
			setSelectedIndex(i);
	}

	// This name was a poor choice. getVal() does the same.
	public int getAddr()
	{
		if (getSelectedIndex() == -1)
		{
			String s = (String)this.getSelectedItem();
			try {
				return Integer.parseInt(s, 16);
			} catch(NumberFormatException e){}

			// Try to read the number in brackets
			int end = s.lastIndexOf(')');
			if (end < 0)
				end = s.length();
			int start = s.lastIndexOf('(')+1;
			try {
				return Integer.parseInt(s.substring(start, end), 16);
			} catch(NumberFormatException e){}
			catch(StringIndexOutOfBoundsException e){}
			// Give up
			return 0;
		}
		return parser.indexToValue(this.getSelectedIndex());
	}
	public int getVal() {
		return getAddr();
	}

	public ValueFileParser getParser() {
		return parser;
	}
}
