package org.pathvisio.plugins;

/* **
 * This code has been designed/implemented and is maintained by:
 * 
 * Miguel Arregui (miguel.arregui@ebi.ac.uk)
 * 
 * Any comments and/or feedback are welcome and encouraged. 
 * 
 * Started on:    5 May 2006.
 * Last reviewed: 7 June 2006.
 */


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;



/***
 * This is a trivial HTTP client that can be used to
 * contact the WHATIZIT pipelines over streamed HTTP.
 * At the other end of the line there is a servlet
 * listening for the following format:
 * 
 *   nameOfAProcessingPipeline
 *   <document xmlns:xlink='http://www.w3.org/1999/xlink' 
 *             xmlns:z='http://www.ebi.ac.uk/z' 
 *             source='Whatizit'><text>
 *     Your text comes here, it can be as long as you 
 *     need and can contain any XML-like tags (except 
 *     </document>). Only blocks of text wrapped with
 *     the "<text> ... </text>" tags will be processed.
 *     There can be as many of these blocks as you need.     
 *   </text></document>
 *   
 * For the sake of clarity the syntax rule follows:
 * 
 *    nameOfAProcessingPipeline
 *   <document xmlns:xlink='http://www.w3.org/1999/xlink' 
 *             xmlns:z='http://www.ebi.ac.uk/z' 
 *             source='Whatizit'>
 *             
 *             (<text> your text </text>)+
 *             
 *   </document>
 * 
 * The first line must contain the name of a pipeline
 * followed by a linefeed. The subsequent blocks of 
 * text must be wrapped with the <text> tags. All of 
 * the text blocks must be wrapped with the <document> 
 * tags. 
 * 
 * The life cycle of this class is as follows:
 * 
 *   HttpClient client = new HttpClient();
 *   client.upload(System.in);
 *   client.download(System.out);
 *   client.close();
 *   
 * Note: To invoque the client set the "http.keepAlive"
 * parameter to false:  -Dhttp.keepAlive=false  
 */
public class HttpClient {
	
	// URL of the processing servlet
  public static final String SERVER_URL 
    = "http://www.ebi.ac.uk/webservices/whatizit/pipe";     
  
  // The actual size of the chunks sent/received to/from the servlet
  protected static final int BUFFER_SIZE = 502; 
  protected byte [] buffer;
  protected boolean uploadDone;
  protected boolean downloadDone;
  protected URL url;
  protected HttpURLConnection conn;
  
  
  
  public HttpClient () throws MalformedURLException, IOException {
  	this(SERVER_URL);
  }
  
  
  public HttpClient (String urlStr) throws MalformedURLException, IOException {
  	uploadDone = false;
    downloadDone = false;
    buffer = new byte [BUFFER_SIZE];
  	this.url = new URL(urlStr);  	
  	conn = (HttpURLConnection)this.url.openConnection();  	  
    conn.setRequestMethod("POST");
    conn.setUseCaches(false);    
    conn.setDoInput(true);
    conn.setDoOutput(true);                   
    conn.setRequestProperty("Content-Type", "UTF-8");
    conn.setRequestProperty("Transfer-Encoding", "chunked");     
    conn.setChunkedStreamingMode(BUFFER_SIZE);
    conn.connect();
  }    
  
    
  public void upload (InputStream in) throws IOException {  	
  	if (uploadDone) throw new IOException("Upload done already.");
  	OutputStream out = conn.getOutputStream();
  	readFromTo(in, out);
    out.close();
    uploadDone = true;  	
  }
  
  
  public void download (OutputStream out) throws IOException {
  	if (downloadDone) throw new IOException("Download done already.");
  	InputStream in = conn.getInputStream();
  	readFromTo(in, out);
    in.close();
    downloadDone = true;
  }
  
  
  public void close (){
  	conn.disconnect();
  }
  
  
  protected void readFromTo (InputStream in, OutputStream out) 
  throws IOException {  	
  	int bread = 0;
  	while (-1 != (bread=in.read(buffer, 0, BUFFER_SIZE))){
  	  out.write(buffer, 0, bread);
  	  out.flush();
  	}  	
  }  
       
  
   
  
  public static void main (String [] args){  	
  	try {
      String serverUrl = (args.length == 1)? args[0] : SERVER_URL;
  	  HttpClient client = new HttpClient(serverUrl);  	
  	  String s = "whatizitProteinInteraction ; pc21";
  	  byte b[] = s.getBytes();
  	  ByteArrayInputStream in = new ByteArrayInputStream(b);
  	  System.out.toString();
  	  client.upload(in);
  	  
      client.download(System.out);
  	 // System.out.println(results);
  	  client.close();
  	}
  	catch (IOException e){
  		e.printStackTrace();
  		System.err.println(e.getMessage());
  	}
  }
}
// Eof - Miguel Arregui (miguel.arregui@ebi.ac.uk)