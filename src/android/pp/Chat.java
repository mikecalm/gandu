package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.app.Activity;
import android.app.NotificationManager;
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
import android.os.Vibrator;
import android.pp.ContactBook.IncomingHandler;
import android.provider.Contacts.Intents;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
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
	String mojNumer = "";
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    boolean mIsBound;
    
    //HashMap<String, String> numerShowName;
    ArrayList<String> numerShowName;
    ArrayList<String> numerIndex;
    ArchiveSQLite archiveSQL;
    public ArrayList<String> hiddenTabs;
    public ArrayList<String> openedTabs;
    public ArrayList<String> savedTabs;
    public HashMap<Integer, Long> hiddenTabsLastMessage;
    public NotificationManager mNM;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Toast.makeText(getApplicationContext(), "[Chat]onCreate()", Toast.LENGTH_SHORT).show();
		setContentView(R.layout.chat);	
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;
		
		
		prefs = getSharedPreferences("otwarteZakladki", 0);
		editor = prefs.edit();	
		tabHost = getTabHost();
		archiveSQL = new ArchiveSQLite(this.getApplicationContext());
		numerShowName = new ArrayList<String>();
		numerIndex = new ArrayList<String>();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    	
}

	@Override
	protected void onResume() {
		//Toast.makeText(getApplicationContext(), "[Chat]onResume()", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
		super.onResume();
		mNM.cancelAll();

		hiddenTabs = new ArrayList<String>();
		openedTabs = new ArrayList<String>();
		savedTabs = new ArrayList<String>();
		hiddenTabsLastMessage = new HashMap<Integer, Long>();
		
        String odz = prefs.getString("text", null);
        String numerGGZKtoregoOtworzonoOknoZRozmowa = "";
        
        Cursor unreadGGNumbers = archiveSQL.readUnreadGGNumbers();
        ArrayList<String> ggNumbers = archiveSQL.showUnreadGGNumbers(unreadGGNumbers);
    	
        Bundle b = this.getIntent().getExtras();
        if(b != null)
        {
	    	if(!b.isEmpty())
	    	{
	    		if(b.containsKey("mojNumer"))
	    			this.mojNumer = b.getString("mojNumer");
	    		if(b.containsKey("ShowNameGGNumber") && b.containsKey("indexGGNumber"))
	    		{
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
		    		
		    		String tabHeader = b.getString("username"); 
		    		firstTabSpec = tabHost.newTabSpec(numerGGZKtoregoOtworzonoOknoZRozmowa);
		    		savedTabs.add(numerGGZKtoregoOtworzonoOknoZRozmowa);
		           
		            //tescik
		            Intent nowyTab = new Intent(this,Tab.class);
		            nowyTab.putExtra("ggnumber", b.getString("ggnumber"));
		            nowyTab.putExtra("ggnumberShowName", pobierzShowName(b.getString("ggnumber")));
		            nowyTab.putExtra("mojNumer", this.mojNumer);
		            firstTabSpec.setIndicator(tabHeader).setContent(nowyTab);
		            //tescik
		            openedTabs.add(b.getString("ggnumber"));
		         
		            /** Add tabSpec to the TabHost to display. */
		        	tabHost.addTab(firstTabSpec);
		            //usuniecie ggnumber z intentu, zeby przy zmianie orientacji
		            //nie dodawal po raz kolejny ostatnio otwartej zakladki
		            this.getIntent().removeExtra("ggnumber");
		            this.getIntent().removeExtra("username");
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
	        		Boolean konferencyjnaZakladka = false;
	        		if(s.matches("^([0-9]+;)+[0-9]+$"))
	        			konferencyjnaZakladka = true;
	        		//jesli rozmowa konferencyjna
	        		if(konferencyjnaZakladka)
	        		{
			            //jesli aktualnie otwarta zakladka byla juz poprzednio otwarta,
			            //to nie dodawaj jej ponownie	            
			            if(numerGGZKtoregoOtworzonoOknoZRozmowa.equals("") || !s.equals(numerGGZKtoregoOtworzonoOknoZRozmowa))
			            {	
			        		firstTabSpec = tabHost.newTabSpec(s);
			        		savedTabs.add(s);
			        		//tescik
				            Intent nowyTab = new Intent(this,Tab.class);
				            nowyTab.putExtra("mojNumer", this.mojNumer);
				            ArrayList<String> odzyskaniKonferenciGG = new ArrayList<String>();
				            ArrayList<String> odzyskaniKonferenciGGShowName = new ArrayList<String>();
				            String labelTaba = pobierzShowNameDlaKonferentow(s,odzyskaniKonferenciGG,odzyskaniKonferenciGGShowName);
							nowyTab.putStringArrayListExtra("konferenciGG", odzyskaniKonferenciGG);
							nowyTab.putStringArrayListExtra("konferenciGGShowName", odzyskaniKonferenciGGShowName);
							nowyTab.putExtra("konferenciWBazie",s);
				            firstTabSpec.setIndicator(labelTaba).setContent(nowyTab);
				            //tescik
				            openedTabs.add(s);
			            	tabHost.addTab(firstTabSpec);
			            }
	        		}
	        		//jesli rozmowa niekonferencyjna
	        		else
	        		{
	        			String ggNum = s;
			            //jesli aktualnie otwarta zakladka byla juz poprzednio otwarta,
			            //to nie dodawaj jej ponownie	            
			            if(numerGGZKtoregoOtworzonoOknoZRozmowa.equals("") || !ggNum.equals(numerGGZKtoregoOtworzonoOknoZRozmowa))
			            {
			            	
			            	ggNumbers.remove(ggNum);
			            	
			        		firstTabSpec = tabHost.newTabSpec(s);
			        		savedTabs.add(s);
			        		//tescik
				            Intent nowyTab = new Intent(this,Tab.class);
				            nowyTab.putExtra("ggnumber", ggNum);
				            nowyTab.putExtra("ggnumberShowName", pobierzShowName(ggNum));
				            nowyTab.putExtra("mojNumer", this.mojNumer);
				            
				            String label = ggNum;
				            if(numerIndex != null)
				            {
					            int indexShowName = 0;
					            if((indexShowName = numerIndex.indexOf(ggNum)) != -1)
					            	label = numerShowName.get(indexShowName);
				            }
				            firstTabSpec.setIndicator(label).setContent(nowyTab);
				            
				            //firstTabSpec.setIndicator(s).setContent(nowyTab);
				            //tescik
				            openedTabs.add(ggNum);
			            	tabHost.addTab(firstTabSpec);
			            }
	        		}
	        	}
	        	editor.remove("text");
	    		editor.commit();
        	}
        }
        
        //dodanie zakladek kontaktow, ktorych nieprzeczytane wiadomosci (niekonferencyjne) sa w bazie
        for(int i=0; i<ggNumbers.size(); i++)
        {
        	String numerGGKontaktu = ggNumbers.get(i);
        	String showNameGG = "";
        	//jesli wiadomosc jest od kogos spoza listy
        	showNameGG = pobierzShowName(numerGGKontaktu);        	
        	//jesli wiadomosc jest od kogos z listy        	
        	String header = showNameGG;
        	firstTabSpec = tabHost.newTabSpec(numerGGKontaktu);
        	savedTabs.add(numerGGKontaktu);
    		//tescik
            Intent nowyTab = new Intent(this,Tab.class);
            nowyTab.putExtra("ggnumber", numerGGKontaktu);
            nowyTab.putExtra("ggnumberShowName", showNameGG);
            nowyTab.putExtra("mojNumer", this.mojNumer);
            firstTabSpec.setIndicator(header).setContent(nowyTab);
            //firstTabSpec.setIndicator(header, getResources().getDrawable(R.drawable.available)).setContent(nowyTab);
            //tescik
            openedTabs.add(numerGGKontaktu);
        	//firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
        	tabHost.addTab(firstTabSpec);
        }
        
        //dodanie zakladek konferencyjnych, ktorych nieprzeczytane wiadomosci (niekonferencyjne) sa w bazie
        Cursor konferencjeZBazyC = archiveSQL.readUnreadGGNumbersConference();
        ArrayList<String> konferencjeZBazy = archiveSQL.showUnreadGGNumbersConference(konferencjeZBazyC);
        for(int i=0; i<konferencjeZBazy.size(); i++)
        {
        	firstTabSpec = tabHost.newTabSpec(konferencjeZBazy.get(i));
        	savedTabs.add(konferencjeZBazy.get(i));
    		//tescik
            Intent nowyTab = new Intent(this,Tab.class);
            nowyTab.putExtra("mojNumer", this.mojNumer);
            ArrayList<String> odzyskaniKonferenciGG = new ArrayList<String>();
            ArrayList<String> odzyskaniKonferenciGGShowName = new ArrayList<String>();
            String labelTaba = pobierzShowNameDlaKonferentow(konferencjeZBazy.get(i), odzyskaniKonferenciGG, odzyskaniKonferenciGGShowName);
			nowyTab.putStringArrayListExtra("konferenciGG", odzyskaniKonferenciGG);
			nowyTab.putStringArrayListExtra("konferenciGGShowName", odzyskaniKonferenciGGShowName);
			nowyTab.putExtra("konferenciWBazie",konferencjeZBazy.get(i));
            firstTabSpec.setIndicator(labelTaba).setContent(nowyTab);
            //tescik
            openedTabs.add(konferencjeZBazy.get(i));
        	tabHost.addTab(firstTabSpec);
        }
        
        //jesli nie otworzono zadnej zakladki, to zakoncz.
        //taka sytuacja moze wystapic jesli ktos z listy kontaktow
        //wybierze "przejdz do rozmow" a w bazie, ani w preferencjach
        //nie bedzie zadnych rozmow do otworzenia.
        if(tabHost.getTabWidget().getChildCount() == 0)
        {
        	Toast.makeText(getApplicationContext(), "Nie prowadzisz ¿adnej rozmowy", Toast.LENGTH_SHORT).show();
        	finish();
        }
        	
        //ustawienie kazdej karty po koleji jako aktualnie uzywanej
        //spowoduje wywolanie metody onCreate kazdej z otwartych zakladek.
        //Bez tego, aby wywolac metode onCreate kazdej z zakladek nalezaloby
        //manualnie do niej przejsc
        for (int i = (tabHost.getTabWidget().getChildCount()-1) ; i>=0 ; i--)
        {
        	tabHost.setCurrentTab(i);
        	tabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 35;
        }
        
	}
	
	@Override
	protected void onPause() {
		//Toast.makeText(getApplicationContext(), "[Chat]onPause()", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Toast.makeText(getApplicationContext(), "[Chat]onDestroy()", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
		if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
            		Message msgContactBookOut = Message.obtain(null,
                    		Common.CLIENT_CONTACTBOOK_OUT);
                    mService.send(msgContactBookOut);
                    Message msg = Message.obtain(null,
                    		Common.CLIENT_UNREGISTER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                	Log.e("[Chat]OnDestroy", "Error: "+e.getMessage());
                }
            }
        }
		//super.onDestroy();
		String tabs = "";
		for(int i=0; i<savedTabs.size(); i++)
			tabs += savedTabs.get(i)+"~";
        editor.putString("text", tabs);
        editor.commit();
        try
        {
        	tabHost.clearAllTabs();
        }catch(Exception excCAT)
        {
        	Log.e("[Chat]OnDestroy", "Error: "+excCAT.getMessage());
        }
		Log.e("[Chat]OnDestroy", "linia 462.");
	}

	@Override
	protected void onStop() {
		super.onStop();
		//Toast.makeText(getApplicationContext(), "[Chat]onStop()", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.chatmenu, menu);
		return true;
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
                	String tresc = odebrany.getString("tresc");
                	String wiadomoscOd = odebrany.getString("wiadomoscOd");
                	String przyszlaO = odebrany.getString("przyszlaO");
                	String konferenci = odebrany.getString("konferenci");
                	ArrayList<String> konferenciGG = odebrany.getStringArrayList("konferenciGG");
                	ArrayList<String> konferenciGGNew = new ArrayList<String>();
                	ArrayList<String> konferenciGGShowName = new ArrayList<String>();
                	Long idSQL = odebrany.getLong("idSQL");
                	String tytulTaba = wiadomoscOd;
                	if(konferenci != null)
                		tytulTaba=konferenci;

                	Long idMsgLastNotification = null;
                	
                	//jesli wiadomosc zostala przyslana przez osobe, ktorej
                	//zakladka zostala wczesniej zamknieta, to wiadomosc bedzie oczekiwac
                	//w bazie na odczytanie (bedzie miec ustawiona flage unread do momentu ponownego
                	//uruchomienia okna z rozmowami)
                	if(!hiddenTabs.contains(tytulTaba))                		
                	{
                		//jesli wiadomosc zostala przyslana przez osobe, ktorej
                		//zaklada nie zostala wczesniej otwarta, to zostanie dodana
                		//nowo zakladka, a flaga unread wiadomosci zostanie odznaczona (wartosc -1)
                		if(!openedTabs.contains(tytulTaba))
                		{
                			String header = "";
                			//wiadomosc niekonferencyjna
                			if(konferenciGG == null)
                			{
	                			//jesli wiadomosc jest od kogos spoza listy                			
	                			String showNameGG = wiadomoscOd;
	                			//jesli wiadomosc jest od kogos z listy
	                			if(numerIndex != null)
	                			{
		                			if(numerIndex.indexOf(wiadomoscOd) != -1)
		                				showNameGG = numerShowName.get(numerIndex.indexOf(wiadomoscOd));
	                			}
	                			header = showNameGG;
	                			
	                			firstTabSpec = tabHost.newTabSpec(wiadomoscOd);
	                			savedTabs.add(wiadomoscOd);
                			}
                			//wiadomosc konferencyjna
                			else
                			{
                				header = pobierzShowNameDlaKonferentow(konferenci, konferenciGGNew, konferenciGGShowName);
	                			
	                			firstTabSpec = tabHost.newTabSpec(konferenci);
	                			savedTabs.add(konferenci);
                			}
    		        		//tescik
    			            Intent nowyTab = new Intent(getApplicationContext(),Tab.class);
    			            nowyTab.putExtra("ggnumber", wiadomoscOd);
    			            nowyTab.putExtra("ggnumberShowName", pobierzShowName(wiadomoscOd));
    			            nowyTab.putExtra("mojNumer", mojNumer);
    			            if(konferenciGG != null)
                			{
    			            	nowyTab.putStringArrayListExtra("konferenciGG", konferenciGG);
    			            	nowyTab.putStringArrayListExtra("konferenciGGShowName", konferenciGGShowName);
    			            	nowyTab.putExtra("konferenciWBazie", konferenci);
                			}
    			            firstTabSpec.setIndicator(header).setContent(nowyTab);
    			            //tescik
    			            openedTabs.add(tytulTaba);
    		            	tabHost.addTab(firstTabSpec);
    		            	tabHost.getTabWidget().getChildAt(tabHost.getTabWidget().getChildCount()-1).getLayoutParams().height = 35;
    		            	int aktualnaZakladka = tabHost.getCurrentTab();
    		            	if(konferenciGG == null)
    		            		tabHost.setCurrentTabByTag(wiadomoscOd);
    		            	else
    		            		tabHost.setCurrentTabByTag(konferenci);
    		            	tabHost.setCurrentTab(aktualnaZakladka);
                		}
	                	Log.i("[Chat]START SQL","oznaczenie wiadomosci"+idSQL+" jako przeczytanej.");
						int liczbaZmienionychWierszy = archiveSQL.setMessageAsRead(idSQL);
						Cursor wynikSQL = archiveSQL.readMessage(idSQL);
						archiveSQL.showMessage(wynikSQL);
						Log.i("[Chat]KONIEC SQL","L. zmienionych: "+liczbaZmienionychWierszy+". Oznaczenie wiadomosci"+idSQL+" jako przeczytanej.");

                		idMsgLastNotification = idSQL;
                	}
                	else
                		idMsgLastNotification = idSQL;
                		
            		if(idMsgLastNotification != null)
            			//mNM.cancel(Integer.parseInt(idMsgLastNotification.toString()));
            			mNM.cancel(Integer.parseInt(wiadomoscOd));
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
        }
    };
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(Chat.this, 
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
    
    public String pobierzShowName(String numerGG)
    {
    	String znalezionyShowName = numerGG;
    	if(numerIndex != null)
    	{
    		int indeks = 0;
    		if((indeks = numerIndex.indexOf(numerGG)) != -1)
    			znalezionyShowName = numerShowName.get(indeks);
    	}
    	return znalezionyShowName;
    }
    
    /*
     * numInd i numSN musza zostac zainicjowane przed wejsciem do funkcji
     * numInd = new ArrayList<String>();
     * numSn = new ArrayList<String>();
     * aby po wyjsciu pozostaly odpowienio uzupelnione
     */
    public String pobierzShowNameDlaKonferentow(String konferenciRozdzieleniSrednikiem, 
    											ArrayList<String> numInd, ArrayList<String> numSN)
    {
    	String[] numeryKonf = konferenciRozdzieleniSrednikiem.split(";");
    	String konferenciShowNameRozdzieleniSrednikiem = "";
    	for(int i=0; i<numeryKonf.length; i++)
    	{
    		numInd.add(numeryKonf[i]);
    		numSN.add(numeryKonf[i]);
    		konferenciShowNameRozdzieleniSrednikiem += numeryKonf[i]+";"; 
    	}
    	if(numerIndex != null)
    	{
    		konferenciShowNameRozdzieleniSrednikiem = "";
    		for(int i=0; i<numInd.size(); i++)
    		{
	    		int indeks = 0;
	    		if((indeks = numerIndex.indexOf(numInd.get(i))) != -1)
	    		{
	    			numSN.remove(i);
	    			numSN.add(i, numerShowName.get(indeks));
	    		}
	    		konferenciShowNameRozdzieleniSrednikiem += numSN.get(i)+";";
    		}
    	}
    	konferenciShowNameRozdzieleniSrednikiem = konferenciShowNameRozdzieleniSrednikiem.substring(0, konferenciShowNameRozdzieleniSrednikiem.length()-1);
    	return konferenciShowNameRozdzieleniSrednikiem;
    }
}

