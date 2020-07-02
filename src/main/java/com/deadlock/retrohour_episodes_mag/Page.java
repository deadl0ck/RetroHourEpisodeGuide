package com.deadlock.retrohour_episodes_mag;

public class Page
{
	private String url	= "";
	private String date	= "";

	private Page(){}

	public Page(String url, String date)
	{
		this();
		this.setUrl(url);
		this.setDate(date);
	}

	public String getUrl() {
		return url;
	}
	private void setUrl(String url) {
		this.url = url;
	}
	public String getDate() {
		return date;
	}
	private void setDate(String date) {
		this.date = date;
	}
}
