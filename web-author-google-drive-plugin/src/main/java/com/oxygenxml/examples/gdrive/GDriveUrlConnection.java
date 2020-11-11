package com.oxygenxml.examples.gdrive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.File;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ro.sync.ecss.extensions.api.webapp.plugin.FilterURLConnection;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;


/**
 * Url connection to a file on google drive.
 */
public class GDriveUrlConnection extends FilterURLConnection {
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
    super(new URLConnection(downloadUrl) {
      @Override
      public void connect() throws IOException {
        connected = true;
      }
    });
    
    this.file = file;
    this.userId = userId;
  }
  
  /**
   * @see java.net.URLConnection#getInputStream()
   */
  @Override
  public synchronized InputStream getInputStream() throws IOException {
    try {
      return GDriveManagerFilter.executeWithRetry(GDriveUrlConnection.this.userId, drive -> {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        drive.files().get(file.getId()).executeMediaAndDownloadTo(outputStream);
        
        return new ByteArrayInputStream(outputStream.toByteArray());
      });
    } catch (AuthorizationRequiredException e) {
      throw new IOException(e);
    }  
  }

  /**
   * @see java.net.URLConnection#getOutputStream()
   */
  @Override
  public synchronized OutputStream getOutputStream() throws IOException {
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        final byte[] byteArray = super.toByteArray();
        logger.debug("writing to file...");
        try {
          // update the file.
          GDriveManagerFilter.executeWithRetry(GDriveUrlConnection.this.userId, new GDriveOperation<Void>() {
            @Override
            public Void executeOperation(Drive drive) throws IOException {
              Update update = drive.files().update(file.getId(), null, new ByteArrayContent(MIME_TYPE, byteArray));
              logger.debug(update);
              update.execute();
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
