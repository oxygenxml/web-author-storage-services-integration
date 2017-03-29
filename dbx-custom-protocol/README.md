Dropbox integration plugin
==========================
This plugin installs a protocol handler for a protocol called **dbx** in
**oXygen** used to access the Dropbox files of the user. That enables **oXygen** to open resources with a URL of the form:
`dbx://${userid}/path/to/file` where `${userid}` is the id of the user
in whose Dropbox the file is located. 


This plugin provides a standard Java URL stream handler class (URLStreamHandlerPluginExtension) and uses the [Dropbox Core API](https://www.dropbox.com/developers/core) to make connections to these URLs.


This plugin contains also the classes used to manage the OAuth flow and to store the user's token and which have to be mapped in the
[web.xml](../../ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L6-49) configuration file.


A special HTML home page which uses the [Dropbox dropins API](https://www.dropbox.com/developers/dropins) is used to allow the user choose the file which he wants to open. This file is located in the webapp module at [src/main/webapp/dbx/index.jsp](../../ss-oxygen-sample-webapp/src/main/webapp/dbx/index.jsp).
    


Implementation details
----------------------

When the user saves the file, the existing version is overwritten even if it was modified since the user
last opened that file. However, all the versions of the file remain in Dropbox's history.


The server used to serve this application must have HTTPS enabled, since Dropbox requires the redirect URL
to be an HTTPS URL.


Classes summary
---------------

- **AuthCode**: The servlet responsible for receiving the OAuth token from Dropbox. This servlet should be linked in the [web.xml](../../ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L23-36) configuration file to handle the **redirect URL** described in the [Credentials Setup](#credentials-setup) section.
- **Credentials**: The class used to load the Dropbox application secrets: application key,  application secret and the redirect URL from a configuration file.
- **CustomProtocolPlugin, CustomProtocolURLHandlerExtension**: The plugin classes.
- **DbxUrlStreamHandler, DbxUrlConnection**: used to map `dbx://` URLs to Dropbox files, make the actual communication with Dropbox and implements the URLStreamHandler.
- **DbxManagerFilter**: The user manager which keeps track of the user on behalf of which the current request is executing.
- **EntryPoint**: The entry point used for opening a file.
- **TokenDb**: A simple db implemented on top of a .properties file.
- **UserData**: Details about an user, including its id and display name.

Credentials Setup
--------------------
In order to access Dropbox files on behalf of the user, you should create a
Dropbox app in the [Dropbox developer console](https://www.dropbox.com/developers/apps/).


In the app creation wizzard you should choose:

- **What type of app do you want to create?** - *Dropbox API app*, in order to be able to access the Dropbox REST API
- **What type of data does your app need to store on Dropbox?** - *The app should be able to access both files and datastores*. In fact only files for now.
- **Can your app be limited to its own folder?** - *No. My app needs access to files already on Dropbox.* The application needs to open any XML document in user's Dropbox.
- **What type of files does your app need access to?** - *All file types*. We should access files with extensions that are not understood by Dropbox, such as **.ditamap**. 

Then, in the next page you should fill in the following fields:

- **Redirect URL**: This is the URL where Dropbox sends the OAuth token and should be the URL handled by the **AuthCode** servlet as detailed above. Depending on your configuration it might be `http://localhost:8080/storage-services-oxy-integration/dbx/oauth_callback`.
- **Drop-ins domains** - *localhost*

The secrets of the app as provided in the app settings page should be provided in the
**ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/dbx-secrets.properties** file with the following format:

```
    app_key=$APP_KEY$
    app_secret=$APP_SECRET$
    redirect_uri=$REDIRECT_URL$
```
The `$REDIRECT_URL$` is the one filled in the settings page as detailed above.

See It in Action
----------------

In order to use the application to edit files in your **Dropbox** account, you can go to [http://127.0.0.1:8080/storage-services-oxy-integration/dbx](http://127.0.0.1:8080/storage-services-oxy-integration/dbx) and follow the instructions there.

