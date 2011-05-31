package android.pp;

import java.io.DataOutputStream;
import java.util.ArrayList;

import android.location.Location;
import android.util.Log;

public class GeoSynchronizedList {
	
	/**
	 * lista uzytkownikow oczekujacych na nasza lokalizacje
	 */
	private ArrayList<String> geoList;
	
	public GeoSynchronizedList()
	{
		this.geoList = new ArrayList<String>();
	}

	/**
	 * [synchronized]Dodanie uzytkownika do listy oczekujacych na nasza lokalizacje
	 * @param ggnum - numer gg uzytkownika oczekujacego na nasza lokalizacje
	 */
    public synchronized void add(String ggnum) {
    	if(!geoList.contains(ggnum))
    		geoList.add(ggnum);
    }

    /**
     * [synchronized]wyslanie wiadomosci z koordynatami do wszystkich uzytkownikow gg z listy geoList.
     * Wyczyszczenie listy geoList po rozeslaniu wiadomosci. 
     */
    public synchronized void sendLocalization(DataOutputStream out, Location location, int time) {
    	for(String ggnum:geoList)
    	{
    		try
    		{
    			byte[] paczka;
    			if(location != null)
    				paczka = new ChatMessage().setMessage(":geoLoc:"+location.getLatitude()+";"
    						+location.getLongitude()+":geoLoc:", Integer.parseInt(ggnum), time);
    			else
    				paczka = new ChatMessage().setMessage(":geoNotAvail:", Integer.parseInt(ggnum), time);
	
			out.write(paczka);
			Log.i("GanduService", "Wyslalem wiadomosc");
			out.flush();
    		}catch(Exception exc)
    		{
    			Log.e(this.getClass().getSimpleName(), exc.getMessage());
    		}
    	}
		
    	geoList.clear();
    }
}
