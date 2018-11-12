package com.webapp.alpha;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Servlet implementation class Alpha
 */
@WebServlet(description = "Alpha Servlet", urlPatterns = {"/Alpha", "/Alpha.do"})
public class Alpha extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String HTML_START = "<html><body>";
	private static final String HTML_END = "</body></html>";
	private Map<String, HtmlPage> pages;
	private Random projectSelector;
    /**
     * @throws MalformedURLException 
     * @see HttpServlet#HttpServlet()
     */
	@SuppressWarnings("unchecked")
    public Alpha() throws MalformedURLException {
        super();

        // First, we're going to get all of the madewith websites that we'll have to scrape later.
		WebClient client = new WebClient();
		client.getOptions().setCssEnabled(false);
		client.getOptions().setJavaScriptEnabled(false);
		HtmlPage page = null;
		try {
			String unityUrl = "https://unity.com/madewith";
			page = client.getPage(unityUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<HtmlElement> items = page.getByXPath("//div[@class='section-home-stories--item col-xs-12 col-sm-6 col-md-4 views-row']");
		if(items.isEmpty()) {
			// This is a placeholder exception, we would probably want to create a new exception to throw.
			throw new RuntimeException("No projects could be found!");
		} else {
			List<String> madeWithLinks = new ArrayList<String>();
			for(HtmlElement item : items) {
				HtmlAnchor itemAnchor = (HtmlAnchor) item.getFirstByXPath(".//a");
				madeWithLinks.add(page.getFullyQualifiedUrl(itemAnchor.getHrefAttribute()).toString());
			}
			
			// Next, let's scrape them now so that users don't have to wait on us scraping them.
			pages = new HashMap<>();
			for(String link : madeWithLinks) {
				try {
					pages.put(link, client.getPage(link));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		// We also need a random object to select each project
		projectSelector = new Random();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter output = response.getWriter();
		
		Cookie[] cookies = request.getCookies();
		// If there's one cookie here, then it's the one that we put here and it's a log of the to be seen projects
		List<String> seenValues = null;
		// System.out.println("Cookies: " + cookies.length);
		// System.out.println();
		if(cookies != null) {
			for(int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				System.out.println(cookie.getName());
				if(cookie.getName().equals("projects_to_view")) {
					String seenValuesAsString = cookie.getValue();
					if(seenValuesAsString.equals("")) {
						seenValues = new ArrayList<>(pages.keySet());
					} else {
						String[] seenValuesAsStringArray = seenValuesAsString.split(",");
						seenValues = Arrays.asList(seenValuesAsStringArray);
						System.out.println("Old cookie found!");
						System.out.println("Length of old cookie: " + seenValues);
						break;
					}
				}
			}
		}
		if(seenValues == null) {
			seenValues = new ArrayList<>(pages.keySet());
		}
		
		// Next, we choose a project.
		int nextProject = projectSelector.nextInt(seenValues.size());
		
		HtmlPage page = pages.get(seenValues.get(nextProject));
		seenValues.remove(nextProject);
		
		List<HtmlElement> div = page.getByXPath("//div[@class='block-region-content']");
		HtmlElement content = div.get(0);
		output.println(content.asXml());
		
		// Lastly, we generate the cookie to return.
		String newSeenValuesAsString = seenValues.toString();
		Cookie retCookie = new Cookie("projects_to_view", newSeenValuesAsString);
		retCookie.setPath("/");
		response.addCookie(retCookie);
		response.setContentType("text/html");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
}
