package com.oxygenxml.examples.dbx;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

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
  private static final Logger logger = Logger.getLogger(EntryPoint.class.getName());

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
    
    String path = httpRequest.getParameter("path");
    if (userData != null && path != null) {
      logger.debug("User requested path: " + path);
      String dbxUrl = "dbx:///" + userId + path;
      logger.debug("dbx url: " + path);
      // Sonar false positive - the redirect is to oxygen.html
      httpResponse.sendRedirect("../app/oxygen.html?url=" + URLUtil.encodeURIComponent(dbxUrl) + // NOSONAR 
          "&author=" + URLUtil.encodeURIComponent(userData.getUserName()));
    } else if (path != null) {
      // User authorization required.
      logger.debug("Starting authorizattion");
      DbxWebAuth flow = Credentials.getFlow();
      String encodedPath = URLUtil.encodeURIComponent(path);
      // The URL to redirect the user to after authorization.
      String nextUrl = httpRequest.getRequestURL()
          .append("?path=").append(encodedPath).toString();
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
