package viewers;

import base.TileSet;
import viewerclients.TileSetViewerClient;
import graphics.*;

import javax.swing.JPanel;
import java.awt.*;
import javax.swing.*;

import java.awt.event.*;
import java.awt.image.*;

public class TileSetViewer extends TileGridViewer {

	TileSet tileSet;
	
	public TileSetViewer(TileGridViewerClient c) {
		super(c);
	}

	public TileSetViewer(TileSet t, TileGridViewerClient c) {
		super(c);
		setTileSet(t);
	}
	
	public void setTileSet(TileSet t)
	{
		tileSet = t;
		if (t != null) {
			setTiles(tileSet.getTileImages(), 8, 1);
		}
	}

	public TileSet getTileSet() {
		return tileSet;
	}
	
	public void deselect()
	{
		selectedTile = -1;
	}
}
