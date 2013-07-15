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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.jdesktop.swingworker.SwingWorker;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.gpmldiff.PwyDoc;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.GpmlFormat;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.view.GeneProduct;
import org.pathvisio.core.view.VPathwayElement;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.gui.PathwayElementMenuListener.PathwayElementMenuHook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Putative Parts plug-in - 
 * shows a sidebar where other plug-ins can add suggestions.
 * 
 * Also hooks into the right-click menu
 */
public class PppPlugin implements Plugin, PathwayElementMenuHook
{
	public static final double DATANODE_MWIDTH = 60;
	public static final double DATANODE_MHEIGHT = 20;

	private PvDesktop desktop;
	PppPane pane;
	SuggestionAction openPhactsActionCompoundByGene;
	SuggestionAction conceptWikiSparql;
	SuggestionAction stitchSparql;
	SuggestionAction hmdbPppAction;
	SuggestionAction phasarPppAction;
	SuggestionAction keggGeneByEnzyme;
	SuggestionAction keggEnzymeByGene;
	SuggestionAction keggEnzymeByCompound;
	SuggestionAction localPppAction;
	SuggestionAction bindPppAction;
	SuggestionAction whatizitPppAction;
	SuggestionAction cwAffects;
	SuggestionAction cwDisrupts;
	SuggestionAction cwExhibits;
	SuggestionAction pathwayCommonsAll;
	SuggestionAction pathwayCommonsBiogrid;
	SuggestionAction pathwayCommonsCell_Map;
	SuggestionAction pathwayCommonsHprd;
	SuggestionAction pathwayCommonsHumancyc;
	SuggestionAction pathwayCommonsIntact;
	SuggestionAction pathwayCommonsMint;
	SuggestionAction pathwayCommonsNci_Nature;
	SuggestionAction pathwayCommonsReactome;
	SuggestionAction wikiPathwaysAction;
	SuggestionAction openPhactsSparql;
	SuggestionAction localInteractionGpml;
	SuggestionAction openPhactsCompoundPharmaAPI;

	ArrayList<SuggestionAction> cwSAList;



	/**
	 * return the existing PppPane
	 */
	public PppPane getPane()
	{
		return pane;
	}

	/**
	 * Initialize plug-in, is called by plugin manager.
	 */
	public void init(PvDesktop desktop) 
	{

		// create new PppPane and add it to the side bar.
		pane = new PppPane(desktop);
		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
		sidebarTabbedPane.add(PppPane.TITLE, pane);
		// register our pathwayElementMenu hook.
		desktop.addPathwayElementMenuHook(this);
		this.desktop = desktop;
		GdbManager gdbManager = desktop.getSwingEngine().getGdbManager();
		hmdbPppAction = new SuggestionAction(this, "HMDB", new HmdbPppPlugin(gdbManager));
		keggGeneByEnzyme = new SuggestionAction(this, "Kegg (Gene by enzyme)", new KeggPppPlugin_getGenesByEnzymes(gdbManager));
		keggEnzymeByGene = new SuggestionAction(this, "Kegg (Enzyme by gene)", new KeggPppPlugin_getEnzymesByGene(gdbManager));
		keggEnzymeByCompound = new SuggestionAction(this, "Kegg (Enzyme by compound)", new KeggPppPlugin_getEnzymesByCompound(gdbManager));
		localPppAction = new SuggestionAction(this, "WikiPathways (local)", new LocalPathways());
		pathwayCommonsAll = new SuggestionAction(this, "All", new PathwayCommonsPppPlugin(gdbManager, PathwayCommonsPppPlugin.SOURCE_ALL));
		pathwayCommonsBiogrid = new SuggestionAction(this, "BIOGRID", new PathwayCommonsPppPlugin(gdbManager, "BIOGRID"));
		pathwayCommonsCell_Map = new SuggestionAction(this, "CELL_MAP", new PathwayCommonsPppPlugin(gdbManager, "CELL_MAP"));
		pathwayCommonsHprd = new SuggestionAction(this, "HPRD", new PathwayCommonsPppPlugin(gdbManager, "HPRD"));
		pathwayCommonsHumancyc = new SuggestionAction(this, "HUMAN_CYC", new PathwayCommonsPppPlugin(gdbManager, "HUMANCYC"));
		pathwayCommonsIntact = new SuggestionAction(this, "INTACT", new PathwayCommonsPppPlugin(gdbManager, "INTACT"));
		pathwayCommonsMint = new SuggestionAction(this, "MINT", new PathwayCommonsPppPlugin(gdbManager, "MINT"));
		pathwayCommonsNci_Nature = new SuggestionAction(this, "NCI_NATURE", new PathwayCommonsPppPlugin(gdbManager, "NCI_NATURE"));
		pathwayCommonsReactome = new SuggestionAction(this, "REACTOME", new PathwayCommonsPppPlugin(gdbManager, "REACTOME"));
		bindPppAction = new SuggestionAction(this, "Bind", new BindPppPlugin(gdbManager));
		whatizitPppAction = new SuggestionAction(this, "Whatizit", new WhatizitPppPlugin(gdbManager));
		wikiPathwaysAction = new SuggestionAction(this, "WikiPathways", new WikiPathwaysPppPlugin(gdbManager));
		conceptWikiSparql = new SuggestionAction(this, "ConceptWiki", new ConceptWikiSparqlPppPlugin(gdbManager));
		openPhactsSparql = new SuggestionAction(this, "Find compound by Gene", new OpenPhactsPppPlugin(gdbManager));
		stitchSparql = new SuggestionAction(this, "Find suggestions", new StitchSparqlPppPlugin(gdbManager));
		localInteractionGpml = new SuggestionAction(this, "Local Gpml Interaction", new LocalInteractionGpmlPppPlugin(gdbManager));
		openPhactsCompoundPharmaAPI = new SuggestionAction(this, "Get Targets of Compound", new OpenPhactsApiPlugin(gdbManager));
	}

	public void done() {}

	/**
	 * Action to be added to right-click menu.
	 * This action is recycled, i.e. it's instantiated only once but
	 * added each time to the new popup Menu.
	 */




	private class SuggestionAction extends AbstractAction
	{
		private final PppPlugin parent;
		private final String name;

		GeneProduct elt;

		/**
		 * set the element that will be used as input for the suggestion.
		 * Call this before adding to the menu. 
		 */
		void setElement (GeneProduct anElt)
		{
			elt = anElt;
			setEnabled (suggestion.canSuggest(elt.getPathwayElement()));
		}

		private final Suggestion suggestion;

		/** called when plug-in is initialized */
		SuggestionAction(PppPlugin aParent, String name, Suggestion suggestion)
		{
			parent = aParent;
			this.suggestion = suggestion;
			this.name = name;
			putValue(NAME, name + " Suggestions");
		}

		/** called when user clicks on the menu item */



		public void actionPerformed(ActionEvent e) 
		{
			final PppPane pane = parent.getPane();
			//pane.removeAll();
			//pane.validate();
			final ProgressKeeper pk = new ProgressKeeper();
			final ProgressDialog pd = new ProgressDialog(desktop.getFrame(), "Querying " +  name, pk, true, true);
			pk.setTaskName("Running query");

			SwingWorker<Pathway, Void> worker = new SwingWorker<Pathway, Void>()
			{

				@Override
				protected Pathway doInBackground() throws Exception 
				{
					Pathway result = suggestion.doSuggestion(elt.getPathwayElement());
					return result;
				}

				@Override
				protected void done()
				{
					if (pk.isCancelled()) return; // don't add if user pressed cancel.
					try {
						pane.addPart("Putative pathway part: " + name, get());
					} 
					catch (InterruptedException e) 
					{
						Logger.log.error("Operation interrupted", e);
						JOptionPane.showMessageDialog(
								pane, "Operation interrupted",
								"Error", JOptionPane.ERROR_MESSAGE
						);
					} 
					catch (ExecutionException e) 
					{
						// exception generated by the suggestion implementation itself
						Throwable cause = e.getCause();
						cause.printStackTrace();
						Logger.log.error("Unable to get suggestions", cause);
						JOptionPane.showMessageDialog(
								pane, "Unable to get suggestions: " + cause.getMessage(),
								"Error", JOptionPane.ERROR_MESSAGE
						);
					}
					pk.finished();
				}
			};
			worker.execute();
			pd.setVisible(true);
		}
	}

	/**
	 * callback, is called when user clicked with RMB on a pathway element.
	 * @throws IOException 
	 * @throws ConverterException 
	 */

		


	public void pathwayElementMenuHook(final VPathwayElement e, JPopupMenu menu) 
	{
        
		if (e instanceof GeneProduct)
		{
			JMenu submenu = new JMenu("Pathway Loom");
			JMenuItem titleMenu = submenu.add("Pathway Loom BETA");
			titleMenu.setEnabled(false);
			titleMenu.setFont(new Font("sansserif", Font.BOLD, 16));
			titleMenu.setBackground(Color.white);
			titleMenu.setForeground(Color.gray);
			//JMenuItem interactionMenu = submenu.add("Find interaction suggestions");
			//interactionMenu.setEnabled(false);
			//interactionMenu.setBackground(Color.orange);
			//interactionMenu.setForeground(Color.yellow);	

			/*JMenuItem undirectedInteractionMenu = new JMenuItem("Undirected Interaction");
			submenu.add(undirectedInteractionMenu);
			JMenuItem directedInteractionMenu = new JMenuItem("Directed Interaction", new ImageIcon("newinteraction.gif"));
			submenu.add(directedInteractionMenu);
			JMenuItem necessaryStimulationMenu = new JMenuItem("Necessary Stimulation");
			submenu.add(necessaryStimulationMenu);

			JMenuItem catalysisMenu = new JMenuItem("Catalysis");
			submenu.add(catalysisMenu);
			JMenu conversionMenu = new JMenu("Conversion");
			submenu.add(conversionMenu);
			JMenu bindingMenu = new JMenu("Binding");
			submenu.add(bindingMenu);
			JMenu stimulationMenu = new JMenu("Stimulation");
			submenu.add(stimulationMenu);
			submenu.addSeparator();
			*/
			JMenuItem WikiPathwaysMenu = submenu.add("WikiPathways");
			WikiPathwaysMenu.setEnabled(false);
			WikiPathwaysMenu.setBackground(Color.orange);
			WikiPathwaysMenu.setForeground(Color.yellow);
			//submenu.add("Get interaction suggestions");
			wikiPathwaysAction.setElement((GeneProduct) e);
			submenu.add(wikiPathwaysAction);
			JMenuItem pathwayCommonsMenu = submenu.add("Pathway Commons");
			pathwayCommonsMenu.setEnabled(false);
			pathwayCommonsMenu.setBackground(Color.orange);
			pathwayCommonsMenu.setForeground(Color.yellow);
			
			pathwayCommonsAll.setElement((GeneProduct) e);
			pathwayCommonsBiogrid.setElement((GeneProduct) e);
			pathwayCommonsCell_Map.setElement((GeneProduct) e);
			pathwayCommonsHprd.setElement((GeneProduct) e);
			pathwayCommonsHumancyc.setElement((GeneProduct) e);
			pathwayCommonsIntact.setElement((GeneProduct) e);
			pathwayCommonsMint.setElement((GeneProduct) e);
			pathwayCommonsNci_Nature.setElement((GeneProduct) e);
			pathwayCommonsReactome.setElement((GeneProduct) e);
			submenu.add(pathwayCommonsAll);
			submenu.add(pathwayCommonsBiogrid);
			submenu.add(pathwayCommonsCell_Map);
			submenu.add(pathwayCommonsHprd);
			submenu.add(pathwayCommonsHumancyc);
			submenu.add(pathwayCommonsIntact);
			submenu.add(pathwayCommonsMint);
			submenu.add(pathwayCommonsNci_Nature);
			submenu.add(pathwayCommonsReactome);
			
			JMenuItem localDataMenu = submenu.add("Open PHACTS");
			localDataMenu.setEnabled(false);
			localDataMenu.setBackground(Color.orange);
			localDataMenu.setForeground(Color.yellow);
			submenu.add(openPhactsCompoundPharmaAPI);
			//submenu.add("Get target compounds");
			

			
		
			
			Icon opsIcon = new ImageIcon("favicon.gif");
			/*openPhactsMenu.setIcon(opsIcon);
			webserviceDataMenu.add(wikiPathwaysAction);
			semanticWebDataMenu.add(openPhactsMenu);
			openPhactsSparql.setElement((GeneProduct)e);
			openPhactsMenu.add(openPhactsSparql);
			conceptWikiSparql.setElement((GeneProduct) e);
			semanticWebDataMenu.add(conceptWikiSparql);
			stitchSparql.setElement((GeneProduct) e);
			semanticWebDataMenu.add(stitchSparql);
			hmdbPppAction.setElement((GeneProduct) e);
			webserviceDataMenu.add(hmdbPppAction);
			whatizitPppAction.setElement((GeneProduct) e);
			webserviceDataMenu.add(whatizitPppAction);

           */
			JMenu kegg = new JMenu("KEGG");
			keggEnzymeByCompound.setElement((GeneProduct) e);
			keggEnzymeByGene.setElement((GeneProduct) e);
			keggGeneByEnzyme.setElement((GeneProduct) e);
			kegg.add(keggEnzymeByCompound);
			kegg.add(keggEnzymeByGene);
			kegg.add(keggGeneByEnzyme);
			//webserviceDataMenu.add(kegg);
			

			
			JMenuItem importInteractionData = new JMenuItem ("Import GPML Interaction Data....");
			localInteractionGpml.setElement((GeneProduct) e);
			//submenu.add(importInteractionData);
			
			JMenuItem importWPRdfInteractionData = new JMenuItem ("Import RDF Interaction Data....");
			localInteractionGpml.setElement((GeneProduct) e);
			//submenu.add(importWPRdfInteractionData);
			
			/*JMenuItem preferencesPL = new JMenuItem("Preferences...");
                submenu.add(preferencesPL);
                preferencesPL.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub
						 JOptionPane.showMessageDialog(null, "");


					}

                });*/
			menu.add(submenu);


		}

	}


}

