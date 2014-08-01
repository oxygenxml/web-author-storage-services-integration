package com.oxygenxml.examples.dbx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dropbox.core.DbxWebAuth;

/**
 * Entry point for our app from the Google Drive UI.
 * 
 * This class handles the "Open" and "New" requests from Google Drive.
 */
public class EntryPoint extends HttpServlet {
  /**
   * Serial version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(EntryPoint.class.getName());

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
    String userId = AuthCode.getUserId(httpRequest);
    logger.debug("User id: " + userId);
    UserData userData = DbxManagerFilter.getCurrentUserData(userId);
    logger.debug("user data: " + userData);
    
    String path = httpRequest.getParameter("path");
    if (userData != null && path != null) {
      logger.debug("User requested path: " + path);
      String dbxUrl = "dbx:///" + userId + URLDecoder.decode(path, "UTF-8");
      logger.debug("dbx url: " + path);
      httpResponse.sendRedirect("../app/demo-mobile.html?url=" + encodeUrl(dbxUrl) + 
          "&author=" + userData.getUserName() +
          "&showSave=true");
    } else if (path != null) {
      // User authorization required.
      logger.debug("Starting authorizattion");
      DbxWebAuth flow = Credentials.getFlow(httpRequest.getSession());
      String encodedPath = encodeUrl(path);
      // The URL to redirect the user to after authorization.
      String nextUrl = httpRequest.getRequestURL()
          .append("?path=").append(encodedPath).toString();
      logger.debug("Next url: " + nextUrl);
      String authorizeUrl = flow.start(nextUrl); 
      logger.debug("Redirecting to auth url, path: " + encodedPath);
      httpResponse.sendRedirect(authorizeUrl);
    } else {
      // User came here for the home page.
      httpResponse.sendRedirect("index.jsp");
    }
  }
  
  /**
   * Encode a Url.
   * 
   * @param dbxUrl The url.
   * 
   * @return The encoded url.
   *
   * @throws UnsupportedEncodingException
   */
  private String encodeUrl(String dbxUrl) throws UnsupportedEncodingException {
    return URLEncoder.encode(dbxUrl, "UTF-8").replace("+", "%20");
  }
}
