package viewers;

import java.util.Arrays;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import record.*;
import base.*;
import graphics.*;

import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class CreditEditor extends JDialog implements PaletteEditorClient {

	RomReader rom = RomReader.rom;

	public final static int textLocation = RomReader.parseInt("58:5d0f");
	public final static int flagLocation = RomReader.parseInt("58:60e5");

	public final static int maxLines = 0x120;

	CreditEditor itself = this;

	TextParser textParser;
	MoveableDataRecord flagRecord;
	ArrayList<Byte> flagData;

	// Raw font gfx data
	MoveableDataRecord gfxDataRecord;
	// gfxData contains gfxDataRecord's new data.
	byte[] gfxData;
	// Raw palette data
	MoveableDataRecord paletteDataRecord;
	int[][] palettes;

	Point lastCaret = new Point(0,0);
	boolean disableListener = true;

	JPanel contentPane;
	JScrollPane textScrollPane;
	JTextArea textArea;
	String oldText = "";
	JScrollPane previewScrollPane;
	PreviewPanel previewPanel;

	JTextField paletteField;
	JCheckBox flipXBox;
	JCheckBox flipYBox;
	JCheckBox priorityBox;

	BufferedImage previewImage;

	public CreditEditor(JFrame owner) {
		super(owner, "Credits Editor", Dialog.ModalityType.APPLICATION_MODAL);
	//	System.out.println(this);

		contentPane = new JPanel();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.PAGE_START;


		textArea = new JTextArea("", 25, 25) {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				FontMetrics metrics = g.getFontMetrics(textArea.getFont());
				int charHeight = metrics.getHeight();
				int destY = (int)(232*charHeight);

				g.setColor(Color.blue);
				g.drawLine(0, destY, textArea.getWidth(), destY);

				destY = (int)(maxLines*charHeight);
				g.setColor(Color.red);
				g.drawLine(0, destY, textArea.getWidth(), destY);
			}
		};
		textArea.setFont(new Font("monospaced", 0, 12));
		textArea.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				if (e.getDot() < textArea.getText().length() && oldText.compareTo(textArea.getText()) != 0) {

					int newDot;
					if (e.getDot() == lastCaret.x)
						newDot = lastCaret.y;
					else
						newDot = lastCaret.x;
					
					int start = Math.min(lastCaret.x, e.getDot());
					String oldString = oldText.substring(start, lastCaret.y);
					String newString = textArea.getText().substring(start, e.getDot());
					updateText(oldString, newString, e.getDot());
				}

				lastCaret = new Point(Math.min(e.getDot(), e.getMark()), Math.max(e.getDot(), e.getMark()));
				oldText = textArea.getText();
			}
		});
		textScrollPane = new JScrollPane(textArea);

		c.fill = GridBagConstraints.BOTH;
		contentPane.add(textScrollPane, c);

		previewPanel = new PreviewPanel();

		previewScrollPane = new JScrollPane(previewPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		previewScrollPane.setBorder(BorderFactory.createTitledBorder("Preview"));

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.VERTICAL;
		contentPane.add(previewScrollPane, c);


		loadText();

		JPanel palettePanel = new PaletteEditorPanel(palettes, false, this);

		c.gridx = 2;
		c.gridy = 0;
		c.gridheight = 1;
		c.fill = 0;
		c.weightx = 0;

		contentPane.add(palettePanel, c);

		JPanel flagPanel = new JPanel();
		flagPanel.setLayout(new BoxLayout(flagPanel, BoxLayout.Y_AXIS));
		flagPanel.setBorder(BorderFactory.createTitledBorder("Flags"));
		paletteField = new JTextField();
		paletteField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int n = Integer.parseInt(paletteField.getText(), 16);
					if (n < 8) {
						for (int i=previewPanel.getFirstSelected(); i<=previewPanel.getLastSelected(); i++) {
							int flags = flagData.get(i);
							flags &= ~7;
							flags |= n;
							flagData.set(i, (byte)flags);
						}
						generatePreview();
						previewPanel.drawImage();
					}
				} catch(NumberFormatException ex){}
			}
		});
		flipXBox = new JCheckBox("Flip X");
		flipXBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				for (int i=previewPanel.getFirstSelected(); i<=previewPanel.getLastSelected(); i++) {
					int flags = flagData.get(i);
					flags &= ~0x20;
					if (e.getStateChange() == ItemEvent.SELECTED)
						flags |= 0x20;
					flagData.set(i, (byte)flags);
				}
				generatePreview();
				previewPanel.drawImage();
			}
		});
		flipYBox = new JCheckBox("Flip Y");
		flipYBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				for (int i=previewPanel.getFirstSelected(); i<=previewPanel.getLastSelected(); i++) {
					int flags = flagData.get(i);
					flags &= ~0x40;
					if (e.getStateChange() == ItemEvent.SELECTED)
						flags |= 0x40;
					flagData.set(i, (byte)flags);
				}
				generatePreview();
				previewPanel.drawImage();
			}
		});
		priorityBox = new JCheckBox("Sprite priority");
		priorityBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				for (int i=previewPanel.getFirstSelected(); i<=previewPanel.getLastSelected(); i++) {
					int flags = flagData.get(i);
					flags &= ~0x80;
					if (e.getStateChange() == ItemEvent.SELECTED)
						flags |= 0x80;
					flagData.set(i, (byte)flags);
				}
			}
		});

		flagPanel.add(new LabelWithComponent(new JLabel("Palette: "), paletteField));
		flagPanel.add(flipXBox);
		flagPanel.add(flipYBox);
		flagPanel.add(priorityBox);

		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		contentPane.add(flagPanel, c);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textParser.setText(textArea.getText()) == 0) {
					save();
					setVisible(false);
				}
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		JButton previewButton = new JButton("Refresh Preview");
		previewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				generatePreview();
				previewPanel.drawImage();
			}
		});

		JButton tileEditorButton = new JButton("Edit font");
		tileEditorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TileEditor tileEditor = new TileEditor(gfxData, palettes);
				tileEditor.setOffsetText("0x" + RomReader.toHexString(gfxDataRecord.getAddr()) + " (compressed)");
				tileEditor.setVisible(true);

				if (tileEditor.clickedOk()) {
					gfxData = tileEditor.getTileData();
					palettes = tileEditor.getPalettes();
					generatePreview();
					previewPanel.drawImage();
				}
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(previewButton);
		buttonPanel.add(tileEditorButton);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.weighty = 0;

		contentPane.add(buttonPanel, c);
		

		generatePreview();
		previewPanel.drawImage();

		add(contentPane);
		pack();
		setMinimumSize(getSize());
		setVisible(true);
	}

	void generatePreview() {
		int height=maxLines;
		int width=13;
		previewImage = new BufferedImage(width*8, height*8, BufferedImage.TYPE_USHORT_555_RGB);
//		previewPanel.setPreferredSize(new Dimension(width*8, height*8));

		textParser.setText(textArea.getText());
		final ArrayList<Byte> data = textParser.getData();

		int x=0, y=0;
		for (int i=0; i<data.size(); i++) {
			if (y >= height)
				break;
			if (data.get(i) == 0x7f) {
				y++;
				x = 0;
			}
			else if (x < width) {
				int dataStart = (data.get(i)+0x100)*16;
				BufferedImage image = RomReader.binToTile(Arrays.copyOfRange(gfxData, dataStart, dataStart+16), flagData.get(x+y*width), palettes);
				previewImage.getGraphics().drawImage(image, x*8, y*8, null);
				x++;
			}
		}

		refreshFlagFields();

		repaint();
	}

	void refreshFlagFields() {
		int flags = flagData.get(previewPanel.getFirstSelected());
		paletteField.setText(""+(flags&7));
		flipXBox.setSelected((flags&0x20) != 0);
		flipYBox.setSelected((flags&0x40) != 0);
		priorityBox.setSelected((flags&0x80) != 0);
	}


	void updateText(String oldText, String newText, int pos) {
		if (!disableListener) {
			int newLines1 = 0, newLines2 = 0;

			for (int i=0; i<oldText.length(); i++) {
				if (oldText.charAt(i) == '\n')
					newLines1++;
			}
			for (int i=0; i<newText.length(); i++) {
				if (newText.charAt(i) == '\n')
					newLines2++;
			}

			if (newLines2 > newLines1) {
				int lines=0;
				for (int i=0; i<pos; i++) {
					if (textArea.getText().charAt(i) == '\n')
						lines++;
				}
				if (lines < maxLines) {
					int numToAdd = newLines2-newLines1;
					byte newFlag=0;
					int j=lines*13-1;
					while (--j >= 0) {
						if ((flagData.get(j)&7) != 0) {
							newFlag = flagData.get(j);
							break;
						}
					}
					for (int i=0; i<numToAdd*13; i++) {
						flagData.add(lines*13, newFlag);
						flagData.remove(flagData.size()-1);
					}
				}
			}
			else if (newLines1 > newLines2) {
				int lines=0;
				for (int i=0; i<pos; i++) {
					if (textArea.getText().charAt(i) == '\n')
						lines++;
				}
				if (lines < maxLines) {
					int numToRemove = newLines1-newLines2;
					//System.out.println(numToRemove);
					for (int i=0; i<numToRemove*13; i++) {
						flagData.remove(lines*13);
						flagData.add((Byte)(byte)0);
					}
				}
			}
		}
	}

	void loadText() {
		RomPointer textPointer = new RomPointer(0x160064, 0x160067, -1);
		textParser = new TextParser(RomReader.BANK(textPointer.getPointedAddr(), 0x58), textPointer, "Credits Text", true);
		textParser.getRecord().setRequiredBank(0x58);

		flagRecord = rom.getMoveableDataRecord(flagLocation, null, false, maxLines*13);
		flagData = flagRecord.toArrayList();

		RomPointer gfxDataPointer = new RomPointer(RomReader.parseInt("58:440d"));
		gfxDataRecord = rom.getMoveableDataRecord(RomReader.BANK(gfxDataPointer.getPointedAddr(), 0x58), gfxDataPointer, true, 0, 0x58);
		gfxDataRecord.setDescription("Credits Font");
		gfxData = gfxDataRecord.toArray();

		paletteDataRecord = rom.getMoveableDataRecord(RomReader.parseInt("58:44cc"), null, false, 0x40);
		palettes = RomReader.binToPalettes(paletteDataRecord.toArray());

		disableListener = true;
		textArea.setText(textParser.getText());
		textArea.setCaretPosition(0);
		disableListener = false;
		oldText = textArea.getText();
	}

	void save() {
		textParser.save();
		paletteDataRecord.setData(RomReader.palettesToBin(palettes));
		flagRecord.setData(flagData);
		gfxDataRecord.setData(gfxData);
	}

	// PaletteEditorClient
	public void setPalettes(int[][] palettes) {
		generatePreview();
		previewPanel.drawImage();
	}


	class PreviewPanel extends JPanel implements Scrollable, MouseListener, MouseMotionListener {
		Point selectionStart = new Point(0,0);
		Point selectionEnd = new Point(0,0);
		Point lastSelectionStart = new Point(0,0);
		Point lastSelectionEnd = new Point(0,0);

		BufferedImage img;

		public PreviewPanel() {
			addMouseListener(this);
			addMouseMotionListener(this);
			setPreferredSize(new Dimension(8*13, maxLines*8));
		}

		public int getFirstSelected() {
			return Math.min(selectionStart.y*13+selectionStart.x, selectionEnd.y*13+selectionEnd.x);
		}
		public int getLastSelected() {
			return Math.max(selectionStart.y*13+selectionStart.x, selectionEnd.y*13+selectionEnd.x);
		}
		public void paint(Graphics g) {
			g.drawImage(img, 0, 0, null);
		}
		public void drawImage() {
			img = new BufferedImage(previewImage.getWidth(), previewImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = img.getGraphics();
			g.drawImage(previewImage, 0, 0, null);
			if (selectionStart.x >= 0) {
				BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
				Graphics imgG = img.getGraphics();
				imgG.setColor(Color.blue);
				imgG.fillRect(0, 0, 8, 8);

				float[] scales = { 1f, 1f, 1f, 0.5f };
				float[] offsets = new float[4];
				RescaleOp rop = new RescaleOp(scales, offsets, null);

				int startY = Math.min(selectionStart.y, selectionEnd.y)+1;
				int endY = Math.max(selectionStart.y, selectionEnd.y)-1;

				for (int y=startY; y<=endY; y++) {
					for (int x=0; x<13; x++)
						((Graphics2D)g).drawImage(img, rop, x*8, y*8);
				}

				if (selectionStart.y == selectionEnd.y) {
					int startX = Math.min(selectionStart.x, selectionEnd.x);
					int endX = Math.max(selectionStart.x, selectionEnd.x);

					for (int x=startX; x<=endX; x++)
						((Graphics2D)g).drawImage(img, rop, x*8, selectionStart.y*8);
				}
				else {
					if (selectionStart.y < selectionEnd.y) {
						for (int x=selectionStart.x; x<13; x++)
							((Graphics2D)g).drawImage(img, rop, x*8, selectionStart.y*8);
						for (int x=selectionEnd.x; x>=0; x--)
							((Graphics2D)g).drawImage(img, rop, x*8, selectionEnd.y*8);
					}
					else {
						for (int x=selectionStart.x; x>=0; x--)
							((Graphics2D)g).drawImage(img, rop, x*8, selectionStart.y*8);
						for (int x=selectionEnd.x; x<13; x++)
							((Graphics2D)g).drawImage(img, rop, x*8, selectionEnd.y*8);
					}
				}
			}

			lastSelectionStart = new Point(selectionStart);
			lastSelectionEnd = new Point(selectionEnd);
		}
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(13*8, 30*8);
		}
		public int getScrollableUnitIncrement(Rectangle visibleRect,
				int orientation,
				int direction) {
			return 8;
		}
		public int getScrollableBlockIncrement(Rectangle visibleRect,
				int orientation,
				int direction) {
			return 32;
		}
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}

		public boolean getScrollableTracksViewportHeight() {
			return false;
		}


		void setSelectionEnd(MouseEvent e) {
			selectionEnd.x = e.getX()/8;
			selectionEnd.y = e.getY()/8;
			if (selectionEnd.x < 0)
				selectionEnd.x = 0;
			if (selectionEnd.x >= 13)
				selectionEnd.x = 13;
			if (selectionEnd.y < 0)
				selectionEnd.y = 0;
			
		}
		public void mousePressed(MouseEvent e) {
			selectionStart.x = e.getX()/8;
			selectionStart.y = e.getY()/8;
			if (selectionStart.x < 0)
				selectionStart.x = 0;
			if (selectionStart.x >= 13)
				selectionStart.x = 13;
			setSelectionEnd(e);

			generatePreview();
			drawImage();
			refreshFlagFields();
			repaint();
		}
		public void mouseReleased(MouseEvent e) {
		}
		public void mouseClicked(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}
		public void mouseDragged(MouseEvent e) {
			if (selectionEnd.x != e.getX()/8 || selectionEnd.y != e.getY()/8) {
				setSelectionEnd(e);
				drawImage();
				repaint();
			}
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
	}
}
