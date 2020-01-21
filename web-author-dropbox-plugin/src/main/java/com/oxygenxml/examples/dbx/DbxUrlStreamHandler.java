package com.oxygenxml.examples.dbx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * URL stream handler implementation backed by Google Drive.
 */
public class DbxUrlStreamHandler extends URLStreamHandler {

  
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(DbxUrlStreamHandler.class.getName());
  
  /**
   * Opens a connection to the file specified by the given url.
   *
   * @param url The url of the file. 
   */
  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    String userId = getUserIdFromUrl(url);
    UserData currentUserData = DbxManagerFilter.getCurrentUserData(userId);
    
    logger.debug("Opening URL " + url + " for user with dropbox " + currentUserData + " on thread " + Thread.currentThread().getId());
    if (currentUserData == null) {
      DbxManagerFilter.authorizationFailedForUser(userId);
    }
    
    String path = getPathFromUrl(url);
    
    return new DbxUrlConnection(url, path, currentUserData);
  }


  /**
   * Returns the user id encoded in the URL.
   * 
   * @param url The url.
   * 
   * @return The user id.
   */
  private String getUserIdFromUrl(URL url) {
    String[] urlPathComponents = url.getPath().split("/");
    logger.debug("url: " + url + " user id " + Arrays.asList(urlPathComponents));
    return urlPathComponents.length == 0 ? null : urlPathComponents[1];
  }
  
  /**
   * Return the path to the file in the user's Dropbox.
   * 
   * @param url The URL.
   * 
   * @return The path.
   */
  private String getPathFromUrl(URL url) throws UnsupportedEncodingException {
    String urlPath = url.getPath().substring(1);
    int pathStart = urlPath.indexOf('/');
    String pathEncoded = urlPath.substring(pathStart);
    return URLDecoder.decode(pathEncoded, "UTF-8");
    
  }
}
