var loadGDriveAuthApi = null;

(function () {

  var tabName = 'Google Drive';

  /**
   * A implementation class for URL choosers for google drive
   *
   * @constructor
   *
   * @extends {sync.api.UrlChooser}
   */
  var GDriveUrlChooser = function () {
    sync.api.UrlChooser.call(this);
    this.obtainAppClienId();
    this.scope = [
      'https://www.googleapis.com/auth/drive',
      'profile'
    ];
    this.gDriveToken = null;
    this.userId = null;
  };
  goog.inherits(GDriveUrlChooser, sync.api.UrlChooser);

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
  /** @override */
  GDriveUrlChooser.prototype.chooseUrl = function (context, chosen, purpose) {
    this.chooseUrlInternal_(context, chosen, purpose);
  };

  /**
   * Chooses the url for internal use.Optains the file url.
   *
   * @param context the context in which the chooser is called.
   * @param chosen the callback method to call when the url has been chosen.
   * @param purpose the purpose of the choosing.
   * @private
   */
  GDriveUrlChooser.prototype.chooseUrlInternal_ = function (context, chosen, purpose) {
    if (this.gDriveToken) {
      this.choose_(context,
          goog.bind(this.chooserCallback, this, chosen));
    } else {
      this.obtainAuthorization(goog.bind(this.chooseUrlInternal_, this, context, chosen, purpose));
    }
  };

  /**
   *  Opens the file selected thru browser redirect.
   *  Calls the callback method with null so that is does not handle the
   *  obtained url.
   *
   * @param context the context in which the chooser is called.
   * @param chosen the callback method to call when the url has been chosen.
   * @param purpose the purpose of the choosing.
   * @private
   */
  GDriveUrlChooser.prototype.chooseUrlForRedirect = function (context, chosen, purpose) {
    if (this.gDriveToken) {
      this.choose_(context,
          goog.bind(function(callback, data) {
            if(data.action === 'picked') {
              var fileId = data.docs[0].id;
              this.openFileById(fileId);
            }
          }, this, chosen));
    } else {
      this.obtainAuthorization(goog.bind(this.chooseUrlForRedirect, this, context, chosen, purpose));
    }
  };

  /**
   * Creates the gdrive picker handling the choose.
   *
   * @private
   */
  GDriveUrlChooser.prototype.choose_ = function (context, callback) {
    var viewID = google.picker.ViewId.DOCS;
    if(context.getType() === sync.api.UrlChooser.Type.IMAGE) {
      viewID = google.picker.ViewId.DOCS_IMAGES;
    } else if(context.getType() === sync.api.UrlChooser.Type.GENERIC) {
      viewID = google.picker.ViewId.DOCS;
    }
    var view = new google.picker.DocsView(viewID);

    var picker = new google.picker.PickerBuilder()
        .addView(view)
        .setOrigin(window.location.protocol + '//' + window.location.host)
        .setOAuthToken(this.gDriveToken)
        .setCallback(callback)
        .build();
    picker.setVisible(true);
  };

  /**
   * Obtains the app client id from the server.
   */
  GDriveUrlChooser.prototype.obtainAppClienId = function() {
    var requestContent = new goog.net.XhrIo();
    goog.events.listenOnce(requestContent, goog.net.EventType.COMPLETE, goog.bind(function () {
      this.appClientId = requestContent.getResponse();
      this.gDrivecheckAuth();
    }, this));
    requestContent.send('../plugins-dispatcher/gdrive-start', 'PUT');
  };

  /**
   * Obtains authorization from the user to acces it's
   * profile and google drive.
   *
   * @param callback the callback to be called when authorization finished.
   */
  GDriveUrlChooser.prototype.obtainAuthorization = function(callback) {
    var authCallback = goog.bind(function(authResult){
      if (authResult.error) {
        console.error(authResult.error);
      }

      if (authResult && !authResult.error && authResult.access_token) {
        this.gDriveToken = authResult.access_token;
        this.obtainUserId();
        callback();
      }
    }, this);
    gapi.auth.authorize({
          'client_id': this.appClientId,
          'scope': this.scope,
          'immediate': false
        },
        authCallback);
  };

  /**
   * Obtain the userId from the gplus api.
   */
  GDriveUrlChooser.prototype.obtainUserId = function() {
    // gapi.client.plus will be undefined until the GDrive Auth Api loads.
    if (!this.userId && gapi.client.plus) {
      var request = gapi.client.plus.people.get({
        'userId': 'me'
      });
      request.execute(goog.bind(function(data) {
        this.userId = data.id;
      }, this));
    }
  };

  /**
   * Handles the drive picker's returned information.
   *
   * @param callback method to call with propper URL.
   * @param data data received from the picker.
   */
  GDriveUrlChooser.prototype.chooserCallback = function (callback, data) {
    if(data.action === 'picked') {
      var fileId = data.docs[0].id;
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = goog.bind(function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
          var chosenUrl = xhr.responseText;
          chosenUrl = 'gdrive:///' + this.userId + chosenUrl;
          callback(chosenUrl);
        }
      }, this);

      xhr.open('POST', '../plugins-dispatcher/gdrive-start', true);
      xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
      xhr.send('userId=' + this.userId + '&' + 'fileId=' + fileId);
    }
  };

  /**
   * Invokes the save chooser.
   *
   * @param fileURL the url of the file to save.
   * @param fileName the suggested name for the file that will be created.
   * @param callback the callback function to call with the new file path as parameter.
   * @param {boolean}opt_externalAccess {true} if the fileURL can be accessed by the dropbox servers.
   */
  GDriveUrlChooser.prototype.saveFile = function (fileURL, fileName, callback, opt_externalAccess) {
    if (this.gDriveToken) {
      var view = new google.picker.DocsView(google.picker.ViewId.FOLDERS);
      view.setIncludeFolders(true);
      view.setSelectFolderEnabled(true);

      var uploadView = new google.picker.DocsUploadView();
      uploadView.setIncludeFolders(true);

      var picker = new google.picker.PickerBuilder().
          addView(view).
        //addView(uploadView).
          setOrigin(window.location.protocol + '//' + window.location.host).
          setOAuthToken(this.gDriveToken).
        //setDeveloperKey(gDriveUrlChooser.developerKey).
          setCallback(goog.bind(this.pickerCallback, this, fileURL, fileName, callback)).
          build();
      picker.setVisible(true);
    } else {
      this.obtainAuthorization(goog.bind(this.saveFile, this, fileURL, fileName, callback, opt_externalAccess));
    }
  };


  /**
   * Picker callback method.
   *
   * @param fileURL the file url.
   * @param fileName the file name.
   * @param callback the callback method.
   * @param data the response from the picker.
   */
  GDriveUrlChooser.prototype.pickerCallback = function(fileURL, fileName, callback, data) {
    if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
      var doc = data[google.picker.Response.DOCUMENTS][0];
      var folderId = doc[google.picker.Document.ID];
      this.saveToDrive(fileURL, fileName, folderId, callback);
    }
  };

  /**
   * Saves a file to the user's drive account.
   *
   * @param fileURL the file url.
   * @param fileName the file name.
   * @param parentId the id of the folder in which to save the file.
   * @param callback method to call with the save result as parameter.
   */
  GDriveUrlChooser.prototype.saveToDrive = function (fileURL, fileName, parentId, callback) {
    var boundary = '-------314159265358979323846';
    var delimiter = "\r\n--" + boundary + "\r\n";
    var close_delim = "\r\n--" + boundary + "--";
    var contentType = 'text/xml';
    var metadata = {
      'title': fileName,
      'mimeType': contentType,
      'parents':[{'id': parentId}]
    };
    var fileContent = '';
    var requestContent = new goog.net.XhrIo();
    goog.events.listenOnce(requestContent, goog.net.EventType.COMPLETE, goog.bind(function () {
      var responseText = requestContent.getResponse();
      fileContent = '' + sync.util.encodeB64(responseText);
      // save to gdrive
      var multipartRequestBody =
          delimiter +
          'Content-Type: application/json\r\n\r\n' +
          JSON.stringify(metadata) + '\r\n\r\n' +
          delimiter +
          'Content-Type: ' + contentType + '\r\n' +
          'Content-Transfer-Encoding: base64\r\n' +
          '\r\n' +
          fileContent +
          close_delim;

      var request = gapi.client.request({
        'path': '/upload/drive/v2/files',
        'method': 'POST',
        'params': {'uploadType': 'multipart'},
        'headers': {
          'Content-Type': 'multipart/mixed; boundary="' + boundary + '"'
        },
        'body': multipartRequestBody
      });
      request.execute(goog.bind(function (file) {
        var fileId = file.id;

        // notify the CreateDocumentAction not to open the file.
        callback(null, sync.api.UrlChooser.SaveResult.SAVED_UNAVAILABLE_URL);

        // we open the new document directly.
        this.openFileById(fileId);
      }, this));
    }, this));
    // get the content of the file.
    requestContent.send(fileURL, 'GET');
  };

  /**
   * Opens the file with the given id.
   *
   * @param fileId the id of the file to open.
   */
  GDriveUrlChooser.prototype.openFileById = function(fileId) {
    var state = JSON.stringify({
      ids : [fileId],
      action: "open",
      userId: this.userId
    });
    var gDriveEntryPoint = '../plugins-dispatcher/gdrive-start';
    window.location.href = gDriveEntryPoint + '?state=' + encodeURIComponent(state);
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
  GDriveUrlChooser.prototype.supports = function (type) {
    return true;
  };


  // callback when the auth library is loaded.
  GDriveUrlChooser.prototype.loadGDriveAuthApi = function () {
    gapi.load('auth', {'callback': goog.bind(this.authLoadedCallback, this)});
    // Load drive and picker libraries.
    gapi.client.load('drive', 'v2');
    gapi.load('picker');
    gapi.client.load('plus', 'v1');
  };

  // The application client ID.
  GDriveUrlChooser.prototype.appClientId = null;

  // Whether the auth library was loaded.
  GDriveUrlChooser.prototype.authLoaded = false;

  /**
   * Callback when the auth library is loaded.
   */
  GDriveUrlChooser.prototype.authLoadedCallback = function () {
    this.authLoaded = true;
    this.gDrivecheckAuth();
  };


  /**
   * Check if current user has authorized this application.
   */
  GDriveUrlChooser.prototype.gDrivecheckAuth = function() {
    if (this.authLoaded && this.appClientId != null) {
      gapi.auth.authorize({
            'client_id': this.appClientId,
            'scope': this.scope,
            'immediate': true
          },
          goog.bind(function(a) {
            this.gDriveToken = a.access_token;
            this.obtainUserId();
          }, this)
      )
    }
  };

  var url = sync.util.getURLParameter('url');
  if(!url || (url.indexOf('gdrive') === 0)) {
    // The google drive url chooser.
    var gDriveUrlChooser = new GDriveUrlChooser();
    loadGDriveAuthApi = goog.bind(gDriveUrlChooser.loadGDriveAuthApi, gDriveUrlChooser);

    /**
     * Load the google client library.
     *
     * Represents the entry point in the integration.
     */
    sync.util.loadJsFile("https://apis.google.com/js/client.js?onload=loadGDriveAuthApi");

    if (!url) {
      var createAction = new sync.api.CreateDocumentAction(gDriveUrlChooser);
      var openAction = new sync.actions.OpenAction(gDriveUrlChooser);

      // set custom icons for the open/create actions.
      var iconUrl = '../plugin-resources/gdrive/Drive70' + (sync.util.getHdpiFactor() > 1 ? '@2x' : '') + '.png';
      createAction.setLargeIcon(iconUrl);
      openAction.setLargeIcon(iconUrl);

      // set tooltip messages.
      createAction.setDescription('Create a new template in your Google Drive');
      openAction.setDescription('Open a document from your Google Drive');

      // set the actions ids
      createAction.setActionId('gdrive-create-action');
      openAction.setActionId('gdrive-open-action');

      // set the action names.
      createAction.setActionName(tabName);
      openAction.setActionName(tabName);

      // override the perform action method to open a file from drive.
      openAction.actionPerformed = goog.bind(function () {
        this.urlChooser.chooseUrlForRedirect(new sync.api.UrlChooser.Context(sync.api.UrlChooser.Type.GENERIC),
            this.openFile,
            sync.api.UrlChooser.Purpose);
      }, openAction);

      // register open and create actions to the workspace actions manager.
      var actionsManager = workspace.getActionsManager();
      actionsManager.registerCreateAction(createAction);
      actionsManager.registerOpenAction(openAction);

    } else if (url.indexOf('gdrive') === 0) {
      workspace.setUrlChooser(gDriveUrlChooser);
    }

    /**
     * Tracks the status of the editor.
     * @type {boolean}
     */
    var editorLoaded = false;
    goog.events.listenOnce(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(e) {
      var editor = e.editor;
      goog.events.listenOnce(editor, sync.api.Editor.EventTypes.CUSTOM_MESSAGE_RECEIVED, function(e) {
        /*
        * We only auto-login once before Editor load.
        * CUSTOM_MESSAGE_RECEIVED can be thrown during editing and we don't want to handle that possibility.
        * */
        if ('Authentication Required' === e.message.title && !editorLoaded) {
          e.preventDefault();

          var state = JSON.stringify({
            ids : [editor.getUrl()],
            action: "load",
            userId: this.userId
          });

          // We navigate to `gdrive-start` which will walk the user through an OAuth flow and redirect back here.
          window.location.href = '../plugins-dispatcher/gdrive-start?state=' + encodeURIComponent(state);
        }
      });
    });
    goog.events.listenOnce(workspace, sync.api.Workspace.EventType.EDITOR_LOADED, function(e) {
      editorLoaded = true;
    });

    // Focus the Google Drive tab if the 'gdrive-focus' parameter is provided.
    if (sync.util.getURLParameter('gdrive-focus') === 'true') {
      goog.events.listenOnce(workspace, sync.api.Workspace.EventType.BEFORE_DASHBOARD_LOADED, function(e) {
        e.options.selectedTabName = tabName;
      });
    }
  }
})();