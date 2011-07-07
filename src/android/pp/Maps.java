package android.pp;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class Maps extends MapActivity implements LocationListener {

	private MapController mapController;
	private MapView mapView;
	private LocationManager locationManager;
	private Location mLocation;
	// private int latitude;
	// private int longitude;
	private GeoPoint gp;
	//GEOtest
	int gpAccuracy = 0;
	//GEOtest
	private String source; 

	/** Called when the activity is first created. */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.map);
		Bundle b = this.getIntent().getExtras();		
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController();
		mapController.setZoom(15);
		
		if (b.containsKey("latitude") && b.containsKey("longitude")) {
			this.gp = new GeoPoint(b.getInt("latitude"), b.getInt("longitude"));
			if(b.containsKey("accuracy"))
				this.gpAccuracy = b.getInt("accuracy");
			this.showOnMap(gp);
		}
		else if (b.containsKey("FromDevice") && b.containsKey("source"))
		{
			source = b.getString("source");
			double [] fix = this.getFix();
			Geo geo = new Geo();
			geo.addFix(source, Double.toString(fix[0]), Double.toString(fix[1]));
			
			int latitude = (int) (fix[0]* 1E6);
			int longitude = (int)(fix[1]* 1E6);			
						
			this.gp = new GeoPoint(latitude,longitude);
			this.showOnMap(this.gp);
		}
		  	mapView.invalidate();
		  	Toast.makeText(getApplicationContext(), LocationToAddress(this.gp), Toast.LENGTH_SHORT).show();
      
	}

	public String LocationToAddress(GeoPoint tmp)
	{
		List<Address> addresses;
		try
		{
			Geocoder gc = new Geocoder(this, Locale.getDefault());
			double lat = (double) tmp.getLatitudeE6() / 1E6;
			double longi = (double) tmp.getLongitudeE6() / 1E6;
			addresses = gc.getFromLocation(lat, longi, 1);
			if (addresses != null)
			{
				Address currentAddr = addresses.get(0);
				
				StringBuilder sb = new StringBuilder("Adres: \n");
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
	
	public String LocationToAddress(Location tmp)
	{
		List<Address> addresses;
		try
		{
			Geocoder gc = new Geocoder(this, Locale.getDefault());
			addresses = gc.getFromLocation(tmp.getLatitude(), tmp.getLongitude(), 1);
			if (addresses != null)
			{
				Address currentAddr = addresses.get(0);
				
				StringBuilder sb = new StringBuilder("Adres: \n");
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
	
	public void showOnMap(GeoPoint tmp) {
		this.gp = tmp;
		mapController.animateTo(tmp);
		MapOverlay mapOverlay = new MapOverlay();
		
		//GEOtest
		SourceOverlay mapAccuracy = new SourceOverlay();
		mapAccuracy.setSource(gp, this.gpAccuracy);
		//GEOtest
		
        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(mapOverlay); 
        //GEOtest
        listOfOverlays.add(mapAccuracy);
        //GEOtest
	}

	public double [] getFix() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		
		List<String> providers = locationManager.getProviders(true);
		double[] tmp = new double[2];
		for (int i = 0; i < providers.size(); i++) {
			locationManager.requestLocationUpdates(providers.get(i), 5000,
					2.0f, this);
			mLocation = locationManager.getLastKnownLocation(providers.get(i));

			if (mLocation != null) {
				tmp[0] = mLocation.getLatitude();
				tmp[1] = mLocation.getLongitude();
			} else {
				;
			}
		}
		if (tmp == null) {
			tmp[0] = 1;
			tmp[1] = 1;
		}
		return tmp;
		
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onLocationChanged(Location location)
	{
		mLocation = location;
		Geo geo = new Geo();
		geo.addFix(source, Double.toString(mLocation.getLatitude()),  Double.toString(mLocation.getLongitude()));
		int latitude = (int) (location.getLatitude()* 1E6);
		int longitude = (int)(location.getLongitude()* 1E6);
		
		showOnMap(new GeoPoint(latitude,longitude));
		Toast.makeText(getApplicationContext(), LocationToAddress(mLocation), Toast.LENGTH_SHORT).show();
		
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

    class MapOverlay extends com.google.android.maps.Overlay
    {
        @Override
        public boolean draw(Canvas canvas, MapView mapView, 
        boolean shadow, long when) 
        {
            super.draw(canvas, mapView, shadow);                   
 
            //---translate the GeoPoint to screen pixels---
            Point screenPts = new Point();
            mapView.getProjection().toPixels(gp, screenPts);
 
            //---add the marker---
            Bitmap bmp = BitmapFactory.decodeResource(
                getResources(), R.drawable.pin);            
            canvas.drawBitmap(bmp, screenPts.x, screenPts.y-32, null);         
            return true;
        }
    }
    
    public class SourceOverlay extends Overlay {

    	private GeoPoint sourcePoint;
    	private float accuracy;

    	public SourceOverlay() {
    		super();
    	}

    	public void setSource(GeoPoint geoPoint, float accuracy) {
    		sourcePoint = geoPoint;
    		this.accuracy = accuracy;
    	}

    	@Override
    	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    		super.draw(canvas, mapView, false);
    		Projection projection = mapView.getProjection();
    		Point center = new Point();

    		int radius = (int) (projection.metersToEquatorPixels(accuracy));
    		projection.toPixels(sourcePoint, center);

    		Paint accuracyPaint = new Paint();
    		accuracyPaint.setAntiAlias(true);
    		accuracyPaint.setStrokeWidth(2.0f);
    		accuracyPaint.setColor(0xff6666ff);
    		accuracyPaint.setStyle(Style.STROKE);

    		canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

    		accuracyPaint.setColor(0x186666ff);
    		accuracyPaint.setStyle(Style.FILL);
    		canvas.drawCircle(center.x, center.y, radius, accuracyPaint);

    	}

    }
}