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
		if (l1.getLevelDataAddr() == l2.getLevelDataAddr() && l1.getLevelDataAddr() >= 0)
		{
			levelDataPanel.add(new JLabel("These levels use the same leveldata (tiles & objects)."));
			actionButton.setText("Separate");
			actionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.getNewLevelData();
				//	l1.tileLayout = rom.getMoveableDataRecord(l2.tileLayout.toArray(), levelDataAddr, levelDataAddr+2, true);
				//	l1.objectLayout = rom.getMoveableDataRecord(l2.objectLayout.toArray(), levelDataAddr+3, levelDataAddr+2, true);
				//	l1.layoutRecord = rom.getJoinedRecord(l1.tileLayout, l1.objectLayout);
				//	l1.save();
					setVisible(false);
				}
			});
		}
		else
		{
			levelDataPanel.add(new JLabel("These levels use different leveldata (tiles & objects)."));
			actionButton.setText("Merge (L-" + RomReader.toHexString(l1.getId(), 2) + " will copy L-" + RomReader.toHexString(l2.getId(), 2) + ")");
			actionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.copyLevelData(l2);
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
		if (l1.regionDataRecord == l2.regionDataRecord)
		{
			warpDataPanel.add(new JLabel("These levels use the same warpdata (regions)."));
			warpButton.setText("Separate");
			warpButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					l1.setRegionDataRecord(RegionRecord.getCopy(l2.getRegionDataRecord(), null));
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
