package com.cyanogenmod.cmparts.activities;

import com.cyanogenmod.cmparts.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import java.io.File;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import java.util.List;


public class TrackballNotificationActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

	private static final int NOTIFICATION_ID = 400;

	public static String[] mPackage;
	public String mPackageSource;
	
	public boolean isNull(String mString) {
		if(mString == null || mString.matches("null") 
		|| mString.length() == 0
		|| mString.matches("|")
		|| mString.matches("")) {
			return true;
		} else {
			return false;
		}
	}

	public String[] getArray(String mGetFrom) {
		if(isNull(mGetFrom)) {
			String[] tempfalse = new String[20];
			return tempfalse;
		}
		String[] temp;
		temp = mGetFrom.split("\\|");
		return temp;
	}

	public String createString(String[] mArray) {
		int i;
		String temp = "";
		for(i = 0; i < mArray.length; i++) {
			if(isNull(mArray[i]))
				continue;
			temp = temp + "|" + mArray[i];
		}
		return temp;
	}

	public String[] getPackageAndColorAndBlink(String mString) {
		if(isNull(mString)) {
			return null;
		}
		String[] temp;
		temp = mString.split("=");
		return temp;
	}
	
	public String[] findPackage(String pkg) {
		String mBaseString = Settings.System.getString(getContentResolver(), Settings.System.NOTIFICATION_PACKAGE_COLORS);
		String[] mBaseArray = getArray(mBaseString);
		for(int i = 0; i < mBaseArray.length; i++) {
			if(isNull(mBaseArray[i])) {
				continue;
			}
			if(mBaseArray[i].contains(pkg)) {
				return getPackageAndColorAndBlink(mBaseArray[i]);
			}
		}
		return null;
	}

	public void updatePackage(String pkg, String color, String blink) {
		String stringtemp = Settings.System.getString(getContentResolver(), Settings.System.NOTIFICATION_PACKAGE_COLORS);
		String[] temp = getArray(stringtemp);
		int i;
		String[] temp2;
		temp2 = new String[temp.length];
		boolean found = false;
		for(i = 0; i < temp.length; i++) {
			temp2 = getPackageAndColorAndBlink(temp[i]);
			if(temp2 == null) {
				continue;
			}
			//Log.i("addPackNew", "Temp2 Pkg="+pkg+": Package="+temp2[0]+" Color="+temp2[1]+" Blink="+temp2[2]);
			if(temp2[0].matches(pkg)) {
				if(blink.matches("0")) {
					temp2[1] = color;
				} else {
					temp2[2] = blink;
				}
				found = true;
				break;
			}
		}
		if(found) {
			String tempcolor = temp2[0] +"="+temp2[1]+"="+temp2[2];
			temp[i] = tempcolor;
		} else {
			int x = 0;
			//Get the last one
			for(x = 0; x < temp.length; x++) {
				if(isNull(temp[x]))
					break;
			}
			String tempcolor;
			if(blink.matches("0")) {
				tempcolor = pkg+"="+color+"=2";
			} else {
				tempcolor = pkg+"=black="+blink;
			}
			temp[x] = tempcolor;
		}
		Settings.System.putString(getContentResolver(), Settings.System.NOTIFICATION_PACKAGE_COLORS, createString(temp));
	}
	
	public void testPackage(String pkg) {
		String[] mTestPackage = findPackage(pkg);
		if(mTestPackage == null) {
			return;
		}
		final Notification notification = new Notification();
		
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		
		int mBlinkRate = Integer.parseInt(mTestPackage[2]);
		
		notification.ledARGB = Color.parseColor(mTestPackage[1]);
        notification.ledOnMS = 500;
        notification.ledOffMS = mBlinkRate * 1000;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        nm.notify(NOTIFICATION_ID, notification);
        
        AlertDialog.Builder endFlash = new AlertDialog.Builder(this);
        endFlash.setMessage("Clear Flash")
        .setCancelable(false)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
        	  NotificationManager dialogNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        	  dialogNM.cancel(NOTIFICATION_ID);
        } });
        endFlash.show();
	}
	
	public String getPackageName(String pkg) {
        PackageManager packageManager = getPackageManager(); 
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        int size = packs.size();
        for (int i = 0; i < size; i++) {
        	PackageInfo p = packs.get(i);
        	if(p.packageName.equals(pkg))
        		return p.applicationInfo.loadLabel(packageManager).toString();
        }
        return null;
	}
	
	public String[] getPackageList() {
		PackageManager packageManager = getPackageManager(); 
        List<PackageInfo> packs = packageManager.getInstalledPackages(0);
        int size = packs.size();
        String[] list = new String[30];
        int x = 0;
        for (int i = 0; i < size; i++) {
        	PackageInfo p = packs.get(i);
        	try {
        		Context appContext = createPackageContext(p.packageName, 0);
        		boolean exists = (new File(appContext.getFilesDir(), "trackball_lights")).exists(); 
			if(exists) {
        			list[x] = p.packageName;
        			x++;
        		}
        	} catch (Exception e) {
        		Log.d("GetPackageList", e.toString());
        	}
        }
        return list;
	}

	private PreferenceScreen createPreferenceScreen() {
		//The root of our system
		String[] packageList = getPackageList();
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
        for(int i = 0; i < packageList.length; i++) {
        	if(isNull(packageList[i]))
        		continue;
        	
        	String packageName = getPackageName(packageList[i]);
        	PreferenceScreen appName = getPreferenceManager().createPreferenceScreen(this);
        	appName.setKey(packageList[i] + "_screen");
        	appName.setTitle(packageName);
        	root.addPreference(appName);
        	
        	ListPreference colorList = new ListPreference(this);
        	colorList.setKey(packageList[i]);
        	colorList.setTitle(R.string.color_trackball_flash);
        	colorList.setSummary("Color to flash");
        	colorList.setDialogTitle(R.string.dialog_color_trackball);
        	colorList.setEntries(R.array.entries_trackball_colors);
        	colorList.setEntryValues(R.array.pref_trackball_colors_values);
        	colorList.setOnPreferenceChangeListener(this);
        	appName.addPreference(colorList);
        	
        	ListPreference blinkList = new ListPreference(this);
        	blinkList.setKey(packageList[i]);
        	blinkList.setTitle(R.string.color_trackball_blink);
        	blinkList.setSummary("Blink Rate");
        	blinkList.setDialogTitle(R.string.dialog_blink_trackball);
        	blinkList.setEntries(R.array.pref_trackball_blink_rate_entries);
        	blinkList.setEntryValues(R.array.pref_trackball_blink_rate_values);
        	blinkList.setOnPreferenceChangeListener(this);
        	appName.addPreference(blinkList);
        	
        	Preference testColor = new Preference(this);
        	testColor.setKey(packageList[i]);
        	testColor.setSummary("Test the flash");
        	testColor.setTitle(R.string.color_trackball_test);
        	appName.addPreference(testColor);
        }
        
        /*Advanced*/
        PreferenceScreen advancedScreen = getPreferenceManager().createPreferenceScreen(this);
        advancedScreen.setKey("advanced_screen");
        advancedScreen.setTitle("Advanced");
    	root.addPreference(advancedScreen);
    	
    	CheckBoxPreference alwaysPulse = new CheckBoxPreference(this);
    	alwaysPulse.setKey("always_pulse");
    	alwaysPulse.setSummary(R.string.pref_trackball_screen_summary);
    	alwaysPulse.setTitle(R.string.pref_trackball_screen_title);
    	advancedScreen.addPreference(alwaysPulse);
    	
    	//TRACKBALL_NOTIFICATION_SUCESSION
    	CheckBoxPreference sucessionPulse = new CheckBoxPreference(this);
    	sucessionPulse.setKey("pulse_sucession");
    	sucessionPulse.setSummary(R.string.pref_trackball_sucess_summary);
    	sucessionPulse.setTitle(R.string.pref_trackball_sucess_title);
    	advancedScreen.addPreference(sucessionPulse);
    	
    	Preference resetColors = new Preference(this);
    	resetColors.setKey("reset_notifications");
    	resetColors.setSummary("Reset all colors and apps.");
    	resetColors.setTitle("Reset");
    	advancedScreen.addPreference(resetColors);
    	
        return root;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        setTitle(R.string.trackball_notifications_title);
        setPreferenceScreen(createPreferenceScreen());
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String value = objValue.toString();
        if(preference.getSummary() != null) {
        	if(preference.getSummary().toString().contains("Blink")) {
        		updatePackage(preference.getKey().toString(), "", value);
        	} else {
        		updatePackage(preference.getKey().toString(), value, "0");
        	}
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference.getKey().toString().equals("reset_notifications")) {
        	Settings.System.putString(getContentResolver(), Settings.System.NOTIFICATION_PACKAGE_COLORS, "");
        	Toast.makeText(this, "Reset all colors", Toast.LENGTH_LONG).show();
        } else if (preference.getKey().toString().equals("always_pulse")) {
        	CheckBoxPreference keyPref = (CheckBoxPreference) preference;
        	value = keyPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.TRACKBALL_SCREEN_ON, value ? 1 : 0);
        } else if (preference.getKey().toString().equals("pulse_sucession")) {
        		CheckBoxPreference keyPref = (CheckBoxPreference) preference;
            	value = keyPref.isChecked();
                Settings.System.putInt(getContentResolver(),
                        Settings.System.TRACKBALL_NOTIFICATION_SUCESSION, value ? 1 : 0);
        } else if(preference.getSummary() != null) {
        	if(preference.getSummary().toString().equals("Test the flash")) {
        		testPackage(preference.getKey().toString());
        	}
        }
        return false;
    }
}
