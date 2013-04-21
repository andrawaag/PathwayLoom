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

public class KeggPppPlugin_getEnzymesByGene extends SuggestionAdapter
{
	private GdbManager gdbManager;
	
	public KeggPppPlugin_getEnzymesByGene(GdbManager gdbManager) 
	{
		this.gdbManager = gdbManager;
	}
	
	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{
		try
		{   
			Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.ENTREZ_GENE);
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
			
	        String keggid = null;
	        keggid = serv.bconv("ncbi-geneid:"+input.getGeneID());
	        String[] bconvoutput = keggid.split("\t");
	        System.out.println("keggid: "+input.getGeneID());
	        String eccode[] = serv.get_enzymes_by_gene(bconvoutput[1]);
	        //results = serv.get_compounds_by_enzyme(eccode[0]);
	        for (int i = 0; i < eccode.length; i++) {
	            PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		    	pchildElt.setDataNodeType (DataNodeType.GENEPRODUCT);
		    	String btitTextLabel = serv.btit(eccode[i]);
		    	System.out.println(eccode[i]);
		    	System.out.println(btitTextLabel);
		    	String[] textLabel = btitTextLabel.split(";");
		    	
		    	String[] textLabel2 = textLabel[0].split(" ");
		    	String textLabel3 = "";
		    	for (int j=1;j<textLabel2.length;j++){
		    		textLabel3 += textLabel2[j]+" ";
		    	}
		    	
		    	pchildElt.setTextLabel(textLabel3);
		    	pchildElt.setDataSource(BioDataSource.ENZYME_CODE);
		    	String[] wellFormedEC = textLabel2[0].split(":");
		    	pchildElt.setGeneID(wellFormedEC[1]);
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
	public static void main(String[] args) throws IOException, ConverterException, SuggestionException
	{
		KeggPppPlugin_getEnzymesByGene keggPpp = new KeggPppPlugin_getEnzymesByGene(null);
	    
		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    test.setDataNodeType(DataNodeType.GENEPRODUCT);
	    test.setTextLabel("ALDH1A2");
		test.setGeneID("8854");
		test.setDataSource(BioDataSource.ENTREZ_GENE);
		
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
