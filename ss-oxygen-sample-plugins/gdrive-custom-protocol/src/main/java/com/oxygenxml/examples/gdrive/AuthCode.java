package com.oxygenxml.examples.gdrive;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
/**
 * Servlet that is called back by the google servers after the user authorized
 * our app to access its Drive.
 */
public class AuthCode extends WebappServletPluginExtension {

  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(AuthCode.class.getName());
  
  /**
   * The session attribute key for holding the user id.
   */
  public static final String USERID = "userid";
  
  /**
   * @see HttpServlet#HttpServlet()
   */
  public AuthCode() {
    super();
    logger.debug("Auth callback initialized");
  }
  
  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response1) throws ServletException, IOException {
    String code = request.getParameter("code");
    logger.debug("oauth callback with code: " + code);
    if (code != null) {
      GoogleAuthorizationCodeTokenRequest tokenRequest = 
          Credentials.getInstance().createAuthorizationCodeTokenRequest(code);
      // Our custom protocols use gzip encoding anyway.
      tokenRequest.setRequestInitializer(new OxygenHttpRequestInitializer());
      GoogleTokenResponse response = tokenRequest.execute();
      logger.debug("token response received.");
      
      String userId;
      try {
        userId = GDriveManagerFilter.setCredential(response);
        
        logger.debug("Setting drive for user: " + userId);
        request.getSession().setAttribute(AuthCode.USERID, userId);
      } catch (ArrayIndexOutOfBoundsException ex) {
        logger.debug("Couldn't process the userId");
      }
      
      String state = request.getParameter("state");
      String redirectURL = state != null ? state : "../app/oxygen.html";
      logger.debug("Redirecting to " + redirectURL);
      response1.sendRedirect(redirectURL);
    } else {
      logger.debug("Error while authorizing user");
      response1.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
          "In order to edit your XML files in Google Drive, you need to authorize "
          + "oXygen XML Web Author first.");
    }
  }
  
  /**
   * Returns the cached used id from the session.
   * 
   * @param request The http request.
   * 
   * @return The user id.
   */
  public static String getUserId(ServletRequest request) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String userId = (String) httpRequest.getSession().getAttribute(AuthCode.USERID);
    return userId;
  }

  @Override
  public String getPath() {
    return "gdrive-oauth-callback";
  }
}
