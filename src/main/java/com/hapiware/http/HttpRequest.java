package com.hapiware.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * {@code HttpRequest} parses the incomimg HTTP request. <b>Notice</b> that message body for
 * the request is not used for any purpouse.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class HttpRequest
{
	private final static Logger LOGGER = Logger.getLogger(HttpRequest.class.getName());
	
	public enum SupportedRequestMethod { GET };
	
	private final SupportedRequestMethod _requestMethod;
	private final URI _uri;
	private final String _httpVersion;
	private final Map<String, String> _headerFields;
	private final HttpStatusCode _statusCode;
	
	
	/**
	 * Parses and creates a request from the {@link InputStream}.
	 * 
	 * @param stream
	 * 		The source of the request.
	 */
	public HttpRequest(InputStream stream)
	{
		HttpStatusCode statusCode = HttpStatusCode.SC500;
		SupportedRequestMethod requestMethod =  null;
		URI uri = null;
		String httpVersion = null;
		Map<String, String> headerFields = new HashMap<String, String>();
		try {
			BufferedReader reader =	new BufferedReader(new InputStreamReader(stream));
			
			// Reads a request line.
			String line = reader.readLine();
			if(line == null) {
				// Notice the finally block!
				return;
			}

			StringTokenizer tokenizer = new StringTokenizer(line);
			requestMethod = SupportedRequestMethod.valueOf(tokenizer.nextToken());
			uri = new URI(tokenizer.nextToken());
			httpVersion = tokenizer.nextToken();
			
			// Reads header fields.
			while((line = reader.readLine()) != null) {
				// Breaks when there are no more header fields.
				if(line.length() == 0)
					break;
				
				StringTokenizer fieldTokenizer = new StringTokenizer(line, ":");
				headerFields.put(fieldTokenizer.nextToken(), fieldTokenizer.nextToken().trim());
			}
			
			// Skips the content.
			statusCode = HttpStatusCode.SC200;
		}
		catch(IOException e) {
			statusCode = HttpStatusCode.SC500;
			LOGGER.log(Level.WARNING, "Reading a request failed.", e);
		}
		catch(URISyntaxException e) {
			statusCode = HttpStatusCode.SC500;
			LOGGER.log(Level.WARNING, "URI cannot be parsed.", e);
		}
		catch(IllegalArgumentException e) {
			statusCode = HttpStatusCode.SC405;
			LOGGER.log(
				Level.INFO,
				"Attempted request method is not supported. See the following stack trace...",
				e
			);
		}
		finally {
			_statusCode = statusCode;
			_requestMethod = requestMethod;
			_uri = uri;
			_httpVersion = httpVersion;
			_headerFields = Collections.unmodifiableMap(headerFields);
			log();
		}
	}

	/**
	 * Makes some logging about the request. The logging level affects how much information
	 * is shown. Levels used are, FINE, FINER and FINEST.
	 */
	private void log()
	{
		if(
			LOGGER.isLoggable(Level.FINE)
			|| LOGGER.isLoggable(Level.FINER) 
			|| LOGGER.isLoggable(Level.FINEST)
		) {
			String reqStr = "";
			Level level = Level.FINE;
			reqStr += "\n-> Method: " + _requestMethod + "\n";
			reqStr += "-> URI: " + _uri + "\n";
			reqStr += "-> Version: " + _httpVersion + "\n";
			if(LOGGER.isLoggable(Level.FINER) || LOGGER.isLoggable(Level.FINEST)) {
				level = Level.FINER;
				reqStr += "->\n";
				Iterator<Entry<String, String>> it = _headerFields.entrySet().iterator();
				while(it.hasNext()) {
					Entry<String, String> entry = it.next();
					reqStr += "-> " + entry.getKey() + " : " + entry.getValue() + "\n";
				}
			}
			if(LOGGER.isLoggable(Level.FINEST)) {
				level = Level.FINEST;
				reqStr += "->\n";
				reqStr += "-> [Message body is not supported]\n";
			}
			LOGGER.log(level, reqStr);
		}
	}
	

	/**
	 * Returns the status code of the parsed request.
	 * 
	 * @return
	 * 		Status code.
	 */
	public HttpStatusCode getStatusCode()
	{
		return _statusCode;
	}
	
	
	/**
	 * Returns the request method.
	 * 
	 * @return
	 * 		Request method.
	 */
	public SupportedRequestMethod getRequestMethod()
	{
		return _requestMethod;
	}


	/**
	 * Retunrs the request URI.
	 * 
	 * @return
	 * 		Request URI.
	 */
	public URI getUri()
	{
		return _uri;
	}


	/**
	 * Returns the HTTP version.
	 * 
	 * @return
	 * 		HTTP version.
	 */
	public String getHttpVersion()
	{
		return _httpVersion;
	}


	/**
	 * Returns header fields as a {@link Map}. The {@code key} contains the field name and
	 * the {@code value} has the value of the field.
	 * 
	 * @return
	 * 		Header fields.
	 */
	public Map<String, String> getHeaderFields()
	{
		return _headerFields;
	}
}
