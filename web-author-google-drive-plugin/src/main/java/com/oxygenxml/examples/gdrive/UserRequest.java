package com.oxygenxml.examples.gdrive;

import javax.annotation.Nullable;

/**
 * Represents a "user request".
 * A user request being a non-validation thread request.
 * 
 * @author gabriel_titerlea
 */
public class UserRequest {
  /**
   * Holds optional user data for this request.
   */
  private final UserData userData;
  
  /**
   * Creates a user request.
   * @param userData The optional user data associated with this request.
   */
  public UserRequest(UserData userData) {
    this.userData = userData;
  }
  
  /**
   * @return The userData associated with this request.
   */
  @Nullable
  public UserData getUserData() {
    return userData;
  }
}
