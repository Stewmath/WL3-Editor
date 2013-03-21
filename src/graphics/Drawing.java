package graphics;

import record.RomReader;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;


public class Drawing {

	static RomReader rom;
	public static int rgbToInt(int r, int g, int b)
	{
		int rgb = r;
		rgb = (rgb<<8) + g;
		rgb = (rgb<<8) + b;
		return rgb;
	}
	public static int[] intToRgb(int rgb)
	{
		int[] c = new int[3];
		c[0] = (rgb>>10)&0x1f;
		c[1] = (rgb>>5)&0x1f;
		c[2] = rgb&0x1f;
		return c;
	}

	public static void drawSquare(Graphics g, int outlineWidth, int x, int y, int width)
	{
		for (int i=0; i<outlineWidth; i++)
		{
			g.drawRect(x+i, y+i, width-1-2*i, width-1-2*i);
		}
	}
	public static void drawRect(Graphics g, int outlineWidth, int x, int y, int width, int height)
	{
		for (int i=0; i<outlineWidth; i++)
		{
			g.drawRect(x+i, y+i, width-1-2*i, height-1-2*i);
		}
	}

	public static void addComponent(JPanel parent, Component child)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.add(child);
		p.add(Box.createHorizontalGlue());
		parent.add(p);
	}


	public static int[][] defaultPalette = {
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
		{
			Drawing.rgbToInt(255,255,255),
			Drawing.rgbToInt(191,191,191),
			Drawing.rgbToInt(92, 92, 92),
			Drawing.rgbToInt(0,0,0)
		},
	};
}
