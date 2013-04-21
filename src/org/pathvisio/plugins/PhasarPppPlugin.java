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
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class PhasarPppPlugin implements Suggestion
{	
	private GdbManager gdbManager;
	
	public PhasarPppPlugin (GdbManager gdbManager) 
	{
		this.gdbManager = gdbManager;
	}
	
    static class MyHandler extends DefaultHandler {
    }
    
    public String getKeggName(String KeggID)
    {
    	String textfromKeggId = "";
    	try {
			KEGGLocator    locator  = new KEGGLocator();
			KEGGPortType   serv     = locator.getKEGGPort();
			textfromKeggId = serv.btit(KeggID);
	    	
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return textfromKeggId;

    }
    
	public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{        
	    int row = 0;
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
	    pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    pelt.setTextLabel(input.getTextLabel());
	    pelt.setDataSource(input.getDataSource());
	    pelt.setGeneID(input.getGeneID());
	    pelt.setCopyright("Phasar (http://www.phasar.cs.ru.nl/)");
	    pelt.setDataNodeType(input.getDataNodeType());

	    String urlString;
	    try
	    {
		    Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.ENZYME_CODE);
			if (ref == null)
			{
				throw new SuggestionException("Could not find a valid Entrez Gene ID to go with this element");
			}
			urlString = "http://www.bioinformatics.nl/biometa/webservice/connections/getsuggestion/EC."+ref.getId()+":";
	    }
		catch (IDMapperException ex)
		{
			throw new SuggestionException(ex);
		}	    

		InputStream urlInputStream = null;
 		
		
		System.out.println(urlString);
		org.w3c.dom.Document doc = null;
		try {
			URL url = new URL(urlString);
			urlInputStream = url.openConnection().getInputStream();
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = db.parse(urlInputStream);
		} catch (SAXException e) {
			throw new SuggestionException(e);
		} catch (ParserConfigurationException e) {
			throw new SuggestionException(e);
		} catch (IOException e) {
			throw new SuggestionException(e);
		}
		
		List<PathwayElement> spokes = new ArrayList<PathwayElement>();
		NodeList list = doc.getElementsByTagName("identifier");
		for(int i = 0, length = list.getLength(); i < length; i++){
			PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    	
			Element neighbour  = (Element)list.item(i);
			Attr identifier = neighbour.getAttributeNode("name");
			Attr type = neighbour.getAttributeNode("type");
			//System.out.println(identifier.getTextContent()+":"+type.getTextContent());
			String ReturnedPhasarResult =identifier.getTextContent();
			//System.out.println(type.getTextContent()+"joepi");
			//System.out.println("Find getKeggName vanuit main:"+this.getKeggName(ReturnedPhasarResult.substring(3)));
	    	String btitTextLabel = "";
			if (type.getTextContent().equals("enzyme")) {
	    		pchildElt.setDataNodeType(DataNodeType.GENEPRODUCT);
	    		pchildElt.setDataSource(BioDataSource.ENZYME_CODE);
	    		pchildElt.setGeneID(identifier.getTextContent().substring(3)); 
	    		btitTextLabel = this.getKeggName("ec:"+ReturnedPhasarResult.substring(3));
	    		String[] textLabel = btitTextLabel.split(";");
		    	
		    	String[] textLabel2 = textLabel[0].split(" ");
		    	String textLabel3 = "";
		    	for (int j=1;j<textLabel2.length;j++){
		    		textLabel3 += textLabel2[j]+" ";
		    	}
		    	
		    	pchildElt.setTextLabel(textLabel3);
	    		
	    	}
	    	if (type.getTextContent().equals("compound")) {
	    		pchildElt.setDataNodeType(DataNodeType.METABOLITE);
	    		pchildElt.setDataSource(BioDataSource.KEGG_COMPOUND);
	    		pchildElt.setGeneID(identifier.getTextContent()); 
	    		// System.out.println("Find getKeggName vanuit main:"+this.getKeggName("cpd:"+ReturnedPhasarResult));
	    		btitTextLabel = this.getKeggName("cpd:"+ReturnedPhasarResult);
	    		String[] textLabel = btitTextLabel.split(";");
		    	
		    	String[] textLabel2 = textLabel[0].split(" ");
		    	String textLabel3 = "";
		    	for (int j=1;j<textLabel2.length;j++){
		    		textLabel3 += textLabel2[j]+" ";
		    	}
		    	
		    	pchildElt.setTextLabel(textLabel3);
	    	}
	    	
				
	    	//String textLabelFromKegg = serv.btit(ReturnedPhasarResult);
	    	
	           	
	    	
		    pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);	    	
	    	spokes.add (pchildElt);

		}
		
			row++; //exclude row with headings
		

	    Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, IOException, ConverterException
	{
		PhasarPppPlugin phasarPpp = new PhasarPppPlugin(null);
	    
		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    test.setDataNodeType(DataNodeType.PROTEIN);
	    test.setTextLabel("MAPK");
		test.setGeneID("2.7.11.24");
		test.setDataSource(BioDataSource.ENZYME_CODE);
		
		Pathway p = phasarPpp.doSuggestion(test);
		
		File tmp = File.createTempFile("phasarppp", ".gpml");
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
		return !(type.equals ("Enzyme"));
	}

}
