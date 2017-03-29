package com.oxygenxml.examples.dbx;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

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
    ClassLoader classLoader = this.getClass().getClassLoader();
    logger.debug("Classloader " + classLoader);
    if (classLoader instanceof URLClassLoader) {
      URL[] urls = ((URLClassLoader)classLoader).getURLs();
      logger.debug("Classpath: " + Arrays.toString(urls));
    }
  }
}
