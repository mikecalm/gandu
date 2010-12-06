package android.pp;

import android.app.TabActivity;
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
import android.pp.ContactBook.IncomingHandler;
import android.provider.Contacts.Intents;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class Chat extends TabActivity{
	public static TabHost tabHost;
	public TabSpec firstTabSpec;
	public SharedPreferences prefs;
	public SharedPreferences.Editor editor;
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    boolean mIsBound;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);	
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		
		Toast.makeText(getApplicationContext(), "onCreate()", Toast.LENGTH_SHORT).show();
		
		//prefs = getPreferences(0);
		prefs = getSharedPreferences("otwarteZakladki", 0);
		editor = prefs.edit();
		//tabHost = (TabHost)findViewById(android.R.id.tabhost);	
		tabHost = getTabHost();
    	
}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Toast.makeText(getApplicationContext(), "onResume()", Toast.LENGTH_SHORT).show();
        String odz = prefs.getString("text", null);
        String numerGGZKtoregoOtworzonoOknoZRozmowa = "";
    	
        Bundle b = this.getIntent().getExtras();
        if(b != null)
        {
	    	if(!b.isEmpty())
	    	{
	    		numerGGZKtoregoOtworzonoOknoZRozmowa = b.getString("ggnumber");
	    		String tabHeader = b.getString("username") + "-" + b.getString("ggnumber");
	            firstTabSpec = tabHost.newTabSpec(tabHeader);        
	            /** TabSpec setIndicator() is used to set name for the tab. */
	            /** TabSpec setContent() is used to set content for a particular tab. */
	           
	            //tescik
	            Intent nowyTab = new Intent(this,Tab.class);
	            nowyTab.putExtra("ggnumber", b.getString("ggnumber"));
	            firstTabSpec.setIndicator(tabHeader).setContent(nowyTab);
	            //tescik
	            
	            //firstTabSpec.setIndicator(tabHeader).setContent(new Intent(this,Tab.class));
	         
	            /** Add tabSpec to the TabHost to display. */
	        	tabHost.addTab(firstTabSpec);
	            //usuniecie ggnumber z intentu, zeby przy zmianie orientacji
	            //nie dodawal po raz kolejny ostatnio otwartej zakladki
	            this.getIntent().removeExtra("ggnumber");     
	            this.getIntent().removeExtra("username");
	        	//this.getIntent().getExtras().clear();
	    	}
        }

        if (odz != null)
        {	
        	String [] tab = odz.split("~");
        	for (String s: tab)
        	{
	            String[] tabText = s.split("-");
	            String ggNum = tabText[tabText.length-1];
	            //jesli aktualnie otwarta zakladka byla juz poprzednio otwarta,
	            //to nie dodawaj jej ponownie	            
	            if(numerGGZKtoregoOtworzonoOknoZRozmowa.equals("") || !ggNum.equals(numerGGZKtoregoOtworzonoOknoZRozmowa))
	            {
	        		firstTabSpec = tabHost.newTabSpec(s);
	        		//tescik
		            Intent nowyTab = new Intent(this,Tab.class);
		            nowyTab.putExtra("ggnumber", ggNum);
		            firstTabSpec.setIndicator(s).setContent(nowyTab);
		            //tescik
	            	//firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
	            	tabHost.addTab(firstTabSpec);
	            }
        	}
        	editor.remove("text");
    		editor.commit();
        }
        
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Toast.makeText(getApplicationContext(), "onPause()", Toast.LENGTH_SHORT).show();
		//preferencje

		String tabs = "";		
        //for (int i =0 ; i<tabHost.getChildCount() ; i++)
		for (int i =0 ; i<tabHost.getTabWidget().getChildCount() ; i++)
        {
			//do nazwy konkretnetnej zakladki dokopalem sie podgladajac w debugu
			//w jakim polu zapisana jest nazwa zakladki.
			//Wydaje mi sie, ze jak bedziemy miec zdefiniowany layout zakladki,
			//to nazwe zakladki bedzie mozna uzyska poprzez odwolanie sie do konkretnego ID (R.id...)
			//RelativeLayout layoutTaba = (RelativeLayout)tabHost.getTabWidget().getChildAt(i);			
			//TextView textViewWLayoutcieTaba = (TextView)layoutTaba.getChildAt(1);
			TextView textViewWLayoutcieTaba = (TextView)tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
        	tabs += textViewWLayoutcieTaba.getText()+"~";
        	//tabHost.setCurrentTab(i);
        	//tabs += tabHost.getCurrentTabTag()+"~";                
        }
        Toast.makeText(getApplicationContext(), tabs, Toast.LENGTH_SHORT).show();
        editor.putString("text", tabs);
        editor.commit();
        //tabHost.setCurrentTab(0);
        //tabHost.clearAllTabs();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "onDestroy()", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		tabHost.clearAllTabs();
		Toast.makeText(getApplicationContext(), "onStop()", Toast.LENGTH_SHORT).show();
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
                	Log.i("Zarejestrowano Chat","Received: "+msg.what);
                	//wyslanie do serwisu wiadomosci, ze pobierana jest lista kontaktow
                	break;
                
                case Common.CLIENT_RECV_MESSAGE:
                	Bundle odebrany = msg.getData();
                	//int num = odebrany.getInt("num");
                	//int seq = odebrany.getInt("seq");
                	//byte [] tresc = odebrany.getByteArray("tresc");
                	String tresc = odebrany.getString("tresc");
                	String wiadomoscOd = odebrany.getString("wiadomoscOd");
                	String przyszlaO = odebrany.getString("przyszlaO");
                	
                	
                	    	
                	//String tmp = tresc.toString();
                	//Log.i("Odebralem wiadomosc od Servicu", Integer.toString(num) + " " +Integer.toString(seq));
                	Log.i("[Chat]Odebralem wiadomosc od Serwisu", tresc);
                	Log.i("[Chat]Od numeru", wiadomoscOd);
                	Log.i("[Chat]O godzinie", przyszlaO);
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
        bindService(new Intent(Chat.this, 
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

