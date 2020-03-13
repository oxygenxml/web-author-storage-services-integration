package com.oxygenxml.examples.dbx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;

/**
 * Class that stores the details of our Google API app.
 */
public class Credentials {
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(Credentials.class.getName());
  
  /**
   * Default request config.
   */
  private static final DbxRequestConfig DBX_REQUEST_CONFIG = DbxRequestConfig
      .newBuilder("oXygenWebapp/1.0")
      .build();

  /**
   * Application key.
   */
  static String APP_KEY;

  /**
   * Application secret.
   */
  static String APP_SECRET;

  /**
   * URI to redirect to after user authorizes our app.
   */
  static String REDIRECT_URI;

  /**
   * Returns the authorization flow for the current session.
   * 
   * @param session The http session for which we are building the dropbox 
   * authorization flow.
   * 
   * @return The authorization flow.
   */
  public static synchronized DbxWebAuth getFlow() {
    DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

    DbxRequestConfig config = getRequestConfig();

    DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
    return webAuth;
  }

  /**
   * Get the Dropbox request configuration.
   * 
   * @return the Dropbox request configuration.
   */
  public static DbxRequestConfig getRequestConfig() {
    return DBX_REQUEST_CONFIG;
  }

  /**
   * Load Dropbox secrets from a file.
   * 
   * @param secretsStream The input stream to load from.
   * 
   * @throws FileNotFoundException If the file cannot be found.
   * @throws IOException If the file cannot be read.
   */
  public static void setCredentialsFromStream(InputStream secretsStream) throws FileNotFoundException, IOException {
    // Load the secrets from the file.
    Properties secrets = new Properties();
    secrets.load(secretsStream);
    logger.debug("Loaded secrets from file.");
    
    APP_KEY = secrets.getProperty("app_key");
    APP_SECRET = secrets.getProperty("app_secret");
    REDIRECT_URI = secrets.getProperty("redirect_uri");
  }
  
  /**
   * @return the application key
   */
  public static String getAppKey() {
    return APP_KEY;
  }
  
  /**
   * @param httpRequest The user HTTP request.
   * @return The session store.
   */
  public static DbxSessionStore getSessionStore(HttpServletRequest httpRequest) {
     return new DbxStandardSessionStore(httpRequest.getSession(), "DBX_ATTR");
  }
}
