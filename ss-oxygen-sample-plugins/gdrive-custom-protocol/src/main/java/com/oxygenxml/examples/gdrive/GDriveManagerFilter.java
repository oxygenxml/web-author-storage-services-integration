package com.oxygenxml.examples.gdrive;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ro.sync.net.protocol.http.HttpExceptionWithDetails;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Filter responsible with determining the context of an URL connection, i.e.
 * which is the user on behalf of which we are making the request.
 */
public class GDriveManagerFilter implements Filter {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(GDriveManagerFilter.class.getName());
  
  /**
   * Attribute that contains the path to the temp directory.
   */
  private static final String TEMPDIR_ATTR = "javax.servlet.context.tempdir";
  
  /**
   * Thread-local variable holding the current user's data. 
   * 
   * This is correct only for code running on the Tomcat's threads. For the
   * code running in thread pools it is null.
   */
  private static final ThreadLocal<UserData> currentUserData = 
      new ThreadLocal<UserData>();

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
    UserData currentUserDrive = currentUserData.get();
    if (currentUserDrive != null && currentUserDrive != fileUserDrive) {
      logger.error("Current user is not the owner of the file. ");
      // The current user trying to access the file is not the same as the user 
      // that the file belongs to.
      fileUserDrive = null;
    }
    return fileUserDrive;
  }
  
  /**
   * The user connections.
   */
  public static final Cache<String, Optional<UserData>> userConnections = CacheBuilder.newBuilder()
      .maximumSize(10000)
      .expireAfterWrite(24, TimeUnit.HOURS)
      .build();
  
  
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
      return operation.executeOperation(userData.getDrive());
    } catch (IOException e) {
      int statusCode = getStatusCodeFromException(e);
      // If the user was not authorized, or we did not manage to find the cause
      // of the error, we retry the operation.
      if (statusCode == HttpServletResponse.SC_UNAUTHORIZED || statusCode == 0) {
        logger.debug("User " + userId + " authorization expired.");
        userConnections.invalidate(userId);
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
      Optional<UserData> maybeUserData = userConnections.getIfPresent(userId);
      if (maybeUserData == null) {
        logger.debug("token expired. loading from the db");
        tryToLoadFromDb(userId);
        
        // Check if we found some user data. 
        maybeUserData = userConnections.getIfPresent(userId);
        if (maybeUserData == null) {
          // Mark the user as non existent in the db.
          logger.debug("User " + userId + " not present in the database.");
          maybeUserData = Optional.<UserData>absent();
          userConnections.put(userId, maybeUserData);
        }
      }
      userData = maybeUserData.orNull();
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
    userConnections.put(userId, Optional.of(userData));
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
	public void destroy() {
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	  HttpServletRequest httpRequest = (HttpServletRequest) request;
	  // Find the user on behalf of which we are executing.
	  String userId = AuthCode.getUserId(httpRequest);
	  logger.debug("Request from user: " + userId);
    UserData service = null;
    try {
      service = getUserDataUnauthenticated(userId);
    } catch (AuthorizationRequiredException e) {
      // The uers revoked the authorization while editing.. 
      // We do not attempt any recovery.
      logger.warn("User " + userId + " revoked access to our app while editing.");
    }
    logger.debug("Found drive " + service);
    
    // Set the current drive if there is one available for the current user.
    currentUserData.set(service);
    logger.debug("Drive set for thread " + Thread.currentThread().getId());
    try {
      chain.doFilter(request, response);
    } finally {
      logger.debug("Drive removed for thread " + Thread.currentThread().getId());
      currentUserData.remove();
    }
    logger.debug("Request from user " + userId + " finished.");
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	  logger.debug("Filter initialized on classloader " + this.getClass().getClassLoader());
	  
	  // Set the temporary dir to be used by for storing files to be uploaded.
	  java.io.File tmpDir = 
	      (File) fConfig.getServletContext().getAttribute(TEMPDIR_ATTR);
    GDriveUrlConnection.setTempDir(tmpDir);
    
    File tokenDbFile = new File(tmpDir, "tokens-gdrive.properties");
    try {
      tokenDb = new TokenDb(tokenDbFile);
    } catch (IOException e) {
      throw new ServletException("Could not create token DB.", e);
    }
    
    InputStream secretsPath = 
        fConfig.getServletContext().getResourceAsStream("/WEB-INF/gdrive-secrets.json");
    try {
      Credentials.setCredentialsFromStream(secretsPath);
    } catch (IOException e) {
      throw new ServletException("Could not read the client secrets.", e);
    }
	}

}
