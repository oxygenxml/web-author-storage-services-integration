oXygen WebApp sample plugins
============================

General structure
-----------------

This project includes two [URLStreamHandlerPluginExtension](http://www.oxygenxml.com/doc/ug-editor/concepts/custom-protocol-plugin.html) oXygen plugins used to open and save files located in [Google Drive](gdrive-custom-protocol/) and [Dropbox](dbx-custom-protocol/). 

The general strategy used to communicate with such file storage services is to implement a handler to be used by oXygen every time it wants to retrieve and save a file specified by an URL. In such a handler, there is no information about the user trying to access the files, so we encoded the id of the user at the beginning of the url. For example, an URL pointing to a file on Dropbox has the form: `dbx://user_id/path/to/file.xml`.

Now, another kind of problem occurs: an user may pass to oXygen an URL containing the ID of another user, causing the so-called [Confused Deputy Problem](http://en.wikipedia.org/wiki/Confused_deputy_problem). In order to avoid this problem, we keep track of   the current user using a cookie and we always compare the current user with the user which is the owner of the requested file. 

Besides the oXygen extension code, the plugins need to implement a user tracking system with a servlet Filter and several OAuth handling servlets. These classes need to be mapped in the **web.xml** file. It contains two configuration snippets for [Google Drive](../ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L54-98) and for [Dropbox](../ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L6-49).
