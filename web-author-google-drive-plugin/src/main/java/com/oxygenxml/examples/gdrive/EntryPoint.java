package com.oxygenxml.examples.gdrive;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ro.sync.basic.util.URLUtil;
import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;


/**
 * Entry point for our app from the Google Drive UI. 
 * 
 * This class handles the "Open" and "New" requests from Google Drive.
 */
public class EntryPoint extends WebappServletPluginExtension {
  /**
   * The token marking a path as referring to a shared file.
   */
  public static final String SHARED_PATH_TYPE = "shared";

  /**
   * The token marking a path as referring to a file in the user's drive.
   */
  public static final String DRIVE_PATH_TYPE = "drive";

  /**
   * Encoding for URL paths.
   */
  private static final String UTF_8_ENCODING = "UTF-8";

  /**
   * The "Open With" action sent by the Google servers.
   */
  private static final String OPEN_ACTION = "open";

  /**
   * The "Create New" action sent by the Google servers.
   */
  private static final String CREATE_ACTION = "create";

  /**
   * The "Load" action sent by the Google servers.
   */
  private static final String LOAD_DOC_ACTION = "load";

  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(EntryPoint.class.getName());

  /**
   * Returns the google api clientId saved
   */
  @Override
  public void doPut(HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) throws ServletException, IOException {

    String clientId = (String) getServletConfig().getServletContext().getAttribute("gdrive.client.id");
    
    if (clientId != null) {
      httpResponse.setStatus(HttpServletResponse.SC_OK);
      httpResponse.getWriter().write(clientId);
      httpResponse.getWriter().flush();  
    } else {
      httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
  
  /**
   * Returns a file path from a user id and file id.
   * 
   * This method assumes that the user is already tracked by our application, so we can find its credentials.
   */
  @Override
  public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
      throws ServletException, IOException {
    String userId = httpRequest.getParameter("userId");
    String fileId = httpRequest.getParameter("fileId");
    
    if (userId != null && fileId != null) {
      try {
        String filePath = computeFilePath(fileId, userId);
        
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        httpResponse.getWriter().write(filePath);
        httpResponse.getWriter().flush();
      } catch (AuthorizationRequiredException e) {
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      }
    } else {
      httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "The request should contain a userId and fileId");
    }
  }
  
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
	  String stateJson = httpRequest.getParameter("state");
    logger.debug("Request with state: " + stateJson);
    String encodedStateJson = stateJson != null ? 
        URLUtil.encodeURIComponent(stateJson) : null;
    
    String userId = AuthCode.getUserId(httpRequest);
    logger.debug("Checking user " + userId + " for authorization");
    UserData userData = null;
    try {
      userData = GDriveManagerFilter.getCurrentUserData(userId);
    } catch (AuthorizationRequiredException e) {
      logger.debug("Access revoked - will request again from the user.");
    }
    logger.debug("Found user data: " + userData);
   
    if (stateJson == null || userData == null) {
      // Ask the user for authorization if he comes from the Open With or Create
      // actions (stateJson != null) and we do not have an authorization token, or
      // if the user starts from this page directly.
      String redirectUri = null;
      if (stateJson != null) {
        redirectUri = httpRequest.getRequestURI() + "?state=" + encodedStateJson;
      } else {
        // User landed on this page and our app is authorized, redirect to dashboard. 
        redirectUri = "../app/oxygen.html?gdrive-focus=true";
      }
      sendAuthorizationRequest(httpResponse, redirectUri);
    } else {
      State state = new State(stateJson);
      logger.debug("Requesting user data for user: " + userId);
      
      String filePath = null;
      if (LOAD_DOC_ACTION.equals(state.action)) {
        String docUrl = state.ids.iterator().next();
        openUrlInWebapp(httpResponse, docUrl, userData.getUserName());
      } else if (CREATE_ACTION.equals(state.action)) {
        String urlParam = httpRequest.getParameter("url");
        if (urlParam != null) {
          // The user has already chosen the template, create it in the
          // Google Drive.
          logger.debug("Creating new document with template: " + urlParam);
          URL url = new URL(URLDecoder.decode(urlParam, UTF_8_ENCODING));
          
          // If the filename is not specified, generate one.
          String fileName = httpRequest.getParameter("file_name");
          if (fileName == null) {
            fileName = generateFileName();
          }
          
          try {
            filePath = createNewTopic(state.folderId, fileName, url, userId);
          } catch (AuthorizationRequiredException e) {
            // Ask the user for authorization.
            String redirectURL = httpRequest.getRequestURI() + "?state=" + encodedStateJson;
            sendAuthorizationRequest(httpResponse, redirectURL);
            return;
          }
          openInWebapp(httpResponse, userId, filePath, userData.getUserName());
        } else {
          logger.debug("Redirecting the user to choose the template.");
          // Redirect the user to choose the template that they want for
          // the new document.
          httpResponse.sendRedirect("../gdrive/NewFile.html?state=" + encodedStateJson);
        }
      } else if (OPEN_ACTION.equals(state.action)) {
        // Open the specified file.
        Iterator<String> idsIterator = state.ids.iterator();
        try {
          filePath = computeFilePath(idsIterator.next(), userId);
        } catch (AuthorizationRequiredException e) {
          // Ask the user for authorization.
          String redirectURL = httpRequest.getRequestURI() + "?state=" + encodedStateJson;
          sendAuthorizationRequest(httpResponse, redirectURL);
          return;
        }
        logger.debug("Opening the file in the webapp: " + filePath);
        openInWebapp(httpResponse, userId, filePath, userData.getUserName());
      }
    }
	}

	/**
	 * URL where the user will be redirected after the authorization.
	 * 
	 * @param httpResponse The httpResponse to use.
	 * @param redirectURL The redirect URL.
	 * 
	 * @throws IOException If the request fails.
	 */
  private void sendAuthorizationRequest(HttpServletResponse httpResponse, String redirectURL) throws IOException {
    logger.debug("Authorizing user.");
    GoogleAuthorizationCodeRequestUrl redirectUri = 
        Credentials.getInstance().createAuthorizationCodeRequestUrl();
    redirectUri.setState(redirectURL);
    String authorizationUrl = redirectUri.build();
    httpResponse.sendRedirect(authorizationUrl);
  }

	/**
	 * Generates a file name for a new file.
	 * 
	 * @return The file name.
	 */
  private String generateFileName() {
    return "Untitled-" + randomId() + ".xml";
  }

	/**
	 * Redirect the user to open the specified file in the webapp.
	 * 
	 * @param httpResponse The response to send to the user.
	 * @param userId The id of the user.
	 * @param filePath The path of the file in the user's drive.
	 * @param userName The name of the author.
	 * 
	 * @throws IOException
	 */
  private void openInWebapp(HttpServletResponse httpResponse, String userId, String filePath, String userName) throws IOException {
    String fileUrl = "gdrive:///" + userId + filePath;
    logger.debug("Opening url: " + fileUrl);
    String encodedFileUrl = encodeUrlComponent(fileUrl);
    String encodedUserName = encodeUrlComponent(userName);
    httpResponse.sendRedirect("../app/oxygen.html?url=" + encodedFileUrl +
        "&author=" + encodedUserName);
  }
  
  /**
   * Redirect the user to open the specified url in the Web Author.
   * 
   * @param httpResponse The response to sent to the user.
   * @param url The URL to open in Web Author. 
   * @param userName The name of the author.
   * 
   * @throws IOException
   */
  private void openUrlInWebapp(HttpServletResponse httpResponse, String url, String userName) throws IOException {
    httpResponse.sendRedirect("../app/oxygen.html?url=" + encodeUrlComponent(url) +
        "&author=" + encodeUrlComponent(userName));
  }

  /**
   * Encodes an url.
   * 
   * @param url The url.
   * @return The encoded url.
   * 
   * @throws UnsupportedEncodingException
   */
  private String encodeUrlComponent(String url) throws UnsupportedEncodingException {
    return URLEncoder.encode(url, UTF_8_ENCODING).replace("+", "%20");
  }
	
	/**
   * An object representing the state parameter passed into this application
   * from the Drive UI integration (i.e. Open With or Create New). Required
   * for Gson to deserialize the JSON into POJO form.
   *
   */
  private static class State {
    /**
     * Action intended by the state.
     */
    public String action;

    /**
     * IDs of files on which to take action.
     */
    public Collection<String> ids;

    /**
     * Parent ID related to the given action.
     */
    public String folderId;
    
    /**
     * Empty constructor required by Gson.
     */
    @SuppressWarnings("unused")
    public State() {}

    /**
     * Create a new State given its JSON representation.
     *
     * @param json Serialized representation of a State.
     */
    public State(String json) {
      GsonBuilder builder = new GsonBuilder();
      Gson gson = builder.create();
      State other = gson.fromJson(json, State.class);
      this.action = other.action;
      this.ids = other.ids;
      this.folderId = other.folderId;
    }
  }
	
  /**
   * Computes the path of the file with the given id. 
   * 
   * Note: Even in in Google drive it is allowed to have more than one parents for
   * a file, we do not support such a scenario since we have to resolve relative links
   * between the files. 
   * 
   * If the file is inside the user's drive, the url structure is:
   * gdrive:///{user_id}/drive/{id_of_root}/path/to/file.xml
   * 
   * If the file is not linked in the user's drive, the url structure is:
   * gdrive:///{user_id}/drive/{id_of_unlinked_ancestor}/path/to/file.xml
   * Note that the unlinked ancestor may be the file itself.
   *
   * If the file is just shared with the user, the url is:
   * gdrive:///{user_id}/shared/path/to/file.xml
   * 
   * @param fileId The id of the file.
   * @param userId The user id in whose drive the file is located.
   * 
   * @return The path relative to the drive.
   * 
   * @throws IOException
   * @throws AuthorizationRequiredException
   */
	public String computeFilePath(String fileId, String userId) throws IOException, AuthorizationRequiredException {
	  logger.debug("determining the path for the file to be opened.");
	  String path = "";
	  while (true) {
	    final String crtFileId = fileId;
	    File file = GDriveManagerFilter.executeWithRetry(userId, new GDriveOperation<File>() {
        @Override
        public File executeOperation(Drive drive) throws IOException {
          return drive.files().get(crtFileId).setFields("*").execute();
        }
      });
	    
      String encodedFileTitle = URLEncoder.encode(file.getName(), UTF_8_ENCODING);
      
	    List<String> parents = file.getParents();
	    if (parents == null) {
	      if (file.getSharedWithMeTime() == null) {
	        // This file is either the root of the user's drive or another file
	        // that is not linked in the user's Drive, for example a file shared 
	        // by link.
	        path = "/" + DRIVE_PATH_TYPE + "/" + file.getId() + "/" + encodedFileTitle + path; 
	      } else {
	        // This file is shared to the user but not linked in the user's drive.
	        path = "/" + SHARED_PATH_TYPE +"/" + encodedFileTitle + path;
	      }
        break;
      }
	    if (parents.size() > 1) {
	      throw new FileNotFoundException("We cannot resolve relative links for files with more than one parent.");
	    }
      path = "/" + encodedFileTitle + path;
      logger.debug("file id {} current path {}", fileId, path);

	    fileId = parents.get(0);
	  }
	  return path;
	}
	
	/**
	 * Creates a new file in the specified folder and returns the path to that file.
	 * 
	 * @param parentId The id of the parent folder.
	 * @param fileName The file name to use.
	 * @param url The url of the template.
	 * @param userId The id of the user in whose drive we create the file.
	 * 
	 * @return The path relative to the drive.
	 * 
	 * @throws IOException
   * @throws AuthorizationRequiredException
	 */
	public String createNewTopic(String parentId, String fileName, URL url, String userId) throws IOException, AuthorizationRequiredException {
	  final File file = new File();
	  file.setMimeType(GDriveUrlConnection.MIME_TYPE);
	  file.setName(fileName);
	  logger.debug("parent id: {}", parentId);
	  if (parentId != null && parentId.length() > 0) {
	    file.setParents(Arrays.asList(parentId));
	  }
	  
	  final AbstractInputStreamContent mediaContent = new InputStreamContent(
	      GDriveUrlConnection.MIME_TYPE, url.openConnection().getInputStream());
	  
	  File insertedFile = GDriveManagerFilter.executeWithRetry(userId, new GDriveOperation<File>() {
      @Override
      public File executeOperation(Drive drive) throws IOException {
        return drive.files().create(file, mediaContent).execute();
      }
    });
	  return computeFilePath(insertedFile.getId(), userId);
	}

	/**
	 * Generates a random id.
	 * 
	 * @return A random id. 
	 */
  private String randomId() {
    return new BigInteger(30, new Random(System.currentTimeMillis())).toString(32);
  }

  @Override
  public String getPath() {
    return "gdrive-start";
  }
}
