package za.co.house4hack.shac;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebPopup extends Activity {
   WebView wv;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Remove title bar
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      
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
      
      wv.setWebViewClient(new WebViewClient() {
         @Override
         public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http://localhost")) {
               Intent data = new Intent();
               data.setData(Uri.parse(url));
               setResult(RESULT_OK, data);
               finish();
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
}