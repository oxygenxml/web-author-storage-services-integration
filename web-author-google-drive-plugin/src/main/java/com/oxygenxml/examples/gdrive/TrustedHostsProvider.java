package com.oxygenxml.examples.gdrive;

import ro.sync.exml.plugin.workspace.security.Response;
import ro.sync.exml.plugin.workspace.security.TrustedHostsProviderExtension;

/**
 * {@link TrustedHostsProviderExtension} implementation that trust required hosts.
 */
public class TrustedHostsProvider implements TrustedHostsProviderExtension {

  @Override
  public Response isTrusted(String hostName) {
    if ("accounts.google:443".equals(hostName) || "googleapis:443".equals(hostName)) {
      return TrustedHostsProvider.TRUSTED;
    } else {
      return TrustedHostsProvider.UNKNOWN;
    }
  }
}