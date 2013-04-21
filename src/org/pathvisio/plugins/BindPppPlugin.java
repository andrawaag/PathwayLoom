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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.apache.log4j.Category;
import org.blueprint.webservices.bind.soap.client.BINDSOAPDemo;
import org.blueprint.webservices.bind.soap.client.stub.BINDSOAPBindingStub;
import org.blueprint.webservices.bind.soap.client.stub.BINDSOAPException;
import org.blueprint.webservices.bind.soap.client.stub.BINDServiceLocator;
import org.blueprint.webservices.bind.soap.client.stub.SearchResultBean;
import keggapi.KEGGLocator;
import keggapi.KEGGPortType;

import org.bridgedb.IDMapperException;
import org.bridgedb.DataSource;
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
public class BindPppPlugin implements Suggestion
{	
	private GdbManager gdbManager;
	
	static Category cat = Category.getInstance(BINDSOAPDemo.class.getName());
	
	public BindPppPlugin (GdbManager gdbManager) 
	{
		this.gdbManager = gdbManager;
	}
	
    static class MyHandler extends DefaultHandler {
    }
    
    
	public Pathway doSuggestion(PathwayElement input) throws SuggestionException
	{        
		//Center element
		String inputFormat = "";
		int row = 0;
		if (input.getDataSource() == BioDataSource.GENBANK) {
			inputFormat = "gi";
		}
		if (input.getDataSource() == BioDataSource.UNIPROT) {
			inputFormat = "uniprot";
		}
		if (input.getDataSource() == BioDataSource.EMBL) {
			inputFormat = "embl";
		}
		if (input.getDataSource() == BioDataSource.ENSEMBL) {
			inputFormat = "ensembl";
		}
		if (input.getDataSource() == BioDataSource.ENTREZ_GENE) {
			inputFormat = "genbank";
		}
		if (input.getDataSource() == BioDataSource.FLYBASE){
			inputFormat = "flybase";
		}
		if (input.getDataSource() == BioDataSource.GENE_ONTOLOGY) {
			inputFormat = "GO";
		}
		if (input.getDataSource() == BioDataSource.INTERPRO) {
			inputFormat = "interpro";
		}
		if (input.getDataSource() == BioDataSource.IPI) {
			inputFormat = "ipi";
		}
		if (input.getDataSource() == BioDataSource.MGI) {
			inputFormat = "MGI";
		}
		if (input.getDataSource() == BioDataSource.OMIM) {
			inputFormat = "omim";
		}
		if (input.getDataSource() == BioDataSource.PDB) {
			inputFormat = "pdb";
		}
		if (input.getDataSource() == BioDataSource.PFAM) {
			inputFormat = "pfam";
		}
		if (input.getDataSource() == BioDataSource.REFSEQ) {
			inputFormat = "refseq";
		}
		if (input.getDataSource() == BioDataSource.RGD) {
			inputFormat = "rgd";
		}
		if (input.getDataSource() == BioDataSource.SGD) {
			inputFormat = "sgd";
		}
		if (input.getDataSource() == BioDataSource.UNIGENE) {
			inputFormat = "unigene";
		}
		if (input.getDataSource() == BioDataSource.WORMBASE) {
			inputFormat = "wormbase";
		}
		if (input.getDataSource() == BioDataSource.ZFIN) {
			inputFormat = "zfin";
		}
		PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    pelt.setMWidth (PppPlugin.DATANODE_MWIDTH);
	    pelt.setMHeight (PppPlugin.DATANODE_MHEIGHT);
	    pelt.setTextLabel(input.getTextLabel());
	    pelt.setDataSource(input.getDataSource());
	    pelt.setGeneID(input.getGeneID());
	    pelt.setCopyright("BIND http://www.bind.ca");
	    pelt.setDataNodeType(input.getDataNodeType());

		List<PathwayElement> spokes = new ArrayList<PathwayElement>();
		
		
		BINDSOAPBindingStub binding;
        try {
			binding = (BINDSOAPBindingStub) new BINDServiceLocator().getBINDSOAP();

        
        // Time out after 120 seconds
        binding.setTimeout(120000);
        
        // Enable the cookie-validation for sessions used by BIND and BINDSOAP
        binding.setMaintainSession(true);


        // isServiceAlive().
        try {
            System.out.print("\nisServiceAlive()\t");
            boolean result = binding.isServiceAlive();
            System.out.println(result);
        } catch (BINDSOAPException e) {
            cat.error(e);
            System.exit(1);
        } catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // idSearch().
        try {
            System.out.println("idSearch()");
          
            
            
            
            //for documentation see:  
            SearchResultBean resultBean = binding.idSearch(input.getXref().toString().substring(2),inputFormat,"gipair");
            System.out.println("query=" + resultBean.getQuery());
            System.out.println(resultBean.getRecords().toString());
            System.out.println("andra waagmeester");
            System.out.println("totalRecordsFound=" + resultBean.getTotalRecordsFound());

            String[] poList = resultBean.getRecords().split("\r\n|\r|\n");
            for (int i = 1; i< poList.length;i++){
            	
            	PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
            	pchildElt.setDataNodeType(DataNodeType.GENEPRODUCT);
	    		//pchildElt.setDataSource(BioDataSource.ENZYME_CODE);
	    		
	    		String[] fields = poList[i].split(",");
            	pchildElt.setGeneID(fields[8]);
            	pchildElt.setTextLabel(fields[6]);
            	
            	pchildElt.setDataNodeType(DataNodeType.GENEPRODUCT);
	    		pchildElt.setDataSource(BioDataSource.GENBANK);
            	
			    pchildElt.setMWidth (PppPlugin.DATANODE_MWIDTH);
			    pchildElt.setMHeight (PppPlugin.DATANODE_MHEIGHT);	    	
		    	spokes.add (pchildElt);
            	
            }
            
        } catch (BINDSOAPException e) {
        	cat.error(e);
        } catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} catch (ServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	    Pathway result = PathwayBuilder.radialLayout(pelt, spokes);
		return result;
		
	}
	
	
	/**
	 * @param args
	 * @throws ConverterException 
	 */
	public static void main(String[] args) throws SuggestionException, IOException, ConverterException
	{
		BindPppPlugin bindPpp = new BindPppPlugin(null);
	    
		PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
	    test.setDataNodeType(DataNodeType.PROTEIN);
	    test.setTextLabel("LAT");
		test.setGeneID("2828026");
		test.setDataSource(BioDataSource.GENBANK);
		
		Pathway p = bindPpp.doSuggestion(test);
		
		File tmp = File.createTempFile("bindppp", ".gpml");
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
