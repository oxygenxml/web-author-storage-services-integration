package com.oxygenxml.examples.gdrive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * URL stream handler implementation backed by Google Drive.
 */
public class GDriveUrlStreamHandler extends URLStreamHandler {

  
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(GDriveUrlStreamHandler.class.getName());
  
  /**
   * Opens a connection to the file specified by the given url.
   *
   * @param url The url of the file. 
   */
  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    String userId = getUserIdFromUrl(url);
    logger.debug("Opening URL " + url + " for user " + userId + " on thread " + Thread.currentThread().getId());    
    
    File file = null;
    try {
      file = getFileToDownload(url, userId);
    } catch (AuthorizationRequiredException e) {
      logger.warn("Authorization revoked while editing", e);
    }
    if (file == null) {
      throw new FileNotFoundException(url.toExternalForm());
    }
    
    return new GDriveUrlConnection(url, file, userId);
  }


  /**
   * Gets the file to download based on the URL.
   * 
   * Visible for tests.
   * 
   * @param url The url.
   * @param userId The user on whose drive we look for the file.
   * 
   * @return
   * 
   * @throws IOException if the file cannot be found on the drive.
   * @throws AuthorizationRequiredException
   */
  File getFileToDownload(URL url, String userId) throws IOException, AuthorizationRequiredException {
    logger.debug("Looking for file that corresponds to the URL: " + url.toExternalForm());
    String path = url.getPath();
    if (path.charAt(0) != '/') {
      throw new FileNotFoundException("Not an absolute path");
    }
    String[] pathEntries = path.substring(1).split("/");
    logger.debug("Path entries: " + Arrays.toString(pathEntries));
    
    File crtFile = null;
    String parentId = null;
    String crtPath = "/";
    // The first element in the path is the userId on behalf of which we 
    // are retrieving the file. The second one is the path type: 'drive', 'shared',
    // See: EntryPoint.computeFilePath(String, String).
    String pathType = pathEntries[1];
    for (int i = 2; i < pathEntries.length; i++) {
      String pathEntry = URLDecoder.decode(pathEntries[i], "UTF-8");
      logger.debug("switching to path " + pathEntry + " with parent id " + parentId);
      List<File> files = Collections.emptyList();
      
      if (i == 2) {
        if (EntryPoint.DRIVE_PATH_TYPE.equals(pathType)) {
          logger.debug("searching a regular file by id: " + pathEntry);
          files = Collections.singletonList(searchFileById(userId, pathEntry));
        } else if (EntryPoint.SHARED_PATH_TYPE.equals(pathType)) {
          logger.debug("searching a shared file");
          files = searchSharedFiles(userId, pathEntry);
        }
      } else if (i == 3 && EntryPoint.DRIVE_PATH_TYPE.equals(pathType)){
        // This path entry is the name of the 'root' ancestor. This ancestor
        // was also identified by ID, so we can skip this path part.
        continue;
      } else {
        logger.debug("searching a regular file by name: " + pathEntry);
        files = searchFileByNameAndParent(userId, pathEntry, parentId);  
      }
        
      if (files.size() == 0) {
        FileNotFoundException ex = 
            new FileNotFoundException("File " + crtPath + " not found, url: " + url);
        logger.debug(ex, ex);
        throw ex;
      }
      if (files.size() > 1) {
        FileNotFoundException ex = 
            new FileNotFoundException("There are multiple files called " + pathEntry +" in folder " + crtPath + ", url: " + url);
        logger.debug(ex, ex);
        throw ex;
      }
      crtFile = files.get(0);
      parentId = crtFile.getId();
      crtPath += pathEntry + "/";
    }
    logger.debug("done.");
    return crtFile;
  }

  /**
   * Search for files shared with the user that have the given name.
   * 
   * @param userId The user id.
   * @param fileName The name of the file.
   * 
   * @return The list of files matching the query.
   * 
   * @throws IOException
   * @throws AuthorizationRequiredException
   */
  private List<File> searchSharedFiles(String userId, String fileName) throws IOException, AuthorizationRequiredException {
    String query = "title='" + fileName + "' and sharedWithMe and trashed = false";
    return searchForFiles(userId, query);
  }


  /**
   * Search for files matching the query.
   * 
   * @param userId The user id.
   * @param query The query used to search for files.
   * 
   * @return The list of files matching the query.
   * 
   * @throws IOException
   * @throws AuthorizationRequiredException
   */
  private List<File> searchForFiles(String userId, final String query) throws IOException, AuthorizationRequiredException {
    List<File> files = GDriveManagerFilter.executeWithRetry(userId, new GDriveOperation<List<File>>() {
      @Override
      public List<File> executeOperation(Drive drive) throws IOException {
        return drive.files().list().setQ(query).execute().getItems();
      }
    });
    return files;
  }


  /**
   * Searches the drive for a file identified by the given query.
   * 
   * Visible for tests.
   * 
   * @param userId The id of the user on whose drive we are searching.
   * @param fileName The file name to look for.
   * @param parentId The id of the parent folder in which to search.
   * 
   * @return The files that match the query.
   * 
   * @throws IOException If the communication with google fails.
   * @throws AuthorizationRequiredException
   */
  List<File> searchFileByNameAndParent(String userId, String fileName, String parentId) throws IOException, AuthorizationRequiredException {
    final String query = "title='" + fileName + "' and '" + parentId + "' in parents and trashed = false";
    List<File> files = GDriveManagerFilter.executeWithRetry(userId, new GDriveOperation<List<File>>() {
      @Override
      public List<File> executeOperation(Drive drive) throws IOException {
        return drive.files().list().setQ(query).execute().getItems();
      }
    });
    return files;
  }

  /**
   * Searches the drive for a file identified by the given id.
   * 
   * @param userId The id of the user on whose drive we are searching.
   * @param fileId The id of the file we are looking for.
   * 
   * @return The file with the given id.
   * 
   * @throws IOException If the communication with google fails.
   * @throws AuthorizationRequiredException
   */
  private File searchFileById(String userId, final String fileId) throws IOException, AuthorizationRequiredException {
    File file = GDriveManagerFilter.executeWithRetry(userId, new GDriveOperation<File>() {
      @Override
      public File executeOperation(Drive drive) throws IOException {
        return drive.files().get(fileId).execute();
      }
    });
    return file;
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
}
