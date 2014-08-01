<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
    <link rel="shortcut icon" type="image/png" href="../img/Author32.png"/>

    <%@ page import="java.util.Properties" %>
    <%
        Properties props = new Properties();
        props.load(getServletConfig().getServletContext().getResourceAsStream("/WEB-INF/dbx-secrets.properties"));
        String app_key = props.getProperty("app_key");

    %>
    <script type="text/javascript" src="https://www.dropbox.com/static/api/2/dropins.js"
    id="dropboxjs" data-app-key="<%=app_key%>"></script>
</head>

<body>
    <div class="container">
        <h1>oXygen Author Webapp</h1>

        <p>You can either:</p>

        <p>Open an existing file from your Dropbox <span id="container"></span></p>
        <p>Or upload one of the oXygen templates to your Dropbox</p>

        <div class="row">
            <div class="col-md-2">
                <li>DITA topic </li>
            </div>
            <div class="col-md-2">
                <a href="../templates/dita/Topic.dita" class="dropbox-saver" data-filename="Topic.dita"></a>
            </div>
        </div>
        <div class="row">
            <div class="col-md-2">
                <li>DITA task </li>
            </div>
            <div class="col-md-2">
                <a href="../templates/dita/Task.dita" class="dropbox-saver" data-filename="Task.dita"></a>
            </div>
        </div>
    </div>

<script>
    var options = {
        // Required. Called when a user selects an item in the Chooser.
        success: function(files) {
          var link = files[0].link;
          var prefix = "https://dl.dropboxusercontent.com/1/view/";
          var codeAndPath = link.substring(prefix.length);
          var path = codeAndPath.substring(codeAndPath.indexOf('/'));
          console.log('path', path, codeAndPath, link);

          var href = "start?path=" + encodeURIComponent(path);
          console.log('href: ', href);
          window.location.href = href;
        },

        linkType: "direct"
    };

    var button = Dropbox.createChooseButton(options);
    document.getElementById("container").appendChild(button);
</script>
</body>
</html>