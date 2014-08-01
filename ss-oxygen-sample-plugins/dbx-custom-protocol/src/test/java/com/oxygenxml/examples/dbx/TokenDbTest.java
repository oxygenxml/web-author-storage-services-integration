package com.oxygenxml.examples.dbx;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TokenDbTest {

  private File dbFile;


  @Before
  public void setUp() {
    dbFile = new File("token-db.properties");
  }
  
  @After
  public void tearDown() {
    dbFile.delete();
  }
  
  
  /**
   * <p><b>Description:</b> Test that loading and storing a token works.</p>
   * <p><b>Bug ID:</b> EXM-30812</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testCreateFile() throws Exception {
    TokenDb tokenDb = new TokenDb(dbFile);
    String token = "asdasDSAD";
    tokenDb.storeToken("gigi", token);
    assertEquals(token, tokenDb.loadToken("gigi"));
  }
  
  
  /**
   * <p><b>Description:</b> Test that we can reuse an old db.</p>
   * <p><b>Bug ID:</b> EXM-30812</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testReuseDb() throws Exception {
    TokenDb tokenDb = new TokenDb(dbFile);
    String token = "asdasDSAD";
    tokenDb.storeToken("gigi", token);
    
    tokenDb = new TokenDb(dbFile);
    assertEquals(token, tokenDb.loadToken("gigi"));
  }
  
  /**
   * <p><b>Description:</b> Test change db.</p>
   * <p><b>Bug ID:</b> EXM-30812</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testChangeDb() throws Exception {
    TokenDb tokenDb = new TokenDb(dbFile);
    String token = "asdasDSAD";
    tokenDb.storeToken("gigi", token);
    assertEquals(token, tokenDb.loadToken("gigi"));
    tokenDb.storeToken("gigi", token + "-new");
    assertEquals(token + "-new", tokenDb.loadToken("gigi"));
  }  
  
  /**
   * <p><b>Description:</b> Test load inexistent.</p>
   * <p><b>Bug ID:</b> EXM-30812</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testNullToken() throws Exception {
    TokenDb tokenDb = new TokenDb(dbFile);
    assertNull(tokenDb.loadToken("gigi"));
  }  
 
}
