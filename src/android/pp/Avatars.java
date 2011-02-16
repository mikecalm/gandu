package android.pp;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Avatars {

	public Bitmap getImageBitmap(String numerGG)
	{
		Bitmap bm = null;			
        try {
        	String url = "http://avatars.gg.pl/"+numerGG;
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
       } catch (Exception e) {
           Log.e("[RegisterAccount]", "Blad pobierania obrazu tokenu: " + e.getMessage());
       }
       return bm;
	}
}
