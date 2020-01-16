package com.oxygenxml.examples.gdrive;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.plugin.workspace.security.TrustedHostsProvider;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class GDriveWorkspaceAccess implements WorkspaceAccessPluginExtension {

  @Override
  public void applicationStarted(StandalonePluginWorkspace pluginWorkspace) {
    ((StandalonePluginWorkspace) pluginWorkspace).addTrustedHostsProvider(
      new TrustedHostsProvider() {
        @Override
        public Response isTrusted(String hostName) {
          if ("accounts.google:443".equals(hostName) || "googleapis:443".equals(hostName)) {
            return TrustedHostsProvider.TRUSTED;
          } else {
            return TrustedHostsProvider.UNKNOWN;
          }
        }
      }
    );
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
}
