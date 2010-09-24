package com.hapiware.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class HttpResponse
{
	private final static Logger LOGGER = Logger.getLogger(HttpResponse.class.getName());
	
	private final static String HTTP_VERSION = "HTTP/1.1";
	private final static String CRLF = "\r\n";
	
	private final HttpStatusCode _statusCode;
	private final String _contentType;
	private final String _content;
	
	
	public HttpResponse(HttpRequest request)
	{
		String uriPath = request.getUri().getPath();
		HttpStatusCode statusCode = HttpStatusCode.SC500;
		String contentType = "text/html";
		String content = "";
		try {
			if(request.getStatusCode() != HttpStatusCode.SC200) {
				statusCode = request.getStatusCode();
				return;
			}
			
			if(uriPath.contains("favicon")) {
				statusCode = HttpStatusCode.SC204;
				return;
			}
			
			if(uriPath.endsWith("/")) {
				File directory = new File(".", uriPath);
				if(!directory.exists()) {
					statusCode = HttpStatusCode.SC404;
					contentType = "text/html";
					content =
						htmlBody(
							statusCode.getStatusCode() + " : " + statusCode.getReasonPhrase() + "\n"
						);
					return;
				}
				
				String fileList = "";
				if(!uriPath.equals("/")) {
					String parent = uriPath.substring(0, uriPath.length() - 2);
					int i = parent.lastIndexOf("/");
					fileList += 
						"<a href=\"" + parent.subSequence(0, i) + "/\">[..]</a><br/>\n";
				}
				for(File f : directory.listFiles()) {
					if(f.isDirectory())
						fileList += 
							"<a href=\"" + uriPath + f.getName() + "/\">[" + f.getName() + "]</a><br/>\n";
				}
				for(File f : directory.listFiles()) {
					if(f.isFile())
						fileList +=
							"<a href=\"" + uriPath + f.getName() + "\">" + f.getName() + "</a><br/>\n";
				}
				content += htmlBody(fileList);
				statusCode = HttpStatusCode.SC200;
			}
			else {
				contentType = "text/plain";
				//contentType = "application/octet-stream";
				BufferedReader reader =	null;
				try {
					reader = new BufferedReader(new FileReader(new File(".", uriPath)));
					String line;
					while((line = reader.readLine()) != null)
						content += line + CRLF;
					statusCode = HttpStatusCode.SC200;
				}
				catch(FileNotFoundException e) {
					statusCode = HttpStatusCode.SC404;
					contentType = "text/html";
					content =
						htmlBody(
							statusCode.getStatusCode() + " : " + statusCode.getReasonPhrase() + "\n"
						);
					LOGGER.log(Level.INFO, uriPath + " was not found.", e);
				}
				catch(IOException e) {
					statusCode = HttpStatusCode.SC500;
					LOGGER.log(Level.SEVERE, "Error reading file: " + uriPath, e);
				}
				finally {
					try {
						if(reader != null)
							reader.close();
					}
					catch(IOException e) {
						// Does nothing.
					}
				}
			}
		}
		finally {
			_contentType = contentType;
			_content = content;
			_statusCode = statusCode;
		}
	}
	
	private String htmlBody(String content)
	{
		String retVal = "";
		retVal += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
		retVal += "<html><body>\n";
		retVal += content;
		retVal += "</body></html>\n";
		return retVal;
	}
	
	public void write(OutputStream os) throws IOException
	{
		String response = HTTP_VERSION + " ";
		response += _statusCode.getStatusCode() + " " + _statusCode.getReasonPhrase() + CRLF;
		if(_statusCode == HttpStatusCode.SC405) {
			response += "Allow: ";
			HttpRequest.SupportedRequestMethod m[] = HttpRequest.SupportedRequestMethod.values();
			for(int i = 0; i < m.length - 1; i++)
				response += m[i] + ", ";
			response += m[m.length - 1];
		}
		response += "Date: " + DateFormat.getDateTimeInstance().format(new Date()) + CRLF;
		response += "Server: FileWeb/1.0.0 (Java/5.0)" + CRLF;
		response += "Content-Type: " + _contentType + CRLF;
		response += "Content-Length: " + _content.length() + CRLF;
		response += "Connection: close" + CRLF;
		response += CRLF;
		response += _content;
		
		os.write(response.getBytes());
		log(response);
	}
	
	private void log(String response)
	{
		if(
			LOGGER.isLoggable(Level.FINE)
			|| LOGGER.isLoggable(Level.FINER) 
			|| LOGGER.isLoggable(Level.FINEST)
		) {
			String respStr = "";
			Level level = Level.FINE;
			respStr += "\n-> HTTP Version: " + HTTP_VERSION + "\n";
			respStr += "-> Status Code: " + _statusCode.getStatusCode() + "\n";
			respStr += "-> Reason Phrase: " + _statusCode.getReasonPhrase() + "\n";
			if(LOGGER.isLoggable(Level.FINER) || LOGGER.isLoggable(Level.FINEST)) {
				level = Level.FINER;
				respStr += "->\n";
				BufferedReader reader =	new BufferedReader(new StringReader(response));
				
				try {
					// Reads status line.
					String line = reader.readLine();
					
					// Reads header.
					while((line = reader.readLine()) != null) {
						// Breaks when headers end.
						if(line.length() == 0)
							break;
						
						StringTokenizer headerTokenizer = new StringTokenizer(line, ":");
						respStr += 
							"-> " + headerTokenizer.nextToken() + " : " 
								+ headerTokenizer.nextToken().trim() + "\n";
					}
				}
				catch(IOException e) {
					LOGGER.log(Level.SEVERE, "Shouldn't come here, but here we are...", e);
				}
			}
			if(LOGGER.isLoggable(Level.FINEST)) {
				level = Level.FINEST;
				respStr += "->\n";
				respStr += "-> " + _content + "\n";
			}
			LOGGER.log(level, respStr);
		}
	}
}
