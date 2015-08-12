var syncGDriveOnGoogleDrivePluginApiLoaded = (function () {

  goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(e) {
    var url = e.options.url;

    // If this is a google drive url, load the google drive picker
    if (url.indexOf('gdrive') == 0) {
      getClientId(function () {
        // Got the client_id we can make google api requests
        sync.util.loadJsFile("https://apis.google.com/js/api.js?onload=syncGDriveOnGoogleDrivePluginApiLoaded");
      });
    }
  });

  /**
   * Gets the google api client_id
   * @param callback
   */
  function getClientId(callback) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4 && xhr.status == 200) {
        clientId = xhr.responseText;
        callback();
      }
    };
    xhr.open('PUT', '../gdrive/start', true);
    xhr.send('');
  }

  // The Client ID obtained from the Google Developers Console.
  var clientId = '';

  // Scope to use to access user's photos.
  var scope = ['https://www.googleapis.com/auth/drive.readonly',
    'profile'];

  var pickerApiLoaded = false;
  var oauthToken;

  /**
   * Create and render a Picker object for picking user Photos
   * @param {function} onResult The result callback
   */
  function createPicker(onResult) {
    if (pickerApiLoaded && oauthToken) {
      var picker = new google.picker.PickerBuilder()
          .addView(google.picker.ViewId.FOLDERS)
          .setOAuthToken(oauthToken)
          .setCallback(onResult)
          .build();
      picker.setVisible(true);
    }
  }

  // Class representing the google drive picker
  sync.api.DriveUrlChooser = function () {
    sync.api.UrlChooser.call(this);
  };
  goog.inherits(sync.api.DriveUrlChooser, sync.api.UrlChooser);

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
  sync.api.DriveUrlChooser.prototype.chooseUrl = function (context, chosen, purpose) {
    createPicker(function (data) {
      if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
        var doc = data[google.picker.Response.DOCUMENTS][0];

        var urlParams = sync.util.getApiParams();
        var urlPathPart = urlParams.url.substring(urlParams.url.indexOf('///') + 3);

        var userId = urlPathPart.substring(0, urlPathPart.indexOf('/'));
        var fileId = doc[google.picker.Document.ID];

        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function () {
          if (xhr.readyState == 4 && xhr.status == 200) {
            var chosenUrl = xhr.responseText;

            var initialUrl = urlPathPart.substring(urlPathPart.indexOf('/'));

            chosenUrl = sync.util.relativizeValueToUrl(chosenUrl, initialUrl);
            chosen(chosenUrl);
          }
        };
        xhr.open('POST', '../gdrive/start', true);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        xhr.send('userId=' + userId + '&' + 'fileId=' + fileId);
      }
    });
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
  sync.api.DriveUrlChooser.prototype.supports = function(type) {
    var supports = false;
    if(type == sync.api.UrlChooser.Type.IMAGE) {
      supports = true;
    }
    return supports;
  };

  return function syncGDriveOnGoogleDrivePluginApiLoaded () {
    gapi.load('auth', {'callback': function () {
      window.gapi.auth.authorize({
        'client_id': clientId,
        'scope': scope,
        'immediate': true
      }, function handleAuthResult(authResult) {
        if (authResult && !authResult.error) {
          oauthToken = authResult.access_token;

          workspace.setUrlChooser(new sync.api.DriveUrlChooser());
        }
      });
    }});

    gapi.load('picker', {'callback': function onPickerApiLoaded() {
      pickerApiLoaded = true;
    }});
  }
})();