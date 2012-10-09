package za.co.house4hack.shac;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends Activity {
   ProgressDialog pd;
   protected static final String LOGIN_RESULT = "Login";
   private static final int AUTH_REQUEST_CODE = 0;
   private SharedPreferences preferences;
   Menu menu = null;

   // private JREngageDelegate mEngageDelegate = ...;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      pd = new ProgressDialog(this);
      preferences = PreferenceManager.getDefaultSharedPreferences(this);

      Intent intent = getIntent();
      if (intent.getAction().equals(getString(R.string.openaction))) {
         String access = intent.getStringExtra("access");
         if (access.equalsIgnoreCase("door")) {
            openDoor(null);
         } else if (access.equalsIgnoreCase("gate")) {
            openGate(null);
         }

      }

   }

   private void opentaskExecute(String url) {
      AsyncTask<String, Void, String> opentask = new AsyncTask<String, Void, String>() {

         @Override
         protected String doInBackground(String... params) {
            String s = "";
            // Log.d("SHAC", "Session cookie is:" +
            // getSessionCookie().length());
            try {
               if (getSessionCookie().length() == 0 && !isInternal()) {
                  s = LOGIN_RESULT;
               } else {
                  String dest = params[0];
                  s = getData(dest);
               }
            } catch (IOException e) {
               s = getString(R.string.openfail_message) + ":" + e.getMessage();
               Log.e("SHAC", "Error opening", e);
            }
            return s;
         }

         @Override
         protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pd.dismiss();
            if (result == LOGIN_RESULT) {
               toastMessage(R.string.pleaseloginfirst);
            } else {
               toastMessage(result);
            }
         }

      };
      opentask.execute(url);

   }

   public void openDoor(View v) {
      Log.d("SHAC", "Door");

      pd.setMessage(getResources().getString(R.string.opendoor_message));
      pd.show();
      if (isInternal()) {
         opentaskExecute(getShacIUrl() + "/door");
      } else {
         opentaskExecute(getShacEUrl() + "/init/android/door");
      }
   }

   public void openGate(View v) {
      Log.d("SHAC", "Gate");
      pd.setMessage(getResources().getString(R.string.opengate_message));
      pd.show();
      if (isInternal()) {
         opentaskExecute(getShacIUrl() + "/gate");
      } else {
         opentaskExecute(getShacEUrl() + "/init/android/gate");
      }
   }

   @Override
   public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);

      // Checks the orientation of the screen
      if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
         setContentView(R.layout.main_landscape);
         Log.d("SHAC", "Landscape");
      } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
         setContentView(R.layout.main);
         Log.d("SHAC", "Portrait");

      }
   }

   public String getData(String url) throws IOException {

      URL urlObject = new URL(url);
      HttpURLConnection http = (HttpURLConnection) urlObject.openConnection();
      http.setRequestMethod("GET");
      // http.setRequestMethod("POST");
      // http.setDoOutput(true);
      http.setReadTimeout(15000);

      http.setRequestProperty("Cookie", "token=" + getSessionCookie());
      http.connect();
      /*
       * OutputStreamWriter wr = new
       * OutputStreamWriter(http.getOutputStream()); String data =
       * URLEncoder.encode("_formname", "UTF-8") + "=" +
       * URLEncoder.encode("gate", "UTF-8"); wr.write(data); wr.flush();
       */

      StringBuffer sb = new StringBuffer();

      if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
         InputStream is = http.getInputStream();
         int c;
         while ((c = is.read()) > 0) {
            sb.append((char) c);
         }
         is.close();
      } else {

      }
      String retVal = sb.toString();

      return (retVal);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      this.menu = menu;
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);

      boolean loggedIn = getSessionCookie().trim().length() > 0;
      try {
         menu.findItem(R.id.login).setVisible(!loggedIn);
         menu.findItem(R.id.logout).setVisible(loggedIn);
      } catch (Exception e) {
      }
      
      return true;
   }

   public String getLocalIpAddress() {
      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
               InetAddress inetAddress = enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) { return inetAddress.getHostAddress().toString(); }
            }
         }
      } catch (SocketException ex) {
         Log.e("SHAC", ex.toString());
      }
      return "";
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.settings) {
         startActivity(new Intent(this, PreferencesActivity.class));
         return true;
      }
      if (item.getItemId() == R.id.login) {
         pd.setMessage(getString(R.string.busylogin));
         pd.show();
         pickAccount();
         return true;
      }
      if (item.getItemId() == R.id.logout) {
         setSessionCookie("");
         updateMenu();
         return true;
      }

      if (item.getItemId() == R.id.directions) {
         String address = Settings.getSettings(this).getAddress();
         String name = Settings.getSettings(this).getName();
         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + URLEncoder.encode(address + "(" + name + ")")));
         startActivity(i);
      }

      if (item.getItemId() == R.id.map) {
         String address = Settings.getSettings(this).getAddress();
         String name = Settings.getSettings(this).getName();
         Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + URLEncoder.encode(address + "(" + name + ")")));
         startActivity(i);
      }

      return false;
   }

   private void updateMenu() {
      menu.clear();
      onCreateOptionsMenu(menu);
   }
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == AUTH_REQUEST_CODE && resultCode == RESULT_OK) {
         updateMenu();
      }
      
      super.onActivityResult(requestCode, resultCode, data);
   }
   
   private void pickAccount() {
      final Activity context = this;
      new Thread() {
         @Override
         public void run() {
            AccountManager mAccountManager = AccountManager.get(context);
            Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            String[] names = new String[accounts.length];
            for (int i = 0; i < names.length; i++) {
               names[i] = accounts[i].name;
            }

            if (names.length > 1) {
               final String[] accountNames = names;
               
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     new AlertDialog.Builder(context)
                     .setTitle(R.string.title_select_account)
                     .setItems(accountNames, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int pos) {
                           doOAuth(context, accountNames[pos]);
                        }
                     })
                     .setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface arg0) {
                           pd.dismiss();
                        }
                     })
                     .create().show();
                  }
               });
            } else if (names.length == 0) {
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     Toast.makeText(context, R.string.err_no_accounts, Toast.LENGTH_LONG).show();
                  }
               });
            } else {
               doOAuth(context, names[0]);
            }
         }

         public void doOAuth(final Activity context, final String accountName) {
            String G_PLUS_SCOPE = 
                     "oauth2:https://www.googleapis.com/auth/plus.me ";
            String USERINFO_SCOPE =   
                     "https://www.googleapis.com/auth/userinfo.profile ";
            String USERINFO_EMAIL =   
                     "https://www.googleapis.com/auth/userinfo.email ";
            
            
            String SCOPES = G_PLUS_SCOPE + USERINFO_EMAIL;
            try {
               setSessionCookie(GoogleAuthUtil.getToken(context, accountName, SCOPES));
            } catch (GooglePlayServicesAvailabilityException playEx) {
               Dialog dialog = GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(), context, AUTH_REQUEST_CODE);
               // Use the dialog to present to the user.
            } catch (UserRecoverableAuthException recoverableException) {
               Intent recoveryIntent = recoverableException.getIntent();
               context.startActivityForResult(recoveryIntent, AUTH_REQUEST_CODE);
               // Use the intent in a custom dialog or just
               // startActivityForResult.
            } catch (final GoogleAuthException authEx) {
               // This is likely unrecoverable.
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     Toast.makeText(context, "Unrecoverable authentication exception: " + authEx.getMessage(), Toast.LENGTH_LONG).show();
                  }
               });
            } catch (final IOException ioEx) {
               runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                     Toast.makeText(context, "transient error encountered: " + ioEx.getMessage(), Toast.LENGTH_LONG).show();
                  }
               });
            }
            
            updateMenu();
            pd.dismiss();
         }
      }.start();

   }

   private void toastMessage(String message) {
      Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
   }

   private void toastMessage(int message) {
      Toast.makeText(MainActivity.this, getString(message), Toast.LENGTH_LONG).show();
   }

   public void setSessionCookie(String token) {
      Editor edit = preferences.edit();
      edit.putString("token", token);
      edit.commit();
   }

   public String getSessionCookie() {
      return (preferences.getString("token", ""));
   }

   public String getShacIUrl() {
      return (preferences.getString("IURL", "http://192.168.1.80"));
   }

   public String getShacEUrl() {
      return (preferences.getString("EURL", "http://enter.house4hack.co.za"));
   }

   public String getUrlPolicy() {
      return (preferences.getString("URLPOLICY", "3"));
   }

   private boolean isInternal() {
      String ip = getLocalIpAddress();
      if (ip.startsWith(getLocalIPStart())) {
         return true;
      } else {
         return false;
      }
   }

   private String getShacUrl() {
      int p = Integer.parseInt(getUrlPolicy());
      switch (p) {
         case 1:
            return (getShacIUrl());
         case 2:
            return (getShacEUrl());
         case 3:
            if (isInternal()) {
               return (getShacIUrl());
            } else {
               return (getShacEUrl());
            }
         default:
            break;
      }
      return "";
   }

   private String getLocalIPStart() {
      return (preferences.getString("LOCALIPSTART", "192.168.1"));
   }

}
