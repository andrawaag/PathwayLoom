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

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class OpenPhactsApiPlugin extends SuggestionAdapter 
{
	final GdbManager gdbManager;

	OpenPhactsApiPlugin (GdbManager gdbManager)
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
		Pathway result = new Pathway();
		try
		{	
			Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.CHEMSPIDER);
			if (ref == null)
			{
				throw new SuggestionException("Could not find a valid BrdigeDb ID to go with this element");
			}

			PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
			pelt.setTextLabel(input.getTextLabel());
			pelt.setDataSource(input.getDataSource());
			pelt.setGeneID(input.getGeneID());
			pelt.setCopyright("Copyright notice");
			pelt.setDataNodeType(input.getDataNodeType());

			List<PathwayElement> spokes = new ArrayList<PathwayElement>();
			ResultSet compoundPharmacology = OpenPhacts.getCompoundPharmacologyPaginated("http://rdf.chemspider.com/"+input.getGeneID());
			while (compoundPharmacology.hasNext()) {
				QuerySolution solution = compoundPharmacology.next();
				String targetTitle = solution.get("targetTitle").toString();
				String target = solution.get("target").toString();
				ResultSet resultSet2 = OpenPhacts.getMapUrl(target);
				while (resultSet2.hasNext()){
					QuerySolution solution2 = resultSet2.next();
					String otherId = solution2.get("otherId").toString();
					if (otherId.contains("http://purl.uniprot.org/uniprot/")){
						ResultSet resultSet3 = OpenPhacts.getTargetInformation(otherId);
						while (resultSet3.hasNext()){
							QuerySolution solution3 = resultSet3.next();
							String prefLabel = solution3.get("mnemonic").toString();
							PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
							pchildElt.setTextLabel(prefLabel);
							pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
							pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
							pchildElt.setDataSource(BioDataSource.UNIPROT);
							pchildElt.setGeneID(otherId.split("/")[otherId.split("/").length-1]);
							//System.out.println(prefLabel+ "\t" +otherId.split("/")[otherId.split("/").length-1]);
							spokes.add (pchildElt);
						}
					}
				}
			}
			
			//String[] potentialExtentions = {"a", "b", "c", "d","e"};
			
			result = PathwayBuilder.radialLayout(pelt, spokes);
			
		}
		catch (IDMapperException ex)
		{
			throw new SuggestionException(ex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;	

	}

	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, ConverterException, IOException
	{
		OpenPhactsApiPlugin hmdbPpp = new OpenPhactsApiPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.METABOLITE);
		test.setTextLabel("Androsterone");
		test.setGeneID("187440");
		test.setDataSource(BioDataSource.CHEMSPIDER);

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
		return !(type.equals ("GeneProduct"));
	}

}
