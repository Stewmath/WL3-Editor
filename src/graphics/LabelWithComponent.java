package graphics;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

public class LabelWithComponent extends JPanel
{
	public LabelWithComponent(Component label, Component comp)
	{
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(label);
		add(comp);
	}
}
