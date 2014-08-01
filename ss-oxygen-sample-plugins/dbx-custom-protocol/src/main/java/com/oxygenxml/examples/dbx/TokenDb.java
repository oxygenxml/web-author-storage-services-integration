package com.oxygenxml.examples.dbx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.schlichtherle.io.FileInputStream;

/**
 * A simple database backed by a properties file.
 */
public class TokenDb {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(TokenDb.class.getName());
  
  /**
   * The backing file of the db.
   */
  private final File propsFile;
  
  /**
   * Constructor.
   * 
   * @param file The file backing the db.
   * 
   * @throws IOException
   */
  public TokenDb(File file) throws IOException {
    propsFile = file;
    propsFile.createNewFile();
  }
  
  /**
   * Store the mapping from the user id to token.
   * 
   * @param userId The user id.
   * @param token The token.
   * 
   * @throws IOException
   */
  public void storeToken(String userId, String token) throws IOException {
    Properties props = loadProps();
    props.setProperty(userId, token);
    
    FileOutputStream outputStream = new FileOutputStream(propsFile);
    props.store(outputStream, "");
    outputStream.close();
  }

  /**
   * Load the token from the db.
   * 
   * @param userId The user id.
   * 
   * @return The corresponding token.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  public String loadToken(String userId) {
    String token = null;
    Properties props;
    try {
      props = loadProps();
      token = (String) props.get(userId);
    } catch (IOException e) {
      // Just assume that the token was not found.
      logger.warn(e, e);
    }
    return token;
  }
  
  /**
   * Loads the token db from the disk file.
   * 
   * @return The db.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private Properties loadProps() throws FileNotFoundException, IOException {
    FileInputStream inputStream = new FileInputStream(propsFile);
    
    Properties props = new Properties();
    props.load(inputStream);
    inputStream.close();
    return props;
  }
  

  
}
