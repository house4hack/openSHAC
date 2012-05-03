package za.co.house4hack.shac;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.http.cookie.Cookie;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;


public class MainActivity extends Activity implements JREngageDelegate {
	ProgressDialog pd;
	protected static final String LOGIN_RESULT = "Login";
	private SharedPreferences preferences;
	//private JREngageDelegate mEngageDelegate = ...;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pd = new ProgressDialog(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        if(intent.getAction().equals(getString(R.string.openaction))){
        	String access = intent.getStringExtra("access");
        	if(access.equalsIgnoreCase("door")){
        		openDoor(null);
        	}else  if(access.equalsIgnoreCase("gate")){
        		openGate(null);
        	}
        	
        }
        
    }
    
	private void opentaskExecute(String url){
	    AsyncTask<String, Void, String> opentask = new AsyncTask<String, Void, String>(){
	    	  
	    	  @Override
	  	    protected String doInBackground(String... params) {
	    		  String s="";
	    		  Log.d("SHAC","Session cookie is:"+getSessionCookie().length());
	    			try {
	    				if(getSessionCookie().length() ==0 && !isInternal()){
	    					s = LOGIN_RESULT; 	
	    				} else {
	  	  				    String dest = params[0]; 
	  	  				    s = getData(dest);
	    				}
	    			} catch (IOException e) {
	    				 s=getString(R.string.openfail_message)+":"+e.getMessage();
	    				 Log.e("SHAC","Error opening",e);
	    			}	    	
	    			return s;
	  	    }
	      @Override
	    	protected void onPostExecute(String result) {
	    		super.onPostExecute(result);
	    		pd.dismiss();
	    		if(result==LOGIN_RESULT){
	    			toastMessage(R.string.pleaseloginfirst);
	    		} else {
	    			toastMessage(result);
	    		}
	      }
	    	  
	      };
	   opentask.execute(url);
		
	}
	
    public void openDoor(View v){      	
      Log.d("SHAC","Door");	
      
	  pd.setMessage(getResources().getString(R.string.opendoor_message));
  	  pd.show();
  	  if(isInternal()){
  		 opentaskExecute(getShacIUrl()+"/door");
  	  } else {
  	     opentaskExecute(getShacEUrl()+"/init/android/door");
  	  }
    }

    
    
    
    public void openGate(View v){
    	Log.d("SHAC","Gate");
  	    pd.setMessage(getResources().getString(R.string.opengate_message));
  	    pd.show();
    	  if(isInternal()){
    	  		 opentaskExecute(getShacIUrl()+"/gate");
  	  	  } else {
    	  	     opentaskExecute(getShacEUrl()+"/init/android/gate");
   	  	  }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	setContentView(R.layout.main_landscape);
        	Log.d("SHAC","Landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        	setContentView(R.layout.main);
        	Log.d("SHAC","Portrait");

        }
    }
    
    
	public String getData(String url) throws IOException{
		
		URL urlObject = new URL(url);
        HttpURLConnection http = (HttpURLConnection)urlObject.openConnection();
        http.setRequestMethod("GET");
        //http.setRequestMethod("POST");
        //http.setDoOutput(true); 
        http.setReadTimeout(15000);

        
        http.setRequestProperty("Cookie", "session_id_init="+getSessionCookie());
        http.connect();
        /*OutputStreamWriter wr = new OutputStreamWriter(http.getOutputStream());        
        String data = URLEncoder.encode("_formname", "UTF-8") + "=" + URLEncoder.encode("gate", "UTF-8");
        wr.write(data);
        wr.flush();
       */
        
        
    	StringBuffer sb = new StringBuffer();

        if(http.getResponseCode()==HttpURLConnection.HTTP_OK){
        	InputStream is = http.getInputStream();
        	int c;
        	while((c = is.read())>0){
        		  sb.append((char)c);
        	}
	        is.close();
        } else {
                    	
        }
        String retVal = sb.toString();
		
		return(retVal);
	}
	
	
	
	   @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        super.onCreateOptionsMenu(menu);
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.menu, menu);
	        return true;
	    }

	   public String getLocalIpAddress() {
		    try {
		        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		            NetworkInterface intf = en.nextElement();
		            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
		                InetAddress inetAddress = enumIpAddr.nextElement();
		                if (!inetAddress.isLoopbackAddress()) {
		                    return inetAddress.getHostAddress().toString();
		                }
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
                janrainLogin();
				return true;
			}	        
	        return false;
	    }
	    
	    private void janrainLogin() {
            setSessionCookie("");
	    	String appId = "ggacbghpjlnhjpnfgdem"; 
        	String tokenUrl = getShacUrl()+"/init/default/user/login"; 
        	JREngage jrEngage = JREngage.initInstance(this, appId, tokenUrl, this);
        	jrEngage.showAuthenticationDialog();       	
			
		}
	    


		private void toastMessage(String message){
	    	Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
	    }
		private void toastMessage(int message){
	    	Toast.makeText(MainActivity.this, getString(message), Toast.LENGTH_LONG).show();
	    }
		public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
	    	setSessionCookie("");
	    	pd.dismiss();
	    	toastMessage(getString(R.string.openfail_message)+":"+ error.getMessage());			
			
		}

	    public void jrAuthenticationDidSucceedForUser(JRDictionary authInfo,
	                                                  String provider) { }

	    public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
	                                                 String tokenUrlPayload,
	                                                 String provider) { }

	    public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
	                                                 HttpResponseHeaders response,
	                                                 String tokenUrlPayload,
	                                                 String provider) { 
	    	Cookie[] cookies = response.getCookies();
	    	if(cookies.length > 0){
	    	     Log.d("SHAC",cookies[0].getValue());
                 setSessionCookie(cookies[0].getValue());
                 toastMessage(getString(R.string.open_success));
	    	}
	    	pd.dismiss();
	    }

	    public void jrAuthenticationDidNotComplete() {
	    	pd.dismiss();
    	     toastMessage(getString(R.string.openfail_message));
	    }
	    public void jrAuthenticationDidFailWithError(JREngageError error, String provider) { 
	    	setSessionCookie("");
	    	pd.dismiss();
	    	toastMessage(getString(R.string.openfail_message)+":"+ error.getMessage());
	    	
	    }

	    public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl,
	                                                      JREngageError error,
	                                                      String provider) {
	    	setSessionCookie("");
	    	pd.dismiss();
	    	toastMessage(getString(R.string.openfail_message)+":"+ error.getMessage());
	    	
	    }

	    public void jrSocialDidNotCompletePublishing() { }

	    public void jrSocialDidCompletePublishing() { }

	    public void jrSocialDidPublishJRActivity(JRActivityObject activity,
	                                             String provider) { }

	    public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
	                                                 JREngageError error,
	                                                 String provider) { }

		public void setSessionCookie(String sessionCookie) {
	   	     Editor edit = preferences.edit();
		     edit.putString("Session",  sessionCookie);
		     edit.commit();
		}

		public String getSessionCookie() {
			return(preferences.getString("Session", ""));
		}

		

		public String getShacIUrl() {
			return(preferences.getString("IURL", "http://192.168.1.80"));
		}

		public String getShacEUrl() {
			return(preferences.getString("EURL", "http://enter.house4hack.co.za"));
		}
		public String getUrlPolicy() {
			return(preferences.getString("URLPOLICY","3"));
		}
				
		private boolean isInternal(){
			String ip = getLocalIpAddress();
			if(ip.startsWith(getLocalIPStart())){
                   return true;}
			else {
				  return false;
			}
		}
		
	    private String getShacUrl() {
			int p = Integer.parseInt(getUrlPolicy());
			switch (p) {
			case 1:
				return(getShacIUrl());
			case 2:
				return(getShacEUrl());
			case 3:
				if(isInternal()){
					return(getShacIUrl());
				}else{
				   return(getShacEUrl());
				}
			default:
				break;
			}
			return "";
		}

		private String getLocalIPStart() {
			return(preferences.getString("LOCALIPSTART","192.168.1"));
		}	    

}