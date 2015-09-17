goog.provide('GDriveUrlChooser');

goog.require('sync.api.UrlChooser');


/**
 * A implementation class for URL choosers for google drive
 *
 * @constructor
 */
GDriveUrlChooser = function () {
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
GDriveUrlChooser.prototype.chooseUrl = function (context, chosen, purpose) {
  if (this.gDriveToken) {
    var viewID = google.picker.ViewId.DOCS;
    if(context.getType() == sync.api.UrlChooser.Type.IMAGE) {
      viewID = google.picker.ViewId.DOCS_IMAGES;
    } else if(context.getType() == sync.api.UrlChooser.Type.GENERIC) {
      viewID = google.picker.ViewId.DOCUMENTS;
    }

    var view = new google.picker.DocsView(viewID);
    //view.setIncludeFolders(true);
    //view.setSelectFolderEnabled(false);

    var picker = new google.picker.PickerBuilder()
        .addView(view)
        .setOrigin(window.location.protocol + '//' + window.location.host)
        .setOAuthToken(this.gDriveToken)
        .setCallback(goog.bind(this.chooserCallback, this, chosen))
        .build();
    picker.setVisible(true);
  } else {
    this.obtainAuthorization(goog.bind(this.chooseUrl, this, context, chosen, purpose));
  }
};

/**
 * Obtains the app client id from the server.
 */
GDriveUrlChooser.prototype.obtainAppClienId= function() {
  var requestContent = new goog.net.XhrIo();
  goog.events.listenOnce(requestContent, goog.net.EventType.COMPLETE, goog.bind(function () {
    this.appClientId = requestContent.getResponse();
  }, this));
  requestContent.send('../gdrive/start', 'PUT');
};


/**
 * Obtains authorization from the user to acces it's
 * profile and google drive.
 *
 * @param callback the callback to be called when authorization finished.
 */
GDriveUrlChooser.prototype.obtainAuthorization = function(callback) {
  var authCallback = goog.bind(function(authResult){
    if (authResult && !authResult.error) {
      this.gDriveToken = authResult.access_token;
      gapi.client.load('plus', 'v1', goog.bind(this.obtainUserId, this));
      callback();
    } else {
      gapi.auth.authorize(
          {
            'client_id': this.appClientId,
            'scope': this.scope,
            'immediate': false
          },
          authCallback);
    }
  }, this);

  gapi.auth.authorize(
      {
        'client_id': this.appClientId,
        'scope': this.scope,
        'immediate': true
      },
      authCallback);
};

/**
 * Obtain the userId from the gplus api.
 */
GDriveUrlChooser.prototype.obtainUserId = function() {
  var request = gapi.client.plus.people.get({
    'userId': 'me'
  });
  request.execute(goog.bind(function(data) {
    this.userId = data.id;
  }, this));
};

/**
 * Handles the drive picker's returned information.
 *
 * @param callback method to call with propper information.
 * @param data data received from the picker.
 */
GDriveUrlChooser.prototype.chooserCallback = function (callback, data) {
  if(data.action == 'picked') {
    var fileId = data.docs[0].id;
    var fileUrl = this.computeFileUrl(fileId);
    callback(fileUrl);
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
  if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
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
  const boundary = '-------314159265358979323846';
  const delimiter = "\r\n--" + boundary + "\r\n";
  const close_delim = "\r\n--" + boundary + "--";
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

      var fileUrl = this.computeFileUrl(fileId);
      callback(fileUrl, sync.api.UrlChooser.SaveResult.SAVED_AVAILABLE_URL);
    }, this));
  }, this));
  // get the content of the file.
  requestContent.send(fileURL, 'GET');
};

/**
 * Computes the url at which the file can be opened from the file id.
 *
 * @param fileId the file id.
 *
 * @return {string} the file's url.
 */
GDriveUrlChooser.prototype.computeFileUrl = function(fileId) {
  var state = JSON.stringify({
    ids : [fileId],
    action: "open",
    userId: this.userId
  });
  var gDriveEntryPoint = '../gdrive/start';
  var fileUrl = gDriveEntryPoint + '?state=' + encodeURIComponent(state);

  return fileUrl;
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


/**
 * Load the google client library.
 *
 * Represents the entry point in the integration.
 */
sync.util.loadJsFile("https://apis.google.com/js/client.js?onload=loadGDriveAuthApi");

/**
 * The google drive url chooser.
 * @type {GDriveUrlChooser}
 */
var gDriveUrlChooser = null;

function loadGDriveAuthApi() {
  gapi.load('auth', {'callback': gDrivecheckAuth});
}

/**
 * Check if current user has authorized this application.
 */
function gDrivecheckAuth() {
  // Load drive and picker libraries.
  gapi.client.load('drive', 'v2');
  gapi.load('picker');
}

// register chooser to dashboard.
(function () {
  gDriveUrlChooser = new GDriveUrlChooser();

  var url = sync.util.getURLParameter('url');

  if (!url) {

    /** Function that opens a file from the user's drive. */
    var openFileDromDrive = function(fileUrl) {
      document.location.href = fileUrl;
    };

    var createAction = new sync.api.CreateDocumentAction(gDriveUrlChooser);
    var openAction = new sync.actions.OpenAction(gDriveUrlChooser);

    // set custom icons for the open/create actions.
    createAction.setLargeIcon('../plugin-resources/gdrive/gdrive.png');
    openAction.setLargeIcon('../plugin-resources/gdrive/gdrive.png');

    // set tooltip messages.
    createAction.setDescription('Create a new template in your Google Drive');
    openAction.setDescription('Open a document from your Google Drive');

    // override the open file methods to open a file from drive.
    createAction.openFile = openFileDromDrive;
    openAction.openFile = openFileDromDrive;

    // register open and create actions to the workspace actions manager.
    var actionsManager = workspace.getActionsManager();
    actionsManager.registerCreateAction(createAction);
    actionsManager.registerOpenAction(openAction);

  } else if (url.indexOf('gdrive') == 0 ) {
    workspace.setUrlChooser(gDriveUrlChooser);
  }

})();