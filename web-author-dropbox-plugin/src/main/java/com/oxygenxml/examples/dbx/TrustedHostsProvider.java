package com.oxygenxml.examples.dbx;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
   * Trusted host.
   */
  private AtomicReference<Set<String>> trustedHostsRef = new AtomicReference<>(null);

  /**
   * Constructor.
   */
  public TrustedHostsProvider() {
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    updateEnforcedHost(optionsStorage);

    optionsStorage.addOptionListener(new WSOptionListener(DbxManagerFilter.DBX_SECRETS_OPTIONS_KEY) {
      @Override
      public void optionValueChanged(WSOptionChangedEvent event) {
        updateEnforcedHost(optionsStorage);
      }
    });
  }

  /**
   * Update the enforced host field.
   */
  private void updateEnforcedHost(WSOptionsStorage optionsStorage) {
    HashSet<String> trustedHosts = null;

    String secretsOptionValue = optionsStorage.getOption(DbxManagerFilter.DBX_SECRETS_OPTIONS_KEY, null);
    boolean isConfigured = secretsOptionValue != null && !secretsOptionValue.isEmpty();
    if (isConfigured) {
      trustedHosts = new HashSet<>();
      trustedHosts.add("api.dropboxapi.com:443");
      trustedHosts.add("content.dropboxapi.com:443");
    }

    this.trustedHostsRef.set(trustedHosts);
  }

  @Override
  public Response isTrusted(String hostName) {
    Set<String> trustedHosts = trustedHostsRef.get();
    if (trustedHosts != null && trustedHosts.contains(hostName)) {
      return TrustedHostsProvider.TRUSTED;
    } else {
      return TrustedHostsProvider.UNKNOWN;
    }
  }
}
