package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.maps.GeoPoint;

import android.location.Location;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;

public class Geo {
	
	public void addFix(String ggnumber, String latitude, String longitude)
	{
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(
					"http://mihoo.cba.pl/gandu/geo.php?action=add&source="+ggnumber+"&latitude="+latitude+"&longitude="+longitude+"");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
	
			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				while (instream.read(tmp) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp);
					BufferedReader br = new BufferedReader(new InputStreamReader(bais));
					String readline = br.readLine();
					Log.i("GeoLocalization", readline);
					
					//ip = readline.split(" ");
				}
			}
			
		}catch(Exception e)
		{
			
		}
	}
	
	public void deleteFix(String ggnumber)
	{
		
	}

	public GeoPoint getFix(String ggnumber)
	{
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(
					"http://mihoo.cba.pl/gandu/geo.php?action=return&source="+ggnumber+"");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
	
			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				while (instream.read(tmp) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp);
					BufferedReader br = new BufferedReader(new InputStreamReader(bais));
					String readline = br.readLine();
					//Log.i("GeoLocalization", readline);
					Spanned data = Html.fromHtml(readline);
					String temp = data.toString();
					String [] tab = temp.split("\n");
					
					Log.i("GeoLocalization", tab[0]);
					
					tab = tab[0].split(";");
					String latitude = tab[0];
					String longitude = tab[1];
					//ip = readline.split(" ");
					GeoPoint g = new GeoPoint((int)(Double.parseDouble(latitude)*1E6), (int)(Double.parseDouble(longitude)*1E6));
				    
				    //l.setLatitude(Double.parseDouble(latitude));
				    //l.setLongitude(Double.parseDouble(longitude));
				    			    
					return g;
				}
			}
			
		}catch(Exception e)
		{
			Log.e("Geo","GET FIX ERROR: "+e.getMessage());
		}
		return null;
	}
	
}
