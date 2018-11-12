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
	private List<String> projects;
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
		
		// Lastly, we want to create another list of just each project name, just to save time later.
		projects = new ArrayList<>(pages.keySet());
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter output = response.getWriter();
		
		Cookie[] cookies = request.getCookies();
		List<String> seenValues = null;
		String lastProject = "";
		boolean bothCookies = false;
		
		// First, we want to check for cookies.
		if(cookies != null) {
			for(int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				// System.out.println(cookie.getName());
				if(cookie.getName().equals("projects_to_view")) {
					String seenValuesAsString = cookie.getValue();
					if(!seenValuesAsString.equals("")) {
						String[] seenValuesAsStringArray = seenValuesAsString.split(",");
						seenValues = Arrays.asList(seenValuesAsStringArray);
					}
					if(bothCookies) {
						break;
					} else {
						bothCookies = true;
					}
				}
				if(cookie.getName().equals("lastProject")) {
					lastProject = cookie.getValue();

					if(bothCookies) {
						break;
					} else {
						bothCookies = true;
					}
				}
			}
		}
		// If we didn't find an old list, then we set a new one and remove the last project.
		if(seenValues == null) {
			seenValues = new ArrayList<>(projects);
		}
		
		// Next, we choose a project. In doing so, we remove the last seen project to ensure we can't double up.
		String proj = "";
		boolean removedVal = seenValues.remove(lastProject);
		while(proj.equals("") || !seenValues.isEmpty()) {
			int nextProject = projectSelector.nextInt(seenValues.size());
			proj = seenValues.get(nextProject);
			seenValues.remove(nextProject);
			if(!pages.containsKey(proj)) {
				proj = "";
			}
		}
		// If we did remove the last seen project, then we add it back in
		if(removedVal) {
			seenValues.add(lastProject);
		}
		
		// If we didn't find a valid project, we have two choices.
		if(proj.equals("")) {
			// Either the list has things in it, which means that we have no choice but to repeat, since
			//  it would mean that the only thing in the list in it is the one that we repeated.
			//  This will occur if there's one project in the list. Then we're gonna have to return a project.
			if(seenValues.isEmpty()) {
				proj = seenValues.remove(0);
				
			// if the list doesn't have things in it, then the client has tampered with the cookie,
			//  so we reset. If the client has tampered with the cookie, then they've violated the integrity
			//  of the site, so we reset and start anew.
			} else {
				seenValues = new ArrayList<>(projects);
				proj = seenValues.get(projectSelector.nextInt(seenValues.size()));
			}
		}
		
		// Next, load the page and return it
		HtmlPage page = pages.get(proj);
		
		List<HtmlElement> div = page.getByXPath("//div[@class='block-region-content']");
		HtmlElement content = div.get(0);
		output.println(content.asXml());
		
		// Lastly, we generate the cookies to return.
		String newSeenValuesAsString = seenValues.toString();
		Cookie retCookie = new Cookie("projects_to_view", newSeenValuesAsString);
		retCookie.setPath("/");
		Cookie lastProjectCookie = new Cookie("last_project", proj);
		lastProjectCookie.setPath("/");
		response.addCookie(retCookie);
		response.addCookie(lastProjectCookie);
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
