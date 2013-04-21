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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Generates Putative Pathway Parts based on a 
 * HMDB metabolic network parsed and stored in MySQL by Andra.
 */
public class WhatizitPppPlugin extends SuggestionAdapter {

    final GdbManager gdbManager;

    WhatizitPppPlugin(GdbManager gdbManager) {
        this.gdbManager = gdbManager;
    }



    public Set getResults(PathwayElement input) throws MalformedURLException, IOException, ParserConfigurationException, SAXException{
        //Get results from Whatizit
            String postvariable = "whatizitProteinInteraction ; " + input.getTextLabel();
            URL url = new URL("http://www.ebi.ac.uk/webservices/whatizit/pipe");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);


            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream());
            out.write(postvariable);
            out.close();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                    connection.getInputStream()));


            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(in));
            doc.getDocumentElement().normalize();

            NodeList medlineCitationLst = doc.getElementsByTagName("MedlineCitation");
            Set extentions = new HashSet();
            for (int a = 0; a < medlineCitationLst.getLength(); a++) {
                Node medlineCitation = medlineCitationLst.item(a);
                Element medlineCitationElement = (Element) medlineCitation;
                NodeList pmidNodeList = medlineCitationElement.getElementsByTagName("PMID");
                Node pmidNode = pmidNodeList.item(0);
                Element pmidElement = (Element) pmidNode;
                //PMID
                String pmid = pmidElement.getTextContent();

                //Abstract
                NodeList textLst = medlineCitationElement.getElementsByTagName("text");
                String abstractText = "";
                for (int s = 0; s < textLst.getLength(); s++) {
                    Node fstNode = textLst.item(s);
                    if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element fstElmnt = (Element) fstNode;
                        abstractText += fstElmnt.getTextContent();
                    }

                }
                //METHODS
                NodeList methodLst = medlineCitationElement.getElementsByTagName("hitPairTable");
                for (int b = 0; b < methodLst.getLength(); b++) {
                    Node hitlistNode = methodLst.item(b);
                    Element hitlistElement = (Element) hitlistNode;
                    Attr methodAttribute = hitlistElement.getAttributeNode("method");
                    String method = methodAttribute.getTextContent();
                    NodeList sentList = hitlistElement.getElementsByTagName("sent");
                    for (int c = 0; c < sentList.getLength(); c++) {
                        Node sentNode = sentList.item(c);
                        Element sentElement = (Element) sentNode;
                        Attr sentAttribute = sentElement.getAttributeNode("sid");
                        String sid = sentAttribute.getTextContent();
                        NodeList abstractTextList = medlineCitationElement.getElementsByTagName("AbstractText");
                        Element abstractTextElement = (Element) abstractTextList.item(0);
                        String informativeSentence;
                        if (!sid.equals("")) {
                            NodeList sentencesList = abstractTextElement.getElementsByTagName("SENT");

                            for (int d = 0; d < sentencesList.getLength(); d++) {
                                Element sentencesElement = (Element) sentencesList.item(d);
                                Attr sentencesAttribute = sentencesElement.getAttributeNode("sid");
                                String sentenceNumber = sentencesAttribute.getTextContent();
                                if (sentenceNumber.equals(sid)) {
                                    NodeList informativeSentenceNodeList = sentencesElement.getElementsByTagName("text");
                                    Element informativeSentenceElement = (Element) informativeSentenceNodeList.item(0);
                                    NodeList textNode = informativeSentenceElement.getChildNodes();
                                    //System.out.println(textNode.getLength());
                                    for (int m = 0; m < textNode.getLength(); m++) {
                                        //System.out.println(textNode.item(m).getNodeName());
                                        if (textNode.item(m).getNodeName().equals("z:uniprot")) {
                                            extentions.add(textNode.item(m).getTextContent().trim());

                                        }
                                    }


                                    informativeSentence = informativeSentenceElement.getTextContent();

                                    //System.out.println();
                                    //System.out.println(informativeSentence);
                                    break;
                                }
                            }
                        }
                        NodeList h1List = hitlistElement.getElementsByTagName("h1");
                        NodeList h2List = hitlistElement.getElementsByTagName("h2");
                        if (h1List.getLength() > 0) {
                            Element h1Element = (Element) h1List.item(0);
                            Element h2Element = (Element) h2List.item(0);
                            /* System.out.println("pmid: " + pmid);
                            System.out.println("method: " + method);

                            System.out.println("h1: " + h1Element.getTextContent());
                            System.out.println("h2: " + h2Element.getTextContent());*/
                            //  System.out.println(abstractText);
                        }
                    }
                }
            }
        return extentions;
    }
    public Pathway doSuggestion(PathwayElement input) throws SuggestionException {
        Pathway result = null;
        try {
            Xref ref = GdbUtil.forceDataSource(input.getXref(), gdbManager, BioDataSource.HMDB);
            /*if (ref == null) {
                throw new SuggestionException("Could not find a valid BrdigeDb ID to go with this element");
            }*/

            PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
            pelt.setMWidth(PppPlugin.DATANODE_MWIDTH);
            pelt.setMHeight(PppPlugin.DATANODE_MHEIGHT);
            pelt.setTextLabel(input.getTextLabel());
            pelt.setDataSource(input.getDataSource());
            pelt.setGeneID(input.getGeneID());
            pelt.setCopyright("Copyright notice");
            pelt.setDataNodeType(input.getDataNodeType());
            List<PathwayElement> spokes = new ArrayList<PathwayElement>();
            //String[] potentialExtentions = {"a", "b", "c", "d", "e"};
            Set extentions = new HashSet();
            extentions = getResults(input);
            String[] potentialExtentions = (String[]) extentions.toArray(new String[extentions.size()]);
            for (String addition : potentialExtentions) {
                PathwayElement pchildElt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
                pchildElt.setTextLabel(addition);
                pchildElt.setMWidth(PppPlugin.DATANODE_MWIDTH);
                pchildElt.setMHeight(PppPlugin.DATANODE_MHEIGHT);
                spokes.add(pchildElt);
            }
            result = PathwayBuilder.radialLayout(pelt, spokes);
        } catch (MalformedURLException ex) {
            Logger.getLogger(WhatizitPppPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WhatizitPppPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(WhatizitPppPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(WhatizitPppPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IDMapperException ex) {
            Logger.getLogger(WhatizitPppPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
   

    }

    /**
     * @param args
     * @throws ConverterException
     */
    public static void main(String[] args) throws SuggestionException, ConverterException, IOException {
        WhatizitPppPlugin hmdbPpp = new WhatizitPppPlugin(null);

        PathwayElement test = PathwayElement.createPathwayElement(ObjectType.DATANODE);
        test.setDataNodeType(DataNodeType.METABOLITE);
        test.setTextLabel("Lpl");
        test.setGeneID("HMDB00031");
        test.setDataSource(BioDataSource.HMDB);

        Pathway p = hmdbPpp.doSuggestion(test);

        File tmp = File.createTempFile("hmdbppp", ".gpml");
        p.writeToXml(tmp, true);

        BufferedReader br = new BufferedReader(new FileReader(tmp));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);


        }
    }

    @Override
    public boolean canSuggest(PathwayElement input) {
        String type = input.getDataNodeType();
        return true;
        //return !(type.equals("GeneProduct"));
    }
}
