package android.pp;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
	
	//ponie¿ej statyczne metody, wiec mozna pobrac konkretne ustawienia
	//bez tworzenia obiektu Prefs
	public static boolean getMessageSoundPref(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dzwiek", false);
	}
	
	public static boolean getMessageVibrationPref(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wibracja", true);
	}
	
	public static String getLoginStatusPref(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString("status_po_zalogowaniu", "Dostepny");
	}
	
	public static String getLoginDescriptionPref(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString("opis_po_zalogowaniu", "http://code.google.com/p/gandu/");
	}
	
	public static Boolean getLongDescritpionState(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("krotkie_opisy", false);
	}
	
	public static Boolean getAvatarsState(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("awatary", false);
	}

}
