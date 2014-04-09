package za.co.house4hack.shac;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebPopup extends Activity {
   WebView wv;
   int redirectsLeft = -1; // how many redirects left till we can parse output
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Remove title bar
      //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      
      setContentView(R.layout.web_popup);

      Bundle extras = getIntent().getExtras();
      if (extras == null) extras = new Bundle();
      
      final ProgressDialog pd = new ProgressDialog(this);
      pd.setMessage(getString(R.string.progress_loading));

      wv = (WebView) findViewById(R.id.web_webview);
      WebSettings settings = wv.getSettings();
      settings.setJavaScriptEnabled(true);
      settings.setTextSize(WebSettings.TextSize.NORMAL);
      if (extras.containsKey("text_size")) {
         settings.setTextSize((WebSettings.TextSize) extras.get("text_size"));
      }
      
      wv.setWebViewClient(new WebViewClient() {
         @Override
         public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d("shac", "Should override: " + url);
            if (url.startsWith("http://localhost")) {
               Uri data = Uri.parse(url);
               String token = data.getQueryParameter("code");

               if (token != null) {
                  final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                  postParams.add(new BasicNameValuePair("code", token));
                  postParams.add(new BasicNameValuePair("client_id", "231571905235.apps.googleusercontent.com"));
                  postParams.add(new BasicNameValuePair("client_secret", "_TQPhcCcw7eAra4PUaY74m_W"));
                  postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
                  postParams.add(new BasicNameValuePair("redirect_uri", "http://localhost:4567"));
                  
                  new Thread() {
                     public void run() {
                        try {
                           httpPost("https://accounts.google.com/o/oauth2/token", postParams, new ResponseHandler<String>() {
                              @Override
                              public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                                 DataInputStream is = new DataInputStream(response.getEntity().getContent());
                                 String line;
                                 StringBuffer sb = new StringBuffer();
                                 while ((line = is.readLine()) != null) {
                                    sb.append(line);
                                 }
                                 is.close();
                                 
                                 String jsonString = sb.toString();
                                 Log.d("shac", "Got Auth response: " + jsonString);
                                 loadJson(jsonString);
                                 return "Done";
                              }
                           });
                        } catch (IOException e) {
                           Log.e("shac", "Error loading auth page", e);
                        }
                     };
                  }.start();
               } else {
               }

               return true;
            }
            if (url.contains("@")) {
               Intent i = new Intent(android.content.Intent.ACTION_SEND);
               i.putExtra(android.content.Intent.EXTRA_EMAIL, url);
               i.setType("text/html");
               startActivity(Intent.createChooser(i, "Send Email"));
               return true;
            }

            return super.shouldOverrideUrlLoading(view, url);
         }
         
         @Override
         public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("shac", "Loading: " + url);
            pd.show();
         }

         @Override
         public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("shac", "Page loaded: " + url);
            if (url.startsWith("https://accounts.google.com/o/oauth2/token")) {
               wv.loadUrl("javascript:window.HtmlViewer.showHTML(document.body.innerText);");
            }
            pd.dismiss();
         }

         @Override
         public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // TODO Auto-generated method stub
            super.onReceivedError(view, errorCode, description, failingUrl);
            Toast.makeText(WebPopup.this, description, Toast.LENGTH_LONG).show();
            finish();
         }

      });
      
      if (extras.containsKey("html")) {
         wv.loadDataWithBaseURL("http://staticpage", extras.getString("html"), 
                  "text/html", "utf-8", null);
      } else {
         wv.loadUrl(getIntent().getDataString());
      }
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
       if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
           wv.goBack();
           return true;
       }
       return super.onKeyDown(keyCode, event);
   }   
   
   // Do HTTP POST
   public static void httpPost(String url, List<? extends NameValuePair> data, ResponseHandler<? extends Object> response) throws IOException {
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(data);
      DefaultHttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost(url);
      post.setEntity(entity); // with list of key-value pairs
      client.execute(post, response); // implement ResponseHandler to handle response correctly.
   }

   public void loadJson(String jsonString) {
      try {
         Log.d("shac", "Parsing JSON result: " + jsonString);
         JSONObject json = new JSONObject(jsonString);
         // get the body as json and look for error or access_token
         
         if (json.has("error")) {
            throw new Exception("ERROR: " + json.getString("error"));
         }
         
         if (json.has("access_token")) {
            Intent data = new Intent();
            data.putExtra(MainActivity.EXTRA_TOKEN, json.getString("access_token"));
            setResult(RESULT_OK, data);
            finish();
            return;
         }

         throw new Exception("Failed to authenticate. Google OAuth may have changed.");
      } catch (Exception e) {
         new AlertDialog.Builder(this)
            .setTitle("ERROR")
            .setMessage(e.getMessage())
            .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
               @Override
               public void onClick(DialogInterface d, int i) {
                  d.dismiss();
               }
            })
            .setCancelable(false)
            .create()
            .show();
      }
   }
}
