package viewers;

import graphics.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class TileGridViewer extends JPanel {

	TileGridViewerClient client;
	BufferedImage viewerImage;
	int selectedTile=0;
	Point cursorPos = new Point(-1, -1);
	Dimension preferredSize = new Dimension(0, 0);
	boolean initialized = false;
	boolean selectable = true;
	Color selectionColor = Color.white;
	Color hoverColor = Color.red;

	BufferedImage[] tiles;
	int tileWidth, tileHeight;
	int numTilesX, numTilesY;

	// The point on the panel where the top-left of the image is placed;
	// It's used to center it in the panel.
	int offsetX, offsetY;

	public final static int NUM_ARRANGEMENTS=2;
	public final static int ARRANGEMENT_NORMAL=0;
	public final static int ARRANGEMENT_SPRITE=1;

	int arrangement = ARRANGEMENT_NORMAL;

	
	public TileGridViewer(TileGridViewerClient c) {
		client = c;
	}
	public TileGridViewer(BufferedImage[] t, int width, int scale, TileGridViewerClient c)
	{
		client = c;
		setTiles(t, width, scale);
	}

	void init() {
		if (!initialized) {
			initialized = true;
			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e)
				{
					setCursorPos(e);

					if (cursorPos.x < numTilesX && cursorPos.y < numTilesY)
					{
						setSelectedTile(getCursorTile());

						repaint();
					}
				}
				public void mouseExited(MouseEvent e) {
					cursorPos.x = -1;
					repaint();
					if (client != null)
					client.tileHoverChanged(-1);
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseMoved(MouseEvent e) {
					setCursorPos(e);
					repaint();
					if (client != null) {
						client.tileHoverChanged(getCursorTile());
					}
				}
			});
		}
	}

	int getCursorTile() {
		switch(arrangement) {
			case ARRANGEMENT_NORMAL:
				return cursorPos.y*numTilesX+cursorPos.x;
			case ARRANGEMENT_SPRITE:
				{
					int c = cursorPos.x*2;
					int r = cursorPos.y;
					if (r%2 == 1)
						c++;
					r -= r%2;
					if (c >= numTilesX) {
						c -= numTilesX;
						r++;
					}
					return r*numTilesX+c;
				}
		}
		return 0;
	}

	public void setTiles(BufferedImage[] t, int width, int scale) {
		init();

		tiles = t;
		tileWidth = tiles[0].getWidth()*scale;
		tileHeight = tiles[0].getHeight()*scale;
		numTilesX = width;
		numTilesY = tiles.length/numTilesX;
		if (tiles.length%numTilesX != 0)
			numTilesY++;

		preferredSize = new Dimension(numTilesX*tileWidth, numTilesY*tileHeight);
		setPreferredSize(preferredSize);

		generateImage();

		revalidate();
	}

	void setCursorPos(MouseEvent e) {
		cursorPos.x = (e.getX()-offsetX)/tileWidth;
		cursorPos.y = (e.getY()-offsetY)/tileHeight;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public void setSelectionColor(Color c) {
		selectionColor = c;
	}

	public int getSelectedTile() {
		return selectedTile;
	}
	
	public void setSelectedTile(int t)
	{
		if (selectedTile != t) {
			selectedTile = t;
			if (client != null)
				client.tileSelectionChanged(selectedTile);
			repaint();
		}
	}

	public int getArrangement() {
		return arrangement;
	}
	
	public void setArrangement(int a) {
		arrangement = a;
		generateImage();
	}
	
	public void deselect()
	{
		selectedTile = -1;
	}
	
	void generateImage()
	{
		viewerImage = new BufferedImage(preferredSize.width, preferredSize.height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = viewerImage.getGraphics();
		
		for (int r=0; r<numTilesY; r++)
		{
			for (int c=0; c<numTilesX; c++)
			{
				int id = r*numTilesX + c;

				if (id < tiles.length) {
					switch(arrangement) {
						case ARRANGEMENT_NORMAL:
							g.drawImage(tiles[id], c*tileWidth, r*tileHeight, tileWidth, tileHeight, null);
							break;
						case ARRANGEMENT_SPRITE:
							{
								int r2 = (r-r%2)+(c%2);
								int c2 = (c/2) + (r%2 == 1 ? numTilesX/2 : 0);
								g.drawImage(tiles[id], c2*tileWidth, r2*tileHeight, tileWidth, tileHeight, null);
							}
							break;
					}
				}
			}
		}
		repaint();
	}

	public void paintComponent(Graphics g)
	{
		if (initialized) {
			offsetX = (getSize().width-preferredSize.width)/2;
			offsetY = (getSize().height-preferredSize.height)/2;

			g.setColor(getBackground());
			g.fillRect(0, 0, size().width, size().height);
			g.drawImage(viewerImage, offsetX, offsetY, null);

			// Draw cursor
			g.setColor(hoverColor);
			Drawing.drawRect(g, 1, cursorPos.x*tileWidth+offsetX, cursorPos.y*tileHeight+offsetY, tileWidth, tileHeight);
			// Draw selected part
			if (selectable) {
				int r = selectedTile/numTilesX;
				int c = selectedTile%numTilesX;
				if (arrangement == ARRANGEMENT_SPRITE) {
					int r2 = (r-r%2)+(c%2);
					int c2 = (c/2) + (r%2 == 1 ? numTilesX/2 : 0);
					r = r2;
					c = c2;
				}
				g.setColor(selectionColor);
				Drawing.drawRect(g, 1, c*tileWidth, r*tileHeight, tileWidth, tileHeight);
			}
		}
		else {
			super.paintComponent(g);
		}
	}
}
