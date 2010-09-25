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
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRequest
{
	private final static Logger LOGGER = Logger.getLogger(HttpRequest.class.getName());
	
	public enum SupportedRequestMethod { GET };
	
	private final SupportedRequestMethod _requestMethod;
	private final URI _uri;
	private final String _httpVersion;
	private final Map<String, String> _headers;
	private final HttpStatusCode _statusCode;
	
	
	public HttpRequest(InputStream stream)
	{
		HttpStatusCode statusCode = HttpStatusCode.SC500;
		SupportedRequestMethod requestMethod =  null;
		URI uri = null;
		String httpVersion = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			BufferedReader reader =	new BufferedReader(new InputStreamReader(stream));
			
			// Reads a request line.
			String line = reader.readLine();
			if(line == null) {
				// Notice the finally block!
				return;
			}

			StringTokenizer tokenizer = new StringTokenizer(line);
			try {
				requestMethod = SupportedRequestMethod.valueOf(tokenizer.nextToken());
			}
			catch(IllegalArgumentException e) {
				statusCode = HttpStatusCode.SC405;
				return;
			}
			uri = new URI(tokenizer.nextToken());
			httpVersion = tokenizer.nextToken();
			
			// Reads headers.
			while((line = reader.readLine()) != null) {
				// Breaks when headers end.
				if(line.length() == 0)
					break;
				
				StringTokenizer headerTokenizer = new StringTokenizer(line, ":");
				headers.put(headerTokenizer.nextToken(), headerTokenizer.nextToken().trim());
			}
			
			// Skips the content.
			statusCode = HttpStatusCode.SC200;
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		catch(URISyntaxException e) {
			e.printStackTrace();
		}
		catch(NoSuchElementException e) {
			e.printStackTrace();
		}
		finally {
			_statusCode = statusCode;
			_requestMethod = requestMethod;
			_uri = uri;
			_httpVersion = httpVersion;
			_headers = Collections.unmodifiableMap(headers);
			log();
		}
	}

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
				Iterator<Entry<String, String>> it = _headers.entrySet().iterator();
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
	

	public HttpStatusCode getStatusCode()
	{
		return _statusCode;
	}
	
	public SupportedRequestMethod getRequestMethod()
	{
		return _requestMethod;
	}


	public URI getUri()
	{
		return _uri;
	}


	public String getHttpVersion()
	{
		return _httpVersion;
	}


	public Map<String, String> getHeaders()
	{
		return _headers;
	}
}
