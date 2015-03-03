(function() {
  var getUrlParameter = function(name) {
    return (new RegExp(name + '=' + '(.+?)(&|$)').exec(location.search) ||
    [, null])[1];
  };
  var url = decodeURIComponent(getUrlParameter("url"));

  //console.log('full url :', url);
  var protoPrefix = "dbx:///";
  if (url.indexOf(protoPrefix) == 0) {
    var partialPath = url.substring(protoPrefix.length);
    var userID = partialPath.substring(0, partialPath.indexOf('/'));
    console.log('user id :', userID);
    var path = partialPath.substring(userID.length);
    //console.log('determined path :', path);

    var dbxScript = document.createElement('script');
    dbxScript.setAttribute("src", "https://www.dropbox.com/static/api/2/dropins.js");
    dbxScript.setAttribute("id", "dropboxjs");
    dbxScript.setAttribute("data-app-key", "nj59lfu1m0hx915");
    document.head.appendChild(dbxScript);


    goog.provide('sync.api.DropboxUrlChooser');
    goog.provide('sync.api.DriveUrlChooser');

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
        success: function(files) {
          var link = files[0].link;
          console.log('decoded link :', decodeURIComponent(link));

          var prefix = "https://dl.dropboxusercontent.com/1/view/";
          var codeAndPath = link.substring(prefix.length);
          var path = codeAndPath.substring(codeAndPath.indexOf('/'));
          console.log('determined path :', path);

          var imageURL = 'dbx:///' + userID + path;
          console.log('image url :', imageURL);
          chosen(imageURL);
        },

        linkType: "direct",
        multiselect: false
      };
      Dropbox.choose(options);
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

    workspace.setUrlChooser(new sync.api.DropboxUrlChooser());
    console.log('set the dropbox url chooser');
  };

})();