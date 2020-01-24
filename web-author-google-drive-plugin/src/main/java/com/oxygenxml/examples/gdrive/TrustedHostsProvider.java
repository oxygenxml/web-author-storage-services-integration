package com.oxygenxml.examples.gdrive;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ro.sync.exml.plugin.workspace.security.Response;
import ro.sync.exml.plugin.workspace.security.TrustedHostsProviderExtension;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionChangedEvent;
import ro.sync.exml.workspace.api.options.WSOptionListener;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * {@link TrustedHostsProviderExtension} implementation that trust imposed host.
 */
public class TrustedHostsProvider implements TrustedHostsProviderExtension {

  /**
   * Safe host.
   */
  private final Set<String> trustedHosts = new HashSet<>(
      Arrays.asList(
          "accounts.google.com:443", 
          "googleapis.com:443", 
          "www.googleapis.com:443"));
  private String userContentHost = ".googleusercontent.com:443";
  
  /**
   * If <code>true</code> the Google Drive plugin is configured.
   */
  private boolean isConfigured = false;
  
  /**
   * Constructor.
   */
  public TrustedHostsProvider() {
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    updateEnforcedHost(optionsStorage);

    optionsStorage.addOptionListener(new WSOptionListener() {
      @Override
      public void optionValueChanged(WSOptionChangedEvent event) {
        if (GDriveManagerFilter.GDRIVE_PASSWORD_OPTION_KEY.equals(event.getOptionKey())
            || GDriveManagerFilter.GDRIVE_SECRETS_OPTION_KEY.equals(event.getOptionKey())) {
          updateEnforcedHost(optionsStorage);
        }
      }
    });
  }

  /**
   * Update the enforced host field.
   */
  private void updateEnforcedHost(WSOptionsStorage optionsStorage) {
    String password = optionsStorage.getOption(GDriveManagerFilter.GDRIVE_PASSWORD_OPTION_KEY, null);
    String secrets = optionsStorage.getOption(GDriveManagerFilter.GDRIVE_SECRETS_OPTION_KEY, null);
    if (password != null && !password.isEmpty() && secrets != null && !secrets.isEmpty()) {
      isConfigured = true;
    } else {
      isConfigured = false;
    }
  }

  @Override
  public Response isTrusted(String hostName) {
    if (this.isConfigured && (trustedHosts.contains(hostName) || hostName.endsWith(userContentHost))) {
      return TrustedHostsProvider.TRUSTED;
    } else {
      return TrustedHostsProvider.UNKNOWN;
    }
  }
}
