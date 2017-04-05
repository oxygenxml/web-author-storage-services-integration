package com.oxygenxml.examples.dbx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxException.InvalidAccessToken;
import com.dropbox.core.DbxException.NetworkIO;
import com.dropbox.core.DbxWriteMode;


/**
 * Url connection to a file on google drive.
 */
public class DbxUrlConnection extends HttpURLConnection {
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(DbxUrlConnection.class.getName());
  
  
  /**
   * Mime type of the edited files.
   */
  public static final String MIME_TYPE = "text/xml";

  /**
   * The id of the user which is trying to open the URL.
   */
  private String userId;

  /**
   * The data about the current user.
   */
  private UserData currentUserData;

  /**
   * The path of the file to connect to.
   */
  private String path;

  /**
   * Downloaded bytes.
   */
  private byte[] downloadedBytes;
  
  /**
   * The exception that was thrown when trying to download the file.
   */
  private IOException downloadException;
  
  /**
   * Constructor.
   * 
   * @param downloadUrl The download url of the file.
   * @param path The file path.
   * @param currentUserData The information about the current user.
   */
  public DbxUrlConnection(URL downloadUrl, String path, UserData currentUserData) throws IOException {
    super(downloadUrl);
    this.path = path;
    this.currentUserData = currentUserData;
    this.userId = currentUserData.getId();
    logger.debug("connection instatiated Path: " + path + " user id " + userId);
  }
  
  /**
   * @see java.net.URLConnection#connect()
   */
  @Override
  public void connect() {
    if (!connected) {
      logger.debug("Connecting...");
      connected = true;
      if (doInput) {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        try {
          logger.debug("Reading file " + path);
          currentUserData.getClient().getFile(path, null, target);
          responseCode = 200; 
        } catch (DbxException e) {
          logger.debug(e, e);
          exceptionWhileConnecting(e); 
        } catch (IOException e) {
          logger.debug(e, e);
          exceptionWhileConnecting(e); 
        }
        downloadedBytes = target.toByteArray();
      }
      logger.debug("done");
    }
  }

  /**
   * Code to handle an exception while downloading the file.
   */
  private void exceptionWhileConnecting(Exception e) {
    boolean invalidAccessToken = false;
    if (e instanceof InvalidAccessToken) {
      invalidAccessToken = true;
    } else if (e instanceof NetworkIO && e.getMessage().indexOf("401") != -1) {
      // The oXygen HTTP protocol implementation may throw an exception
      // instead of returning 401. The error message will contain the "401" 
      // message.
      invalidAccessToken = true;
    }
    
    if (invalidAccessToken) {
      try {
        DbxManagerFilter.authorizationFailedForUser(userId);
      } catch (IOException ex) {
        // Cannot throw anything, just record it.
        downloadException = ex;
      }
      responseCode = 401;
    }
    responseCode = 500;
  }

  /**
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public synchronized InputStream getInputStream() throws IOException {
    if (doInput) {
      // Make sure the connection is connected.
      connect();
      if (downloadException != null) {
        throw downloadException;
      } else {
        return new ByteArrayInputStream(downloadedBytes);
      }
    } else {
      logger.warn("Dbx output connection used for input");
      return new ByteArrayInputStream(new byte[0]);
    }
  }

  /**
   * @see java.net.URLConnection#getContentLength()
   */
  @Override
  public int getContentLength() {
    int length = -1;
    if (downloadedBytes != null) {
      length = downloadedBytes.length;
    }
    return length;
  }

  /**
   * Always returns false. No caching.
   * 
   * @return false.
   * 
   * @see java.net.URLConnection#getUseCaches()
   */
  @Override
  public boolean getUseCaches() {
    return false;
  }

  /**
   * Always returns false. No caching.
   * 
   * @see java.net.URLConnection#getDefaultUseCaches()
   */
  @Override
  public boolean getDefaultUseCaches() {
    return false;
  }

  @Override
  public int getResponseCode() throws IOException {
    return responseCode;
  }
  
  @Override
  public void disconnect() {
    if (connected) {
      connected = false;
    }
  }

  @Override
  public boolean usingProxy() {
    return false;
  }


  /**
   * @see java.net.URLConnection#getOutputStream()
   */
  @Override
  public synchronized OutputStream getOutputStream() throws IOException {
    logger.debug("output stream given to the user");
    ByteArrayOutputStream os = new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        byte[] bytesToUpload = super.toByteArray();
        logger.debug("User finished writing to the file. # of bytes: " + bytesToUpload.length);
        try {
          logger.debug("Uploading file");
          currentUserData.getClient().uploadFile(path, DbxWriteMode.force(), bytesToUpload.length, new ByteArrayInputStream(bytesToUpload));
        } catch (DbxException e) {
          DbxManagerFilter.authorizationFailedForUser(userId);
          logger.debug(e, e);
        }
        logger.debug("done.");
      }
    };
    return os;
  }
}
