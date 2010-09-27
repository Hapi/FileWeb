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
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * {@code HttpResponse} gets the {@link HttpRequest} and writes the HTTP response to
 * {@link OutputStream}.
 * <p/>
 * In general the response is a HTML representation of the current directory. However, if
 * a file is requested then what happens depends on the ending of the file name. Basically
 * there are two possible results; file is shown as is in browser or browser starts to
 * download it. In general, text based files are shown and unknown (i.e. potential binary)
 * files are downloaded. See {@link DocType} how different file name endings are handled.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * @see #write(OutputStream)
 * @see DocType
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
	private final static String HOME_CLASS = "home";
	private final static String PARENT_CLASS = "parent";
	private final static String DIR_CLASS = "directory";
	private final static String ZIP_CLASS = "zip";
	private final static String JAR_CLASS = "jar";
	private final static String RESOURCE_ROOT = "/com.hapiware.style/";
	private final static String CSS =  "css/";
	private final static String CSS_ROOT =  RESOURCE_ROOT + CSS;
	private final static String CSS_NAME = "style.css";
	private final static String TEXT_PLAIN = "text/plain";
	private final static String APPLICATION_XML = "application/xml";
	private final static int MAX_NUMBER_OF_ZIP_ENTRIES = 500;
	
	
	private final HttpRequest _request;
	
	/**
	 * This variable is only for logging purpouses.
	 */
	private String _contentForLogging;
	
	
	public HttpResponse(HttpRequest request)
	{
		_request = request;
	}

	
	/**
	 * Writes the HTTP response to a given {@link OutputStream}.
	 * 
	 * @param os
	 * 		The {@link OutputStream} where the HTTP response is to be written.
	 * 
	 * @throws IOException
	 * 		If an I/O error occurs.
	 */
	public void write(OutputStream os)
		throws
			IOException
	{
		if(_request.getStatusCode() != HttpStatusCode.SC200) {
			String contentType = "text/html";
			writeHeader(os, _request.getStatusCode(), contentType, 0);
			return;
		}
		
		String uriPath = _request.getUri().getPath();
		
		// Handles favicon.ico
		if(uriPath.contains("favicon.ico")) {
			writeFavicon(os);
			return;
		}
		
		// Handles CSS style sheet request.
		if(uriPath.equals(CSS_ROOT + CSS_NAME)) {
			writeCss(os);
			return;
		}
		
		// Handles required resources by url values (images).
		if(uriPath.contains(CSS_ROOT)) {
			writeImage(os, uriPath.substring(CSS_ROOT.length()));
			return;
		}
		
		// Handles directories and files.
		if(uriPath.endsWith("/"))
			writeDirectories(os);
		else
			writeFiles(os);
	}
	


	private String htmlBody(String content)
	{
		String retVal = "";
		retVal += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
		retVal += "<html><head>\n";
		retVal +=
			"<link href=\"" + CSS_ROOT + CSS_NAME + "\" rel=\"stylesheet\" type=\"text/css\" />\n";
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
			linkForFileName = "<a href=\"" + uriPath + fileName + "/\">" + fileName + "</a>";
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
				fileModifiedTime
			);
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
		row += "    <td class=\""+ documentClass + "\"/>\n";
		row += "    <td class=\"peek\">" + peekLink + "</td>\n";
		row += "    <td class=\"file-name\">" + fileName + "</td>\n";
		String[] size = fileSizeToString(fileSize);
		row +="    <td class=\"file-size\">" + (size == null ? "" : size[0]) + "</td>\n";
		row +="    <td class=\"file-unit\">" + (size == null ? "" : size[1]) + "</td>\n";
		row += "    <td class=\"file-modified-date\">" + fileModifiedDate + "</td>\n";
		row += "    <td class=\"file-modified-time\">" + fileModifiedTime + "</td>\n";
		row += "  </tr>\n";
		return row;
	}
	
	
	/**
	 * Converts file size to a {@code String} representation using prefix multipliers
	 * (i.e. k, M, G, etc.).
	 * 
	 * @param fileSize
	 * 		File size.
	 * 
	 * @return
	 * 		{@code String[0]} has the file size and {@code String[1]} has the prefix multiplier.
	 */
	private String[] fileSizeToString(Long fileSize)
	{
		if(fileSize == null)
			return null;
		
		for(PrefixMultiplier pm : PrefixMultiplier.values())
			if(fileSize >= pm.multiplier)
				return
					new String [] { ((Long)(fileSize / pm.multiplier)).toString(), pm.prefix + "B"};
		return new String[] { ((Long)fileSize).toString(), "B" };
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
		responseHeader += "Server: FileWeb/1.0 (Java/5.0)" + CRLF;
		responseHeader += "Content-Type: " + contentType + CRLF;
		responseHeader += "Content-Length: " + contentLength + CRLF;
		responseHeader += "Connection: close" + CRLF;
		responseHeader += CRLF;
		log(responseHeader, statusCode);
		os.write(responseHeader.getBytes());
	}
	
	
	private void writeError(OutputStream os)
		throws
			IOException
	{
		HttpStatusCode statusCode = HttpStatusCode.SC404;
		String content =
			htmlBody(
				statusCode.getStatusCode() + " : " + statusCode.getReasonPhrase() + "\n"
			);
		_contentForLogging = content;
		writeHeader(os, statusCode, "text/html", content.length());
		os.write(content.getBytes());
	}
	

	/**
	 * Fetches a requested favicon.ico and writes it to {@link OutputStream}.
	 * 
	 * @param os
	 * 
	 * @throws IOException
	 */
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
			if(is == null) {
				writeHeader(os, HttpStatusCode.SC204, "", 0);
				return;
			}
			
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
	
	/**
	 * Fetches a requested CSS style sheet and writes it to {@link OutputStream}.
	 * 
	 * @param os
	 * 
	 * @throws IOException
	 */
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
					.getResourceAsStream(CSS + CSS_NAME);
			if(is == null) {
				writeHeader(os, HttpStatusCode.SC204, "", 0);
				throw new IllegalStateException(CSS + CSS_NAME + " is missing.");
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while((b = is.read()) != -1)
				out.write(b);
			byte[] styleSheet = out.toByteArray();
			out.close();
			_contentForLogging = CSS_ROOT + CSS_NAME;
			String contentType = "text/css";
			writeHeader(os, HttpStatusCode.SC200, contentType, styleSheet.length);
			os.write(styleSheet);
		}
		finally {
			if(is != null)
				is.close();
		}
	}
	
	
	/**
	 * Fetches a requested image and writes it to {@link OutputStream}.
	 * 
	 * @param os
	 * 
	 * @param imageName
	 * 
	 * @throws IOException
	 */
	private void writeImage(OutputStream os, String imageName)
		throws
			IOException
	{
		InputStream is = null;
		try {
			is =
				Thread
					.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(imageName);
			if(is == null) {
				writeHeader(os, HttpStatusCode.SC204, "", 0);
				throw new IllegalStateException("Image '" + imageName + "' was not found.");
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int b;
			while((b = is.read()) != -1)
				out.write(b);
			byte[] image = out.toByteArray();
			out.close();
			_contentForLogging = imageName;
			DocType docType = new DocType(imageName);
			writeHeader(os, HttpStatusCode.SC200, docType.getMimeType(), image.length);
			os.write(image);
		}
		finally {
			if(is != null)
				is.close();
		}
	}

	
	/**
	 * Writes the requested directory content to {@link OutputStream} as an HTML document.
	 * 
	 * @param os
	 * 
	 * @throws IOException
	 */
	private void writeDirectories(OutputStream os)
		throws
			IOException
	{
		String uriPath = _request.getUri().getPath();
		File directory = new File(".", uriPath);
		if(!directory.exists()) {
			LOGGER.info(uriPath + " was not found.");
			writeError(os);
			return;
		}
		
		String fileList = "<h1>" + uriPath + "</h1>\n";
		fileList += "<table>\n";
		if(!uriPath.equals("/")) {
			String parent = uriPath.substring(0, uriPath.length() - 2);
			int i = parent.lastIndexOf("/");
			fileList +=
				addTableRowFixed(HOME_CLASS, "<a href=\"/\">root</a>", "", null, "", "");
			fileList += 
				addTableRowFixed(
					PARENT_CLASS,
					"<a href=\"" + parent.substring(0, i) + "/\">..</a>",
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
	
	
	/**
	 * Writes a requested file with a correct MIME type to {@link OutputStream}.
	 * 
	 * @param os
	 * 
	 * @throws IOException
	 */
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
				String query = _request.getUri().getQuery(); 
				if(query != null && query.equals("op=peek")) {
					// Lists the content of jar and zip files.
					
					_contentForLogging = f.getName() + " : peek()";
					if(
						docType.getDocumentClass().equals(JAR_CLASS) ||
						docType.getDocumentClass().equals(ZIP_CLASS)
					) {
						ZipFile z = new ZipFile(f);
						int numOfZipEntries = 0;
						Enumeration<? extends ZipEntry> entries = z.entries();
						String list = "";
						while(
							numOfZipEntries++ < MAX_NUMBER_OF_ZIP_ENTRIES
							&& entries.hasMoreElements()
						)
							list += entries.nextElement().getName() + "\n";
						
						if(numOfZipEntries >= MAX_NUMBER_OF_ZIP_ENTRIES)
							list += "... entries. First " + numOfZipEntries + " was shown.\n";
						
						writeHeader(os, HttpStatusCode.SC200, TEXT_PLAIN, list.length());
						os.write(list.getBytes());
					}
					else {
						LOGGER.warning("An attempt to peek uri: " + uriPath);
						writeError(os);
					}
				}
				else {
					_contentForLogging = f.getName();
					writeHeader(os, HttpStatusCode.SC200, docType.getMimeType(), f.length());
					is = new BufferedInputStream(new FileInputStream(f));
					byte[] buffer = new byte[4096];
					while((is.read(buffer)) != -1)
						os.write(buffer);
				}
			}
			else {
				LOGGER.info(uriPath + " was not found.");
				writeError(os);
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
	
	
	/**
	 * Makes some logging about the response. The logging level affects how much information
	 * is shown. Levels used are, FINE, FINER and FINEST.
	 * 
	 * @param responseHeader
	 * 		A header to be parsed.
	 * 
	 * @param statusCode
	 * 		Current status code.
	 */
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
					
					// Reads header fields.
					while((line = reader.readLine()) != null) {
						// Breaks when there are no more header fields.
						if(line.length() == 0)
							break;
						
						StringTokenizer fieldTokenizer = new StringTokenizer(line, ":");
						respStr += 
							"-> " + fieldTokenizer.nextToken() + " : " 
								+ fieldTokenizer.nextToken().trim() + "\n";
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
	
	
	/**
	 * This class figures out the document classes and MIME types for a given file name.
	 * Document class is used CSS styling purpouses and MIME for obvious reasons.
	 * <p/>
	 * Document type recognition is based how file name ends (e.g. .jpg, .txt, .pdf, README, etc).
	 * There are two kind of recognition endings; fixed and customisable. Fixed endings are:
	 * 	<ul>
	 * 		<li>.jpg -> class="picture", MIME = image/jpeg</li>
	 * 		<li>.jpeg -> class="picture", MIME = image/jpeg</li>
	 * 		<li>.png -> class="picture", MIME = image/png</li>
	 * 		<li>.gif -> class="picture", MIME = image/gif</li>
	 * 		<li>.tiff -> class="picture", MIME = image/tiff</li>
	 * 		<li>.jar -> class="jar", MIME = application/octet-stream</li>
	 * 		<li>.war -> class="jar", MIME = application/octet-stream</li>
	 * 		<li>.ear -> class="jar", MIME = application/octet-stream</li>
	 * 		<li>.zip -> class="zip", MIME = application/zip</li>
	 * 		<li>.pdf -> class="pdf", MIME = application/pdf</li>
	 * 		<li>.html -> class="html", MIME = text/html</li>
	 * 		<li>.htm -> class="html", MIME = text/html</li>
	 * 	</ul>
	 *
	 * Customisable endings are further divided to two different groups based on their MIME type,
	 * {@code plain/text} and {@code application/xml} and their document classes are
	 * {@code class="text"} and {@code class="xml"} respectively. To have either MIME type for
	 * a certain file name ending the respective {@code .conf} file must be edited.
	 * 
	 * If file name does not match either fixed or customisable set of endings then
	 * {@code class="unknown"} and MIME type is {@code application/octet-stream}.
	 * 
	 * 
	 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
	 *
	 */
	public static class DocType {
		private final static String[] PIC_TYPES = { "jpg", "jpeg", "png", "gif", "tiff" };
		private final static String[] JAR_TYPES = { "jar", "war", "ear" };
		private final static String APPLICATION_XML_CONFIG = readConfig("application-xml.conf");
		private final static String PLAIN_TEXT_CONFIG = readConfig("plain-text.conf");
		

		/**
		 * Reads a customisable file name ending file and converts it to {@link String}.
		 * 
		 * @param configName
		 * 		File name for the configuration file.
		 * 
		 * @return
		 * 		Configuration file as {@link String}.
		 */
		private final static String readConfig(String configName)
		{
			String retVal = "";
			try {
				InputStream is =
					Thread
						.currentThread()
						.getContextClassLoader()
						.getResourceAsStream(configName);
				if(is == null) {
					LOGGER.log(Level.WARNING, "'" + configName + "' is missing.");
					return "";
				}
				BufferedReader reader =	new BufferedReader(new InputStreamReader(is));
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
				getConfiguredType(fileName, PLAIN_TEXT_CONFIG, "text", TEXT_PLAIN);
			if(docType == null)
				docType =
					getConfiguredType(fileName, APPLICATION_XML_CONFIG, "xml", APPLICATION_XML);
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
				_documentClass = "unknown";
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
				if(fileNameToCheck.endsWith("." + type))
					return new DocType("image", "image/" + (type.equals("jpg") ? "jpeg" : type));
			
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
		
		
		/**
		 * Returns the document class.
		 * 
		 * @return
		 * 		Document class.
		 */
		public String getDocumentClass()
		{
			return _documentClass;
		}

		/**
		 * Returns the MIME type.
		 * 
		 * @return
		 * 		MIME type.
		 */
		public String getMimeType()
		{
			return _mimeType;
		}
		
		/**
		 * Prints only the document class and the MIME type. The form is:
		 * <pre>
		 * 	[DOCUMENT_CLASS:MIME_TYPE]
		 * </pre>
		 */
		public String toString()
		{
			return "[" + _documentClass + ":" + _mimeType + "]";
		}
	}
}
