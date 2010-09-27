package com.hapiware.http;


/**
 * {@code Controller} is an utility class for collecting some statistical information about
 * running {@link FileWeb}. All the utility methods are synchronized. 
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class Controller
{
	private final FileWeb _fileWeb;

	private int _numberOfCurrentRequests = 0;
	private long _numberOfRequests = 0;
	private long _numberOfTotalRequests = 0;
	private long _numberOfErrors = 0;
	private long _startTime;
	
	public Controller(FileWeb fileWeb)
	{
		_fileWeb = fileWeb;
		resetStartTime();
 	}

	public synchronized void enter()
	{
		_numberOfCurrentRequests++;
		_numberOfRequests++;
		_numberOfTotalRequests++;
	}
	
	public synchronized void exit()
	{
		if(_numberOfCurrentRequests > 0)
			_numberOfCurrentRequests--;
	}
	
	public synchronized int getNumberOfCurrentRequests()
	{
		return _numberOfCurrentRequests;
	}

	public synchronized long getNumberOfRequests()
	{
		return _numberOfRequests;
	}

	public synchronized long getNumberOfTotalRequests()
	{
		return _numberOfTotalRequests;
	}

	public synchronized void registerError()
	{
		_numberOfErrors++;
	}
	
	public synchronized long getNumberOfErrors()
	{
		return _numberOfErrors;
	}

	public synchronized void resetNumberOfRequests()
	{
		_numberOfRequests = 0;
	}
	
	public synchronized void resetStartTime()
	{
		_startTime = System.currentTimeMillis();
	}
	
	public synchronized long getStartTime()
	{
		return _startTime;
	}
	
	public synchronized long getUptime()
	{
		return (System.currentTimeMillis() - _startTime) / 1000;
	}
	
	public synchronized void stopServer()
	{
		_fileWeb.stop();
	}
	
	public synchronized void resetErrors()
	{
		_numberOfErrors = 0;
	}
}
