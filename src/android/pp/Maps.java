package android.pp;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class Maps extends MapActivity {
    
    private MapController mapController;
    private MapView mapView;
    private LocationManager locationManager;
   // private int latitude;
    //private int longitude;
    private GeoPoint gp;
    
/** Called when the activity is first created. */
    public void onCreate(Bundle icicle) {
       super.onCreate(icicle);
       Bundle b = this.getIntent().getExtras();
       if(b.containsKey("latitude") && b.containsKey("longitude"))
       {
    	  // this.latitude = b.getInt("latitude");
    	   //this.longitude = b.getInt("longitude");
    	   this.gp = new GeoPoint(b.getInt("latitude"), b.getInt("longitude"));
       }
       // create a map view
       //mapView = (MapView) findViewById(R.id.mapview); //fuck!!??
       mapView = new MapView(this,"0fwLhF406wY_tWe0QTMNWVBoJWENzwvwdh4wM1g");
      
       mapView.setBuiltInZoomControls(true);
       mapView.setSatellite(true);
       mapController = mapView.getController();
       mapController.setZoom(16);
       setContentView(mapView);       
       mapController.animateTo(gp);

       // get a hangle on the location manager
       locationManager =
         (LocationManager) getSystemService(Context.LOCATION_SERVICE);

       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 
                       0, new LocationUpdateHandler());
    }
            
// this inner class is the intent reciever that recives notifcations
// from the location provider about position updates, and then redraws
// the MapView with the new location centered.
public class LocationUpdateHandler implements LocationListener {

            @Override
            public void onLocationChanged(Location loc) {
            int lat = (int) (loc.getLatitude()*1E6);
            int lng = (int) (loc.getLongitude()*1E6);
            GeoPoint point = new GeoPoint(lat, lng);
           // mapController.setCenter(point);
            setContentView(mapView);
            }

            @Override
            public void onProviderDisabled(String provider) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, 
                            Bundle extras) {}
}

    @Override
    protected boolean isRouteDisplayed() {
            return false;
    }
}
