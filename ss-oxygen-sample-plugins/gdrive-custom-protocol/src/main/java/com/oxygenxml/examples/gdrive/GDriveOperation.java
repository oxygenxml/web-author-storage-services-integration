package com.oxygenxml.examples.gdrive;

import java.io.IOException;

import com.google.api.services.drive.Drive;

/**
 * Operation executed on a google drive.
 * 
 * @param <T> The return type of the operation.
 */
public interface GDriveOperation<T> {
  /**
   * The actual operation behavior.
   * 
   * @param drive The drive to operate on.
   * 
   * @param <T> The return type of the operation.
   * 
   * @return The result of the operation.
   */
  public T executeOperation(Drive drive) throws IOException;
}
