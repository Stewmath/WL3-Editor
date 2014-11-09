package dialogs;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import base.*;

import javax.swing.*;

import record.*;

public class CompareLevelDialog extends JDialog {

	RomReader rom;
	
	MoveableDataRecord[] record1 = new MoveableDataRecord[100];
	MoveableDataRecord[] record2 = new MoveableDataRecord[100];
	int numEntries = 0;
	
	Level l1 = null;
	Level l2;
	
	public CompareLevelDialog(JFrame parent, Level level1, Level level2)
	{
		super(parent, "Compare levels " + RomReader.toHexString(level1.getId(), 2) + " and " + RomReader.toHexString(level2.getId(), 2), 
				Dialog.ModalityType.APPLICATION_MODAL);
		rom = RomReader.rom;
		
		this.l1 = level1;
		this.l2 = level2;
		
		if (l1.getId() == l2.getId()) {
			JOptionPane.showMessageDialog(
					this,
					"You can't compare a level with itself!",
					"Compare levels",
					JOptionPane.ERROR_MESSAGE
					);
			return;
		}
		if (l1.getId()/8 != l2.getId()/8)
		{
			JOptionPane.showMessageDialog(
					this,
					"You should only compare different versions of the same level.",
					"Compare levels",
					JOptionPane.WARNING_MESSAGE
					);
		}
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		contentPane.add(new JLabel("All operations will be applied to level " + RomReader.toHexString(l1.getId(), 2) + "."));
		contentPane.add(Box.createRigidArea(new Dimension(0, 5)));
		
		JPanel levelDataPanel = new JPanel();
		levelDataPanel.setLayout(new BoxLayout(levelDataPanel, BoxLayout.Y_AXIS));
		JButton actionButton = new JButton("Action");
		if (l1.getLevelDataRecord() == l2.getLevelDataRecord())
		{
			levelDataPanel.add(new JLabel("These levels use the same leveldata (tiles & objects)."));
			actionButton.setText("Separate (L-" + RomReader.toHexString(l1.getId(), 2) + " will be isolated)");
			actionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.makeNewLevelData();
					setVisible(false);
				}
			});
		}
		else
		{
			levelDataPanel.add(new JLabel("These levels use different leveldata (tiles & objects)."));
			actionButton.setText("Merge (L-" + RomReader.toHexString(l1.getId(), 2) + " will reference L-" + RomReader.toHexString(l2.getId(), 2) + ")");
			actionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.mergeLevelDataWith(l2);
					setVisible(false);
				}
			});
		}
		levelDataPanel.add(actionButton);
		levelDataPanel.setBorder(BorderFactory.createTitledBorder("Leveldata"));

		// Warp data
		JPanel warpDataPanel = new JPanel();
		warpDataPanel.setLayout(new BoxLayout(warpDataPanel, BoxLayout.Y_AXIS));
		JButton warpButton = new JButton("Action");
		if (l1.getRegionDataRecord() == l2.getRegionDataRecord())
		{
			warpDataPanel.add(new JLabel("These levels use the same warpdata (regions)."));
			warpButton.setText("Separate");
			warpButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.makeNewRegionData();
					setVisible(false);
				}
			});
		}
		else
		{
			warpDataPanel.add(new JLabel("These levels use different warpdata (regions)."));
			warpButton.setText("Merge (L-" + RomReader.toHexString(l1.getId(), 2) + " will copy L-" + RomReader.toHexString(l2.getId(), 2) + ")");
			warpButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					if (l1.getId()/0x64 != l2.getId()/0x64) {
						JOptionPane.showMessageDialog(null,
								"Can't merge these regions (banks differ).",
								"Error",
								JOptionPane.ERROR_MESSAGE);
						setVisible(false);
						return;
					}
					l1.setRegionDataRecord(l2.getRegionDataRecord());
					setVisible(false);
				}
			});
		}
		warpDataPanel.add(warpButton);
		warpDataPanel.setBorder(BorderFactory.createTitledBorder("Warpdata"));
		
		contentPane.add(levelDataPanel);
		contentPane.add(warpDataPanel);
		add(contentPane);
		pack();
		setVisible(true);
	}
	
	JPanel makeComparisonPanel(String title, MoveableDataRecord r1, MoveableDataRecord r2)
	{
		record1[numEntries] = r1;
		record2[numEntries] = r2;
		
		JPanel panel = new JPanel();
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		if (r1.getAddr() == r2.getAddr())
			panel.add(new JLabel("These levels use the same " + title.toLowerCase() + "."));
		else
			panel.add(new JLabel("These levels use different " + title.toLowerCase() + "."));
		
		JPanel subPanel = new JPanel();
		JButton shallowButton = new JButton("Shallow");
		shallowButton.setActionCommand(""+numEntries);
		shallowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int i=Integer.parseInt(e.getActionCommand());
				
				//if (r1
			}
		});
		subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
		subPanel.add(shallowButton);
		
		panel.add(subPanel);
		panel.setBorder(BorderFactory.createTitledBorder(title));
		
		numEntries++;
		return panel;
	}
}
