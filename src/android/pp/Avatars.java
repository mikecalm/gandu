package android.pp;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Avatars {

	   public Bitmap getImageBitmap(String ggnumber) {
	        Bitmap bm = null;
	        try {
	            URL aURL = new URL("http://avatars.gg.pl/"+ggnumber);
	            URLConnection conn = aURL.openConnection();
	            conn.connect();
	            InputStream is = conn.getInputStream();
	            BufferedInputStream bis = new BufferedInputStream(is);
	            bm = BitmapFactory.decodeStream(bis);
	            //bm = Bitmap.createScaledBitmap(bm,(int)(bm.getWidth()*0.7),(int)(bm.getHeight()*0.7),false);
	            bis.close();
	            is.close();
	       } catch (Exception e) {
	           Log.e("[Avatars]", "Blad pobierania avataru: " + e.getMessage());
	       }
	       return bm;
	    } 
	  
	    
}
