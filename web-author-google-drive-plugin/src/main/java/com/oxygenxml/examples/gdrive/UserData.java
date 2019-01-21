package com.oxygenxml.examples.gdrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

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
    
    String accessToken = credential.getAccessToken();
    UserInfoJson userInfo = getUserInfo(accessToken);
    
    this.userId = userInfo.getId();
    this.userName = userInfo.getName();
  }
  
  /**
   * Make a google API request and retrieve user information.
   * @param accessToken The access token.
   * @return User information.
   * @throws IOException
   */
  private UserInfoJson getUserInfo(String accessToken) throws IOException {
    URL userInfoApiUrl = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
    
    HttpURLConnection conn = (HttpURLConnection) userInfoApiUrl.openConnection();
    conn.addRequestProperty("Authorization", "Bearer " + accessToken);
    conn.addRequestProperty("Host", "www.googleapis.com");
    
    try (InputStream is = conn.getInputStream()) {
      return new Gson().fromJson(
        new JsonReader(new InputStreamReader(is)), 
        UserInfoJson.class
      );
    }
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
