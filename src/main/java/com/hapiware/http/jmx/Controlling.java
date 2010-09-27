package com.hapiware.http.jmx;

import com.hapiware.http.Controller;


/**
 * The implementation of the {@link ControllingMBean}.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * @see Controller
 */
public class Controlling
	implements
		ControllingMBean
{
	private final Controller _controller;
	
	
	public Controlling(Controller controller)
	{
		_controller = controller;
	}
	
	public int getNumberOfCurrentRequests()
	{
		return _controller.getNumberOfCurrentRequests();
	}

	public long getNumberOfTotalRequests()
	{
		return _controller.getNumberOfTotalRequests();
	}
	
	public long getNumberOfRequests()
	{
		return _controller.getNumberOfRequests();
	}
	
	public long getNumberOfErrors()
	{
		return _controller.getNumberOfErrors();
	}

	public long getUptime()
	{
		return _controller.getUptime();
	}

	public void stop()
	{
		_controller.stopServer();
	}

	public void resetErrors()
	{
		_controller.resetErrors();
	}
	
	public void resetNumberOfRequests()
	{
		_controller.resetNumberOfRequests();
	}
}
