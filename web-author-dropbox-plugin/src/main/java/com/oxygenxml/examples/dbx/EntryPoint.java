package com.oxygenxml.examples.dbx;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxWebAuth;

import ro.sync.basic.util.URLUtil;
import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;

/**
 * Entry point for our app from the Google Drive UI.
 * 
 * This class handles the "Open" and "New" requests from Google Drive.
 */
public class EntryPoint extends WebappServletPluginExtension {
  /**
   * Logger for logging.
   */
  private static final Logger logger = LogManager.getLogger(EntryPoint.class.getName());

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
    String userId = AuthCode.getUserId(httpRequest);
    logger.debug("User id: " + userId);
    UserData userData = DbxManagerFilter.getCurrentUserData(userId);
    logger.debug("user data: " + userData);
    
    String encodedPath = httpRequest.getParameter("path");
    if (userData != null && encodedPath != null) {
      logger.debug("User requested path: " + encodedPath);
      String dbxUrl = "dbx:///" + userId + URLUtil.decodeURIComponent(encodedPath);
      logger.debug("dbx url: " + dbxUrl);
      // Sonar false positive - the redirect is to oxygen.html
      httpResponse.sendRedirect("../app/oxygen.html?url=" + URLUtil.encodeURIComponent(dbxUrl) + // NOSONAR 
          "&author=" + URLUtil.encodeURIComponent(userData.getUserName()));
    } else if (encodedPath != null) {
      // User authorization required.
      logger.debug("Starting authorizattion");
      DbxWebAuth flow = Credentials.getFlow();
      // The URL to redirect the user to after authorization.
      String nextUrl = httpRequest.getRequestURL()
          .append("?path=").append(URLUtil.encodeURIComponent(encodedPath)).toString();
      logger.debug("Next url: " + nextUrl);
      
      DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
          // After we redirect the user to the Dropbox website for authorization,
          // Dropbox will redirect them back here.
          .withRedirectUri(
              Credentials.REDIRECT_URI, 
              Credentials.getSessionStore(httpRequest))
          .withState(nextUrl)
          .build();
      String authorizeUrl = flow.authorize(authRequest); 
      
      logger.debug("Redirecting to auth url, path: " + encodedPath);
      httpResponse.sendRedirect(authorizeUrl);
    } else {
      // User came here for the home page.
      httpResponse.sendRedirect("index.jsp");
    }
  }

  @Override
  public String getPath() {
    return "dbx-start";
  }
}
