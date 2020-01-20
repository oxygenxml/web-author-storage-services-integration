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
   * Enforced host.
   */
  private Set<String> trustedHosts = null;

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
    Set<String> toSet = null;
    
    String password = optionsStorage.getOption(GDriveManagerFilter.GDRIVE_PASSWORD_OPTION_KEY, null);
    String secrets = optionsStorage.getOption(GDriveManagerFilter.GDRIVE_SECRETS_OPTION_KEY, null);
    if (password != null && !password.isEmpty() && secrets != null && secrets.isEmpty()) {
      toSet = new HashSet<>(
          Arrays.asList(
              "accounts.google.com:443", 
              "googleapis.com:443", 
              "www.googleapis.com:443", 
              "*.googleusercontent.com:443"));
    }

    this.trustedHosts = toSet;
  }

  @Override
  public Response isTrusted(String hostName) {
    if (trustedHosts != null && trustedHosts.contains(hostName)) {
      return TrustedHostsProvider.TRUSTED;
    } else {
      return TrustedHostsProvider.UNKNOWN;
    }
  }
}
