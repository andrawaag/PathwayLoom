package org.pathvisio.plugins;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.impl.xpath.regex.REUtil;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.DC;


public class OpenPhacts {

	/**
	 * @param args
	 * @throws URISyntaxException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */

	public static ResultSet runQuery(String sparqlQuery, Model model){
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		return queryExecution.execSelect();		
	}
	/* getOpenPhactsAPI gets the results from the OpenPHACTS api. It submits the appropriate url's through curl and 
	 * and ingests the triples in a Jena model
	 */
	public static Model getOpenPhactsAPI(String urlString) throws IOException{
		URL url = new URL(urlString);		
		InputStream is = url.openStream();
		Model model = ModelFactory.createDefaultModel();
		model.read(is, "", "TURTLE");
		return model;
	}

	public static ResultSet getUniProtSparql(String sparqlQuery) throws IOException{
		Query query2 = QueryFactory.create(sparqlQuery);
		QueryExecution queryExecution2 = QueryExecutionFactory.sparqlService("http://beta.sparql.uniprot.org", query2);
		return 	queryExecution2.execSelect();
	}

	/* The following set of calls are a SPARQL queries to get results from the model returns by 
	 * getOpenPHACTSAPI.	
	 */

	//Get targets of the returns from getCompoundPharmacologyPaginated
	public static String getTargetQuery(){
		String sparqlQueryString = "PREFIX bibo: <http://purl.org/ontology/bibo/> " +
		"SELECT DISTINCT * WHERE {" +
		"?s <http://rdf.farmbio.uu.se/chembl/onto/#hasTarget> ?target ." +
		"?target <"+DC.title+"> ?targetTitle . " +				
		"}";
		return sparqlQueryString;
	}

	public static ResultSet getCompoundPharmacologyPaginated(String compoundURI) throws IOException{
		String urlString = "https://beta.openphacts.org/compound/pharmacology/pages?uri=" +
		URLEncoder.encode(compoundURI, "UTF-8")+
		"&app_id=50320fbb&app_key=3ce7a56fa1b53aeaf10be712c3fd6a37&_format=ttl";		
		return runQuery(getTargetQuery(), getOpenPhactsAPI(urlString));
	}

	// Get mapped identifiers out of 
	public static String getOtherIdQuery(){
		String sparqlQueryString = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		"SELECT DISTINCT * WHERE {" +
		" ?s skos:exactMatch ?otherId .}";
		return sparqlQueryString;
	}

	public static ResultSet getMapUrl(String url) throws IOException{
		String urlString = "https://beta.openphacts.org/mapURL?app_id=50320fbb&app_key=3ce7a56fa1b53aeaf10be712c3fd6a37&URL=" +
		URLEncoder.encode(url, "UTF-8")+
		"&_format=ttl";
		return runQuery(getOtherIdQuery(), getOpenPhactsAPI(urlString));
	}

	public static String getPrefLabelQuery(String uniprot){
		String sparqlQueryString = "PREFIX up:<http://purl.uniprot.org/core/> " + 
		"SELECT ?protein ?mnemonic " + 
		"WHERE " + 
		"{ " + 
		"<"+uniprot+"> a up:Protein . " + 
		"<"+uniprot+"> up:mnemonic ?mnemonic " + 
		"}";
		return sparqlQueryString;
	}

	public static ResultSet getTargetInformation( String url) throws IOException{
		return getUniProtSparql(getPrefLabelQuery(url));
	}
	
	

	public static void main(String[] args) throws URISyntaxException, ParserConfigurationException, SAXException, IOException {	
		ResultSet resultSet = getCompoundPharmacologyPaginated("http://rdf.chemspider.com/187440");
		while (resultSet.hasNext()) {
			QuerySolution solution = resultSet.next();
			String targetTitle = solution.get("targetTitle").toString();
			String target = solution.get("target").toString();
			//System.out.println(target);
			ResultSet resultSet2 = getMapUrl(target);
			while (resultSet2.hasNext()){
				QuerySolution solution2 = resultSet2.next();
				String otherId = solution2.get("otherId").toString();
				if (otherId.contains("http://purl.uniprot.org/uniprot/")){
					ResultSet resultSet3 = getTargetInformation(otherId);
					while (resultSet3.hasNext()){
						QuerySolution solution3 = resultSet3.next();
						String prefLabel = solution3.get("mnemonic").toString();
						System.out.println(prefLabel+ "\t" +otherId.split("/")[otherId.split("/").length-1] + otherId);
					}
				}
			}
		}

	}

}
