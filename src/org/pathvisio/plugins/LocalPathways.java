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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.Xref;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.LineType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.GraphLink.GraphRefContainer;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.PathwayElement.MPoint;
import org.pathvisio.core.preferences.GlobalPreference;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.FileUtils;

/**
 * Make suggestions based on relationships in a Local set of Pathways.
 * 
 * This will read pathways from a local directory and make suggestions based on that.
 * By default, the directory used is ~/PathVisio-Data/wikipathways,
 * but this can be changed by adding the line PPP_PATHWAY_SET=/path/to/pathways
 * in the PathVisio preferences file (~/.PathVisio/.PathVisio)
 * 
 * TODO: add a GUI option for changing this preference
 */
public class LocalPathways extends SuggestionAdapter
{
	/** Preferences for this plug-in */
	enum PppPrefs implements Preference
	{
		PPP_PATHWAY_SET (new File(GlobalPreference.getDataDir().toString(), "wikipathways").toString());

		PppPrefs (String defaultValue) 
		{
			this.defaultValue = defaultValue;
		}
		
		private String defaultValue;
		
		public String getDefault() {
			return defaultValue;
		}
		
		public void setDefault(String defValue) {
			defaultValue = defValue;
		}
	};
	
	@Override public boolean canSuggest(PathwayElement input) 
	{
		return true;
	}

	// lazily initialized
	private static RelationshipIndexer indexer = null;
	
	@Override public Pathway doSuggestion(PathwayElement input)
			throws SuggestionException 
	{
		if (indexer == null)
		{
			indexer = new RelationshipIndexer(
					PreferenceManager.getCurrent().getFile(PppPrefs.PPP_PATHWAY_SET)
				);
		}
		
		Xref ref = input.getXref();
		RelationshipIndexer.Relation r = indexer.relations.get(ref);
		
		PathwayElement hub = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    hub.setMWidth (PppPlugin.DATANODE_MWIDTH);
	    hub.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    hub.setTextLabel(input.getTextLabel());
	    hub.setDataSource(input.getDataSource());
	    hub.setGeneID(input.getGeneID());
	    hub.setCopyright("Human metabolome database (http://www.hmdb.ca)");
	    hub.setDataNodeType(input.getDataNodeType());

	    List<PathwayElement> spokes = new ArrayList<PathwayElement>();
		
		for (PathwayElement pelt : r.getLefts())	
		{
			PathwayElement pchildElt = pelt.copy();
		    pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    	spokes.add (pchildElt);
		}

		for (PathwayElement pelt : r.getMediators())	
		{
			PathwayElement pchildElt = pelt.copy();
		    pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    	spokes.add (pchildElt);
		}
		for (PathwayElement pelt : r.getRights())	
		{
			PathwayElement pchildElt = pelt.copy();
		    pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    	spokes.add (pchildElt);
		}
			
	    Pathway result = PathwayBuilder.radialLayout(hub, spokes);
		return result;
		
	}

	
	/** copied from org.pathvisio.indexer.RelationshipIndexer.
	 * TODO: refactor common code
	 */
	public class RelationshipIndexer 
	{
		public RelationshipIndexer(File pwDir) 
		{
			List<File> files = FileUtils.getFiles(pwDir, "gpml", true);
			indexFiles (files);
		}
		
		public void indexFiles(List<File> files)
		{
			for (File f : files)
			{
				try
				{
					Pathway p = new Pathway();
					p.readFromXml(f, true);
					indexPathway (p);
				}
				catch (ConverterException ex)
				{
					Logger.log.warn ("Could not read pathway ", ex);
				}
				catch (IOException ex)
				{
					Logger.log.warn ("Could not read pathway ", ex);
				}
			}
		}
		
		public void indexPathway(Pathway pathway) throws IOException
		{
			//Find all connectors that do not connect to an anchor
			for(PathwayElement pe : pathway.getDataObjects()) {
				if(isRelation(pathway, pe)) {
					indexRelationship(pe);
				}
			}		
		}
		
		Map<Xref, Relation> relations = new HashMap<Xref, Relation>();
		
		void indexRelationship(PathwayElement relation) throws IOException 
		{
			Relation r = new Relation(relation);
			for (Xref ref : r.getRefs())
			{
				relations.put (ref, r);
			}			
		}
		
//		void addElements(String field, Collection<PathwayElement> elms) {
//			for(PathwayElement e : elms) {
//				addElement(field, e);
//			}
//		}
//		
//		void addElement(String field, PathwayElement pe) {
//			//We need something to identify the element
//			//for now, use textlabel
//			//TODO: use stable pathway id + graphid!
//			String text = pe.getTextLabel();
//			if(text != null) {
//				doc.add(new Field(
//						field, text, Field.Store.YES, Field.Index.TOKENIZED
//				));
//			} else {
//				Logger.log.error(
//						"Unable to add " + pe + " to relationship index: no text label"
//				);
//			}
//		}
		
		boolean isRelation(Pathway pathway, PathwayElement pe) {
			if(pe.getObjectType() == ObjectType.LINE) {
				MPoint s = pe.getMStart();
				MPoint e = pe.getMEnd();
				if(s.isLinked() && e.isLinked()) {
					//Objects behind graphrefs should be PathwayElement
					//so not MAnchor
					if(pathway.getElementById(s.getGraphRef()) != null &&
							pathway.getElementById(e.getGraphRef()) != null)
					{
						return true;
					}
				}
			}
			return false;
		}
		
		class Relation {
			private Set<PathwayElement> lefts = new HashSet<PathwayElement>();
			private Set<PathwayElement> rights = new HashSet<PathwayElement>();
			private Set<PathwayElement> mediators = new HashSet<PathwayElement>();
			private Set<Xref> refs = new HashSet<Xref>();
			
			public Set<Xref> getRefs()
			{
				return refs;
			}
			
			public Relation(PathwayElement relationLine) {
				if(relationLine.getObjectType() != ObjectType.LINE) {
					throw new IllegalArgumentException("Object type should be line!");
				}
				Pathway pathway = relationLine.getParent();
				if(pathway == null) {
					throw new IllegalArgumentException("Object has no parent pathway");
				}
				//Add obvious left and right
				addLeft(pathway.getElementById(
						relationLine.getMStart().getGraphRef()
				));
				addRight(pathway.getElementById(
						relationLine.getMEnd().getGraphRef()
				));
				//Find all connecting lines (via anchors)
				for(MAnchor ma : relationLine.getMAnchors()) {
					for(GraphRefContainer grc : ma.getReferences()) {
						if(grc instanceof MPoint) {
							MPoint mp = (MPoint)grc;
							PathwayElement line = mp.getParent();
							if(line.getMStart() == mp) {
								//Start linked to anchor, make it a 'right'
								if(line.getMEnd().isLinked()) {
									addRight(pathway.getElementById(line.getMEnd().getGraphRef()));
								}
							} else {
								//End linked to anchor
								if(line.getEndLineType() == LineType.LINE) {
									//Add as 'left'
									addLeft(pathway.getElementById(line.getMStart().getGraphRef()));
								} else {
									//Add as 'mediator'
									addMediator(pathway.getElementById(line.getMStart().getGraphRef()));
								}
							}
						} else {
							Logger.log.warn("unsupported GraphRefContainer: " + grc);
						}
					}
				}
			}
			
			void addLeft(PathwayElement pwe) {
				addElement(pwe, lefts);
			}
			
			void addRight(PathwayElement pwe) {
				addElement(pwe, rights);
			}
			
			void addMediator(PathwayElement pwe) {
				addElement(pwe, mediators);
			}
			
			void addElement(PathwayElement pwe, Set<PathwayElement> set) {
				if(pwe != null) {
					//If it's a group, add all subelements
					if(pwe.getObjectType() == ObjectType.GROUP) {
						for(PathwayElement ge : pwe.getParent().getGroupElements(pwe.getGroupId())) {
							addElement(ge, set);
						}
					}
					set.add(pwe);
					if (pwe.getObjectType() == ObjectType.DATANODE)
					{
						refs.add (pwe.getXref());
					}
				}
			}
			
			Set<PathwayElement> getLefts() { return lefts; }
			Set<PathwayElement> getRights() { return rights; }
			Set<PathwayElement> getMediators() { return mediators; }
		}
		
		/**
		 * Field that contains all elements participating on the
		 * left (start) side of this interaction
		 */
		public static final String FIELD_LEFT = "left";
		/**
		 * Field that contains all elements participating on the
		 * right (end) side of this interaction
		 */
		public static final String FIELD_RIGHT = "right";
		/**
		 * Field that contains all elements participating as
		 * mediator of this interaction
		 */
		public static final String FIELD_MEDIATOR = "mediator";
	}

}
