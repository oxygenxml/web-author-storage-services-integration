package com.oxygenxml.examples.dbx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;



/**
 * Default HttpRequestor provided with the Dbx client assume it uses the SUN's
 * protocol handlers. So, we need to implement our own.
 * 
 * Copy of the {@link StandardHttpRequestor}.
 */
public class OxygenHttpRequestor extends HttpRequestor{
  
  private final Proxy proxy;

  /**
   * Creates an instance that uses a direct HTTP connection (as opposed to
   * using a proxy).
   */
  public OxygenHttpRequestor()
  {
      this(Proxy.NO_PROXY);
  }

  /**
   * Creates an instance that connects through the given proxy.
   */
  public OxygenHttpRequestor(Proxy proxy)
  {
      this.proxy = proxy;
  }

  /**
   * We pass this value to {@link HttpURLConnection#setConnectTimeout}.  You can
   * change this setting by creating a subclass and overriding
   * {@link #configureConnection}.
   */
  public static final int DefaultConnectTimeoutMillis = 35 * 1000;

  /**
   * We pass this value to {@link HttpURLConnection#setReadTimeout}.  You can
   * change this setting by creating a subclass and overriding
   * {@link #configureConnection}.
   */
  public static final int DefaultReadTimeoutMillis = 35 * 1000;

  /**
   * A thread-safe instance of {@code StandardHttpRequestor} that connects directly
   * (as opposed to using a proxy).
   */
  public static final StandardHttpRequestor Instance = new StandardHttpRequestor();

  private static Response toResponse(HttpURLConnection conn)
      throws IOException
  {
      int responseCode = conn.getResponseCode();
      InputStream bodyStream;
      if (responseCode >= 400) {
          bodyStream = conn.getErrorStream();
      } else {
          bodyStream = conn.getInputStream();
      }
      return new Response(conn.getResponseCode(), bodyStream, conn.getHeaderFields());
  }

  @Override
  public Response doGet(String url, Iterable<Header> headers) throws IOException
  {
      HttpURLConnection conn = prepRequest(url, headers);
      conn.setRequestMethod("GET");
      conn.connect();
      return toResponse(conn);
  }

  @Override
  public Uploader startPost(String url, Iterable<Header> headers) throws IOException
  {
      HttpURLConnection conn = prepRequest(url, headers);
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      return new Uploader(conn);
  }

  @Override
  public Uploader startPut(String url, Iterable<Header> headers) throws IOException
  {
    HttpURLConnection conn = prepRequest(url, headers);
      conn.setRequestMethod("PUT");
      return new Uploader(conn);
  }

  /**
   * Can be overridden to configure the underlying {@link HttpURLConnection} used to make
   * network requests.  If you override this method, you should probably call
   * {@code super.configureConnection(conn)} in your overridden method.
   */
  protected void configureConnection(HttpURLConnection conn)
      throws IOException
  {
  }

  private static class Uploader extends HttpRequestor.Uploader
  {
      private HttpURLConnection conn;

      public Uploader(HttpURLConnection conn)
          throws IOException
      {
          super(getOutputStream(conn));

          this.conn = conn;
          conn.connect();
      }

      private static OutputStream getOutputStream(HttpURLConnection conn)
          throws IOException
      {
          conn.setDoOutput(true);

           final OutputStream os = conn.getOutputStream();
          return new OutputStream() {
            
            @Override
            public void write(int b) throws IOException {
              os.write(b);
            }
            
            @Override
            public void close() throws IOException {
              os.close();
            }
            
            @Override
            public void flush() throws IOException {
              os.flush();
            }
          };
      }

      @Override
      public void abort()
      {
          if (conn == null) {
              throw new IllegalStateException("Can't abort().  Uploader already closed.");
          }
          this.conn.disconnect();
      }
      

      @Override
      public void close()
      
      {
          if (conn == null) return;
          this.conn.disconnect();
      }

      @Override
      public Response finish() throws IOException
      {
          HttpURLConnection conn = this.conn;
          if (conn == null) {
              throw new IllegalStateException("Can't finish().  Uploader already closed.");
          }
          this.conn = null;
          this.body.close();
          return toResponse(conn);
      }
  }

  @SuppressWarnings("restriction")
  public class Handler extends sun.net.www.protocol.https.Handler {
    
    @Override
    public URLConnection openConnection(URL arg0) throws IOException {
      return super.openConnection(arg0);
    }
    
    @Override
    protected URLConnection openConnection(URL arg0, Proxy arg1) throws IOException {
      return super.openConnection(arg0, arg1);
    }
  }
  
  private HttpURLConnection prepRequest(String url, Iterable<Header> headers) throws IOException
  {
      URL urlObject = new URL(url);
      HttpURLConnection conn = null;
//      conn = (HttpURLConnection) new Handler().openConnection(urlObject, this.proxy);
      conn = (HttpURLConnection) urlObject.openConnection(this.proxy);
//      SSLConfig.apply(conn);
      conn.setConnectTimeout(DefaultConnectTimeoutMillis);
      conn.setReadTimeout(DefaultReadTimeoutMillis);
      conn.setUseCaches(false);
      conn.setAllowUserInteraction(false);
      configureConnection(conn);

      for (Header header : headers) {
          conn.addRequestProperty(header.key, header.value);
      }

      return conn;
  }


}
