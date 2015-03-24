package com.oxygenxml.examples.gdrive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import de.schlichtherle.io.FileInputStream;

/**
 * A simple database backed by a properties file.
 */
public class TokenDb {

  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger
      .getLogger(TokenDb.class.getName());

  /**
   * The backing file of the db.
   */
  private final File propsFile;

  /**
   * The password to encrypt/decrypt with.
   */
  private String password;

  /**
   * Constructor.
   * 
   * @param file
   *          The file backing the db.
   * 
   * @throws IOException
   */
  public TokenDb(File file, String password) throws IOException {
    propsFile = file;
    this.password = password;
    propsFile.createNewFile();
  }

  /**
   * Store the mapping from the user id to token.
   * 
   * @param userId
   *          The user id.
   * @param token
   *          The token.
   * 
   * @throws IOException
   */
  public void storeToken(String userId, String token) throws IOException {
    Properties props = loadProps();

    // Encrypt the token.
    String encryptedToken = encrypt(token);

    // Store the entry.
    props.setProperty(userId, encryptedToken);

    persist(props);
  }

  /**
   * Persists the properties to the on-disk file.
   * 
   * @param props
   *          The properties.
   * 
   * @throws FileNotFoundException
   *           If the on-disk file cannot be found.
   * @throws IOException
   *           If an error occurs while storing the props.
   */
  private void persist(Properties props) throws FileNotFoundException,
      IOException {
    FileOutputStream outputStream = new FileOutputStream(propsFile);
    try {
      props.store(outputStream, "");
    } finally {
      outputStream.close();
    }
  }

  /**
   * Remove the mapping for the given user id.
   * 
   * @param userId
   *          The user id.
   * 
   * @throws IOException
   */
  public void removeToken(String userId) throws IOException {
    Properties props = loadProps();
    props.remove(userId);
    persist(props);
  }

  /**
   * Load the token from the db.
   * 
   * @param userId
   *          The user id.
   * 
   * @return The corresponding token.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  public String loadToken(String userId) {
    String encryptedToken = null;
    String decryptedToken = null;
    Properties props;
    try {
      props = loadProps();
      encryptedToken = (String) props.get(userId);
      decryptedToken = decrypt(encryptedToken);
    } catch (IOException e) {
      // Just assume that the token was not found.
      logger.warn(e, e);
    }
    return decryptedToken;
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

  /**
   * Encrypt.
   * 
   * @param text
   *          the text to encrypt.
   * 
   * @return the encrypted text.
   */
  private String encrypt(final String text) {
    byte[] xor = this.xor(text.getBytes());
    return Base64.encodeBase64String(xor);
  }

  /**
   * Decrypt.
   * 
   * @param hash
   *          the hash to be decrypted.
   * 
   * @return the decrypted hash.
   */
  private String decrypt(final String hash) {
    try {
      byte[] decodeBase64 = Base64.decodeBase64(hash.getBytes());
      byte[] xor = this.xor(decodeBase64);
      return new String(xor, "UTF-8");
    } catch (java.io.UnsupportedEncodingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * XOR between input and secret key.
   * 
   * @param input
   *          the input as bytes.
   * 
   * @return the result of XOR between input and secret key.
   */
  private byte[] xor(final byte[] input) {
    final byte[] output = new byte[input.length];
    final byte[] secret = password.getBytes();
    int spos = 0;
    for (int pos = 0; pos < input.length; ++pos) {
      output[pos] = (byte) (input[pos] ^ secret[spos]);
      spos += 1;
      if (spos >= secret.length) {
        spos = 0;
      }
    }
    return output;
  }
}
