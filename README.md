oXygen WebApp storage services integration
==========================================

This project is an example integration of the oXygen WebApp with two of the most popular online file storage services: Google Drive and Dropbox.
        
License
--------

The license of this project can be found at http://www.oxygenxml.com/sdk_agreement.html.
    
The contents of this project
----------------------------

This project is structured as a multi-module Maven project with the following submodules:
           
* [ss-oxygen-sample-plugins](ss-oxygen-sample-plugins/): You can find here a the plugins used to communicate with [Google Drive](ss-oxygen-sample-plugins/gdrive-custom-protocol/) and [Dropbox](ss-oxygen-sample-plugins/dbx-custom-protocol/). Please read the corresponding documentation in order to set them up properly.
* [ss-oxygen-sample-webapp](ss-oxygen-sample-webapp): This module builds the WebApp artifact and contains additional HTML pages that allow the user to choose the files he wants to edit. It also contains several configuration files which are described in more detail in the corresponding documentation.
* **ss-bundle-frameworks:** This module contains the frameworks that will be available in the WebApp. In order to add additional frameworks, you just need to copy them in the **ss-bundle-frameworks/oxygen-frameworks/** folder.
* **ss-bundle-options:** This module contains the oXygen options.
* **ss-bundle-plugins:** Just a wrapper module used to bundle the plugins in a single .zip file.

How to build the project
------------------------

First of all, in order to communicate with Google Drive and Dropbox, you need to register an application with Google and Dropbox following their specific mechanism. You can find more details in the corresponding plugin's README file.

In order to build all the artifacts, you need to setup Maven to use the oXygen repository. More details can be found on the oXygen [website](http://oxygenxml.com/oxygen_sdk_maven.html#maven_sdk_configuration). After that, running the following command will build all artifacts.

`mvn install`

At the end you can find the resulting **.war** file in the **ss-oxygen-sample-webapp/target/** folder.

**Note:** If you want to test only one of the two integration, comment out from the [web.xml](ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml) file the two servlets and one filter that correspond to the integration that you are not intereseted in. For example, in the **Dropbox** case, the filter [Dropbox credentials manager](ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L6-20) and the two servlets [Dropbox OAuth Callback]((ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L23-36)) and [Dropbox Entry Point](ss-oxygen-sample-webapp/src/main/webapp/WEB-INF/web.xml#L38-49).

Deploying the webapp
--------------------

You can now copy the **.war** file in the **webapps** folder of your Tomcat instance. The application can now be accessed at [http://localhost:8080/oxygen-storage-services-integration/](http://localhost:8080/oxygen-storage-services-integration/).

**Note:** The version 16.0.0 of the webapp does not work with Tomcat 8.

**Note:** After any change to the webapp, please stop and start the Tomcat instance again. The oXygen WebApp does not support hot reload.
