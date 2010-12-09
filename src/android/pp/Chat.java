package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.pp.ContactBook.IncomingHandler;
import android.provider.Contacts.Intents;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RemoteViews.ActionException;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class Chat extends TabActivity{
	public static TabHost tabHost;
	public TabSpec firstTabSpec;
	public SharedPreferences prefs;
	public SharedPreferences.Editor editor;
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    boolean mIsBound;
    
    //HashMap<String, String> numerShowName;
    ArrayList<String> numerShowName;
    ArrayList<String> numerIndex;
	
    ArchiveSQLite archiveSQL;
    public ArrayList<String> hiddenTabs;
    public ArrayList<String> openedTabs;
	
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);	
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;
		
		//Toast.makeText(getApplicationContext(), "onCreate()", Toast.LENGTH_SHORT).show();
		
		
		//prefs = getPreferences(0);
		prefs = getSharedPreferences("otwarteZakladki", 0);
		editor = prefs.edit();
		//tabHost = (TabHost)findViewById(android.R.id.tabhost);	
		tabHost = getTabHost();
		/*tabHost.setOnTabChangedListener(new OnTabChangeListener(){
			@Override
			public void onTabChanged(String tabId) {
				//tabHost.getcu
			    int indeksZakladki = tabHost.getCurrentTab();
			    ImageView ImageViewWLayoutcieTaba = (ImageView)tabHost.getTabWidget().getChildAt(indeksZakladki).findViewById(android.R.id.icon);
			    ImageViewWLayoutcieTaba.setImageDrawable(getResources().getDrawable(R.drawable.available));
			    //tabHost.getTabWidget().getChildAt(indeksZakladki).setBackgroundResource(R.drawable.available);
			    //ImageViewWLayoutcieTaba.
			}});*/
		
		archiveSQL = new ArchiveSQLite(this.getApplicationContext());		
		//numerShowName = new HashMap<String, String>();
		numerShowName = new ArrayList<String>();
		numerIndex = new ArrayList<String>();
		hiddenTabs = new ArrayList<String>();
		openedTabs = new ArrayList<String>();
    	
}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//Toast.makeText(getApplicationContext(), "onResume()", Toast.LENGTH_SHORT).show();
        String odz = prefs.getString("text", null);
        String numerGGZKtoregoOtworzonoOknoZRozmowa = "";
        
        Cursor unreadGGNumbers = archiveSQL.readUnreadGGNumbers();
        ArrayList<String> ggNumbers = archiveSQL.showUnreadGGNumbers(unreadGGNumbers);
    	
        Bundle b = this.getIntent().getExtras();
        if(b != null)
        {
	    	if(!b.isEmpty())
	    	{
	    		if(b.containsKey("ShowNameGGNumber") && b.containsKey("indexGGNumber"))
	    		{
	    			//numerShowName = (HashMap<String, String>)b.getParcelable("ShowNameGGNumber");
	    			numerShowName = b.getStringArrayList("ShowNameGGNumber");
	    			numerIndex = b.getStringArrayList("indexGGNumber");
	    			this.getIntent().removeExtra("ShowNameGGNumber");     
		            this.getIntent().removeExtra("indexGGNumber");
	    		}
	    		//dodanie zakladki kontaktu, ktory zostal wybrany z listy kontaktow
	    		if(b.containsKey("ggnumber") && b.containsKey("username"))
	    		{
		    		numerGGZKtoregoOtworzonoOknoZRozmowa = b.getString("ggnumber");
		    		
		    		ggNumbers.remove(numerGGZKtoregoOtworzonoOknoZRozmowa);
		    		
		    		String tabHeader = b.getString("username") + "-" + b.getString("ggnumber");
		            firstTabSpec = tabHost.newTabSpec(tabHeader);        
		            /** TabSpec setIndicator() is used to set name for the tab. */
		            /** TabSpec setContent() is used to set content for a particular tab. */
		           
		            //tescik
		            Intent nowyTab = new Intent(this,Tab.class);
		            nowyTab.putExtra("ggnumber", b.getString("ggnumber"));
		            firstTabSpec.setIndicator(tabHeader).setContent(nowyTab);
		            //tescik
		            openedTabs.add(b.getString("ggnumber"));
		            
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
        }

        if (odz != null)
        {
        	if (!odz.equals(""))
        	{
        		//dodanie zakladek kontaktow, z ktorymi wczesniej prowadzilismy rozmowe
	        	String [] tab = odz.split("~");
	        	for (String s: tab)
	        	{
		            String[] tabText = s.split("-");
		            String ggNum = tabText[tabText.length-1];
		            //jesli aktualnie otwarta zakladka byla juz poprzednio otwarta,
		            //to nie dodawaj jej ponownie	            
		            if(numerGGZKtoregoOtworzonoOknoZRozmowa.equals("") || !ggNum.equals(numerGGZKtoregoOtworzonoOknoZRozmowa))
		            {
		            	
		            	ggNumbers.remove(ggNum);
		            	
		        		firstTabSpec = tabHost.newTabSpec(s);
		        		//tescik
			            Intent nowyTab = new Intent(this,Tab.class);
			            nowyTab.putExtra("ggnumber", ggNum);
			            firstTabSpec.setIndicator(s).setContent(nowyTab);
			            //tescik
			            openedTabs.add(ggNum);
		            	//firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
		            	tabHost.addTab(firstTabSpec);
		            }
	        	}
	        	editor.remove("text");
	    		editor.commit();
        	}
        }
        
        //dodanie zakladek kontaktow, ktorych nieprzeczytane wiadomosci sa w bazie
        for(int i=0; i<ggNumbers.size(); i++)
        {
        	String numerGGKontaktu = ggNumbers.get(i);
        	//String header = numerShowName.get(numerGGKontaktu)+"-"+numerGGKontaktu;
        	String showNameGG = "";
        	//showNameGG = numerShowName.get(numerIndex.indexOf(numerGGKontaktu));
        	//jesli wiadomosc jest od kogos spoza listy
        	showNameGG = numerGGKontaktu;
        	//jesli wiadomosc jest od kogos z listy
        	if(numerIndex.indexOf(numerGGKontaktu) != -1)
        		showNameGG = numerShowName.get(numerIndex.indexOf(numerGGKontaktu));
        	/*for(int j=0; j<numerShowName.size(); j++)
        	{
        		if(numerShowName.get(j).endsWith("-"+numerGGKontaktu))
        		{
        			//showNameGG = numerShowName.get(j).substring(0, (numerGGKontaktu.length()+1));
        			showNameGG = numerShowName.get(j).substring(0, numerShowName.get(j).lastIndexOf("-"));
        			break;
        		}
        	}*/
        	String header = showNameGG+"-"+numerGGKontaktu;
        	firstTabSpec = tabHost.newTabSpec(header);
    		//tescik
            Intent nowyTab = new Intent(this,Tab.class);
            nowyTab.putExtra("ggnumber", numerGGKontaktu);
            firstTabSpec.setIndicator(header).setContent(nowyTab);
            //firstTabSpec.setIndicator(header, getResources().getDrawable(R.drawable.available)).setContent(nowyTab);
            //tescik
            openedTabs.add(numerGGKontaktu);
        	//firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
        	tabHost.addTab(firstTabSpec);
        }
        	
        //ustawienie kazdej karty po koleji jako aktualnie uzywanej
        //spowoduje wywolanie metody onCreate kazdej z otwartych zakladek.
        //Bez tego, aby wywolac metode onCreate kazdej z zakladek nalezaloby
        //manualnie do niej przejsc
        for (int i = (tabHost.getTabWidget().getChildCount()-1) ; i>=0 ; i--)
        {
        	tabHost.setCurrentTab(i);
        }
        
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		//Toast.makeText(getApplicationContext(), "onPause()", Toast.LENGTH_SHORT).show();
		//preferencje

		/*String tabs = "";		
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
        	//tabs += textViewWLayoutcieTaba.getText()+"~";
			String headerText = textViewWLayoutcieTaba.getText().toString();
			if(!headerText.equals(""))
				tabs += headerText+"~";
        	//tabHost.setCurrentTab(i);
        	//tabs += tabHost.getCurrentTabTag()+"~";                
        }
        Toast.makeText(getApplicationContext(), tabs, Toast.LENGTH_SHORT).show();
        editor.putString("text", tabs);
        editor.commit();*/
        //tabHost.setCurrentTab(0);
        //tabHost.clearAllTabs();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
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
            //unbindService(mConnection);
            //mIsBound = false;
            //mCallbackText.setText("Unbinding.");
        }
		super.onDestroy();
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
        	//tabs += textViewWLayoutcieTaba.getText()+"~";
			String headerText = textViewWLayoutcieTaba.getText().toString();
			if(!headerText.equals(""))
				tabs += headerText+"~";
        	//tabHost.setCurrentTab(i);
        	//tabs += tabHost.getCurrentTabTag()+"~";                
        }
        Toast.makeText(getApplicationContext(), tabs, Toast.LENGTH_SHORT).show();
        editor.putString("text", tabs);
        editor.commit();
		tabHost.clearAllTabs();
		//Toast.makeText(getApplicationContext(), "onDestroy()", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		//Toast.makeText(getApplicationContext(), "onStop()", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.chatmenu, menu);
		return true;
	}
	
	/*//zdarzenia zwiazane z wyborem opcji menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId())
		{
			case R.id.ZamknijOkno01:			
				Log.i("[Chat]OptionsMenu", "Zamknij");
				//tabHost.getCurrentTabView().dispatchKeyEvent(new KeyEvent(0, KeyEvent.getMaxKeyCode()+1));
				if(tabHost.getTabWidget().getChildCount() > 1)
				{
					int indeksZamykanej = tabHost.getCurrentTab();
					tabHost.getCurrentTabView().setVisibility(View.GONE);
					if(indeksZamykanej != 0)
						tabHost.setCurrentTab(0);
					else
						tabHost.setCurrentTab(1);
				}
				else
					finish();
				//Tab zamykana = (Tab)tabHost.getCurrentTabView().getContext();
				//zamykana.finish();
				//tabHost.getCurrentTab()
				break;
		}
		return false;
	}*/
	
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
                	String konferenci = odebrany.getString("konferenci");
                	ArrayList<String> konferenciGG = odebrany.getStringArrayList("konferenciGG");
                	String tytulTaba = wiadomoscOd;
                	if(konferenci != null)
                		tytulTaba=konferenci;

                	//if(!hiddenTabs.contains(wiadomoscOd) && openedTabs.contains(wiadomoscOd))
                	
                	//jesli wiadomosc zostala przyslana przez osobe, ktorej
                	//zakladka zostala wczesniej zamknieta, to wiadomosc bedzie oczekiwac
                	//w bazie na odczytanie (bedzie miec ustawiona flage unread do momentu ponownego
                	//uruchomienia okna z rozmowami)
                	//if(!hiddenTabs.contains(wiadomoscOd))
                	if(!hiddenTabs.contains(tytulTaba))                		
                	{
                		//jesli wiadomosc zostala przyslana przez osobe, ktorej
                		//zaklada nie zostala wczesniej otwarta, to zostanie dodana
                		//nowo zakladka, a flaga unread wiadomosci zostanie odznaczona (wartosc -1)
                		//if(!openedTabs.contains(wiadomoscOd))
                		if(!openedTabs.contains(tytulTaba))
                		{
                			String header = "";
                			//wiadomosc niekonferencyjna
                			if(konferenciGG == null)
                			{
	                			//jesli wiadomosc jest od kogos spoza listy                			
	                			String showNameGG = wiadomoscOd;
	                			//jesli wiadomosc jest od kogos z listy
	                			if(numerIndex.indexOf(wiadomoscOd) != -1)
	                				showNameGG = numerShowName.get(numerIndex.indexOf(wiadomoscOd));
	                			header = showNameGG+"-"+wiadomoscOd;
                			}
                			//wiadomosc konferencyjna
                			else
                			{
                				for(int i=0; i<konferenciGG.size(); i++)
                				{
	                				//jesli wiadomosc jest od kogos spoza listy                			
		                			String showNameGG = konferenciGG.get(i);
		                			//jesli wiadomosc jest od kogos z listy
		                			if(numerIndex.indexOf(konferenciGG.get(i)) != -1)
		                				showNameGG = numerShowName.get(numerIndex.indexOf(konferenciGG.get(i)));
		                			header += showNameGG+"-"+konferenciGG.get(i)+";";
		                			//wyciecie ostatniego srednika
                				}
	                			header = header.substring(0, header.length()-1);
                			}
                			firstTabSpec = tabHost.newTabSpec(header);
    		        		//tescik
    			            Intent nowyTab = new Intent(getApplicationContext(),Tab.class);
    			            nowyTab.putExtra("ggnumber", wiadomoscOd);
    			            if(konferenciGG != null)
                			{
    			            	nowyTab.putStringArrayListExtra("konferenciGG", konferenciGG);
    			            	nowyTab.putExtra("konferenciWBazie", konferenci);
                			}
    			            firstTabSpec.setIndicator(header).setContent(nowyTab);
    			            //tescik
    			            //openedTabs.add(wiadomoscOd);
    			            openedTabs.add(tytulTaba);
    		            	//firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
    		            	tabHost.addTab(firstTabSpec);
    		            	int aktualnaZakladka = tabHost.getCurrentTab();
    		            	tabHost.setCurrentTabByTag(header);
    		            	tabHost.setCurrentTab(aktualnaZakladka);
                		}
	                	Long idSQL = odebrany.getLong("idSQL");
	                	Log.i("[Chat]START SQL","oznaczenie wiadomosci"+idSQL+" jako przeczytanej.");
						int liczbaZmienionychWierszy = archiveSQL.setMessageAsRead(idSQL);
						Cursor wynikSQL = archiveSQL.readMessage(idSQL);
						archiveSQL.showMessage(wynikSQL);
						Log.i("[Chat]KONIEC SQL","L. zmienionych: "+liczbaZmienionychWierszy+". Oznaczenie wiadomosci"+idSQL+" jako przeczytanej.");
                	}
                	    	
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

