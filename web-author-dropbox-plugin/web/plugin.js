(function() {
  // The app key.
  var DATA_APP_KEY = "nj59lfu1m0hx915";

  var url = decodeURIComponent(sync.util.getURLParameter("url"));

  var protoPrefix = null;
  var useDbxProtocol = false;
  if(url.indexOf("dbx:///") === 0) {
    protoPrefix= "dbx:///";
    useDbxProtocol = true;
  } else if(url.indexOf("dbx://") === 0) {
    protoPrefix= "dbx://";
    useDbxProtocol = true;
  } else if(url.indexOf("dbx:/") === 0) {
    protoPrefix= "dbx:/";
    useDbxProtocol = true;
  }
  if (url == 'null'  || useDbxProtocol) {
    if(useDbxProtocol) {
      var partialPath = url.substring(protoPrefix.length);
      var userID = partialPath.substring(0, partialPath.indexOf('/'));
      var path = partialPath.substring(userID.length);
    }
    var dbxScript = document.createElement('script');
    dbxScript.setAttribute("src", "https://www.dropbox.com/static/api/2/dropins.js");
    dbxScript.setAttribute("id", "dropboxjs");
    dbxScript.setAttribute("data-app-key", DATA_APP_KEY);
    document.head.appendChild(dbxScript);


    goog.provide('DropboxUrlChooser');

    goog.require('sync.api.UrlChooser');

    /**
     * A implementation class for URL choosers.
     *
     * @constructor
     */
    var DropboxUrlChooser = function() {
      sync.api.UrlChooser.call(this);
    };
    goog.inherits(DropboxUrlChooser, sync.api.UrlChooser);

    /**
     * Invokes the URL chooser.
     *
     * @param {sync.api.UrlChooser.Context} context The context in which
     * the chooser is invoked - it contains information like the type of
     * the resource that we want the user to choose: image, external xml file,
     * etc.
     *
     * @param {function(string)} chosen The function to be called with the result
     * of the choice. If the user failed to choose anything, pass null to
     * the function.
     *
     * @param {sync.api.UrlChooser.Purpose} purpose The purpose the chooser is
     * invoked with.
     */
    DropboxUrlChooser.prototype.chooseUrl = function(context, chosen, purpose) {
      var supportedExtensions = null;
      // set the correct extensions depending on the context type.
      if(context.getType() === sync.api.UrlChooser.Type.IMAGE) {
        supportedExtensions = ['.bmp', '.cr2', '.gif', '.ico', '.ithmb', '.jpeg', '.jpg', '.nef', '.png', '.raw', '.svg', '.tif', '.tiff', '.wbmp', '.webp'];
      } else if(context.getType() === sync.api.UrlChooser.Type.GENERIC) {
        supportedExtensions = ['.xml', '.dita', '.ditamap', '.ditaval', '.mathml', 'xhtml'];
      }

      var options = {
        // Required. Called when a user selects an item in the Chooser.
        success: function (files) {
          var link = files[0].link;

          var prefix = "https://dl.dropboxusercontent.com/1/view/";
          var codeAndPath = link.substring(prefix.length);
          var path = codeAndPath.substring(codeAndPath.indexOf('/'));

          if(userID) {
            var imageURL = 'dbx:///' + userID + path;
            chosen(imageURL);
          } else {
            // if we do not have the user id we open the file ourselves and request authorization.
            chosen(null);
            // we open the dropbox file in the same tab in order to prevent it to be
            // considered a pop-up and blocked.
            window.location.href = "../plugins-dispatcher/dbx-start?path=" + encodeURIComponent(encodeURIComponent(path));
          }
        },
        cancel: function() {
          chosen(null);
        },

        linkType: "direct",
        multiselect: false,
        extensions: supportedExtensions
      };
      
      Dropbox.choose(options);
    };

    /**
     * Invokes the save chooser.
     *
     * @param fileURL the url of the file to save.
     * @param fileName the suggested name for the file that will be created.
     * @param callback the callback function to call with the new file path as parameter.
     * @param {boolean}opt_externalAccess {true} if the fileURL can be accessed by the dropbox servers.
     */
    DropboxUrlChooser.prototype.saveFile = function (fileURL, fileName, callback, opt_externalAccess) {
      var options = {
        success: function (e) {
          callback(null, sync.api.UrlChooser.SaveResult.SAVED_UNAVAILABLE_URL);
        },
        // Error is called in the event of an unexpected response from the server
        error: function (errorMessage) {
          console.log('An error occurred :' + errorMessage);
        }
      };
      var deployedVersion = 'v21.1.1.0';
      fileURL = fileURL.replace(new RegExp('https://staging-webapp.sync.ro/oxygen-xml-web-author/rest/[^/]+/'),
          'https://www.oxygenxml.com/oxygen-xml-web-author/rest/' + deployedVersion + '/');
      Dropbox.save(fileURL, fileName, options);
    };

    /**
     * Checks whether the URL chooser supports choosing a given type of
     * resource {@link sync.api.UrlChooser.Type}. If not supported, the
     * default UI (which is usually a text-field) will be presented to the user.
     *
     * @param {sync.api.UrlChooser.Type} type The type of the URL to be chosen.
     *
     * @return {boolean} true if the chooser supports the given type.
     */
    DropboxUrlChooser.prototype.supports = function(type) {
      var supports = false;
      if(type === sync.api.UrlChooser.Type.IMAGE || type === sync.api.UrlChooser.Type.EXTERNAL_REF) {
        supports = true;
      }
      return supports;
    };

    var urlChooser = new DropboxUrlChooser();
    // if the current opened file is from dropbox use the Dropbox url chooser.
    if(useDbxProtocol) {
      workspace.setUrlChooser(urlChooser);

      // Remove insert new topic ref action from the toolbar.
      var actionId = 'insert.new.topic';
      goog.events.listen(workspace, sync.api.Workspace.EventType.EDITOR_LOADED, function(e) {
        goog.events.listen(e.editor, sync.api.Editor.EventTypes.ACTIONS_LOADED, function() {
          e.editor.getActionsManager().unregisterAction(actionId);
        });
      });
    }

    var openAction = new sync.actions.OpenAction(urlChooser);
    openAction.setDescription('Open a document from your Dropbox');
    openAction.setLargeIcon('../plugin-resources/dbx/Dropbox70' + (sync.util.getHdpiFactor() > 1 ? '@2x' : '') + '.png');
    openAction.setActionId('dbx-open-action');
    openAction.setActionName('Dropbox');
    workspace.getActionsManager().registerOpenAction(
        openAction);

    var createAction = new sync.api.CreateDocumentAction(urlChooser);
    createAction.setDescription('Save a template document in your Dropbox');
    var largeIcon = '../plugin-resources/dbx/Dropbox70' + (sync.util.getHdpiFactor() > 1 ? '@2x' : '') + '.png';
    createAction.setLargeIcon(largeIcon);
    createAction.setActionId('dbx-create-action');
    createAction.setActionName('Dropbox');
    workspace.getActionsManager().registerCreateAction(
        createAction);
  }

})();
