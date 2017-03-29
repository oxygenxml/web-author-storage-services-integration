package com.oxygenxml.examples.dbx;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuth;

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
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    DbxWebAuth auth = Credentials.getFlow(request.getSession());
    DbxAuthFinish authFinish;
    try {
        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (logger.isDebugEnabled()) {
        	logger.debug("Callback from Dropbox.. :");
        	for (Map.Entry<String, String[]> param: parameterMap.entrySet()) {
        		logger.debug(param.getKey() + " = " + Arrays.toString(param.getValue()));
        	}
        }
        authFinish = auth.finish(parameterMap);
    }
    catch (DbxWebAuth.BadRequestException ex) {
        logger.debug("On /dropbox-auth-finish: Bad request: " + ex.getMessage());
        response.sendError(400);
        return;
    }
    catch (DbxWebAuth.BadStateException ex) {
      logger.debug("Bad state exception", ex);
        // Send them back to the start of the auth flow.
        response.sendRedirect("start");
        return;
    }
    catch (DbxWebAuth.CsrfException ex) {
        logger.error("On /dropbox-auth-finish: CSRF mismatch: " + ex.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "CSRF mismatch");
        return;
    }
    catch (DbxWebAuth.NotApprovedException ex) {
        // When Dropbox asked "Do you want to allow this app to access your
        // Dropbox account?", the user clicked "No".
        response.getOutputStream().println("You should authorize the "
            + "oXygen Author Webapp in order to edit your Dropbox files.");
        return;
    }
    catch (DbxWebAuth.ProviderException ex) {
      logger.debug("On /dropbox-auth-finish: Auth failed: " + ex.getMessage());
        response.sendError(503, "Error communicating with Dropbox.");
        return;
    }
    catch (DbxException ex) {
      logger.debug("On /dropbox-auth-finish: Error getting token: " + ex.getMessage());
        response.sendError(503, "Error communicating with Dropbox.");
        return;
    }
    String accessToken = authFinish.accessToken;
    logger.debug("Authorization fisnished with access token: " + accessToken);

    // Save the access token somewhere (probably in your database) so you
    // don't need to send the user through the authorization process again.
    try {
      DbxManagerFilter.setCredential(accessToken, authFinish.userId);
      setUserId(request, authFinish.userId);
      logger.debug("Set user id : " + authFinish.userId);
    } catch (DbxException ex) {
      DbxManagerFilter.authorizationFailedForUser(authFinish.userId);
      // Normally should not happen. We just got the token, so it is supposed to work.
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
          "Error communicating with Dropbox.");
      logger.error(ex, ex);
    }
    
    logger.debug("Redirecting to " + authFinish.urlState.substring(1));
    response.sendRedirect(authFinish.urlState.substring(1));
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
  
  /**
   * Sets the user id for the current session.
   * 
   * @param request The http request.
   * @param userId The id of the user which is the owner of the current session.
   */
  public static void setUserId(ServletRequest request, String userId) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    httpRequest.getSession().setAttribute(AuthCode.USERID, userId);
  }

  @Override
  public String getPath() {
    return "dbx-oauth-callback";
  }
}
