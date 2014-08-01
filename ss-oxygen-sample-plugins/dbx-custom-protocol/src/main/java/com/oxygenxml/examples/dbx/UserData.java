package com.oxygenxml.examples.dbx;

import java.io.IOException;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;


/**
 * Data stored for a connected user.
 */
public class UserData {
  
  /**
   * The user's id on google plus..
   */
  private final String userId;
  
  /**
   * Dropbox client for the client user.
   */
  private final DbxClient client;
  
  /**
   * The name of the user.
   */
  private final String userName;
  
  
  /**
   * Constructor.
   * 
   * @param accessToken The access token for the current user.
   * @param userId The id of the current user.
   */
  public UserData(String accessToken, String userId) throws IOException, DbxException {
    client = new DbxClient(Credentials.getRequestConfig(), accessToken);
    this.userId = userId;
    userName = client.getAccountInfo().displayName;
  }
  
  /**
   * Return the user id.
   * 
   * @return The user id.
   */
  public String getId() {
    return this.userId;
  }

  /**
   * Returns the dropbox client for the user.
   * 
   * @return the dropbox client.
   */
  public DbxClient getClient() {
    return client;
  }
  
  /**
   * Returns the user name.
   * 
   * @return the user name.
   */
  public String getUserName() {
    return userName;
  }
}
