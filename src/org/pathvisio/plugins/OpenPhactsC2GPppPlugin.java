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


import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class OpenPhactsC2GPppPlugin extends SuggestionAdapter 
{
	private GdbManager gdbManager;

	public OpenPhactsC2GPppPlugin (GdbManager gdbManager)
	{
		this.gdbManager = gdbManager;
	}

	@Override public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{	
		try
		{	        
			Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.KEGG_GENES);
			if (ref == null)
			{
				throw new SuggestionException("Could not find a valid KEGG Gene ID to go with this element");
			}

		List<PathwayElement> spokes = new ArrayList<PathwayElement>();	
		String sparqlQueryString = "PREFIX kegg: <http://chem2bio2rdf.org/kegg/resource/> select distinct ?gene where " +
				"{?interaction a kegg:kegg_interaction ." +
				" ?interaction kegg:compound_id <http://chem2bio2rdf.org/kegg/resource/kegg_ligand/"+input.getGeneID()+"> ." +
				" ?interaction <http://www.w3.org/2000/01/rdf-schema#label> ?geneLabel ." +
				" ?interaction kegg:CID_GENE ?gene}";

		
		System.out.println(sparqlQueryString);
		Query query = QueryFactory.create(sparqlQueryString);
		QueryExecution queryExecution = QueryExecutionFactory.sparqlService("http://192.168.10.231:8183/sparql", query);
		ResultSet resultSet = queryExecution.execSelect();
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
		pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
		pelt.setTextLabel(input.getTextLabel());
		pelt.setDataSource(input.getDataSource());
		pelt.setGeneID(input.getGeneID());
		pelt.setCopyright("OpenPhacts (http://www.openphacts.org");
		pelt.setDataNodeType(input.getDataNodeType());

		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			RDFNode compoundLabel = solution.get("gene");
			RDFNode compoundId = solution.get("gene");
			PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			pchildElt.setDataNodeType (DataNodeType.GENEPRODUCT);
			pchildElt.setTextLabel(compoundLabel.toString());
			pchildElt.setDataSource (BioDataSource.KEGG_GENES);
			pchildElt.setGeneID(compoundId.toString());
			pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
			spokes.add (pchildElt);
		}

		Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;
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
		OpenPhactsC2GPppPlugin hmdbPpp = new OpenPhactsC2GPppPlugin(null);

		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		test.setDataNodeType(DataNodeType.GENEPRODUCT);
		test.setTextLabel("C02814");
		test.setGeneID("C02814");
		test.setDataSource(BioDataSource.KEGG_GENES);

		Pathway p = hmdbPpp.doSuggestion(test);

		File tmp = File.createTempFile("openPhactsPwLoom", ".gpml");
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
