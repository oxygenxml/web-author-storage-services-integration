package com.oxygenxml.examples.dbx;


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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxException;

import ro.sync.ecss.extensions.api.webapp.SessionStore;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.exml.plugin.PluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * Filter responsible with determining the context of an URL connection, i.e.
 * which is the user on behalf of which we are making the request.
 */
public class DbxManagerFilter implements Filter, PluginExtension {

  /**
   * The key of the option that holds the secrets.
   */
  static final String DBX_SECRETS_OPTIONS_KEY = "dbx.secrets";

  /**
   * Key used to store user data in session store.
   */
  private static final String DBX_USR_DATA_KEY = "dbx.usr.data";

  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(DbxManagerFilter.class.getName());
  
  /**
   * Attribute used to retrieve the Tomcat temporary directory.
   */
  static final String JAVAX_SERVLET_CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";

  
  /**
   * Thread-local variable holding the current user's data. 
   * 
   * This is correct only for code running on the Tomcat's threads. For the
   * code running in thread pools it is null.
   */
  private static final ThreadLocal<UserData> currentUserData = 
      new ThreadLocal<UserData>();

  /**
   * Returns the user data of the specified user id.
   * 
   * If the call is made inside the context of a user request, we check
   * that the current user is the same as the one given as argument.
   * 
   * @param userId The user id.
   * 
   * @return The drive.
   */
  public static UserData getCurrentUserData(String userId) {
    UserData fileUserDrive = getUserDataUnauthenticated(userId);
    UserData currentUserDrive = currentUserData.get();
    if (currentUserDrive != null && !currentUserDrive.equals(fileUserDrive)) {
      logger.error("Current user is not the owner of the file. ");
      logger.error(currentUserDrive.getId() + " != " + userId);
      // The current user trying to access the file is not the same as the user 
      // that the file belongs to.
      fileUserDrive = null;
    }
    return fileUserDrive;
  }
  
  /**
   * The token db.
   */
  private static TokenDb tokenDb;

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
   */
  private static synchronized UserData getUserDataUnauthenticated(String userId) {
    UserData userData = null;
    if (userId != null) {
      Optional<UserData> maybeUserData = getSessionStore().get(userId, DBX_USR_DATA_KEY);
      if (maybeUserData == null) {
        logger.debug("token expired. loading from the db");
        tryToLoadFromDb(userId);
        
        // Check if we found some user data. 
        maybeUserData = getSessionStore().get(userId, DBX_USR_DATA_KEY);
        if (maybeUserData == null) {
          // Mark the user as non existent in the db.
          logger.debug("User " + userId + " not present in the database.");
          maybeUserData = Optional.<UserData>empty();
          getSessionStore().put(userId, DBX_USR_DATA_KEY, maybeUserData);
        }
      }
      userData = maybeUserData.orElse(null);
    }
    return userData;
  }

  /**
   * Try to load the token from the database.
   * 
   * @param userId The user id.
   */
  private static void tryToLoadFromDb(String userId) {
    String token = tokenDb.loadToken(userId);
    if (token != null) {
      logger.debug("Found token: " + token);
      try {
        setCredential(token, userId);
      } catch (IOException ex) {
        // Could not retrieve the token.
        logger.debug(ex, ex);
      } catch (DbxException e) {
        // The token was invalid.
        logger.debug(e, e);
      }
    }
  }

  /**
   * Sets the user data for the current user.
   * 
   * @param authToken The authorization token for the current user.
   * @param userId The id of the current user.
   * 
   * @return the user data.
   */
  public static synchronized UserData setCredential(String authToken, String userId) 
      throws IOException, DbxException {
    UserData userData = new UserData(authToken, userId);
    
    getSessionStore().put(userData.getId(), DBX_USR_DATA_KEY, Optional.of(userData));
    
    tokenDb.storeToken(userId, authToken);
    logger.debug("Setting credential for user " + userData.getId());
    return userData;
  }

  /**
   * Clears the user's token. 
   * 
   * @param userId The user id for which we could not authenticate.
   * 
   * @throws IOException
   */
  public static void authorizationFailedForUser(String userId) throws IOException {
    logger.debug("User " + userId + " authorization expired.");
    getSessionStore().remove(userId, DBX_USR_DATA_KEY);
    
    logger.warn("Failed login attempt of user " + userId);
    throw new IOException("Please authorize oXygen Author Webapp to access your Dropbox files.");
  }
  
  /**
   * Default constructor. 
   */
  public DbxManagerFilter() {
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
    UserData service = getUserDataUnauthenticated(userId);
    logger.debug("Found drive: " + service);
    
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
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();

    ServletContext servletContext = fConfig.getServletContext();

    String passwordToEncryptWith = optionsStorage.getOption("dbx.password", null);
    if (passwordToEncryptWith == null) {
      logger.error("dbx.password option not found.");
    }

    // Set the temporary dir to be used by for storing files to be uploaded.
    java.io.File workDir = (File) servletContext.getAttribute(WebappPluginWorkspace.OXYGEN_WEBAPP_DATA_DIR);
		
		File tokenDbFile = new File(workDir, "tokens-dbx.properties");
		try {
			tokenDb = new TokenDb(tokenDbFile, passwordToEncryptWith);
		} catch (IOException e) {
			throw new ServletException("Could not create token DB.", e);
		}

    String secrets = optionsStorage.getOption(DBX_SECRETS_OPTIONS_KEY, null);
    if (secrets == null) {
      logger.error("gdrive.secrets option not found.");
    }

		try {
			Credentials.setCredentialsFromStream(new ByteArrayInputStream(secrets.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new ServletException("Could not read the client secrets.", e);
		}
		// Set the app key so that it can be used in JSP.
		String appKey = Credentials.getAppKey();
		fConfig.getServletContext().setAttribute("dbx.app.key", appKey);
	}
	
	/**
	 * @return The plugin session store.
	 */
	private static SessionStore getSessionStore() {
	  WebappPluginWorkspace workspace = (WebappPluginWorkspace) PluginWorkspaceProvider.getPluginWorkspace();
	  return workspace.getSessionStore();
	}
}
