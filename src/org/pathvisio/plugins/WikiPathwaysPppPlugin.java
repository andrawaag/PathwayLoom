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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

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
import org.pathvisio.plugins.Suggestion.SuggestionException;
import org.pathvisio.wikipathways.webservice.WSSearchResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wikipathways.client.WikiPathwaysClient;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class WikiPathwaysPppPlugin extends SuggestionAdapter 
{
	final GdbManager gdbManager;

	WikiPathwaysPppPlugin (GdbManager gdbManager)
	{
		this.gdbManager = gdbManager;
	}

	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException 
	{
		/*	try {
            // The newInstance() call is a work around for some
            // broken Java implementations
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            // handle the error
        }*/

		try
		{	

			URL url = new URL("http://wikipathways.org/wpi/webservice/webservice.php/findInteractions?query="+input.getTextLabel());
			URLConnection connection = url.openConnection();
	        connection.setDoOutput(true);
			
			BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    connection.getInputStream()));
			 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            DocumentBuilder db = dbf.newDocumentBuilder();
	            Document doc = db.parse(new InputSource(in));
	            doc.getDocumentElement().normalize();
	            
	            NodeList wikiPathwaysInteractionsLst = doc.getElementsByTagName("ns2:name");
	             //System.out.println(wikiPathwaysInteractionsLst.getLength());
			            

			Model model = ModelFactory.createDefaultModel();
			String type = input.getDataNodeType();
			String wpNS = "http://vocabularies.wikipathways.org/";
			Resource origin = model.createResource(wpNS+type);
			origin.addProperty(DC.identifier, input.getGeneID());
			origin.addProperty(DC.source, input.getDataSource().toString());
			origin.addProperty(RDFS.label, input.getTextLabel());
			Property interaction = model.createProperty(wpNS+"interaction");
			interaction.addProperty(RDF.type, Biopax_level3.Interaction);
			
			//Populate a RDF datamodel according to WP vocabulary
			for (int counter = 0; counter <wikiPathwaysInteractionsLst.getLength(); counter++){
				if (wikiPathwaysInteractionsLst.item(counter).getTextContent().equals("left") || wikiPathwaysInteractionsLst.item(counter).getTextContent().equals("right")) {
				    String object = wikiPathwaysInteractionsLst.item(counter).getParentNode().getChildNodes().item(1).getTextContent();
				    if (!(input.getTextLabel().equalsIgnoreCase(object))){
				    	model.add(origin, interaction, object);
				    	//System.out.println(object);
				    }
				}
			}
			
			PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
			pelt.setTextLabel(origin.getProperty(RDFS.label).getString());
			pelt.setDataSource(input.getDataSource()); //TODO omvormen naar RDF
			pelt.setGeneID(origin.getProperty(DC.identifier).getString());
			pelt.setCopyright("Copyright notice");
			pelt.setDataNodeType(input.getDataNodeType());
		
			List<PathwayElement> spokes = new ArrayList<PathwayElement>();
			String sparqlQueryString = "PREFIX wp: <http://vocabularies.wikipathways.org/>" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+	
			"SELECT DISTINCT ?o WHERE {?s ?p ?o .}";
			Query query = QueryFactory.create(sparqlQueryString);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			ResultSet resultSet = queryExecution.execSelect();
			int a = 0;
			while (resultSet.hasNext()) {
				//System.out.println(a++);
				QuerySolution candidate = resultSet.next();
				RDFNode candidateNode = candidate.get("o");
				PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
				pchildElt.setDataNodeType (DataNodeType.UNKOWN);
				pchildElt.setTextLabel(candidateNode.toString());
				//pchildElt.setDataSource (BioDataSource.CHEBI);
				pchildElt.setGeneID("NOT_ASSIGNED");
				pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
				pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
				spokes.add (pchildElt);
			}

			Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
			return result;
		}
		catch (MalformedURLException ex) {
			throw new SuggestionException(ex);
		} catch (RemoteException ex) {
			// TODO Auto-generated catch block
			throw new SuggestionException(ex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
	}

	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws IOException, ConverterException, SuggestionException
	{
		WikiPathwaysPppPlugin hmdbPpp = new WikiPathwaysPppPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.METABOLITE);
		test.setTextLabel("P53");
		test.setGeneID("HMDB00031");
		test.setDataSource(BioDataSource.HMDB);

		Pathway p = hmdbPpp.doSuggestion(test);

		File tmp = File.createTempFile("hmdbppp", ".gpml");
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
		return true;
	}

}
