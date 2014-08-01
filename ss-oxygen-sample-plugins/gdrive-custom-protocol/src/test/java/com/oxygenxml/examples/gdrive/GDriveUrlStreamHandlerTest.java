package com.oxygenxml.examples.gdrive;


public class GDriveUrlStreamHandlerTest {

//  
//  /**
//   * <p><b>Description:</b> Test searching for a file, basic scenario.</p>
//   * <p><b>Bug ID:</b> EXM-30812</p>
//   *
//   * @author costi
//   *
//   * @throws Exception
//   */
//  @Test
//  public void testFileSearching() throws Exception {
//    Drive drive = Mockito.mock(Drive.class);
//    File ditamapInRoot = new File();
//    Map<List<String>, List<File>> results = new HashMap<List<String>, List<File>>();
//    results.put(Arrays.asList("flowers.ditamap", "root"), Arrays.asList(ditamapInRoot));
//    GDriveUrlStreamHandler handler = createHandler(results);
//    
//    File toDownload = handler.getFileToDownload(new URL("gdrive", "", 0, "/user-1/flowers.ditamap", handler), drive);
//    assertTrue(ditamapInRoot == toDownload);
//  }
//
//  
//  /**
//   * <p><b>Description:</b> Test searching for a file, file not found.</p>
//   * <p><b>Bug ID:</b> EXM-30812</p>
//   *
//   * @author costi
//   *
//   * @throws Exception
//   */
//  @Test
//  public void testFileSearchingNotFound() throws Exception {
//    Drive drive = Mockito.mock(Drive.class);
//    Map<List<String>, List<File>> results = new HashMap<List<String>, List<File>>();
//    results.put(Arrays.asList("flowers.ditamap", "root"), Arrays.<File>asList());
//    GDriveUrlStreamHandler handler = createHandler(results);
//    
//    try {
//      handler.getFileToDownload(new URL("gdrive", "", 0, "/user-1/flowers.ditamap", handler), drive);
//      fail();
//    } catch (FileNotFoundException ex) {
//    }
//  }
//  
//  /**
//   * <p><b>Description:</b> Test searching for a file, multiple file with same name.</p>
//   * <p><b>Bug ID:</b> EXM-30812</p>
//   *
//   * @author costi
//   *
//   * @throws Exception
//   */
//  @Test
//  public void testFileSearchingMultipleFiles() throws Exception {
//    Drive drive = Mockito.mock(Drive.class);
//    File projFile1 = new File();
//    File projFile2 = new File();
//    Map<List<String>, List<File>> results = new HashMap<List<String>, List<File>>();
//    results.put(Arrays.asList("proj", "root"), Arrays.asList(projFile1, projFile2));
//    GDriveUrlStreamHandler handler = createHandler(results);
//    
//    try {
//      handler.getFileToDownload(new URL("gdrive", "", 0, "/user-1/proj/flowers.ditamap", handler), drive);
//      fail();
//    } catch (FileNotFoundException ex) {
//    }
//  }
//  
//  
//  /**
//   * <p><b>Description:</b> Test searching for a file, longer path.</p>
//   * <p><b>Bug ID:</b> EXM-30812</p>
//   *
//   * @author costi
//   *
//   * @throws Exception
//   */
//  @Test
//  public void testFileSearchingPath() throws Exception {
//    Drive drive = Mockito.mock(Drive.class);
//    File projDir = new File();
//    projDir.setId("id_proj");
//    File imagesDir = new File();
//    imagesDir.setId("id_images");
//    File imgFile = new File();
//    Map<List<String>, List<File>> results = new HashMap<List<String>, List<File>>();
//    results.put(Arrays.asList("proj", "root"), Arrays.asList(projDir));
//    results.put(Arrays.asList("images", "id_proj"), Arrays.asList(imagesDir));
//    results.put(Arrays.asList("img.png", "id_images"), Arrays.asList(imgFile));
//    GDriveUrlStreamHandler handler = createHandler(results);
//    
//    File toDownload = handler.getFileToDownload(new URL("gdrive", "", 0, "/user-1/proj/images/img.png", handler), drive);
//    assertEquals(imgFile, toDownload);
//  }  
//
//  /**
//   * Creates a stream handler that responds to queries for files with the prescribed 
//   * results. 
//   * 
//   * @param results The results.
//   * @return The stream handler.
//   */
//  private GDriveUrlStreamHandler createHandler(final Map<List<String>, List<File>> results) {
//    return new GDriveUrlStreamHandler() {
//      @Override
//      List<File> searchFile(Drive currentDrive, String fileName, String parentId) throws IOException {
//        return results.get(Arrays.asList(fileName, parentId));
//      }
//    };
//  }
}
