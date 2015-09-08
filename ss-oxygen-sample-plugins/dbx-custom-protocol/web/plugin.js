(function() {
  var getUrlParameter = function(name) {
    return (new RegExp(name + '=' + '(.+?)(&|$)').exec(location.search) ||
    [, null])[1];
  };

  var url = decodeURIComponent(getUrlParameter("url"));

  var protoPrefix = null;
  var useDbxProtocol = false;
  if(url.indexOf("dbx:///") == 0) {
    protoPrefix= "dbx:///";
    useDbxProtocol = true;
  } else if(url.indexOf("dbx://") == 0) {
    protoPrefix= "dbx://";
    useDbxProtocol = true;
  } else if(url.indexOf("dbx:/") == 0) {
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
    dbxScript.setAttribute("data-app-key", "nj59lfu1m0hx915");
    document.head.appendChild(dbxScript);


    goog.provide('sync.api.DropboxUrlChooser');

    goog.require('sync.api.UrlChooser');


    /**
     * A implementation class for URL choosers.
     *
     * @constructor
     */
    sync.api.DropboxUrlChooser = function() {
      sync.api.UrlChooser.call(this);
    };
    goog.inherits(sync.api.DropboxUrlChooser, sync.api.UrlChooser);

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
    sync.api.DropboxUrlChooser.prototype.chooseUrl = function(context, chosen, purpose) {
      var options = {
        // Required. Called when a user selects an item in the Chooser.
        success: function (files) {
          var link = files[0].link;

          var prefix = "https://dl.dropboxusercontent.com/1/view/";
          var codeAndPath = link.substring(prefix.length);
          var path = codeAndPath.substring(codeAndPath.indexOf('/'));

          var imageURL = 'dbx:///' + userID + path;
          chosen(imageURL);
        },

        linkType: "direct",
        multiselect: false,
        extensions: ['.bmp', '.cr2', '.gif', '.ico', '.ithmb', '.jpeg', '.jpg', '.nef', '.png', '.raw', '.svg', '.tif', '.tiff', '.wbmp', '.webp']
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
    sync.api.DropboxUrlChooser.prototype.saveFile = function (fileURL, fileName, callback, opt_externalAccess) {
      var options = {
        success: function (e) {
          callback(null);
        },
        // The value passed to this callback is a float
        // between 0 and 1.
        progress: function (progress) {
        },
        // Cancel is called if the user presses the Cancel button or closes the Saver.
        cancel: function () {
        },
        // Error is called in the event of an unexpected response from the server
        error: function (errorMessage) {
          // TODO: handle the case in which an error occurs
          console.log('An error occurred :' + errorMessage);
        }
      };
      // dropbox server cannot access this url so we convert it to a data: url.
      if (!opt_externalAccess) {
        var request = new goog.net.XhrIo();
        goog.events.listenOnce(request, goog.net.EventType.COMPLETE, goog.bind(function(){
          var responseText = request.getResponse();
          var encodedContent = sync.util.encodeB64(responseText);
          fileURL = 'data:text/xml;base64,' + encodedContent;
          Dropbox.save(fileURL, fileName, options);
        }, this));
        request.send(fileURL, 'GET');
      } else {
        Dropbox.save(fileURL, fileName, options);
      }
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
    sync.api.DropboxUrlChooser.prototype.supports = function(type) {
      var supports = false;
      if(type == sync.api.UrlChooser.Type.IMAGE) {
        supports = true;
      }
      return supports;
    };

    var urlChooser = new sync.api.DropboxUrlChooser();
    // if the current opened file is from dropbox use the Dropbox url chooser.
    if(useDbxProtocol) {
      workspace.setUrlChooser(urlChooser);
    }

    // register a create action for the url chooser.
    var createAction = new sync.api.CreateDocumentAction(urlChooser);
    createAction.setLargeIcon('../plugin-resources/dbx/dropbox-blue.png');
    workspace.getActionsManager().registerCreateAction(
        createAction);
  };

})();
