package com.hapiware.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;




/**
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class HttpResponse
{
	private final static Logger LOGGER = Logger.getLogger(HttpResponse.class.getName());

	
	enum PrefixMultiplier {
		KILO(1024l, "k"),
		MEGA(1024l^2, "M"),
		GIGA(1024l^3, "G"),
		TERA(1024l^4, "T"),
		PETA(1024l^5, "P"),
		EXA(1024l^6, "E");
		
		long multiplier;
		String prefix;
		
		PrefixMultiplier(long multiplier, String prefix)
		{
			this.multiplier = multiplier;
			this.prefix = prefix;
		}
	};

	
	private final static String HTTP_VERSION = "HTTP/1.1";
	private final static String CRLF = "\r\n";
	private final static String DIR_CLASS = "directory";
	private final static String ZIP_CLASS = "zip";
	private final static String JAR_CLASS = "jar";
	
	private final HttpRequest _request;
	
	/**
	 * This variable is only for loggin purpouses.
	 */
	private String _contentForLogging;
	
	
	public HttpResponse(HttpRequest request)
	{
		_request = request;
	}
	
	private String htmlBody(String content)
	{
	  	//<link href="../css/#{globalBB.style}/style.min.css" rel="stylesheet" type="text/css" />

		String retVal = "";
		retVal += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
		retVal += "<html><head>\n";
		retVal += "<link href=\"__hapiware__/css/style.css\" rel=\"stylesheet\" type=\"text/css\" />\n";
		retVal += "</head><body>\n";
		retVal += content;
		retVal += "</body></html>\n";
		return retVal;
	}
	
	private String addTableRow(
		String documentClass,
		String uriPath,
		String fileName,
		Long fileSize,
		String fileModifiedDate,
		String fileModifiedTime
	)
	{
		String linkForFileName = null;
		if(documentClass.equals(DIR_CLASS))
			linkForFileName = "<a href=\"" + uriPath + fileName + "/\">[" + fileName + "]</a>";
		else
			linkForFileName = "<a href=\"" + uriPath + fileName + "\">" + fileName + "</a>";
		String peekLink  = "";
		if(documentClass.equals(ZIP_CLASS) || documentClass.equals(JAR_CLASS))
			peekLink = "<a href=\"" + uriPath + fileName + "?op=peek" + "\"/>";
		return 
			addTableRowFixed(
				documentClass,
				linkForFileName,
				peekLink,
				fileSize,
				fileModifiedDate,
				fileModifiedTime)
			;
	}
	
	private String addTableRowFixed(
		String documentClass,
		String fileName,
		String peekLink,
		Long fileSize,
		String fileModifiedDate,
		String fileModifiedTime
	)
	{
		String row = "  <tr>\n";
		if(peekLink.length() > 0)
			row += "    <td class=\"peek\">" + peekLink + "</td>\n";
		else
			row += "    <td/>\n";
		row += "    <td class=\""+ documentClass + "\"/>\n";
		row += "    <td class=\"file-name\">" + fileName + "</td>\n";
		row +=
			"    <td class=\"file-size\">"
			+ (fileSize == null ? "" : fileSizeToString(fileSize))
			+ "</td>\n";
		row += "    <td class=\"file-modified-date\">" + fileModifiedDate + "</td>\n";
		row += "    <td class=\"file-modified-time\">" + fileModifiedTime + "</td>\n";
		row += "  </tr>\n";
		return row;
	}
	
	private String fileSizeToString(long fileSize)
	{
		for(PrefixMultiplier pm : PrefixMultiplier.values())
			if(fileSize >= pm.multiplier)
				return fileSize / pm.multiplier + " " + pm.prefix + "B";
		return fileSize + " B";
	}
	
	
	private void writeHeader(
		OutputStream os,
		HttpStatusCode statusCode,
		String contentType,
		long contentLength
	) throws IOException
	{
		String responseHeader = HTTP_VERSION + " ";
		responseHeader += statusCode.getStatusCode() + " " + statusCode.getReasonPhrase() + CRLF;
		if(statusCode == HttpStatusCode.SC405) {
			responseHeader += "Allow: ";
			HttpRequest.SupportedRequestMethod m[] = HttpRequest.SupportedRequestMethod.values();
			for(int i = 0; i < m.length - 1; i++)
				responseHeader += m[i] + ", ";
			responseHeader += m[m.length - 1];
		}
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);
		responseHeader += "Date: " + df.format(new Date()) + CRLF;
		responseHeader += "Server: FileWeb/1.0.0 (Java/5.0)" + CRLF;
		responseHeader += "Content-Type: " + contentType + CRLF;
		responseHeader += "Content-Length: " + contentLength + CRLF;
		responseHeader += "Connection: close" + CRLF;
		responseHeader += CRLF;
		log(responseHeader, statusCode);
		os.write(responseHeader.getBytes());
	}
	
	
	private void writeFavicon(OutputStream os)
		throws
			IOException
	{
		InputStream is = null;
		try {
			is =
				Thread
					.currentThread()
					.getContextClassLoader()
					.getResourceAsStream("img/favicon.ico");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while((b = is.read()) != -1)
				out.write(b);
			byte[] favicon = out.toByteArray();
			out.close();
			_contentForLogging = "favicon.ico";
			String contentType = "image/x-icon";
			writeHeader(os, HttpStatusCode.SC200, contentType, favicon.length);
			os.write(favicon);
		}
		finally {
			if(is != null)
				is.close();
		}
	}
	
	private void writeCss(OutputStream os)
		throws
			IOException
	{
		InputStream is = null;
		try {
			is =
				Thread
					.currentThread()
					.getContextClassLoader()
					.getResourceAsStream("css/style.css");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while((b = is.read()) != -1)
				out.write(b);
			byte[] styleSheet = out.toByteArray();
			out.close();
			_contentForLogging = "css/style.css";
			String contentType = "text/css";
			writeHeader(os, HttpStatusCode.SC200, contentType, styleSheet.length);
			os.write(styleSheet);
		}
		finally {
			if(is != null)
				is.close();
		}
	}
	
	private void writeDirectories(OutputStream os)
		throws
			IOException
	{
		String uriPath = _request.getUri().getPath();
		File directory = new File(".", uriPath);
		if(!directory.exists()) {
			LOGGER.info(uriPath + " was not found.");
			HttpStatusCode statusCode = HttpStatusCode.SC404;
			String content =
				htmlBody(
					statusCode.getStatusCode() + " : " + statusCode.getReasonPhrase() + "\n"
				);
			writeHeader(os, statusCode, "text/html", content.length());
			os.write(content.getBytes());
			return;
		}
		
		String fileList = "<table>\n";
		if(!uriPath.equals("/")) {
			String parent = uriPath.substring(0, uriPath.length() - 2);
			int i = parent.lastIndexOf("/");
			fileList +=
				addTableRowFixed(DIR_CLASS, "<a href=\"/\">[root]</a>", "", null, "", "");
			fileList += 
				addTableRowFixed(
					DIR_CLASS,
					"<a href=\"" + parent.subSequence(0, i) + "/\">[..]</a>",
					"",
					null,
					"",
					""
				);
		}
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
		for(File f : directory.listFiles()) {
			if(f.isDirectory()) {
				Date d = new Date(f.lastModified());
				fileList +=
					addTableRow(
						DIR_CLASS,
						uriPath,
						f.getName(),
						null,
						dateFormat.format(d),
						timeFormat.format(d)
					);
			}
		}
		for(File f : directory.listFiles()) {
			if(f.isFile()) {
				Date d = new Date(f.lastModified());
				DocType type = new DocType(f.getName());
				fileList +=
					addTableRow(
						type.getDocumentClass(),
						uriPath,
						f.getName(),
						f.length(),
						dateFormat.format(d),
						timeFormat.format(d)
					);
			}
		}
		fileList += "</table>\n";
		String content = htmlBody(fileList);
		_contentForLogging = content;
		writeHeader(os, HttpStatusCode.SC200, "text/html", content.length());
		os.write(content.getBytes());
	}
	
	private void writeFiles(OutputStream os)
		throws
			IOException
	{
		String uriPath = _request.getUri().getPath();
		BufferedInputStream is = null;
		try {
			File f = new File(".", uriPath);
			if(f.exists()) {
				DocType docType = new DocType(f.getName());
				_contentForLogging = f.getName();
				writeHeader(os, HttpStatusCode.SC200, docType.getMimeType(), f.length());
				is = new BufferedInputStream(new FileInputStream(f));
				byte[] buffer = new byte[4096];
				while((is.read(buffer)) != -1)
					os.write(buffer);
			}
			else {
				LOGGER.info(uriPath + " was not found.");
				HttpStatusCode statusCode = HttpStatusCode.SC404;
				String content =
					htmlBody(
						statusCode.getStatusCode() + " : " + statusCode.getReasonPhrase() + "\n"
					);
				_contentForLogging = content;
				writeHeader(os, statusCode, "text/html", content.length());
				os.write(content.getBytes());
			}
		}
		catch(FileNotFoundException e) {
			LOGGER.log(Level.INFO, uriPath + " was not found.", e);
		}
		catch(IOException e) {
			LOGGER.log(Level.SEVERE, "Error reading file: " + uriPath, e);
		}
		finally {
			try {
				if(is != null)
					is.close();
			}
			catch(IOException e) {
				// Does nothing.
			}
		}
	}
	
	public void write(OutputStream os) throws IOException
	{
		if(_request.getStatusCode() != HttpStatusCode.SC200) {
			String contentType = "text/html";
			writeHeader(os, _request.getStatusCode(), contentType, 0);
			return;
		}
		
		String uriPath = _request.getUri().getPath();
		if(uriPath.contains("favicon.ico")) {
			writeFavicon(os);
			return;
		}
		
		if(uriPath.equals("/__hapiware__/css/style.css")) {
			writeCss(os);
			return;
		}
		
		if(uriPath.endsWith("/"))
			writeDirectories(os);
		else
			writeFiles(os);
	}

	
	private void log(String responseHeader, HttpStatusCode statusCode)
	{
		if(
			LOGGER.isLoggable(Level.FINE)
			|| LOGGER.isLoggable(Level.FINER) 
			|| LOGGER.isLoggable(Level.FINEST)
		) {
			String respStr = "";
			Level level = Level.FINE;
			respStr += "\n-> HTTP Version: " + HTTP_VERSION + "\n";
			respStr += "-> Status Code: " + statusCode.getStatusCode() + "\n";
			respStr += "-> Reason Phrase: " + statusCode.getReasonPhrase() + "\n";
			if(LOGGER.isLoggable(Level.FINER) || LOGGER.isLoggable(Level.FINEST)) {
				level = Level.FINER;
				respStr += "->\n";
				BufferedReader reader =	new BufferedReader(new StringReader(responseHeader));
				
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
				respStr += "-> " + _contentForLogging + "\n";
			}
			LOGGER.log(level, respStr);
		}
	}
	
	
	static class DocType {
		private final static String[] PIC_TYPES = { "jpg", "jpeg", "png", "gif", "tiff" };
		private final static String[] JAR_TYPES = { "jar", "war", "ear" };
		private final static String APPLICATION_XML_CONFIG = readConfig("application-xml.conf");
		private final static String PLAIN_TEXT_CONFIG = readConfig("plain-text.conf");
		
		private final static String readConfig(String configName)
		{
			String retVal = "";
			try {
				BufferedReader reader =
					new BufferedReader(
						new InputStreamReader(
							Thread
								.currentThread()
								.getContextClassLoader()
								.getResourceAsStream(configName)
						)
					);
				Pattern commentPattern = Pattern.compile("^ *#.*");
				String line;
				while((line = reader.readLine()) != null) {
					if(line.trim().length() == 0 || commentPattern.matcher(line).matches())
						continue;

					retVal += line + "\n";
				}
			}
			catch(IOException e) {
				// Does nothing.
			}

			return retVal;
		}
		
		
		private final String _documentClass;
		private final String _mimeType;
		
		private DocType(String documentClass, String mimeType)
		{
			_documentClass = documentClass;
			_mimeType = mimeType;
		}
		
		public DocType(String fileName)
		{
			fileName = fileName.toLowerCase();
			DocType docType =
				getConfiguredType(fileName, APPLICATION_XML_CONFIG, "xml", "application/xml");
			if(docType == null)
				docType =
					getConfiguredType(fileName, PLAIN_TEXT_CONFIG, "text", "text/plain");
			if(docType == null)
				docType = getHtmlType(fileName);
			if(docType == null)
				docType = getPdfType(fileName);
			if(docType == null)
				docType = getPicType(fileName);
			if(docType == null)
				docType = getZipType(fileName);
			if(docType == null)
				docType = getJarType(fileName);
			if(docType == null) {
				_documentClass = "binary";
				_mimeType = "application/octet-stream"; 
			}
			else {
				_documentClass = docType.getDocumentClass();
				_mimeType = docType.getMimeType();
			}
		}

		private static DocType getHtmlType(String fileNameToCheck)
		{
			return
			 	Pattern.matches("^.*\\.html?$", fileNameToCheck) ? 
			 		new DocType("html", "text/html")
					: null;
		}
		
		private static DocType getPdfType(String fileNameToCheck)
		{
			return
				fileNameToCheck.endsWith(".pdf") ? new DocType("pdf", "application/pdf") : null;
		}
		
		private static DocType getPicType(String fileNameToCheck)
		{
			for(String type : PIC_TYPES)
				if(fileNameToCheck.endsWith(type))
					return new DocType(type, "image/" + type);
			
			return null;
		}
		
		private static DocType getZipType(String fileNameToCheck)
		{
			return
				fileNameToCheck.endsWith(".zip") ? new DocType(ZIP_CLASS, "application/zip") : null;
		}
		
		private static DocType getJarType(String fileNameToCheck)
		{
			for(String type : JAR_TYPES)
				if(fileNameToCheck.endsWith(type))
					return new DocType(JAR_CLASS, "application/octet-stream");
			
			return null;
		}
		
		private static DocType getConfiguredType(
			String fileNameToCheck,
			String config,
			String documentClass,
			String mimeType
		)
		{
			try {
				BufferedReader reader =	new BufferedReader(new StringReader(config));
				Pattern commentPattern = Pattern.compile("^ *#.*");
				String line;
				while((line = reader.readLine()) != null) {
					if(line.trim().length() == 0 || commentPattern.matcher(line).matches())
						continue;
					
					if(fileNameToCheck.endsWith(line.toLowerCase()))
						return new DocType(documentClass, mimeType);
				}
			}
			catch(IOException e) {
				// Does nothing.
			}
			
			return null;
		}
		
		public String getDocumentClass()
		{
			return _documentClass;
		}

		public String getMimeType()
		{
			return _mimeType;
		}
		
		public String toString()
		{
			return "[" + _documentClass + ":" + _mimeType + "]";
		}
	}
}
