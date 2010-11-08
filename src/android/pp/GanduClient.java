package android.pp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;


public class GanduClient extends Activity {

	//
	public SharedPreferences.Editor editor;
	public SharedPreferences prefs;
	//
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
	public Toast toastGandu;
	String[] ip = null;
	private Button connectPhones;
	private EditText ggNumberEdit;
	private EditText ggPasswordEdit;
	// ------------------> OnCreate()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ggNumberEdit = (EditText) findViewById(R.id.EditText01);
		ggNumberEdit.setText("23543809");
		
		ggPasswordEdit = (EditText) findViewById(R.id.EditText02);
		ggPasswordEdit.setText("password");
		connectPhones = (Button) findViewById(R.id.Button01);
		connectPhones.setText("Zaloguj...");
		connectPhones.setOnClickListener(connectListener);
			
		//uruchomienie serwisu Gandu
		startService(new Intent("android.pp.GanduS"));
		//zbindowanie aktywnosci do serwisu
		doBindService();
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		prefs = getPreferences(0);
		editor = prefs.edit();
		editor.remove("text");
		editor.commit();
	}


	private OnClickListener connectListener = new OnClickListener() {
		public void onClick(View v) {
			Message msg = Message.obtain(null,Common.CLIENT_LOGIN, 0, 0);
			Bundle wysylany = new Bundle();
			wysylany.putString("numerGG", ggNumberEdit.getText().toString());
			wysylany.putString("hasloGG" , ggPasswordEdit.getText().toString());
			msg.setData(wysylany);
			
			try
			{
				mService.send(msg);
				
			}catch(Exception excccc)
			{
				Log.e("Blad","Blad!!!!\n"+excccc.getMessage());
			}
		}
	};
	
  
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i("GanduClient","Received: "+msg.what);
            switch (msg.what) {
                case Common.CLIENT_START_INTENT_CONTACTBOOK:
                	Log.i("GanduClient","Odebralem"+msg.what);
                	Intent intent = new Intent(getApplicationContext(),ContactBook.class);
					try{
                		startActivity(intent);
                	}catch(Exception  e)
                	{
                		Log.e("GanduClient",""+e.getMessage());
                	}
                	break;
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("GanduClient","Zarejestrowany przez serwis.");
                	break;
                default:
                    super.handleMessage(msg);
            }
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

            // As part of the sample, tell the user what happened.
            //Toast.makeText(Binding.this, R.string.remote_service_connected,
            //Toast.makeText(GanduClient.this, "remote_service_connected",
            //        Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            //mCallbackText.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            //Toast.makeText(Binding.this, R.string.remote_service_disconnected,
            //Toast.makeText(GanduClient.this, "remote_service_disconnected",
            //        Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(GanduClient.this, 
                GanduService.class), mConnection, Context.BIND_AUTO_CREATE);
        
        mIsBound = true;
        //mCallbackText.setText("Binding.");
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
            //mCallbackText.setText("Unbinding.");
        }
    }
}
