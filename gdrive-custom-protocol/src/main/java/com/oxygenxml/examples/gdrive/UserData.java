package com.oxygenxml.examples.gdrive;

import java.io.IOException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;


/**
 * Data stored for a connected user.
 */
public class UserData {
  
  /**
   * The user's drive.
   */
  private final Drive drive;
  
  /**
   * The user's id on google plus..
   */
  private final String userId;

  /**
   * The user name.
   */
  private final String userName;
  
  /**
   * Constructor.
   * 
   * @param credential The credentials of the user.
   */
  public UserData(GoogleCredential credential) throws IOException {
    Credentials credentials = Credentials.getInstance();
    this.drive = credentials.newDrive(credential);
    
    Plus plus = credentials.newPlus(credential);
    Person user = plus.people().get("me").execute();
    this.userId = user.getId();
    this.userName = user.getDisplayName();
  }
  
  /**
   * Return the drive of the user.
   * 
   * @return the drive of the user.
   */
  public Drive getDrive() {
    return drive;
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
   * Returns the user name.
   * 
   * @return the user name.
   */
  public String getUserName() {
    return userName;
  }
}
