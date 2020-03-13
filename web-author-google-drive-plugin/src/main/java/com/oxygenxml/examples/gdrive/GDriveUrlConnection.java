package com.oxygenxml.examples.gdrive;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ro.sync.net.protocol.http.HttpExceptionWithDetails;


/**
 * Url connection to a file on google drive.
 */
public class GDriveUrlConnection extends HttpURLConnection {
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(GDriveUrlConnection.class.getName());
  
  
  /**
   * Mime type of the edited files.
   */
  public static final String MIME_TYPE = "text/xml";
  /**
   * The http request used to download the file.
   */
  private HttpRequest request;
  /**
   * The http response received when downloading the file.
   */
  private HttpResponse response;
  
  /**
   * The file to which this connection refers.
   */
  private File file;
  
  /**
   * The id of the user which is trying to open the URL.
   */
  private String userId;
  
  /**
   * Constructor.
   * 
   * @param downloadUrl The download url of the file.
   * @param file The file.
   * @param userId The id of the user to whose drive we connect.
   */
  public GDriveUrlConnection(URL downloadUrl, File file, String userId) throws IOException {
    super(downloadUrl);
    this.file = file;
    this.userId = userId;
  }
  
  /**
   * @see java.net.URLConnection#connect()
   */
  @Override
  public void connect() throws IOException  {
    if (!connected) {
      logger.debug("Connecting...");
      connected = true;
      if (doInput) {
        logger.debug("Starting download of the file " + file.getTitle());
        final String fileDownloadUrl = file.getDownloadUrl();
        if (fileDownloadUrl == null || fileDownloadUrl.length() == 0) {
          // File has no content on drive.
          throw new FileNotFoundException(file.getTitle());
        }
        
        try {
          GDriveManagerFilter.executeWithRetry(this.userId, new GDriveOperation<Void>() {
            @Override
            public Void executeOperation(Drive drive) throws IOException {
              request = drive.getRequestFactory().buildGetRequest(new GenericUrl(fileDownloadUrl));
              response = request.execute();   
              return null;
            }
          });
          
          // init base class fields.
          responseCode = response.getStatusCode();
          responseMessage = response.getStatusMessage();
        } catch (AuthorizationRequiredException e) {
          logger.warn("User revoked access while editing.", e);
          
          responseCode = HttpServletResponse.SC_UNAUTHORIZED;
          responseMessage = "Access revoked during editing";
        }
        
        logger.debug("Request executed");
        
      }
    }
  }

  /**
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public synchronized InputStream getInputStream() throws IOException {
    connect();
    return response.getContent();
  }

  /**
   * @see java.net.URLConnection#getContentLength()
   */
  @Override
  public int getContentLength() {
    int length = -1;
    try {
      connect();
      Long contentLength = response.getHeaders().getContentLength();
      if (contentLength != null) {
        length = contentLength.intValue();
      }
    } catch (Throwable t) {
      // We could not determine the length;
    }
    logger.debug("Content length: " + length);
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
    return response.getStatusCode();
  }
  
  @Override
  public void disconnect() {
    if (connected) {
      connected = false;
      try {
        response.disconnect();
      } catch (IOException e) {
        logger.error(e, e);
      }
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
    logger.debug("Starting to output in file " + file.getTitle());
    
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        final byte[] byteArray = super.toByteArray();
        logger.debug("writing to file...");
        try {
          file.setMimeType(MIME_TYPE);
          // update the file.
          GDriveManagerFilter.executeWithRetry(GDriveUrlConnection.this.userId, new GDriveOperation<Void>() {
            @Override
            public Void executeOperation(Drive drive) throws IOException {
              Update update = drive.files().update(file.getId(), file, new ByteArrayContent(MIME_TYPE, byteArray));
              logger.debug(update);
              File uploadedFile = update.execute();
              logger.debug(file.getVersion() + " -> " + uploadedFile.getVersion());
              return null;
            }
          });  
        } catch (AuthorizationRequiredException e) {
          logger.warn("Access revoked during editing.", e);
        } catch (HttpExceptionWithDetails e) {
          logger.warn("Error saving file: " + e.getReason(), e);
          String reason = e.getReason();
          try {
            JsonObject jreason = new JsonParser().parse(reason).getAsJsonObject();
            reason = jreason.getAsJsonObject("error").get("message").getAsString();
          } catch (Exception jsonParsingException) {
            // It seems that the message was not a JSON-formatted one.
            logger.warn(jsonParsingException, jsonParsingException);
          }
          throw new IOException(reason, e);
        }
      }
    };
  }
}
