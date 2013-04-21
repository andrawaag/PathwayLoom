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

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class ConceptWikiPppPlugin extends SuggestionAdapter 
{
	final GdbManager gdbManager;

	ConceptWikiPppPlugin (GdbManager gdbManager)
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
			Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.HMDB);
			if (ref == null)
			{
				throw new SuggestionException("Could not find a valid HMDB ID to go with this element");
			}

			PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
			pelt.setTextLabel(input.getTextLabel());
			pelt.setDataSource(input.getDataSource());
			pelt.setGeneID(input.getGeneID());
			pelt.setCopyright("Human metabolome database (http://www.hmdb.ca)");
			pelt.setDataNodeType(input.getDataNodeType());

			List<PathwayElement> spokes = new ArrayList<PathwayElement>();
			int noRecords = 0;
			String aLine;

			String urlString = "http://www.hmdb.ca/cgi-bin/extractor_runner.cgi?metabolites_hmdb_id="+ref.getId()+"&format=csv&select_enzymes_gene_name=on&select_enzymes_swissprot_id=on";
			System.out.println(urlString);
			URL url = new URL(urlString);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((br.readLine()) != null ) {
				noRecords += 1;
			}
			int row = 0;
			in.close();
			InputStream in2 = url.openStream();
			BufferedReader br2 = new BufferedReader(new InputStreamReader(in2));
			while ((aLine = br2.readLine()) != null ) {
				String[] velden = null;
				velden = aLine.split(",");
				if (row >0) { //Each row except the header
					PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
					String swissId = velden[3];
					System.out.println(swissId);
					String geneName = velden[2];
					pchildElt.setDataNodeType (DataNodeType.PROTEIN);
					pchildElt.setTextLabel(geneName);
					pchildElt.setDataSource (BioDataSource.UNIPROT);
					pchildElt.setGeneID(swissId);
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
		catch (IDMapperException ex)
		{
			throw new SuggestionException(ex);
		}	

	}

	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, ConverterException, IOException
	{
		ConceptWikiPppPlugin hmdbPpp = new ConceptWikiPppPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.METABOLITE);
		test.setTextLabel("Androsterone");
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
		return !(type.equals ("GeneProduct"));
	}

}
