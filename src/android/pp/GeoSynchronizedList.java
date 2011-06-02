package android.pp;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

public class GeoSynchronizedList {
	
	/**
	 * lista uzytkownikow oczekujacych na nasza lokalizacje
	 */
	private Context context;
	private ArrayList<String> geoList;
	
	public GeoSynchronizedList()
	{
		this.geoList = new ArrayList<String>();
	}
	
	public GeoSynchronizedList(Context con)
	{
		this.context = con;
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
    						+location.getLongitude()+";"+location.getAccuracy()
    						+";"+LocationToAddress(location)+":geoLoc:"
    						, Integer.parseInt(ggnum), time);
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
    
    public String LocationToAddress(Location tmp)
	{
		List<Address> addresses;
		try
		{
			Geocoder gc = new Geocoder(this.context, Locale.getDefault());
			addresses = gc.getFromLocation(tmp.getLatitude(), tmp.getLongitude(), 1);
			if (addresses != null)
			{
				Address currentAddr = addresses.get(0);
				
				StringBuilder sb = new StringBuilder("");
				for (int i=0; i<currentAddr.getMaxAddressLineIndex(); i++)
				{
					sb.append(currentAddr.getAddressLine(i)).append("\n");
				}
				return sb.toString();
			}
		}catch(Exception e)
		{
			Log.e("Maps.java",""+e.getMessage());
		}
		return null;
	}
}
