package com.oxygenxml.examples.dbx;

import java.io.IOException;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;


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
  private final DbxClientV2 client;
  
  /**
   * The name of the user.
   */
  private final String userName;
  
  
  /**
   * Constructor.
   * 
   * @param accessToken The access token for the current user.
   * @param userId The id of the current user.
   * 
   * @throws DbxException If it fails to retrieve the user name.
   */
  public UserData(String accessToken, String userId) throws IOException, DbxException {
    client = new DbxClientV2(Credentials.getRequestConfig(), accessToken);
    this.userId = userId;
    userName = client.users().getCurrentAccount().getName().getDisplayName();
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
  public DbxClientV2 getClient() {
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
