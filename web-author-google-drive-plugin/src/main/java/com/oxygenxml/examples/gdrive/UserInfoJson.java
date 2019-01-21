/*
 * Copyright (c) 2019 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package com.oxygenxml.examples.gdrive;

import com.google.gson.annotations.SerializedName;

public class UserInfoJson {

  @SerializedName("sub")
  public String id;
  
  @SerializedName("name")
  public String name;

  /**
   * @return the userId
   */
  public String getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
}
