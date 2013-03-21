package viewers;

import viewerclients.ObjectSetViewerClient;
import graphics.Drawing;

import javax.swing.*;


import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;

public class ObjectSetViewer extends JPanel {
	
	final int cols=4;
	final int rows=16/cols;
	
	int startX=0, startY=0;
	Point cursorPos = new Point(-1,-1);
	
	ObjectSetViewerClient client;
	BufferedImage viewerImage;
	int selectedObject=-1;
	
	public ObjectSetViewer(ObjectSetViewerClient c)
	{
		client = c;
		
		setPreferredSize(new Dimension(16*cols, rows*16));
		viewerImage = new BufferedImage(16*cols, 16*rows, BufferedImage.TYPE_3BYTE_BGR);
		
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e)
			{
				if (e.getX()-startX >= 0 && e.getY()-startY >= 0) {
					int x = (e.getX()-startX)/16;
					int y = (e.getY()-startY)/16;
					if (x < cols && x >= 0 && y < rows && y >= 0)
						setSelectedObject(x+y*cols);
				}
			}
			public void mouseExited(MouseEvent e) {
				cursorPos.x = -1;
				repaint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int pixelsX = e.getX()-startX;
				int pixelsY = e.getY()-startY;
				cursorPos.x = pixelsX/16;
				cursorPos.y = pixelsY/16;
				if (cursorPos.x >= cols || cursorPos.y >= rows || pixelsX < 0 || pixelsY < 0)
					cursorPos.x = -1;
				repaint();
			}
		});
		
		generateImage();
	}
	
	public void setSelectedObject(int obj)
	{
		if (obj < 16)
		{
			selectedObject = obj;
			if (client != null)
				client.objectSetSelectionChanged(obj);
			repaint();
		}
	}
	
	public void generateImage()
	{
		viewerImage = new BufferedImage(16*cols, 16*rows, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = viewerImage.getGraphics();
		
		for (int y=0; y<rows; y++)
		{
			for (int x=0; x<cols; x++)
			{
				int i = x+y*cols;
				LevelViewer.drawObject(g, x, y, i);
			}
		}
	}
	
	public void paintComponent(Graphics g)
	{
		g.setColor(this.getBackground());
		g.fillRect(0, 0, size().width, size().height);
		startX = (size().width-getPreferredSize().width)/2;
		startY = (size().height-getPreferredSize().height)/2;
		g.drawImage(viewerImage, startX, startY, null);
		
		g.setColor(Color.red);
		if (cursorPos.x >= 0)
			Drawing.drawSquare(g, 1, cursorPos.x*16+startX, cursorPos.y*16+startY, 16);
		g.setColor(Color.white);
		if (selectedObject >= 0 && selectedObject < 16)
			Drawing.drawSquare(g, 1, (selectedObject%cols)*16+startX, (selectedObject/cols)*16+startY, 16);
	}
}
