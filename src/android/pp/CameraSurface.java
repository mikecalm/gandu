package android.pp;

import java.io.IOException;
import java.util.Random;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class CameraSurface extends Activity implements SurfaceHolder.Callback,OnClickListener
{
	/** Messenger for communicating with service. */
    Messenger mService = null;
    
    //variable which controls the ping thread and initialThread
    private ConditionVariable mCondition;
    private ConditionVariable mInitial;
	boolean mIsBound;
	Camera mCamera;
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	boolean mPreview = false;
	Context mContext = this;
	String numerGG = null;
	String fileName = null;
	String filePath = null;
	Random r = new Random();
	int seed = r.nextInt(1000);
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mCondition = new ConditionVariable(false);
	    mInitial = new ConditionVariable(false);
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;
		
		Bundle extras = getIntent().getExtras();
		this.numerGG = extras.getString("GGNumber");
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.camera);
		mSurfaceView = (SurfaceView) findViewById(R.id.camera);
		mSurfaceView.setOnClickListener(this);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		Toast.makeText(getApplicationContext(), "Kliknij w ekran, aby zrobiæ\ni wys³aæ zdjêcie", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
		
		@Override
		public void onPictureTaken(byte[] imageData,Camera c) {
			// TODO Auto-generated method stub
			if (imageData != null) {
				try
				{
					Intent mIntent = new Intent();
					//this.StoreByteImage(this, imageData, 50, "ImageName");
					
					FileUtilities.StoreByteImage(mContext, imageData, 50, "Obraz"+seed);
					c.startPreview();				
					setResult(0,mIntent);
					finish();
				}catch(Exception e)
				{
					Log.e("Camera",""+e.getMessage());
				}
			}
			
		}

};


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		if(mPreview)
		{
			mCamera.stopPreview();
		}
		Camera.Parameters p = mCamera.getParameters();
		p.setPreviewSize(width,height);
		mCamera.setParameters(p);
		try
		{
			mCamera.setPreviewDisplay(holder);
		}
		catch(IOException ioe)
		{
			Log.e("Camera.java","B£AD!");
		}
		mCamera.startPreview();
		mPreview = true;
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera = Camera.open();
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera.stopPreview();
		mPreview = false;
		mCamera.release();
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		mCamera.takePicture(null, mPictureCallback, mPictureCallback);
		Message msg3 = Message.obtain(null,Common.CLIENT_SEND_FILE, 0, 0);	        
		try
		{
    		Bundle wysylany = new Bundle();
			wysylany.putString("numerGG", this.numerGG);
			wysylany.putString("fileName", "Obraz"+seed+".jpg");
			wysylany.putString("filePath", "/sdcard/Obraz"+seed+".jpg");
			wysylany.putString("klasa", "photo");
			msg3.setData(wysylany);
			mService.send(msg3);
		}catch(Exception excMsg)
		{
			Log.e("Camera","Blad wyslania info do serwisu o wysylaniu pliku:\n"+
					excMsg.getMessage());
		}
		
	}
	/**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            mInitial.open();
            mCondition.open();
            //mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        Common.CLIENT_REGISTER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(CameraSurface.this, 
                GanduService.class), mConnection, Context.BIND_AUTO_CREATE);
        
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                    		Common.CLIENT_UNREGISTER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                  
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what) {
              
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("CameraSurface","Zarejestrowany przez serwis.");
                	break;
              
                default:
                    super.handleMessage(msg);
            }
        }
    }
	

	
	
}
