package com.oxygenxml.examples.gdrive;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.plus.Plus;


/**
 * Class that stores the details of our Google API app.
 */
public class Credentials {
  private static final String APPLICATION_NAME = "oXygen XML Author WebApp";
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(Credentials.class.getName());
  /**
   * The http transport to use.
   */
  private static final NetHttpTransport httpTransport = new NetHttpTransport(); 
  /**
   * The json factory to use.
   */
  private static final JacksonFactory jsonFactory = new JacksonFactory();
  
  /**
   * Singleton instance.
   */
  private static Credentials instance;
  
  /**
   * Returns the instance.
   * 
   * @return The credentials.
   */
  public static Credentials getInstance() {
    return instance;
  }
  
  /**
   * Initialize the credentials from a JSON file.
   * 
   * @param secretsStream The input stream from which the secrets in JSON 
   * format can be read.
   */
  public static void setCredentialsFromStream(InputStream secretsStream) throws FileNotFoundException, IOException {
    Preconditions.checkState(instance == null);
    GoogleClientSecrets clientSecrets = 
        GoogleClientSecrets.load(jsonFactory, new InputStreamReader(secretsStream));
    Details secrets = clientSecrets.getWeb();
    logger.debug("Loaded secrets from file.");
    instance = new Credentials(
        secrets.getClientId(), 
        secrets.getClientSecret(), 
        secrets.getRedirectUris().get(0));
  }
  
  /**
   * Client id of our app.
   */
  private final String clientId; 
  /**
   * Client secret of our app.
   */
  private final String clientSecret;

  /**
   * URI to redirect to after user authorizes our app.
   */
  private final String redirectUri;
  
  /**
   * The authorization flow.
   */
  private final GoogleAuthorizationCodeFlow clientFlow;
  

  /**
   * Constructor.
   * 
   * @param clientId The client id of the app.
   * @param clientSecret The client secret.
   * @param redirectUri The redirect uri.
   */
  private Credentials(String clientId, String clientSecret, String redirectUri) {
    clientFlow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, jsonFactory, 
        clientId, clientSecret, 
        Arrays.asList(
            "profile", // For getting the user id.
            "https://www.googleapis.com/auth/drive.install",
            DriveScopes.DRIVE
        ))
    .setAccessType("offline")
    .setApprovalPrompt("auto").build();
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
  }
  /**
   * Create a token request.
   * 
   * @param code The code to exchange for the token request.
   * 
   * @return The token request.
   */
  public GoogleAuthorizationCodeTokenRequest createAuthorizationCodeTokenRequest(String code) {
    return clientFlow.newTokenRequest(code).setRedirectUri(redirectUri);
  }
  
  /**
   * Create an authorization code request.
   * 
   * @return The request.
   */
  public GoogleAuthorizationCodeRequestUrl createAuthorizationCodeRequestUrl() {
    return clientFlow.newAuthorizationUrl().setRedirectUri(redirectUri);
  }
  
  /**
   * Create the refresh token request.
   * 
   * @param refreshToken The refresh token.
   * 
   * @return The refresh token request.
   */
  public GoogleRefreshTokenRequest createRefreshTokenReqeust(String refreshToken) throws IOException {
    GoogleRefreshTokenRequest refreshTokenRequest = new GoogleRefreshTokenRequest(httpTransport, jsonFactory, 
        refreshToken, clientId, clientSecret)
        .setRequestInitializer(new OxygenHttpRequestInitializer());
    return refreshTokenRequest;
  }
  
  /**
   * Returns a new Drive client.
   * 
   * @param credential The credential of the user.
   * 
   * @return The drive instance.
   */
  public Drive newDrive(GoogleCredential credential) {
    return new Drive.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(APPLICATION_NAME)
      .build();
  }
  
  /**
   * Returns a new Google Plus client.
   * 
   * @param credential The credential of the user.
   * 
   * @return The client instance.
   */  
  public Plus newPlus(GoogleCredential credential) {
    return new Plus.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(APPLICATION_NAME)
      .build();
  }
  
  /**
   * @return the client id.
   */
  public String getClientId() {
    return clientId;
  }
}
