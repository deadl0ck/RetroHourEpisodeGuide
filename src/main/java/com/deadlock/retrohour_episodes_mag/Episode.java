package com.deadlock.retrohour_episodes_mag;

public class Episode
{
	public final static String DEFAULT_IMAGE = "https://theretrohour.com/wp-content/uploads/2019/01/retrohour.png";
	private String title		= "";
	private String description	= "";
	private String image		= "";
	private String mp3			= "";
	private String readMoreLink	= "";
	private String dateStamp	= "";
	private String url			= "";

	private Episode(){}

	public Episode(String title, String description, String image, String mp3, String readMore, String dateStamp, String url)
	{
		this();
		this.setTitle(title);
		this.modifyTitle();

		this.setDescription(description);
		if (image == null || image.isEmpty())
			image = DEFAULT_IMAGE;
		this.setImage(image);
		this.setMp3(mp3);
		this.setReadMoreLink(readMore);
		this.setDateStamp(dateStamp);
		this.setUrl(url);
	}

	private void modifyTitle()
	{
		String search =  " - The Retro Hour";
		int pos = this.getTitle().indexOf(search);
		if (pos == -1)
			return;

		String tempTitle1 = this.getTitle().substring(0, pos).trim();
		String episodeNum = this.getTitle().substring(pos + search.length()).trim();
		String newTitle = episodeNum + " - " + tempTitle1;
		this.setTitle(newTitle);
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getImage() {
		return image;
	}

	public String getMp3() {
		return mp3;
	}

	private void setTitle(String title) {
		this.title = title;
	}

	private void setDescription(String description) {
		description = description.replaceAll("\"", "'");
		this.description = description + "...";
	}

	private void setImage(String image) {
		this.image = image;
	}

	private void setMp3(String mp3) {
		this.mp3 = mp3;
	}

	public String getReadMoreLink() {
		return readMoreLink;
	}

	private void setReadMoreLink(String readMoreLink) {
		this.readMoreLink = readMoreLink;
	}

	public String getDateStamp() {
		return dateStamp;
	}

	private void setDateStamp(String dateStamp) {
		this.dateStamp = dateStamp;
	}

	public String getUrl()
	{
		return url;
	}

	private void setUrl(String url)
	{
		this.url = url;
	}


}
