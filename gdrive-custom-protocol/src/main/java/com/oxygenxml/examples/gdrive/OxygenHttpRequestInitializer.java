package com.oxygenxml.examples.gdrive;

import java.io.IOException;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

/**
 * Request initializer of the http requests to make them work with our URL stream
 * handlers that handle Gzip compression transparently.
 */
final class OxygenHttpRequestInitializer implements HttpRequestInitializer {
  
  /**
   * @see com.google.api.client.http.HttpRequestInitializer#initialize(com.google.api.client.http.HttpRequest)
   */
  @Override
  public void initialize(HttpRequest request) throws IOException {
    request.getHeaders().setAcceptEncoding("identity");
  }
}