package com.oxygenxml.examples.gdrive;

import java.net.URLStreamHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerPluginExtension;


/**
 * Plugin extension.
 */
public class CustomProtocolURLHandlerExtension implements URLStreamHandlerPluginExtension {
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      LogManager.getLogger(CustomProtocolURLHandlerExtension.class.getName());
  
  /**
   * Constructor.
   */
  public CustomProtocolURLHandlerExtension() {
    logger.debug("Extension created");
  }

  /**
   * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerPluginExtension#getURLStreamHandler(java.lang.String)
   */
  @Override
  public URLStreamHandler getURLStreamHandler(String protocol) {
    if ("gdrive".equals(protocol)) {
      return new GDriveUrlStreamHandler();
    }
    return null;
  }

}
