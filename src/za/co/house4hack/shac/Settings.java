package za.co.house4hack.shac;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Manage persistent storage using preferences
 * 
 * @author toby
 * 
 */
public class Settings {

   private SharedPreferences pref;
   private static HashMap settingsCache = new HashMap();

   private Settings(SharedPreferences preferences) {
      pref = preferences;
   }

   /**
    * Factory method to cache instances of settings class, since it's called a
    * lot.
    * 
    * @param preferences
    * @return
    */
   public static Settings getSettings(Context context) {
      if (settingsCache.get(context) == null) {
         SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
         settingsCache.put(context, new Settings(preferences));
      }

      return (Settings) settingsCache.get(context);
   }

   public String getAddress() {
      return pref.getString("ADDRESS", "4 Burger Ave, Centurion, Pretoria, South Africa");      
   }
   
   public String getName() {
      return pref.getString("NAME", "House4Hack");      
   }
}
