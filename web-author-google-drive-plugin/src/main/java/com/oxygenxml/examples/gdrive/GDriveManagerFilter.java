package com.oxygenxml.examples.gdrive;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpResponseException;

import ro.sync.ecss.extensions.api.webapp.SessionStore;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.exml.plugin.PluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

/**
 * Filter responsible with determining the context of an URL connection, i.e.
 * which is the user on behalf of which we are making the request.
 */
public class GDriveManagerFilter implements Filter, PluginExtension {
  
  /**
   * The password option key.
   */
  static final String GDRIVE_PASSWORD_OPTION_KEY = "gdrive.password";
  /**
   * The secrets option key.
   */
  static final String GDRIVE_SECRETS_OPTION_KEY = "gdrive.secrets";


  /**
   * Key used to store google drive session information on the session store.
   */
  private static final String G_DRIVE_SESSIONS_KEY = "g-drive.sessions";


  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(GDriveManagerFilter.class.getName());
  
  
  /**
   * Thread-local variable holding the current user's data. 
   * 
   * This is correct only for code running on the Tomcat's threads. For the
   * code running in thread pools it is null.
   */
  private static final ThreadLocal<UserRequest> currentUserRequest = 
      new ThreadLocal<>();

  /**
   * Db to store the crendentials.
   */
  private static TokenDb tokenDb;
  
  
  /**
   * Returns the user data of the specified user id.
   * 
   * If the call is made inside the context of a user request, we check
   * that the current user is the same as the one given as argument.
   * 
   * @param userId The user id.
   * 
   * @return The drive.
   * @throws AuthorizationRequiredException
   */
  public static UserData getCurrentUserData(String userId) throws AuthorizationRequiredException {
    UserData fileUserDrive = getUserDataUnauthenticated(userId);
    
    UserRequest userRequest = currentUserRequest.get();
    
    if (userRequest != null) {
      UserData currentUserDrive = userRequest.getUserData();
      
      if (currentUserDrive == null || currentUserDrive != fileUserDrive) {
        logger.error("Current user is not the owner of the file. ");
        // The current user trying to access the file is not the same as the user 
        // that the file belongs to.
        fileUserDrive = null;
      }
    } else {
      // This is not a user request. We are more lenient with these requests.
    }
    
    return fileUserDrive;
  }
  
  /**
   * Returns the status code of the HTTP request that thrown the given exception
   * or 0 if it cannot be determined.
   * 
   * @param e The exception.
   * 
   * @return The status code, or 0 if it could not be determined.
   */
  private static int getStatusCodeFromException(IOException e) {
    // Our protocol handlers return HttpExcptionWithDetails.
    if (e instanceof HttpExceptionWithDetails) {
      return ((HttpExceptionWithDetails) e).getReasonCode();
    }
    
    // Standard ones return HttpResponseException.
    if (e instanceof HttpResponseException) {
      return ((HttpResponseException) e).getStatusCode();
    }
    
    return 0;
  }
  
  
  /**
   * Executes the given operation, if it fails with authorization error, the token for 
   * the specified user is refreshed and the operation is retried.
   * 
   * @param userId The user whose token is used.
   * @param operation The operation.
   * 
   * @throws Exception If it fails.
   * @throws AuthorizationRequiredException
   */
  public static <T> T executeWithRetry(String userId, GDriveOperation<T> operation) 
      throws IOException, AuthorizationRequiredException {
    try {
      UserData userData = getCurrentUserData(userId);
      
      if (userData == null) {
        throw new AuthorizationRequiredException();
      }
      
      return operation.executeOperation(userData.getDrive());
    } catch (IOException e) {
      int statusCode = getStatusCodeFromException(e);
      // If the user was not authorized, or we did not manage to find the cause
      // of the error, we retry the operation.
      if (statusCode == HttpServletResponse.SC_UNAUTHORIZED || statusCode == 0) {
        logger.debug("User " + userId + " authorization expired.");
        
        getSessionStore().remove(userId, G_DRIVE_SESSIONS_KEY);
        
        logger.warn("Failed login attempt of user " + userId);
        logger.debug("retrying operation....");
        UserData userData = getCurrentUserData(userId);
        return operation.executeOperation(userData.getDrive());
      }
      throw e;
    }
  }

  /**
   * Returns the drive for the given user id.
   * 
   * Note: This can be used to get the user data for other users
   * than the one performing the current request. Beware of its security
   * implications.
   * 
   * @param userId the user id.
   * 
   * @return The drive.
   *
   * @throws AuthorizationRequiredException
   */
  private synchronized static UserData getUserDataUnauthenticated(String userId) throws AuthorizationRequiredException {
    UserData userData = null;
    if (userId != null) {
      Optional<UserData> maybeUserData = getSessionStore().get(userId, G_DRIVE_SESSIONS_KEY);
      if (maybeUserData == null) {
        logger.debug("token expired. loading from the db");
        tryToLoadFromDb(userId);
        
        // Check if we found some user data. 
        maybeUserData = getSessionStore().get(userId, G_DRIVE_SESSIONS_KEY);
        if (maybeUserData == null) {
          // Mark the user as non existent in the db.
          logger.debug("User " + userId + " not present in the database.");
          maybeUserData = Optional.<UserData>empty();
          getSessionStore().put(userId, G_DRIVE_SESSIONS_KEY, maybeUserData);
        }
      }
      userData = maybeUserData.orElse(null);
    }
    return userData;
  }

  /**
   * Try to load the credential from the database.
   * 
   * @param userId The user id to look up.
   * 
   * @throws AuthorizationRequiredException
   */
  private static void tryToLoadFromDb(String userId) throws AuthorizationRequiredException {
    String refreshToken = tokenDb.loadToken(userId);
    if (refreshToken != null) {
      logger.debug("Found refresh token: " + refreshToken);
      try {
        // Refresh token.
        logger.debug("Need to refresh token with 'refresh token': " + refreshToken);
        GoogleRefreshTokenRequest tokenReqeust = 
            Credentials.getInstance().createRefreshTokenReqeust(refreshToken);
        GoogleTokenResponse tokenResponse;
        try {
          tokenResponse = tokenReqeust.execute();
        } catch (IOException e) {
          // The token refresh failed - user should re-authorize our app. 
          tokenDb.removeToken(userId);
          throw new AuthorizationRequiredException();
        }
        
        logger.debug("Token refreshed: " + tokenResponse.toPrettyString());
        setCredential(tokenResponse);
        logger.debug("User data recreated");
        
      } catch (IOException e) {
        // If we could not refresh the token, just log the error and return null.
        logger.debug(e, e);
      }
    }
  }

  /**
   * Sets the user data for the current user.
   * 
   * @param response token response.
   */
  public synchronized static String setCredential(TokenResponse response) throws IOException {
    String refreshToken = response.getRefreshToken();
    response.setRefreshToken(null);
    GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response); 
    UserData userData = new UserData(credential);
    String userId = userData.getId();
    
    getSessionStore().put(userId, G_DRIVE_SESSIONS_KEY, Optional.of(userData));
    if (refreshToken != null) {
      logger.debug("Storing in the db: " + refreshToken);
      tokenDb.storeToken(userId, refreshToken);
    } else {
      logger.debug("Refresh token not passed in, not adding anything to the database");
    }
    logger.debug("Setting credential for user " + userId);
    return userId;
  }

  /**
   * Default constructor. 
   */
  public GDriveManagerFilter() {
  }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {/**/}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	  HttpServletRequest httpRequest = (HttpServletRequest) request;
	  // Find the user on behalf of which we are executing.
	  String userId = AuthCode.getUserId(httpRequest);
	  logger.debug("Request from user: " + userId);
    UserData userData = null;
    try {
      userData = getUserDataUnauthenticated(userId);
    } catch (AuthorizationRequiredException e) {
      // The uers revoked the authorization while editing.. 
      // We do not attempt any recovery.
      logger.warn("User " + userId + " revoked access to our app while editing.");
    }
    logger.debug("Found drive " + userData);
    
    // Set the current drive if there is one available for the current user.
    currentUserRequest.set(new UserRequest(userData));
    logger.debug("Drive set for thread " + Thread.currentThread().getId());
    try {
      chain.doFilter(request, response);
    } finally {
      logger.debug("Drive removed for thread " + Thread.currentThread().getId());
      currentUserRequest.remove();
    }
    logger.debug("Request from user " + userId + " finished.");
	}

  public void init(FilterConfig fConfig) throws ServletException {
    logger.debug("Filter initialized on classloader " + this.getClass().getClassLoader());
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();

    ServletContext servletContext = fConfig.getServletContext();

    String passwordToEncryptWith = optionsStorage.getOption(GDRIVE_PASSWORD_OPTION_KEY, null);
    if (passwordToEncryptWith == null) {
      logger.error("gdrive.password option not found.");
    }

    // Set the temporary dir to be used by for storing files to be uploaded.
    java.io.File tmpDir = (File) servletContext.getAttribute(WebappPluginWorkspace.OXYGEN_WEBAPP_DATA_DIR);
    
    File tokenDbFile = new File(tmpDir, "tokens-gdrive.properties");
    try {
      tokenDb = new TokenDb(tokenDbFile, passwordToEncryptWith);
    } catch (IOException e) {
      throw new ServletException("Could not create token DB.", e);
    }

    String secrets = optionsStorage.getOption(GDRIVE_SECRETS_OPTION_KEY, null);
    if (secrets == null) {
      logger.error("gdrive.secrets option not found.");
    }
    try {
      Credentials.setCredentialsFromStream(
          new ByteArrayInputStream(secrets.getBytes(StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new ServletException("Could not read the client secrets.", e);
    }
    
    // Set the client id here so that it can be used from JSP.
    String clientId = Credentials.getInstance().getClientId();
    servletContext.setAttribute("gdrive.client.id", clientId);
  }
  
  /**
   * @return The plugin session store.
   */
  private static SessionStore getSessionStore() {
    WebappPluginWorkspace workspace = (WebappPluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
    return workspace.getSessionStore();
  }
}
