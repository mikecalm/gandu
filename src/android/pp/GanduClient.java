package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
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


public class GanduClient extends Activity {

	//
	static final int MSG_POSTLOGIN = 1;
	/** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
	public Toast toastGandu;
	String[] ip = null;
	private Button connectPhones;
	private EditText ggNumberEdit;
	private EditText ggPasswordEdit;
	private boolean connected = false;

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

	private OnClickListener connectListener = new OnClickListener() {
		public void onClick(View v) {
			Message msg = Message.obtain(null,GanduService.MSG_LOGIN, 0, 0);
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
	
	/*
	  final Runnable runInUIThread = new Runnable() {
	    public void run() {
	      Toast.makeText(getApplicationContext(), "wywolane z watku", Toast.LENGTH_LONG).show();
	      Intent intent = new Intent(getApplicationContext(),ContactBook.class);
			//intent.addFlags(CONTEXT_IGNORE_SECURITY);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try{
      		startActivity(intent);
	      	}catch(Exception  e)
	      	{
	      		Log.e("KLIENT",""+e.getMessage());
	      	}
	    }
	  };*/
	/*private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg)
		{
			Intent intent = new Intent(getApplicationContext(),ContactBook.class);
			startActivity(intent);
		}
	};*/
	  
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i("ganduClient","Odebra³em"+msg.what);
            switch (msg.what) {
                case GanduClient.MSG_POSTLOGIN:
                	Log.i("ganduClient","Odebra³em"+msg.what);
                	//handler.sendEmptyMessage(1);
                	//this.post(runInUIThread);
					Intent intent = new Intent(getApplicationContext(),ContactBook.class);
					//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try{
                		startActivity(intent);
                	}catch(Exception  e)
                	{
                		Log.e("KLIENT",""+e.getMessage());
                	}
                	break;
                case ContactBook.REGISTERED:
                	Log.i("GanduClient","zarejestrowany przez serwis.");
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
                        GanduService.MSG_REGISTER_CLIENT);
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
                            GanduService.MSG_UNREGISTER_CLIENT);
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
