package za.co.house4hack.shac;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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
      pd.show();

      wv = (WebView) findViewById(R.id.web_webview);
      WebSettings settings = wv.getSettings();
      settings.setJavaScriptEnabled(true);
      settings.setTextSize(WebSettings.TextSize.NORMAL);
      if (extras.containsKey("text_size")) {
         settings.setTextSize((WebSettings.TextSize) extras.get("text_size"));
      }
      wv.addJavascriptInterface(new MyJavaScriptInterface(this), "HtmlViewer");
      
      wv.setWebViewClient(new WebViewClient() {
         @Override
         public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://localhost")) {
               Uri data = Uri.parse(url);
               String token = data.getQueryParameter("code");

               if (token != null) {
                  String postHtml = 
                           "<html><body>Authenticating..." +
                           "<form action='https://accounts.google.com/o/oauth2/token'" +
                           " method='POST'>" + 
                           "<input type='hidden' name='code' value='{code}'/>" + 
                           "<input type='hidden' name='client_id' value='231571905235.apps.googleusercontent.com'/>" + 
                           "<input type='hidden' name='client_secret' value='_TQPhcCcw7eAra4PUaY74m_W'/>" + 
                           "<input type='hidden' name='grant_type' value='authorization_code'/> " + 
                           "<input type='hidden' name='redirect_uri' value='http://localhost:4567'/>" + 
                           "</form><script type='text/javascript'>document.forms[0].submit();</script></body></html>";
                  postHtml = postHtml.replace("{code}", token);
                  wv.loadData(postHtml, "text/html", "utf-8");
                  wv.setVisibility(View.INVISIBLE); // so that access token doesn't display
                  redirectsLeft = 2;
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
         public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (redirectsLeft == 0) {
               wv.loadUrl("javascript:window.HtmlViewer.showHTML(document.body.innerText);");
            }
            pd.dismiss();
            redirectsLeft--;
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
   
   class MyJavaScriptInterface {

      private Context ctx;

      MyJavaScriptInterface(Context ctx) {
          this.ctx = ctx;
      }

      public void showHTML(String html) {
         try {
            JSONObject json = new JSONObject(html);
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
            new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage(e.getMessage())
            .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();
         }
      }

  }   
}
