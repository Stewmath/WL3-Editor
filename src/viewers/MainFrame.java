package viewers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import viewers.*;
import viewerclients.*;
import viewers.LevelViewer;
import dialogs.*;
import base.*;
import graphics.*;

import java.io.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import record.RomReader;
import record.RegionRecord;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame
	implements TileGridViewerClient, ObjectSetViewerClient {

	RomReader rom;
	MainFrame itself = this;
	
	private JPanel contentPane;
	JScrollPane scrollPane;
	LevelViewer levelViewer;
	TileSetViewer tileSetViewer;
	JButton tileSetEditorButton;
	ObjectSetViewer objectSetViewer;
	JButton objectSetEditorButton;
	ComboBoxFromFile levelField;
	ComboBoxFromFile musicField;
	private JPanel leftPanel;
	private JSplitPane splitPane;
	JCheckBoxMenuItem viewObjectCheckBox;
	JCheckBoxMenuItem viewSectorCheckBox;
	JCheckBoxMenuItem viewRegionCheckBox;
	JRadioButton levelEditButton;
	JRadioButton warpEditButton;
	JPanel specialEditPane;
	JPanel levelEditPane;
	JPanel warpEditPane;
	JPanel sectorEditor;
	JLabel sectorLabel;
	JTextField sectorDestinationField;
	JTextField regionSectorField;
	JTextField regionWidthField;
	JTextField regionHeightField;
	ComboBoxFromFile regionScrollModeField;
	JTextField regionObjectSetField;
	JTextField regionTileSetField;
	JTextField regionByte5Field;
	JTextField regionByte6Field;
	JCheckBox cropLeft;
	JCheckBox cropRight;
	JCheckBox cropTop;
	JCheckBox cropBottom;
	
	boolean disableRegionListener = false;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(java.util.logging.Level.FINE);

		ValueFileParser.reloadValueFiles(); // These are used for some combo boxes



		JPanel tileSetViewerPanel = new JPanel();
		tileSetViewerPanel.setLayout(new BoxLayout(tileSetViewerPanel, BoxLayout.Y_AXIS));
		tileSetViewerPanel.setBorder(BorderFactory.createTitledBorder("TileSet"));
		tileSetViewer = new TileSetViewer(this);
		tileSetEditorButton = new JButton("Edit...");
		tileSetEditorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		tileSetEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MetaTileEditor editor = new MetaTileEditor(itself, tileSetViewer.getTileSet());
				// Refresh the tileSet
				levelViewer.level.generateImage();
				tileSetViewer.setTileSet(tileSetViewer.tileSet);
				levelViewer.repaint();
			}
		});
		tileSetViewerPanel.add(tileSetViewer);
		tileSetViewerPanel.add(tileSetEditorButton);
		JPanel objectSetViewerPanel = new JPanel();
		objectSetViewerPanel.setLayout(new BoxLayout(objectSetViewerPanel, BoxLayout.Y_AXIS));
		objectSetViewerPanel.setBorder(BorderFactory.createTitledBorder("ObjectSet"));
		objectSetViewer = new ObjectSetViewer(this);
		objectSetEditorButton = new JButton("Edit...");
		objectSetEditorButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		objectSetEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				ObjectSetEditor editor = new ObjectSetEditor(itself, levelViewer.selectedRegion.objectSet);
			}
		});
		objectSetViewerPanel.add(objectSetViewer, BorderLayout.NORTH);
		objectSetViewerPanel.add(objectSetEditorButton, BorderLayout.SOUTH);
		levelViewer = new LevelViewer(this);
		specialEditPane = new JPanel();
		//specialEditPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		levelEditPane = new JPanel();
		levelEditPane.setLayout(new BoxLayout(levelEditPane, BoxLayout.X_AXIS));
		Drawing.addComponent(levelEditPane, tileSetViewerPanel);
		levelEditPane.add(objectSetViewerPanel);
		
		ActionListener regionActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (disableRegionListener == false)
					writeRegionFields();
			}
		};
		warpEditPane = new JPanel();
		warpEditPane.setLayout(new BoxLayout(warpEditPane, BoxLayout.Y_AXIS));
		sectorEditor = new JPanel();
		sectorEditor.setLayout(new BoxLayout(sectorEditor, BoxLayout.X_AXIS));
		sectorLabel = new JLabel("Destination: ");
		sectorDestinationField = new JTextField("00 ");
		sectorDestinationField.addActionListener(regionActionListener);
		sectorEditor.add(sectorLabel);
		sectorEditor.add(sectorDestinationField);
		sectorEditor.setPreferredSize(new Dimension(125, 50));
		sectorEditor.setBorder(BorderFactory.createTitledBorder("Sector 0"));
		JPanel regionEditor = new JPanel();
		regionEditor.setLayout(new BoxLayout(regionEditor, BoxLayout.Y_AXIS));
		regionSectorField = new JTextField("");
		regionWidthField = new JTextField("");
		regionHeightField = new JTextField("");
		regionScrollModeField = new ComboBoxFromFile(this, ValueFileParser.getScrollFile());
		regionObjectSetField = new JTextField();
		regionTileSetField = new JTextField();
		regionByte5Field = new JTextField();
		regionByte6Field = new JTextField();
		regionSectorField.addActionListener(regionActionListener);
		regionWidthField.addActionListener(regionActionListener);
		regionHeightField.addActionListener(regionActionListener);
		regionScrollModeField.addActionListener(regionActionListener);
		regionObjectSetField.addActionListener(regionActionListener);
		regionTileSetField.addActionListener(regionActionListener);
		regionByte5Field.addActionListener(regionActionListener);
		regionByte6Field.addActionListener(regionActionListener);
		regionEditor.add(new LabelWithComponent(
				new JLabel("Top-left Sector: "),
				regionSectorField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Width: "),
				regionWidthField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Height: "),
				regionHeightField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Scroll: "),
				regionScrollModeField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Object Set: "),
				regionObjectSetField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Tile Set: "),
				regionTileSetField
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Animations: "),
				regionByte5Field
				));
		regionEditor.add(new LabelWithComponent(
				new JLabel("Palette Flashing: "),
				regionByte6Field
				));
		cropLeft = new JCheckBox("Crop Left");
		cropRight = new JCheckBox("Crop Right");
		cropTop = new JCheckBox("Crop Top");
		cropBottom = new JCheckBox("Crop Bottom");
		cropLeft.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.selectedRegion.setCropLeft(true);
				else
					levelViewer.selectedRegion.setCropLeft(false);
			}
		});
		cropRight.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.selectedRegion.setCropRight(true);
				else
					levelViewer.selectedRegion.setCropRight(false);
			}
		});
		cropTop.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.selectedRegion.setCropTop(true);
				else
					levelViewer.selectedRegion.setCropTop(false);
			}
		});
		cropBottom.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.selectedRegion.setCropBottom(true);
				else
					levelViewer.selectedRegion.setCropBottom(false);
			}
		});
		Drawing.addComponent(regionEditor, cropLeft);
		Drawing.addComponent(regionEditor, cropRight);
		Drawing.addComponent(regionEditor, cropTop);
		Drawing.addComponent(regionEditor, cropBottom);
		regionEditor.setBorder(BorderFactory.createTitledBorder("Region"));
		warpEditPane.add(sectorEditor);
		warpEditPane.add(regionEditor);
		
		warpEditPane.setPreferredSize(new Dimension(200, warpEditPane.getPreferredSize().height));
		Drawing.addComponent(specialEditPane, levelEditPane);
		
		
		setTitle("Wario Land 3 Editor v0.4.1");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		
		JMenuBar menuBar_1 = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		JMenuItem mntmOpen_1 = new JMenuItem("Open");
		fileMenu.add(mntmOpen_1);
		JMenuItem mntmSave_1 = new JMenuItem("Save");
		fileMenu.add(mntmSave_1);
		
		JMenu viewMenu = new JMenu("View");
		viewObjectCheckBox = new JCheckBoxMenuItem("Objects");
		viewSectorCheckBox = new JCheckBoxMenuItem("Sectors");
		viewRegionCheckBox = new JCheckBoxMenuItem("Regions");
		viewObjectCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.viewObjects = true;
				else
					levelViewer.viewObjects = false;
				levelViewer.repaint();
			}
		});
		viewSectorCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.viewSectors = true;
				else
					levelViewer.viewSectors = false;
				levelViewer.repaint();
			}
		});
		viewRegionCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED)
					levelViewer.viewRegions = true;
				else
					levelViewer.viewRegions = false;
				levelViewer.repaint();
			}
		});
		viewObjectCheckBox.setSelected(true);
		viewSectorCheckBox.setSelected(false);
		viewRegionCheckBox.setSelected(true);
		
		viewMenu.add(viewObjectCheckBox);
		viewMenu.add(viewSectorCheckBox);
		viewMenu.add(viewRegionCheckBox);
		
		JMenu levelMenu = new JMenu("Level");
		JMenuItem mntm_addRegion = new JMenuItem("Add Region");
		mntm_addRegion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				//AddRegionDialog d = new AddRegionDialog(itself);
				String s = (String)JOptionPane.showInputDialog(
						itself,
						"Specify the new region's top-left sector.\n"
						+ "Note: You must set a sector destination to point to this region, or it can't be saved.",
						"Add region",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						"");
				if (s != null)
				{
					try {
						int n = Integer.parseInt(s, 16);
						byte[] bytes = new byte[8];
						bytes[1] = (byte)(((n/0xa)<<4)|(n/0xa+1));
						bytes[2] = (byte)(((n%0xa)<<4)|((n%0xa)+1));
						bytes[7] = 1;
						if (n <= 0x1D && levelViewer.level.getRegion((n%0xa)*16, (n/0xa)*16) == null)
						{
							Region r = new Region(bytes);
							levelViewer.level.getRegionDataRecord().addRegion(r);
							levelViewer.level.generateImage();
							levelViewer.setSelectedRegion(r);
							levelViewer.repaint();
						}
						else if (n > 0x1D) {
							JOptionPane.showMessageDialog(itself,
									"There are only 0x1D sectors.",
									"Error",
									JOptionPane.ERROR_MESSAGE);
						}
						else {
							JOptionPane.showMessageDialog(itself,
									"That sector is already occupied by a region.",
									"Error",
									JOptionPane.ERROR_MESSAGE);
						}
					} catch(NumberFormatException ex) {
						JOptionPane.showMessageDialog(itself,
								"That wasn't a number!",
								"Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		levelMenu.add(mntm_addRegion);

		JMenuItem deleteRegionButton = new JMenuItem("Delete Region");
		deleteRegionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int option = JOptionPane.showOptionDialog(
						itself,
						"Delete the currently selected region?",
						"Delete region",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						"");
				if (option == JOptionPane.YES_OPTION) {
					RegionRecord r = levelViewer.level.getRegionDataRecord();
					r.deleteRegion(levelViewer.selectedRegion);

					levelViewer.level.generateImage();
					levelViewer.setSelectedRegion(r.getRegion(0));
				}
			}
		});
		levelMenu.add(deleteRegionButton);
		
		JMenuItem mntm_compare = new JMenuItem("Compare...");
		mntm_compare.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				ComboBoxFromFile levelBox = new ComboBoxFromFile(itself, ValueFileParser.getLevelFile());
				levelBox.setSelected(levelViewer.level.getId());
				JComponent[] inputs = {new JLabel("Select a level to compare with level " + RomReader.toHexString(levelViewer.level.getId(), 2) + ":"),
					levelBox};
				int ret = JOptionPane.showConfirmDialog(
					itself,
					inputs,
					"Compare",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null
					);
				
				if (ret == JOptionPane.OK_OPTION) {
					int n = levelBox.getVal();
					if (n >= 0 && n <= Level.lastLevel) {
						CompareLevelDialog d = new CompareLevelDialog(itself, levelViewer.level, Level.getLevel(n));
						// This is a little weird, it's just a way to make it reload the level.
						levelViewer.setLevel(levelViewer.level);
					}
				}
			}
		});
		levelMenu.add(mntm_compare);

		JMenuItem exportLevelButton = new JMenuItem("Export Level");
		exportLevelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				ImportExportDialog d = new ImportExportDialog(true, levelViewer.level.getId());
			}
		});
		levelMenu.add(exportLevelButton);
		JMenuItem importLevelButton = new JMenuItem("Import Level");
		importLevelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				ImportExportDialog d = new ImportExportDialog(false, 0);
				levelViewer.setLevel(levelViewer.level);
			}
		});
		levelMenu.add(importLevelButton);
		
		JMenu miscMenu = new JMenu("Other");

		JMenuItem miscMusicButton = new JMenuItem("Misc. Music");
		miscMusicButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				MiscMusicDialog d = new MiscMusicDialog(itself);
			}
		});
		miscMenu.add(miscMusicButton);

		JMenuItem miscGfxButton = new JMenuItem("Misc. Graphics");
		miscGfxButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				MiscGfxDialog d = new MiscGfxDialog(itself);
				levelViewer.setLevel(levelViewer.level);
			}
		});
		miscMenu.add(miscGfxButton);

		JMenuItem textEditorButton = new JMenuItem("Edit Text");
		textEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				TextEditor editor = new TextEditor(itself);
			}
		});
		miscMenu.add(textEditorButton);

		JMenuItem creditEditorButton = new JMenuItem("Edit Credits");
		creditEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				CreditEditor editor = new CreditEditor(itself);
			}
		});
		miscMenu.add(creditEditorButton);

		
		menuBar_1.add(fileMenu);
		menuBar_1.add(viewMenu);
		menuBar_1.add(levelMenu);
		menuBar_1.add(miscMenu);
		
		setJMenuBar(menuBar_1);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		scrollPane = new JScrollPane(levelViewer);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setBlockIncrement(16*4);
		scrollPane.getVerticalScrollBar().setBlockIncrement(16*4);
		scrollPane.setPreferredSize(new Dimension(500, 500));
		
		levelEditButton = new JRadioButton("Edit level");
		
		levelEditButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				levelViewer.setEditMode(LevelViewer.EDIT_LEVEL);
				if (specialEditPane.isAncestorOf(warpEditPane))
				{
					specialEditPane.removeAll();
					specialEditPane.add(levelEditPane);
					leftPanel.revalidate();
					leftPanel.repaint();
				}
			}
		});
		warpEditButton = new JRadioButton("Edit warps & regions");
		warpEditButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				levelViewer.setEditMode(LevelViewer.EDIT_WARPS);
				if (specialEditPane.isAncestorOf(levelEditPane))
				{
					specialEditPane.removeAll();
					specialEditPane.add(warpEditPane);
					leftPanel.revalidate();
					leftPanel.repaint();
				}
			}
		});
		levelEditButton.setSelected(true);
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(levelEditButton);
		modeGroup.add(warpEditButton);
		
		leftPanel = new JPanel();
		levelField = new ComboBoxFromFile(this, ValueFileParser.getLevelFile());
		levelField.setMaximumSize(levelField.getPreferredSize());
//		levelField.setMaximumSize(new Dimension(200, levelField.getPreferredSize().height));
//		levelField.setPreferredSize(levelField.getMaximumSize());
		levelField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int level=levelField.getAddr();

				if (level >= 0)
				{
					setLevel(Level.getLevel(level));
					repaint();
					levelField.setSelected(level);
				}
			}
		});
		musicField = new ComboBoxFromFile(this, ValueFileParser.getMusicFile());
		musicField.setMaximumSize(new Dimension(200, 30));
		musicField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int n = musicField.getAddr();
				if (n >= 0)
					levelViewer.level.setMusicId(n);
				musicField.setSelected(levelViewer.level.getMusicId());
			}
		});

		JLabel levelIdLabel = new JLabel("Level ID:");
		JLabel musicLabel = new JLabel("Music ID:");
		
		//panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		Drawing.addComponent(leftPanel, levelIdLabel);
		leftPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		Drawing.addComponent(leftPanel, levelField);
		JSeparator separator = new JSeparator();
		separator.setMaximumSize(new Dimension(10000, 1));
		leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		leftPanel.add(separator);
		leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		Drawing.addComponent(leftPanel, musicLabel);
		leftPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		Drawing.addComponent(leftPanel, musicField);
		Drawing.addComponent(leftPanel, levelEditButton);
		Drawing.addComponent(leftPanel, warpEditButton);
		Drawing.addComponent(leftPanel, specialEditPane);
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.add(leftPanel);
		splitPane.add(scrollPane);

		leftPanel.setMinimumSize(new Dimension(200, 0));
		
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		
		mntmOpen_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				if ((arg0.getButton() & MouseEvent.BUTTON1) != 0) 
				{
					JFileChooser fc = new JFileChooser();
					ExampleFileFilter filter = new ExampleFileFilter();
					filter.addExtension("gb");
					filter.addExtension("gbc");
					filter.setDescription("Gameboy roms");
					fc.setFileFilter(filter);
					try {
						BufferedReader in = new BufferedReader(new FileReader(new File("ref/location.txt")));
						String dir = in.readLine();
						fc.setCurrentDirectory(new File(dir));
					} catch (FileNotFoundException e1) {
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (fc.showOpenDialog(arg0.getComponent()) == JFileChooser.APPROVE_OPTION)
					{
						String dir = fc.getCurrentDirectory().toString();
						try {
							PrintWriter out = new PrintWriter(new File("ref/location.txt"));
							out.println(dir);
							out.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						loadRom(fc.getSelectedFile());
					}
				}
			}
		});
		mntmSave_1.addMouseListener(new MouseAdapter() {	
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					rom.save();
					if (rom.savedSuccessfully()) {
						ValueFileParser.saveMetadataFile();
						ValueFileParser.reloadValueFiles();
					}
				}
			}
		});


		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				if (rom.isModified()) {
					int option = JOptionPane.showOptionDialog(null,
							"Save the rom before exiting?",
							"Exit",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							null,
							null,
							null);
					if (option == JOptionPane.CANCEL_OPTION)
						setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					else if (option == JOptionPane.YES_OPTION) {
						rom.save();
						if (rom.savedSuccessfully()) {
							ValueFileParser.saveMetadataFile();
						}
						else {
							setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
						}
					}
				}
			}
		});
		
		disableFields();
		pack();
	}

	public void disableFields() {
		levelEditButton.setEnabled(false);
		warpEditButton.setEnabled(false);
		tileSetEditorButton.setEnabled(false);
		objectSetEditorButton.setEnabled(false);
		musicField.setEnabled(false);
		levelField.setEnabled(false);
	}
	public void enableFields() {
		levelEditButton.setEnabled(true);
		warpEditButton.setEnabled(true);
		tileSetEditorButton.setEnabled(true);
		objectSetEditorButton.setEnabled(true);
		musicField.setEnabled(true);
		levelField.setEnabled(true);
	}
	
	void writeRegionFields()
	{
		boolean generateImage=false;
		
		if (levelViewer.level != null)
		{
			try {
				int dest = Integer.parseInt(sectorDestinationField.getText(), 16);
				levelViewer.setSectorDestination(dest);
			} catch(NumberFormatException ex){}
			
			if (levelViewer.selectedRegion != null)
			{
				try {
					int n = Integer.parseInt(regionSectorField.getText(), 16);
					if (levelViewer.selectedRegion.getFirstSector() != n)
					{
						levelViewer.setRegionSector(n);
						generateImage = true;
					}
				} catch(NumberFormatException ex){}
				try {
					int n = Integer.parseInt(regionWidthField.getText(), 16);
					if (levelViewer.selectedRegion.getWidth() != n)
					{
						levelViewer.setRegionWidth(n);
						generateImage = true;
					}
				} catch(NumberFormatException ex){}
				try {
					int n = Integer.parseInt(regionHeightField.getText(), 16);
					if (levelViewer.selectedRegion.getHeight() != n)
					{
						levelViewer.setRegionHeight(n);
						generateImage = true;
					}
				} catch(NumberFormatException ex){}
				try {
					int n = regionScrollModeField.getAddr();
					levelViewer.selectedRegion.scrollMode &= 0xF0;
					levelViewer.selectedRegion.scrollMode |= (n&0xf);
				} catch(NumberFormatException ex){}
				try {
					int n = Integer.parseInt(regionObjectSetField.getText(), 16);
					levelViewer.selectedRegion.setObjectSet(n);
				} catch(NumberFormatException ex){}
				try {
					int n = Integer.parseInt(regionTileSetField.getText(), 16);
					if (n != levelViewer.selectedRegion.tileSetId)
					{
						levelViewer.selectedRegion.setTileSet(n);
						tileSetViewer.setTileSet(TileSet.getTileSet(n));
						generateImage = true;
					}
				} catch(NumberFormatException ex){}
				try {
					levelViewer.selectedRegion.b5 = Integer.parseInt(regionByte5Field.getText(), 16);
				} catch(NumberFormatException ex){}
				try {
					levelViewer.selectedRegion.b6 = Integer.parseInt(regionByte6Field.getText(), 16);
				} catch(NumberFormatException ex){}
			}
			
			if (generateImage)
				levelViewer.level.generateImage();
			levelViewer.refreshRegionFields();
			levelViewer.repaint();
		}
	}

	void loadRom(File f)
	{
		if (rom != null)
			rom.clearRecords();

		rom = new RomReader(f);
		TileSet.rom = rom;
		RomReader.rom = rom;

		String filename = f.getAbsolutePath();
		String metadataFilename = filename.substring(0, filename.lastIndexOf('.'));
		metadataFilename = metadataFilename + ".mtd";
		
		// Mind the order
		ValueFileParser.reloadMetadataFile(metadataFilename);
		if (!ValueFileParser.getMetadataFile().isOpened()) {
			disableFields();
			return;
		}

		ValueFileParser.reloadValueFiles();
		EnemySet.reloadEnemySets();
		ObjectSet.reloadObjectSets();
		TileSet.reloadTileSets();
		RegionRecord.reloadRecords();

		Level.reloadLevels();
		
		setLevel(Level.getLevel(0));
		contentPane.repaint();
		enableFields();
	}

	void setLevel(Level l) {
		levelViewer.setLevel(l);
		// Check if it's the boss level... it's kind of weird, so it needs certain things disabled.
		if (l.getId() == 0xc8) {
			musicField.setEnabled(false);
			objectSetEditorButton.setEnabled(false);
		}
		else {
			musicField.setEnabled(true);
			objectSetEditorButton.setEnabled(true);
		}
	}

	// from TileSetViewerClient
	public void tileSelectionChanged(int tile)
	{
		levelViewer.tileMode = true;
		objectSetViewer.selectedObject = -1;
		objectSetViewer.repaint();
	}
	public void tileHoverChanged(int tile) {
	}
	
	// from ObjectSetViewerClient
	public void objectSetSelectionChanged(int obj)
	{
		if (levelViewer.viewObjects == false)
		{
			viewObjectCheckBox.setSelected(true);
			levelViewer.repaint();
		}
		levelViewer.tileMode = false;
		tileSetViewer.selectedTile = -1;
		tileSetViewer.repaint();
	}
}
