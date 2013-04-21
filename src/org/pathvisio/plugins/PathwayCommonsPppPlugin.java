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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import org.apache.log4j.Category;
import org.blueprint.webservices.bind.soap.client.BINDSOAPDemo;

import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class PathwayCommonsPppPlugin implements Suggestion
{	
	public static final String SOURCE_ALL = "ALL";
	private GdbManager gdbManager;
	
	static Category cat = Category.getInstance(BINDSOAPDemo.class.getName());
	
	String sourceParameter;
	
	public PathwayCommonsPppPlugin (GdbManager gdbManager, String source) 
	{
		this.gdbManager = gdbManager;
		this.sourceParameter = source;
	}
	
    static class MyHandler extends DefaultHandler {
    }
    
    
	public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{        
		try{
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
	    pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    pelt.setTextLabel(input.getTextLabel());
	    pelt.setDataSource(input.getDataSource());
	    pelt.setGeneID(input.getGeneID());
	    pelt.setCopyright("Pathway Commons (http://www.pathwaycommons.org)");
	    pelt.setDataNodeType(input.getDataNodeType());

		List<PathwayElement> spokes = new ArrayList<PathwayElement>();
		String aLine;
		String inputSource = "";
		if (input.getDataSource().equals(BioDataSource.UNIPROT)) {
			inputSource = "UNIPROT";
		}
		if (input.getDataSource().equals(BioDataSource.ENTREZ_GENE)) {
			inputSource = "ENTREZ_GENE";
		}
		
		
		String urlString = "http://www.pathwaycommons.org/pc/webservice.do?version=3.0&cmd=get_neighbors&q="
			+input.getGeneID()+
			"&input_id_type="+
			inputSource+
			"&output_id_type="+
			inputSource+
			"&output=id_list";
		if (!SOURCE_ALL.equals(sourceParameter)){
			urlString += "&data_source="+sourceParameter;
		}
		

		URL url = new URL(urlString);
		int row = 0;
		InputStream in2 = url.openStream();
		BufferedReader br2 = new BufferedReader(new InputStreamReader(in2));
		while ((aLine = br2.readLine()) != null ) {
			String[] velden = null;
			//System.out.println(urlString);
			//System.out.println(aLine);
			velden = aLine.split("\t");
			//System.out.println(velden[0]+" -- "+ velden[2]);
			if ((row >0) && (velden.length == 3))  { //Each row except the header
				PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		    	
		 
		    	pchildElt.setDataNodeType (DataNodeType.PROTEIN);
		    	pchildElt.setTextLabel(velden[0]);
		    	if (!(velden[2].contains("EXTERNAL_ID_NOT_FOUND") || velden[2].contains("N/A"))){
		    	     pchildElt.setDataSource (input.getDataSource());
		    	     pchildElt.setGeneID(velden[2].substring(velden[2].indexOf(":")+1));
		    	}
		    	
		    	pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		    	spokes.add (pchildElt);
			}
			
			row++; //exclude row with headings
		}
		
		

	    Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;
		}
		catch (IOException ex)
		{
			throw new SuggestionException(ex);
		}	
		
	}
	
	
	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, IOException, ConverterException
	{
		PathwayCommonsPppPlugin pwcPpp = new PathwayCommonsPppPlugin(null, SOURCE_ALL);
	    
		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    test.setDataNodeType(DataNodeType.PROTEIN);
	    test.setTextLabel("LAT");
		test.setGeneID("27040");
		test.setDataSource(BioDataSource.ENTREZ_GENE);
		
		Pathway p = pwcPpp.doSuggestion(test);
		
		File tmp = File.createTempFile("pwcppp", ".gpml");
		p.writeToXml(tmp, true);
		
		BufferedReader br = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = br.readLine()) != null)
		{
			System.out.println (line);
			
			
		}
	}

	public boolean canSuggest(PathwayElement input) 
	{
		String type = input.getDataNodeType();	
		return !(type.equals ("Metabolite"));
	}


	

}
