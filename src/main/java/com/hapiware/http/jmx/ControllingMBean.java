package com.hapiware.http.jmx;


/**
 * A simple MBean to collect some statistics at runtime.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public interface ControllingMBean
{
	/**
	 * Returns the number of current running requests (i.e. active threads).
	 * 
	 * @return
	 * 		Number of requests.
	 */
	public int getNumberOfCurrentRequests();
	
	/**
	 * Returns the number of requests since the last reset.
	 *  
	 * @return
	 * 		Number of requests.
	 * 
	 * @see #resetNumberOfRequests()
	 * @see #getNumberOfTotalRequests()
	 */
	public long getNumberOfRequests();
	
	/**
	 * Returns the number of total requests since the starting of the server.
	 *  
	 * @return
	 * 		Number of total requests.
	 * 
	 * @see #getNumberOfRequests()
	 */
	public long getNumberOfTotalRequests();
	
	/**
	 * Returns the number of errors since the last reset.
	 * @return
	 * 		Number of errors.
	 */
	public long getNumberOfErrors();
	
	/**
	 * Returns the uptime of the server in seconds.
	 * 
	 * @return
	 * 		Uptime in seconds.
	 */
	public long getUptime();

	/**
	 * Stops the server.
	 */
	public void stop();
	
	/**
	 * Resets the number of errors counter.
	 * 
	 * @see #getNumberOfErrors()
	 */
	public void resetErrors();
	
	/**
	 * Resets the number of requests counter.
	 * 
	 * @see #getNumberOfRequests()
	 * @see #getNumberOfTotalRequests()
	 */
	public void resetNumberOfRequests();
}