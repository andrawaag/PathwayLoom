// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.view.VPathwaySwing;

import edu.stanford.ejalbert.BrowserLauncher;


/**
 * A side panel to which Putative Pathway Parts can be added.
 */
public class PppPane extends JPanel
{
	static final String TITLE = "Loom";
	PvDesktop desktop;
	JPanel panel;
	
	private static class CopyAction extends AbstractAction
	{
		private final VPathway vPwy;
		CopyAction (VPathway vPwy)
		{
			this.vPwy = vPwy;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			putValue(NAME, "Copy");
		}
		
		public void actionPerformed(ActionEvent arg0) 
		{
			vPwy.copyToClipboard();
		}
	}
	
	/**
	 * Add a new Pathway part to the panel, with the given description displayed above it.
	 */
	public void addPart(String desc, Pathway part)
	{
		panel.removeAll();
		panel.add (new JLabel(desc));
		JScrollPane scroller =  
			new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.setMinimumSize(new Dimension(150, 150));
		VPathwaySwing wrapper = new VPathwaySwing(scroller);
		VPathway vPwy = wrapper.createVPathway();
		vPwy.setEditMode(false);
		vPwy.fromModel(part);
		vPwy.setPctZoom(66.7);
		vPwy.addVPathwayListener(desktop.getVisualizationManager());
		CopyAction a = new CopyAction(vPwy);
		wrapper.registerKeyboardAction((KeyStroke)a.getValue(Action.ACCELERATOR_KEY), a);
		scroller.add(wrapper);

		panel.add (scroller);
		
		JTabbedPane pane = desktop.getSwingEngine().getApplicationPanel().getSideBarTabbedPane();
		int index = pane.indexOfTab(TITLE);
		if (index > 0)
		{
			pane.setSelectedIndex (index);
		}
		validate();
	}
	
	/**
	 * Create a new Ppp Pane with Help button. Parts can be added later.
	 */
	public PppPane (final PvDesktop desktop)
	{
		this.desktop = desktop;
	
		setLayout (new BorderLayout());
	
		panel = new JPanel ();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add (new JScrollPane (panel), BorderLayout.CENTER);
		
		// help button
		JButton help = new JButton("Help");
		JButton clear = new JButton("Clear");
		//JButton drawDataNode = new JButton("Draw New Datanode");
		panel.add (help);
		panel.add(clear);
		
		JTextPane detailpane = new JTextPane();
		detailpane.setContentType("text/html");
		detailpane.setEditable(false);
		try {
			detailpane.setPage("file:///tmp/test.html");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		panel.add(detailpane);
		//panel.add(drawDataNode);
		
		// The following section is to experiment with adding spokes to parent pathway.
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
	    pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    pelt.setTextLabel("This is a test");
	    
		
		clear.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				new Thread (new Runnable() 
				{		
					public void run() 
					{
						try
						{
							
							//panel;
							//panel.revalidate();
						}
						catch (Exception ex)
						{
							javax.swing.SwingUtilities.invokeLater(new Runnable() 
							{		
								public void run() 
								{
									JOptionPane.showMessageDialog (desktop.getFrame(), 
											"Could not launch browser\nSee error log for details.", 
											"Error", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				}).start();
			}
		});	
		
		help.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				new Thread (new Runnable() 
				{		
					public void run() 
					{
						try
						{
							BrowserLauncher bl = new BrowserLauncher(null);
							bl.openURLinBrowser("http://www.pathvisio.org/Ppp");
						}
						catch (Exception ex)
						{
							javax.swing.SwingUtilities.invokeLater(new Runnable() 
							{		
								public void run() 
								{
									JOptionPane.showMessageDialog (desktop.getFrame(), 
											"Could not launch browser\nSee error log for details.", 
											"Error", JOptionPane.ERROR_MESSAGE);
								}
							});
						}
					}
				}).start();
			}
		});
		
	}	
}

