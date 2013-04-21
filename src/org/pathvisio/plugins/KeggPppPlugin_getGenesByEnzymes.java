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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import keggapi.KEGGLocator;
import keggapi.KEGGPortType;

import org.bridgedb.IDMapperException;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

/**
 * Generates Putative Pathway Parts based on a 
 * KEGG
 */
public class KeggPppPlugin_getGenesByEnzymes extends SuggestionAdapter 
{
	private GdbManager gdbManager;
	
	public KeggPppPlugin_getGenesByEnzymes (GdbManager gdbManager) 
	{
		this.gdbManager = gdbManager;
	}
	
	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{
		try
		{	        
			Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.ENZYME_CODE);
			if (ref == null)
			{
				throw new SuggestionException("Could not find a valid Entrez Gene ID to go with this element");
			}

			PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		    pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		    pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		    pelt.setTextLabel(input.getTextLabel());
		    pelt.setDataSource(input.getDataSource());
		    pelt.setGeneID(input.getGeneID());
		    pelt.setCopyright("KEGG (http://www.genome.jp/kegg/");
		    pelt.setDataNodeType(input.getDataNodeType());
		    
	        KEGGLocator    locator  = new KEGGLocator();
	        KEGGPortType   serv     = locator.getKEGGPort();
			List<PathwayElement> spokes = new ArrayList<PathwayElement>();
			
			String[] results  = null;
			String query = "ec:" + ref.getId();
	        results = serv.get_genes_by_enzyme(query, input.getOrganism());
	        System.out.println(results.length +":"+input.getGeneID()+" "+input.getOrganism());
	        for (int i = 0; i < results.length; i++) {
	            PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		    	pchildElt.setDataNodeType (DataNodeType.GENEPRODUCT);
		    	String btitTextLabel = serv.btit(results[i]);
		    	System.out.println(btitTextLabel);
		    	String[] textLabel_intermediate = btitTextLabel.split("; ");
		    	String[] textLabel = textLabel_intermediate[0].split(" ");
		    	pchildElt.setTextLabel(textLabel[1]);
		    	pchildElt.setDataSource(BioDataSource.KEGG_COMPOUND);
		    	pchildElt.setGeneID(textLabel[0]);
		    	pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		    	spokes.add (pchildElt);
	        }
		    Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
			return result;
		}
		catch (ServiceException ex)
		{
			throw new SuggestionException(ex);
		}
		catch (IOException ex)
		{
			throw new SuggestionException(ex);
		}
		catch (IDMapperException ex)
		{
			throw new SuggestionException(ex);
		}
	}
	
	
	/**
	 * @param args
	 * @throws ConverterException 
	 * @throws ServiceException 
	 */
	public static void main(String[] args) throws SuggestionException, ConverterException, IOException
	{
		KeggPppPlugin_getGenesByEnzymes keggPpp = new KeggPppPlugin_getGenesByEnzymes(null);
	    
		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    test.setDataNodeType(DataNodeType.PROTEIN);
	    test.setTextLabel("ALDH1A2");
		test.setGeneID("1.3.99.6");
		test.setOrganism("hsa");
		test.setDataSource(BioDataSource.ENZYME_CODE);
		
		Pathway p = keggPpp.doSuggestion(test); 
		
		File tmp = File.createTempFile("keggppp", ".gpml");
		p.writeToXml(tmp, true);
		
		BufferedReader br = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = br.readLine()) != null)
		{
			System.out.println (line);
			
			
		}
	}

	@Override public boolean canSuggest(PathwayElement input) 
	{
		String type = input.getDataNodeType();	
		return !(type.equals ("Metabolite"));
	}
}
