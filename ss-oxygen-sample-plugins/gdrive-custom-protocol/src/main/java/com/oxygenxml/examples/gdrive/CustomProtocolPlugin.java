package com.oxygenxml.examples.gdrive;

import org.apache.log4j.Logger;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Plugin that enables the google drive protocol.
 */
public class CustomProtocolPlugin extends Plugin {
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = 
      Logger.getLogger(CustomProtocolPlugin.class.getName());
  
  /**
   * Constructor.
   * 
   * @param descriptor The plugin descriptor.
   */
  public CustomProtocolPlugin(PluginDescriptor descriptor) {
    super(descriptor);
    logger.debug("Plugin created");
  }
}
