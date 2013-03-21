package dialogs;

import graphics.*;

import javax.swing.*;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddRegionDialog extends JDialog {
	
	JTextField sectorField;
	JButton okButton;
	
	public AddRegionDialog(JFrame parent)
	{
		super(parent, "Add Region", Dialog.ModalityType.APPLICATION_MODAL);
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		JLabel label1 = new JLabel("If nothing warps to this region,");
		JLabel label2 = new JLabel("it will not be saved.");
		label1.setAlignmentX(Component.CENTER_ALIGNMENT);
		label2.setAlignmentX(Component.CENTER_ALIGNMENT);
		contentPane.add(label1);
		contentPane.add(label2);
		
		sectorField = new JTextField();

		contentPane.add(new LabelWithComponent(
				new JLabel("Top-left sector: "),
				sectorField
				));
		
		add(contentPane);
		pack();
		setVisible(true);
	}
}
