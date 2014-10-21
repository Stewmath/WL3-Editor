package viewers;

import graphics.Drawing;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JPanel;

import base.Level;
import base.Region;

import java.awt.Font;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class LevelViewer extends JPanel  {

	// Values for editMode
	final static int EDIT_LEVEL=1;
	final static int EDIT_WARPS=2;
	
	int editMode = EDIT_LEVEL;
	boolean viewObjects=false;
	boolean viewSectors=false;
	boolean viewRegions=false;
	int selectedSector=0;
	
	MainFrame mainFrame;
	TileSetViewer tileSetViewer;
	
	public Region selectedRegion=null;
	public Level level;
	
	public Point cursorPos;
	boolean dragging=false;
	int draggingObject=0;
	
	// If false, objects are added instead
	boolean placingTile=true;
	
	public LevelViewer(MainFrame f)
	{
		mainFrame = f;
		tileSetViewer = f.tileSetViewer;
		setPreferredSize(new Dimension(0xa0*16, 0x30*16));
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e)
			{
				if (level != null)
				{
					cursorPos = new Point(e.getX()/16, e.getY()/16);

					boolean changedRegion = false;
					if ((editMode == EDIT_LEVEL || editMode == EDIT_WARPS) && level.getRegion(cursorPos.x, cursorPos.y) != selectedRegion)
					{
						Region newRegion = level.getRegion(cursorPos.x, cursorPos.y);
						if (newRegion != null) {
							selectedRegion = newRegion;
							tileSetViewer.setTileSet(selectedRegion.getTileSet());
						}
						changedRegion = true;
					}
					int newSector = cursorPos.y/16*0xa + cursorPos.x/16;
					if (newSector != selectedSector) {
						selectedSector = newSector;
						refreshRegionFields();
					}
					
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						if (viewObjects && (placingTile || mainFrame.objectSetViewer.selectedObject != 0 || editMode == EDIT_WARPS)
							&& level.getObject(cursorPos.x, cursorPos.y) != 0)
						{
							draggingObject = level.getObject(cursorPos.x, cursorPos.y);
							level.setObject(cursorPos.x, cursorPos.y, 0);
						}
						else if (editMode == EDIT_LEVEL && !changedRegion)
						{
							if (placingTile)
							{
								dragging = true;
								setTile(cursorPos.x, cursorPos.y, tileSetViewer.selectedTile);
							}
							else
							{
								dragging = true;
								level.setObject(cursorPos.x, cursorPos.y, mainFrame.objectSetViewer.selectedObject);
							}
						}

					}
					else if (e.getButton() == MouseEvent.BUTTON3)
					{
						int obj = level.getObject(cursorPos.x, cursorPos.y);
						if (viewObjects && obj != 0)
							mainFrame.objectSetViewer.setSelectedObject(obj);
						else
							tileSetViewer.setSelectedTile(level.getTile(cursorPos.x, cursorPos.y));
					}

					repaint();
				}
			}
			public void mouseReleased(MouseEvent e)
			{
				if (level != null)
				{
					dragging = false;
					if (draggingObject != 0)
					{
						level.setObject(cursorPos.x, cursorPos.y, draggingObject);
						draggingObject = 0;
					}
				}
			}
			public void mouseExited(MouseEvent e) {
				cursorPos.x = -1;
				repaint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e)
			{
				Point p = e.getPoint();
				Point oldPos = cursorPos;
				cursorPos = new Point(p.x/16, p.y/16);
				if (oldPos.x != cursorPos.x || oldPos.y != cursorPos.y)
					repaint();
			}
			public void mouseDragged(MouseEvent e)
			{
				Point p = e.getPoint();
				cursorPos = new Point(p.x/16, p.y/16);
				if (dragging)
				{
					if (placingTile)
						setTile(cursorPos.x, cursorPos.y, tileSetViewer.selectedTile);
					else
						level.setObject(cursorPos.x, cursorPos.y, mainFrame.objectSetViewer.selectedObject);
				}
				repaint();
			}
		});
		
		
		level = null;
		
		cursorPos = new Point(0, 0);
	}

	public void setTile(int x, int y, int tile) {
		level.setTile(cursorPos.x, cursorPos.y, tileSetViewer.selectedTile);
	}
	
	public void setRegionSector(int sector)
	{
		if (selectedRegion == null)
			return;
		int x = sector%0xa;
		int y = sector/0xa;
		if (y < 3 && (level.getRegion(x*16, y*16) == null || level.getRegion(x*16, y*16) == selectedRegion))
		{
			selectedRegion.firstHSector = x;
			selectedRegion.firstVSector = y;
			selectedRegion.lastHSector = x+1;
			selectedRegion.lastVSector = y+1;
		}
	}
	public void setRegionWidth(int width)
	{
		if (selectedRegion == null)
			return;
		boolean okay = true;
		if (width <= 0 || selectedRegion.firstHSector+width > 0xa)
			okay = false;
		
		for (int x=selectedRegion.firstHSector+1; x<selectedRegion.firstHSector+width && okay==true; x++)
		{
			for (int y=selectedRegion.firstVSector; y<selectedRegion.lastVSector; y++)
			{
				if (level.getRegion(x*16, y*16) != selectedRegion && level.getRegion(x*16, y*16) != null)
				{
					okay = false;
					break;
				}
			}
		}
		
		if (okay)
		{
			selectedRegion.lastHSector = selectedRegion.firstHSector+width;
		}
	}
	public void setRegionHeight(int height)
	{
		if (selectedRegion == null)
			return;
		boolean okay = true;
		if (height <= 0 || selectedRegion.firstVSector+height > 3)
			okay = false;
		
		for (int y=selectedRegion.firstVSector+1; y<selectedRegion.firstVSector+height && okay==true; y++)
		{
			for (int x=selectedRegion.firstHSector; x<selectedRegion.lastHSector; x++)
			{
				if (level.getRegion(x*16, y*16) != null && level.getRegion(x*16, y*16) != selectedRegion)
				{
					okay = false;
					break;
				}
			}
		}
		
		if (okay)
		{
			selectedRegion.lastVSector = selectedRegion.firstVSector+height;
		}
	}
	void refreshRegionFields()
	{
		mainFrame.disableRegionListener = true;

		mainFrame.regionSectorField.setText(Integer.toHexString(selectedRegion.firstVSector*0xa+selectedRegion.firstHSector).toUpperCase());
		mainFrame.regionWidthField.setText(Integer.toHexString(selectedRegion.lastHSector-selectedRegion.firstHSector).toUpperCase());
		mainFrame.regionHeightField.setText(Integer.toHexString(selectedRegion.lastVSector-selectedRegion.firstVSector).toUpperCase());
		mainFrame.regionScrollModeField.setSelected(selectedRegion.scrollMode&0xf);
		mainFrame.regionObjectSetField.setText(Integer.toHexString(selectedRegion.objectSetId).toUpperCase());
		mainFrame.regionTileSetField.setText(Integer.toHexString(selectedRegion.tileSetId).toUpperCase());
		mainFrame.regionByte5Field.setText(Integer.toHexString(selectedRegion.b5).toUpperCase());
		mainFrame.regionByte6Field.setText(Integer.toHexString(selectedRegion.b6).toUpperCase());

		mainFrame.cropLeft.setSelected(selectedRegion.getCropLeft());
		mainFrame.cropRight.setSelected(selectedRegion.getCropRight());
		mainFrame.cropTop.setSelected(selectedRegion.getCropTop());
		mainFrame.cropBottom.setSelected(selectedRegion.getCropBottom());

		mainFrame.sectorEditor.setBorder(BorderFactory.createTitledBorder("Sector " + Integer.toHexString(selectedSector).toUpperCase()));
		mainFrame.sectorDestinationField.setText(Integer.toHexString(level.getRegionDataRecord().getSectorDestination(selectedSector)).toUpperCase());
		mainFrame.disableRegionListener = false;
	}
	public void setSectorDestination(int dest)
	{
		level.getRegionDataRecord().setSectorDestination(selectedSector, dest);
	}
	public void setEditMode(int mode)
	{
		editMode = mode;
		
		if (editMode == EDIT_LEVEL)
		{
		//	viewSectors = false;
		//	viewRegions = false;
		}
		else if (editMode == EDIT_WARPS)
		{
		//	viewSectors = true;
		//	viewRegions = true;
		}
		repaint();
	}
	public void setLevel(Level l)
	{
		// Those images really suck up ram, so delete them when they're not needed.
		if (level != null)
			level.freeImage();

		level = l;
		if (level == null)
			setPreferredSize(new Dimension(0, 0));
		else
		{
			setPreferredSize(new Dimension(0xa00, 0x300));
			selectedRegion = level.getRegionDataRecord().getRegion(0);
			selectedSector = 0;
			refreshRegionFields();
			tileSetViewer.setTileSet(selectedRegion.getTileSet());
			mainFrame.musicField.setSelected(level.getMusicId());
			mainFrame.levelField.setSelected(level.getId());
		}
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		if (level == null)
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, size().width, size().height);
		}
		else
		{
			// All tiles on the level
			g.drawImage(level.getImage(), 0, 0, null);
			
			
			if (viewSectors)
			{
				// Draw sector divisions
				for (int c=0; c<0xa; c++)
				{
					for (int r=0; r<3; r++)
					{
						int sectorNum = r*0xa + c;
						if (sectorNum == selectedSector)
							g.setColor(new Color(0, 255, 0));
						else
							g.setColor(new Color(0, 150, 0));
						Drawing.drawSquare(g, 2, c*16*16, r*16*16, 16*16);
						g.fillRect(c*16*16, r*16*16, 20, 15);
						
						g.setColor(Color.black);
						g.drawString(Integer.toHexString(sectorNum).toUpperCase(), c*16*16+1, r*16*16+13);
					}
				}
			}
			if (viewRegions)
			{
				// Draw region divisions
				for (int i=0; i<level.getRegionDataRecord().getNumRegions(); i++)
				{
					Region r = level.getRegionDataRecord().getRegion(i);
					
					if (r == selectedRegion)
						g.setColor(new Color(255, 0, 0));
					else
						g.setColor(new Color(100, 0, 0));
					Drawing.drawRect(g, 2, r.firstHSector*16*16, r.firstVSector*16*16, (r.lastHSector-r.firstHSector)*16*16, (r.lastVSector-r.firstVSector)*16*16);
				}
			}
			if (viewObjects)
			{
				// Draw objects
				for (int x=0; x<0xa0; x++)
				{
					for (int y=0; y<0x30; y++)
					{
						int obj = level.getObject(x, y);
						if (obj != 0)
						{
							drawObject(g, x, y, obj);
						}
					}
				}
				// Draw object being dragged
				if (draggingObject != 0)
					drawObject(g, cursorPos.x, cursorPos.y, draggingObject);
			}
			
			
			// Draw cursor
			g.setColor(Color.red);
			Drawing.drawSquare(g, 2, cursorPos.x*16, cursorPos.y*16, 16);
		}
	}
	
	static void drawObject(Graphics g, int x, int y, int obj)
	{
		g.setColor(Color.blue);
		g.fillRect(x*16, y*16, 16, 16);
		g.setFont(new Font("monospaced", Font.BOLD, 16));
		g.setColor(Color.black);
		if (obj == 0xf)
			g.drawString("W", x*16+3, y*16+13);
		else if (obj == 0)
		{
			g.drawLine(x*16, y*16, x*16+15, y*16+15);
			g.drawLine(x*16+15, y*16, x*16, y*16+15);
		}
		else
			g.drawString(Integer.toHexString(obj).toUpperCase(), x*16+3, y*16+13);
	}
}
