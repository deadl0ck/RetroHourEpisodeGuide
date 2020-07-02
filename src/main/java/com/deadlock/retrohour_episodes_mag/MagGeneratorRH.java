package com.deadlock.retrohour_episodes_mag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

public class MagGeneratorRH
{
    public final static String START_URL			= "https://theretrohour.com/episodes/";
//    public final static String START_URL			= "https://theretrohour.com/page/23/";


    public final static String PDF_NAME				= "/Users/martinstephenson/Desktop/The Retro Hour Episode Guide.pdf";
    public final static String CSV_NAME				= "/Users/martinstephenson/Desktop/The Retro Hour Episode Guide.csv";
    public final static double IMAGE_SCALE			= 0.85;
    public final static double LARGE_IMAGE_SCALE	= 0.55;
    public final static String GUIDE_TITLE			= "\n\n\nThe Retro Hour Podcast\n\nEpisode Guide";

    public final static int NUM_CONTENT_ITEMS_PER_PAGE	= 40;

    // Use these for debug to build a smaller PDF
    public final static boolean FULL_PDF				= true;
//    public final static String PARTIAL_PDF_PATTERN		= "2019/";

    public static void main(String[] args) throws IOException, DocumentException, InterruptedException
    {
    	debugOut("Starting to process....");
        MagGeneratorRH gen = new MagGeneratorRH();
        gen.process();
        debugOut("Processing complete - PDF is available at: " + PDF_NAME);
    }

    private List<Page> getPageList() throws IOException
    {
    	List<Page> pages = new ArrayList<Page>();

    	String nextPage = START_URL;

    	while (nextPage != null)
    	{
    		System.out.println("Processing " + nextPage);
    		Elements body = this.getElementsFromHtml(nextPage);

	        // Get the tag and ID we are interested in
	        Element episodesList = this.getTagAndClassAndChildren(body, "div", "oxy-posts");
	        Elements children = episodesList.children();
	        String episode = null;
	        String url = null;
	        for (Element child: children)
	        {
	        	Element detail = this.getTagAndClassAndChildren(child.children(), "a", "oxy-post-title");
	        	if (detail != null) {	// This changes somewhere
		          	int pos = detail.text().lastIndexOf("-");
		          	episode = detail.text().substring(pos + 1).trim();
		          	url = detail.attr("href");
	        	}
	        	else
	        	{
		        	Element altDetail = this.getTagAndClassAndChildren(child.children(), "h2", "oxy-post-title");
		          	int pos = altDetail.text().lastIndexOf("-");
		          	episode = altDetail.text().substring(pos + 1).trim();
		          	url = child.attr("href");
	        	}
	        	System.out.println("URL     : " + url);
	        	System.out.println("Episode : " + episode);
	          	pages.add(new Page(url, episode));
	        }
	        nextPage = this.getNextPageUrl(body);
    	}
        return pages;
    }

    private String getTitle(Element post)
    {
    	Element title = this.getTagAndIdAndChildren(post.children(), "h1", "headline-60-110");
    	return title.text().trim();
    }

    private String getNextPageUrl(Elements body) {
    	Elements elements = body.select("a");

    	for (Element element: elements) {
    		String c = element.attr("class");
    		if (c != null) {
    			if (c.equals("next page-numbers")) {
    				return element.attr("href");
    			}
    		}
    	}

    	return null;
    }

    private String getDescription(Element post)
    {
    	Element a = this.getTagAndIdAndChildren(post.children(), "div", "div_block-116-110");
    	Element b = this.getTagAndIdAndChildren(a.children(), "div", "text_block-119-110");
    	Element c = this.getTagAndIdAndChildren(b.children(), "span", "span-120-110");
    	Element desc = c.select("p").get(0);
    	if (desc.text().trim().isEmpty())
    		desc= c.select("p").get(1);
    	return desc.text().trim();
    }


    private String getDate(Element post)
    {
    	Element a = this.getTagAndIdAndChildren(post.children(), "h1", "headline-63-110");
    	String text = a.text();
    	int pos = text.indexOf("Published on ");
    	if (pos != -1)
    	{
    		text = text.substring(pos + "Published on ".length());
    	}
    	return text.trim();
    }

    private String getMP3Link(Element e)
    {
    	Elements elements = e.select("a");
    	for (Element element: elements)
    	{
    		String attr = element.attr("href");
    		if (attr.endsWith(".mp3"))
    				return attr;
    	}
    	return "";
    }

    private String getImage(Element element)
    {
		String data = element.attr("style");

		int start = data.indexOf("http");
		data = data.substring(start);
		int end = data.indexOf(")");
		String image = data.substring(0, end).trim();

    	return image;
    }


    private Elements getElementsFromHtml(String url) throws IOException
    {
    	String html = this.getHtml(url);
        Document doc = Jsoup.parse(html);
        Elements body = doc.body().children();
        return body;
    }

    private String getDateText()
    {
    	DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
    	Date date = new Date();
    	return dateFormat.format(date);
    }

    public static String getTimestamp()
    {
    	DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	Date date = new Date();
    	return "[" + dateFormat.format(date) + "]";
    }

    private static void debugOut(String text)
    {
    	System.out.println(MagGeneratorRH.getTimestamp() + text);
    }

    private void process() throws IOException, DocumentException, InterruptedException
    {
    	List<Episode> episodes = new ArrayList<Episode>();
    	List<Page> pages = this.getPageList();
    	for (Page page: pages)
    	{

    		debugOut("Processing URL: :" + page.getUrl());
            Elements body = this.getElementsFromHtml(page.getUrl());

            Element a = this.getTagAndIdAndChildren(body, "section" ,"section-42-110");
            Element b = this.getTagAndClassAndChildren(a.children(), "div", "ct-section-inner-wrap");
            Element c = this.getTagAndIdAndChildren(b.children(), "div", "div_block-57-110");
			String image = this.getImage(c);
			String title = this.getTitle(c);
	    	String description = this.getDescription(c);
	    	String date = this.getDate(c);
	    	String mp3 = this.getMP3Link(c);
	    	if (mp3.trim().isEmpty())
	    		mp3 = page.getUrl();

	    	episodes.add(new Episode(title, description, image, mp3, /*readMore*/"", date, page.getUrl()));
    	}

    	// Create the CSV
    	this.saveToCsv(episodes);

        // Create the PDF
        com.itextpdf.text.Document pdfDoc = new com.itextpdf.text.Document();
        pdfDoc.setPageSize(new Rectangle(PageSize.A4.getWidth(), PageSize.A4.getHeight()));
        PdfWriter.getInstance(pdfDoc, new FileOutputStream(PDF_NAME));
        pdfDoc.open();

        this.createCover(pdfDoc);
        this.createContents(pdfDoc, episodes);

        for (Episode episode: episodes)
        {
//        	Episode episode = episodes.get(i);
        	debugOut("Building PDF page for " + episode.getTitle());
            // The title
            Font titleFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 20, BaseColor.BLACK);
            Chunk titleChunk = new Chunk(episode.getTitle() + "\n   ", titleFont);

            // The description
            Font descFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, BaseColor.BLACK);
            Chunk descChunk = new Chunk(episode.getDescription() /*+ "\n\n"*/, descFont);

            String imageFile = saveImage(episode.getImage());
            Image img = Image.getInstance(imageFile);

            // Scale image and centre it
            img.scaleToFit((float)(PageSize.A4.getWidth() * IMAGE_SCALE), (float)(PageSize.A4.getHeight() * IMAGE_SCALE));
            float x = (PageSize.A4.getWidth() - img.getScaledWidth()) / 2;
            float y = (PageSize.A4.getHeight() - img.getScaledHeight()) / 2;
            img.setAbsolutePosition(x, y);

            // Create the episode link
            Font linkFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, BaseColor.BLUE);
            Chunk linkChunk = new Chunk("\nClick here for the podcast", linkFont);
            linkChunk.setAnchor(episode.getMp3());

            // Create the webpage link
            Font weblinkFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, BaseColor.BLUE);
            Chunk weblinkChunk = new Chunk("\nClick here for this podcast web page", weblinkFont);
            weblinkChunk.setAnchor(episode.getUrl());

            pdfDoc.newPage();

            Paragraph titleParagraph = new Paragraph();
            titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
            titleParagraph.add(titleChunk);

            Paragraph linkParagraph = new Paragraph();
            linkParagraph.setAlignment(Paragraph.ALIGN_CENTER);
            linkParagraph.add(linkChunk);

            Paragraph weblinkParagraph = new Paragraph();
            weblinkParagraph.setAlignment(Paragraph.ALIGN_CENTER);
            weblinkParagraph.add(weblinkChunk);

            Paragraph descParagraph = new Paragraph();
            descParagraph.setAlignment(Paragraph.ALIGN_LEFT);
            descParagraph.add(descChunk);

            pdfDoc.add(titleParagraph);
            pdfDoc.add(descParagraph);
            pdfDoc.add(img);
            if (episode.getMp3().endsWith(".mp3"))
            	pdfDoc.add(linkParagraph);
        	pdfDoc.add(weblinkParagraph);
        }
        pdfDoc.close();
    }

    private void saveToCsv(List<Episode> episodes) throws IOException
    {
    	File f = new File(CSV_NAME);
    	BufferedWriter bw = new BufferedWriter(new FileWriter(f));
    	bw.write("Episode Title,Episode Description,MP3 Link,Date\n");
    	for (Episode e: episodes) {
    		bw.write(quote(e.getTitle()) + "," + quote(e.getDescription()) + "," + quote(e.getMp3()) + "," + quote(e.getDateStamp()) + "\n");
    	}
    	bw.close();
    }

    private String quote(String s)
    {
    	return "\"" + s + "\"";
    }

    public String saveImage(String imageUrl) throws IOException
    {
    	System.setProperty("http.agent", "Chrome");
    	URL url = new URL(imageUrl);
        String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);

    	String fileName = url.getFile();
    	String destName = "./" + fileName.substring(fileName.lastIndexOf("/"));
    	File f = new File(destName);
    	System.out.println(f.getAbsolutePath());

        InputStream inputStream = con.getInputStream();
        OutputStream outputStream = new FileOutputStream(f);
        byte[] buffer = new byte[2048];

        int length;
        while ((length = inputStream.read(buffer)) != -1)
            outputStream.write(buffer, 0, length);

        outputStream.close();
        inputStream.close();

    	return f.getAbsolutePath();
    }
    private void createCover(com.itextpdf.text.Document doc) throws DocumentException, MalformedURLException, IOException
    {
    	debugOut("Creating cover");
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30, BaseColor.BLACK);
        Paragraph p = new Paragraph();
        p.setAlignment(Paragraph.ALIGN_CENTER);
        Chunk c = new Chunk(GUIDE_TITLE, f);
        p.add(c);
        doc.add(p);


        String defaultImage = this.saveImage(Episode.DEFAULT_IMAGE);
        Image img = Image.getInstance(defaultImage);
    	img.scaleToFit((float)(PageSize.A4.getWidth() * IMAGE_SCALE), (float)(PageSize.A4.getHeight() * IMAGE_SCALE));
	    float x = (PageSize.A4.getWidth() - img.getScaledWidth()) / 2;
	    float y = (PageSize.A4.getHeight() - img.getScaledHeight()) / 2;
	    img.setAbsolutePosition(x, y);
	    doc.add(img);
    }

    private void createContents(com.itextpdf.text.Document doc, List<Episode> episodes) throws DocumentException
    {
    	debugOut("Creating table of contents");
    	int currentItem = 1;
        List<Chunk> chunks = new ArrayList<Chunk>();
        Font contentFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 12, BaseColor.BLACK);
        doc.newPage();

        Font contentTitleFont = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, BaseColor.BLACK);
        Paragraph contentTitleParagrapoh = new Paragraph();
        contentTitleParagrapoh.setAlignment(Paragraph.ALIGN_CENTER);
        Chunk contentTitleChunk = new Chunk("The Retro Hour List of Episodes as of " + this.getDateText() + "\n\n(Scroll past contents for Episode Cover Images and podcast/mp3 links)\n\n", contentTitleFont);
        contentTitleParagrapoh.add(contentTitleChunk);
        doc.add(contentTitleParagrapoh);


        boolean blockWritten = false;
        for (Episode e: episodes)
        {
            Chunk chunk = new Chunk(e.getTitle() + "  (" + e.getDateStamp() + ")", contentFont);
            chunks.add(chunk);
            currentItem++;
            blockWritten = false;
            if (currentItem > NUM_CONTENT_ITEMS_PER_PAGE)
        	{
        		for (Chunk c: chunks)
        		{
        			Paragraph p = new Paragraph();
        			p.add(c);
        			doc.add(p);
        		}
        		currentItem = 1;
        		doc.newPage();
        		chunks.clear();
        		blockWritten = true;
        	}
        }
        if (!blockWritten)
        {
    		for (Chunk c: chunks)
    		{
    			Paragraph p = new Paragraph();
    			p.add(c);
    			doc.add(p);
    		}
        }
    }

    private Element getTagAndClassAndChildren(Elements elements, String tagName, String className)
    {
        for (Element e: elements)
        {
            if (e.tagName().equals(tagName))
            {
                if (e.className().equals(className))
                    return e;
            }
            Element el = this.getTagAndClassAndChildren(e.children(), tagName, className);
            if (el != null)
                return el;
        }
        return null;
    }

    private Element getTagAndIdAndChildren(Elements elements, String tagName, String id)
    {
        for (Element e: elements)
        {
            if (e.tagName().equals(tagName))
            {
                if (e.id().equals(id))
                    return e;
            }
            Element el = this.getTagAndIdAndChildren(e.children(), tagName, id);
            if (el != null)
                return el;
        }
        return null;
    }

    private String getHtml(String url) throws IOException
    {
        return Jsoup.connect(url).get().html();
    }
}
