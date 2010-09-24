package com.hapiware.http.jmx;

public interface ControllingMBean
{
	public void stop();
	public void resetErrors();
	public void resetNumberOfRequests();
	public int getNumberOfCurrentRequests();
	public long getNumberOfRequests();
	public long getNumberOfTotalRequests();
	public long getNumberOfErrors();
	public long getUptime();
}