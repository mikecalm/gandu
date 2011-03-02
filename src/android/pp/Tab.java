package android.pp;

import java.util.ArrayList;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;

public class Tab extends Activity{
	/** Messenger for communicating with service. */
    Messenger mService = null;
    boolean mIsBound;
    EditText et;
    public ScrollView sv;
    String ggnumber = "";
    String ggnumberShowName = "";
    ArrayList<String> konferenciGG = null;
    ArrayList<String> konferenciGGShowName = null;
    String konferenciWBazie = "";
    String mojNumer = "";
    ListView l1;
	MessagesAdapter efAdapter;
	int lastMessageID = Integer.MAX_VALUE;
	public ClipboardManager clipboard;
	String kopiowany = "";
    
    ArchiveSQLite archiveSQL;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.tab_content);
		clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
		l1 = (ListView) findViewById(R.id.MessagesListView01);
		efAdapter = new MessagesAdapter(this);
		l1.setAdapter(efAdapter);
	    registerForContextMenu(l1);
		
		Button btn = (Button) findViewById(R.id.ok);
		et = (EditText) findViewById(R.id.entry);
		//tv = (TextView) findViewById(R.id.lblComments);
		//sv = (ScrollView) findViewById(R.id.ScrollView01);
		//tv.setMovementMethod(ScrollingMovementMethod.getInstance());
		//tv.setText("");
		//tv.setTextSize(18);
		btn.setOnClickListener(listener);
		//doBindService();
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;	
		Bundle b = this.getIntent().getExtras();
		if(b != null)
		{
			if(b.containsKey("mojNumer"))
				this.mojNumer = b.getString("mojNumer");
			if(b.containsKey("ggnumber") && b.containsKey("ggnumberShowName"))
			{
				this.ggnumber = b.getString("ggnumber");
				this.ggnumberShowName = b.getString("ggnumberShowName");
	    		Log.i("[Tab]onCreate, przyjalem ggnumber: ",this.ggnumber);
			}
			if(b.containsKey("konferenciGG") && b.containsKey("konferenciWBazie") && b.containsKey("konferenciGGShowName"))
			{
				konferenciGG = new ArrayList<String>();
				konferenciGG = b.getStringArrayList("konferenciGG");
				konferenciGGShowName = new ArrayList<String>();
				konferenciGGShowName = b.getStringArrayList("konferenciGGShowName");
				konferenciWBazie = b.getString("konferenciWBazie");
			}
		}
		
		OnCreateContextMenuListener contextmenulistener = new OnCreateContextMenuListener() {
			
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				// TODO Auto-generated method stub
				if(v.getId() == R.id.MessagesListView01)
				{					
					menu.add(Menu.NONE,0,0,"Kopiuj numer GG");
					menu.add(Menu.NONE,1,1,"Kopiuj treœæ wiadomoœci");
				}
				
			}
		};
		l1.setOnCreateContextMenuListener(contextmenulistener );
		
		archiveSQL = new ArchiveSQLite(this.getApplicationContext());		
		//zakladka z rozmowa konferencyjna
		if(konferenciGG != null)
		{
			if((lastMessageID = archiveSQL.getLastMessageIDConference(konferenciWBazie)) == -1)
				lastMessageID = archiveSQL.getLastArchiveMessageID();
			lastMessageID++;
			ArrayList<MessageClass> dod = new ArrayList<MessageClass>();
			//odczyt z bazy nieprzeczytanych wiadomosci konferencyjnych
			Log.i("[Tab"+ggnumber+"]START SQL","Odczyt ostatnich/nieprzeczytanych wiadomosci.");
	    	
			Cursor wynikSQL = archiveSQL.readUnreadMessagesFromConference(konferenciWBazie);
			ArrayList<String> nieprzeczytane = archiveSQL.showUnreadMessagesConferece(wynikSQL, konferenciWBazie);
			for(int i=0; i<nieprzeczytane.size(); i++)
			{
				String wiadomoscIData = nieprzeczytane.get(i);
				int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
				String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
				int indeksOstatniegoSrednika = wiadomoscIData.lastIndexOf(";");
				String sender = wiadomoscIData.substring(indeksOstatniegoSrednika+1,wiadomoscIData.length());
				Long dataEpoch = Long.parseLong(data);
				String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1, indeksOstatniegoSrednika);
				//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+sender+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
				int indeksKonfShow = konferenciGG.indexOf(sender);
				dod.add(new MessageClass(dataEpoch*1000L,ggnumber,konferenciGGShowName.get(indeksKonfShow),wiadomosc));
				//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+konferenciGGShowName.get(indeksKonfShow)+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
				//tv.append(wiadomosc + "\n");
			}
			efAdapter.addItemsToBeginning(dod);
			Log.i("[Tab"+ggnumber+"]KONIEC SQL","Odczyt ostatnich/nieprzeczytanych wiadomosci.");

		}
		//zakladka z rozmowa niekonferencyjna
		else
		{
			if((lastMessageID = archiveSQL.getLastMessageID(Integer.parseInt(ggnumber))) == -1)
				lastMessageID = archiveSQL.getLastArchiveMessageID();
			lastMessageID++;
			ArrayList<MessageClass> dod = new ArrayList<MessageClass>();
	    	Log.i("[Tab"+ggnumber+"]START SQL","Odczyt ostatnich/nieprzeczytanych wiadomosci.");
	    	
			Cursor wynikSQL = archiveSQL.readUnreadMessagesFrom(Integer.parseInt(ggnumber));
			ArrayList<String> nieprzeczytane = archiveSQL.showUnreadMessages(wynikSQL,Integer.parseInt(ggnumber));
			for(int i=0; i<nieprzeczytane.size(); i++)
			{
				String wiadomoscIData = nieprzeczytane.get(i);
				int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
				String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
				Long dataEpoch = Long.parseLong(data);
				String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1);
				dod.add(new MessageClass(dataEpoch*1000L,ggnumber,ggnumberShowName,wiadomosc));
				//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+ggnumberShowName+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
				//tv.append(wiadomosc + "\n");
				
			}
			efAdapter.addItemsToBeginning(dod);
			Log.i("[Tab"+ggnumber+"]KONIEC SQL","Odczyt ostatnich/nieprzeczytanych wiadomosci.");
		}	
		efAdapter.notifyDataSetChanged();
		l1.setSelection(0);
		//Linkify.addLinks(tv, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);		
		

		l1.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {		
				//jesli wybrano element ze szczytu listy
				if(position == 0)
				{					
					int maxLOstatDoPobra = 5;
					//START Dodanie listy na poczatek (np po odczytaniu starszych wiadomosci z archiwum)
					ArrayList<MessageClass> dod = new ArrayList<MessageClass>();
					
					Cursor wynikSQLLast;
					ArrayList<String> ostatnie;
					//zakladka z rozmowa konferencyjna
					if(konferenciGG != null)
					{
						wynikSQLLast = archiveSQL.readLastXMessagesFromConference(konferenciWBazie,maxLOstatDoPobra,lastMessageID);
						ostatnie = archiveSQL.showLastXMessagesConference(wynikSQLLast,konferenciWBazie);
						if(ostatnie.size() > 0)
						{
							lastMessageID = Integer.parseInt(ostatnie.get(0));
							ostatnie.remove(0);
							for(int i=0; i<ostatnie.size(); i++)
							{
								String wiadomoscIData = ostatnie.get(i);
								int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
								String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
								int indeksOstatniegoSrednika = wiadomoscIData.lastIndexOf(";");
								String nadawca = wiadomoscIData.substring(indeksOstatniegoSrednika+1,wiadomoscIData.length());
								Long dataEpoch = Long.parseLong(data);
								String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1,indeksOstatniegoSrednika);
								//jesli nasza wiadomosc
								if(nadawca.equals(mojNumer))
								{
									dod.add(new MessageClass(dataEpoch*1000L,"-1","Ja",wiadomosc));
								}
								//wiadomosc od ktoregos z naszych konferentow
								else
								{
									int indeksKonfShow = konferenciGG.indexOf(nadawca);
									dod.add(new MessageClass(dataEpoch*1000L,ggnumber,konferenciGGShowName.get(indeksKonfShow),wiadomosc));
									
								}
							}
						}
					}
					//zaladka z rozmowa niekonferencyjna
					else
					{
						wynikSQLLast = archiveSQL.readLastXMessagesFrom(Integer.parseInt(ggnumber),maxLOstatDoPobra,lastMessageID);
						ostatnie = archiveSQL.showLastXMessages(wynikSQLLast,Integer.parseInt(ggnumber));
						if(ostatnie.size() > 0)
						{
							lastMessageID = Integer.parseInt(ostatnie.get(0));
							ostatnie.remove(0);
							for(int i=0; i<ostatnie.size(); i++)
							{
								if(ostatnie.get(i).startsWith("X"))
								{
									//wyciecie X oznaczajacego wiadomosc wyslana do kontaktu
									String wiadomoscIData = ostatnie.get(i).substring(1);
									int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
									String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
									Long dataEpoch = Long.parseLong(data);
									String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1);
									dod.add(new MessageClass(dataEpoch*1000L,"-1","Ja",wiadomosc));								
								}
								else
								{
									String wiadomoscIData = ostatnie.get(i);
									int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
									String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
									Long dataEpoch = Long.parseLong(data);
									String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1);
									dod.add(new MessageClass(dataEpoch*1000L,ggnumber,ggnumberShowName,wiadomosc));
								}
						}
						}
					}									
					efAdapter.addItemsToBeginning(dod);
					//KONIEC Dodanie listy na poczatek (np po odczytaniu starszych wiadomosci z archiwum)
					efAdapter.notifyDataSetChanged();
					l1.setSelection(ostatnie.size());
				}
				/*else
				{
					clipboard.setText(efAdapter.messages.get(position).message);
				}*/
				//jesli wybrano element nie ze szczytu listy
				//if(position != 0)
				/*
				else
				{
					Toast.makeText(getApplicationContext(), efAdapter.messages.get(position).senderNum, Toast.LENGTH_SHORT).show();				
					//START Dodanie jednego elementu na koniec listy
					int val = new Random().nextInt(2)+100;
					efAdapter.addItem(new MessageClass(System.currentTimeMillis(),""+val,"Sender: "+val,"Message: "+(val+1000)));
					//KONIEC Dodanie jednego elementu na koniec listy
				}		
				*/
				//efAdapter.notifyDataSetChanged();
				/*
				//jesli wybrano element nie ze szczytu listy
				if(position != 0)
				{
					//przesuniecie listy na sam dol pod warunkiem, ze dotad lista
					//ustawiona byla na samym dole.
					//Jesli lista byla gdzie wyzej, to nie zostanie przewinieta
					//na sam dol. To na wypadek, gdyby ktos przegladal rozmowe,
					//a ktos by do niego pisal, to zeby lista caly czas mu nie zjezdzala
					//na sam dol.
					//ewentualnie w tym warunku bedzie mozna tez ustawic, zeby zjezdzal na
					//sam dol listy, jesli to uzytkownik wysyla wiadomosc.
					if(l1.getLastVisiblePosition() >= (efAdapter.getCount()-3))
						l1.setSelection(efAdapter.getCount());
				}
				*/
			}
		});		
		
	}
	
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		//return super.onContextItemSelected(item);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo(); 
		switch(item.getItemId())
		{
			case 0: 
			{
				if (efAdapter.messages.get(info.position).senderNum == "-1")
				{
					clipboard.setText(mojNumer);
				}
				else
				{
					clipboard.setText(efAdapter.messages.get(info.position).senderNum);
				}
				break;
			}
			case 1:
			{
				clipboard.setText(efAdapter.messages.get(info.position).message);
				break;
			}
		}
		return true;
	}



	public void onResume(){
		super.onResume();
		/*Bundle b = this.getIntent().getExtras();
		if(!b.isEmpty())
		{
			if(b.containsKey("ggnumber"))
			{
				this.ggnumber = b.getString("ggnumber");
	    		Log.i("[Tab]onReasume, przyjalem ggnumber: ",this.ggnumber);
			}
		}
		int cos = 123;*/
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		//Toast.makeText(getApplicationContext(), "onPause() "+ggnumber, Toast.LENGTH_SHORT).show();
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
		//Toast.makeText(getApplicationContext(), "onDestroy()"+ggnumber, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub		
		super.onStop();
		//doUnbindService();
		//Toast.makeText(getApplicationContext(), "onStop()"+ggnumber, Toast.LENGTH_SHORT).show();
	}

	OnClickListener listener = new OnClickListener() {
		@SuppressWarnings("unchecked")
		public void onClick(View v) {
			Message msg = Message.obtain(null,Common.CLIENT_SEND_MESSAGE, 0, 0);
			if(konferenciGG != null)
				msg = Message.obtain(null,Common.CLIENT_SEND_CONFERENCE_MESSAGE, 0, 0);
			Bundle wysylany = new Bundle();
			wysylany.putString("text", et.getText().toString());
			if(konferenciGG != null)
				wysylany.putStringArrayList("konferenciGG", (ArrayList<String>)konferenciGG.clone());
			else
			{
				String ggnumber = Chat.tabHost.getCurrentTabTag();
			    //wysylany.putInt("ggnumber", Integer.parseInt(getNumber(ggnumber)));
				wysylany.putInt("ggnumber", Integer.parseInt(ggnumber));
			}
			
			//tv.append(Html.fromHtml("<b><FONT COLOR=\"GREEN\">"+"Ja"+"</FONT></b>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(System.currentTimeMillis())+"<//FONT><br />")));
			//tv.append(et.getText().toString() + "\n");			
			efAdapter.addItem(new MessageClass(System.currentTimeMillis(),"-1","Ja",et.getText().toString()));
			efAdapter.notifyDataSetChanged();
			l1.setSelection(efAdapter.getCount());
			et.setText("");
			msg.setData(wysylany);
			try
			{
				mService.send(msg);				
			}
			catch(Exception e)
			{
				Log.e("Blad Tab",""+e.getMessage());
			}
		}
	};
	
	//zdarzenia zwiazane z wyborem opcji menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		//TabActivity tabActiv = (TabActivity) getParent();
		Chat tabActiv = (Chat) getParent();
		TabHost tabHost = ((TabActivity) getParent()).getTabHost();
		switch (item.getItemId())
		{
			case R.id.ZamknijOkno01:			
				Log.i("[Tab]OptionsMenu", "Zamknij");
				//if(tabHost.getTabWidget().getChildCount() > 1)
				//{
					int indeksZamykanej = tabHost.getCurrentTab();
					if(konferenciGG == null)
						tabActiv.hiddenTabs.add(ggnumber);
					else
						tabActiv.hiddenTabs.add(konferenciWBazie);
					tabHost.getCurrentTabView().setVisibility(View.GONE);					
					TextView asd = (TextView)tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).findViewById(android.R.id.title);
					//tabActiv.savedTabs.remove(asd.getText());
					tabActiv.savedTabs.remove(tabHost.getCurrentTabTag());
					asd.setText("");
					//finish();
					onStop();
					int liczbaZakladek = tabHost.getTabWidget().getChildCount();
					int iteratorPetli =0;
					for (iteratorPetli=0; iteratorPetli<liczbaZakladek ; iteratorPetli++)
			        {
						TextView textViewWLayoutcieTaba = (TextView)tabHost.getTabWidget().getChildAt(iteratorPetli).findViewById(android.R.id.title);
			        	//tabs += textViewWLayoutcieTaba.getText()+"~";
						String headerText = textViewWLayoutcieTaba.getText().toString();
						if(!headerText.equals(""))
						{
							break;
						}
			        }
					if(iteratorPetli == liczbaZakladek)
						tabActiv.finish();
						//finish();
					else
					{
						/*if(indeksZamykanej != 0)
							tabHost.setCurrentTab(0);
						else
							tabHost.setCurrentTab(1);*/
						tabHost.setCurrentTabByTag(tabActiv.savedTabs.get(0));
					}
				//}
				//else
				//	finish();		
				break;
			case R.id.photo:
				try
				{
					Intent i =  new Intent(this,CameraSurface.class);
					i.putExtra("GGNumber",this.ggnumber);
					startActivity(i);
				}catch(Exception e)
				{
					Log.e("Tab z Photo",""+e.getMessage());
				}
				break;
		}
		return false;
	}
	
	/*OnKeyListener keyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			if(keyCode == (KeyEvent.getMaxKeyCode()+1))
			{
				//Tab.this.
				Log.i("[Tab]keyListener", "wybrano zamknij karte");
				finish();
			}
			return false;
		}
		// TODO Auto-generated method stub
		
	};*/
	
	
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what) {
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("Zarejestrowano Tab","Received: "+msg.what);
                	//wyslanie do serwisu wiadomosci, ze pobierana jest lista kontaktow
                	break;
                case Common.CLIENT_RECV_MESSAGE:
                	Bundle odebrany = msg.getData();
                	//int num = odebrany.getInt("num");
                	//int seq = odebrany.getInt("seq");
                	//byte [] tresc = odebrany.getByteArray("tresc");                	
                	String wiadomoscOd = odebrany.getString("wiadomoscOd");
                	String konferenciDB = odebrany.getString("konferenci");
                	String wiadomoscOdN = odebrany.getString("wiadomoscOdName");
                	//sprawdzenie, czy przyszla wiadomosc konferencyjna
                	if(konferenciDB != null)
                	{
                		//jesli konferencyjna, czy nalezy do tej zakladki
                		if(!konferenciWBazie.equals(konferenciDB))
                			break;
                	}
                	//jesli nie konferencyjna, to czy nalezy do tej zakladki
                	else if(!wiadomoscOd.equalsIgnoreCase(Tab.this.ggnumber))
                		break;
                	String tresc = odebrany.getString("tresc");
                	String przyszlaO = odebrany.getString("przyszlaO");
                	    	
                	//String tmp = tresc.toString();
                	//Log.i("Odebralem wiadomosc od Servicu", Integer.toString(num) + " " +Integer.toString(seq));
                	//Tab.this.tv.setBackgroundColor(R.color.conctactbookdown);
                	//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+wiadomoscOd+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(System.currentTimeMillis()))+"<//FONT><br />"));
                	//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+wiadomoscOdN+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(System.currentTimeMillis()))+"<//FONT><br />"));                	
                	//tv.append(tresc+"\n");
                	
                	//Linkify.addLinks(tv, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                	//Tab.this.tv.append(""+przyszlaO + "\n" + tresc + "\n");
                	//String przyszlaO = odebrany.getString("przyszlaO");                	                  	
                	
					efAdapter.addItem(new MessageClass(System.currentTimeMillis(),wiadomoscOd,wiadomoscOdN,tresc));
					efAdapter.notifyDataSetChanged();
					//przesuniecie listy na sam dol pod warunkiem, ze dotad lista
					//ustawiona byla na samym dole.
					//Jesli lista byla gdzie wyzej, to nie zostanie przewinieta
					//na sam dol. To na wypadek, gdyby ktos przegladal rozmowe,
					//a ktos by do niego pisal, to zeby lista caly czas mu nie zjezdzala
					//na sam dol.
					//ewentualnie w tym warunku bedzie mozna tez ustawic, zeby zjezdzal na
					//sam dol listy, jesli to uzytkownik wysyla wiadomosc.
					if(l1.getLastVisiblePosition() >= (efAdapter.getCount()-3))
						l1.setSelection(efAdapter.getCount());
                	//tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+wiadomoscOdN+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(System.currentTimeMillis()))+"<//FONT><br />"));                	
                	//tv.append(tresc+"\n");
                	//Linkify.addLinks(tv, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                	Log.i("[Tab]Odebralem wiadomosc od Serwisu", tresc);
                	Log.i("[Tab]Od numeru", wiadomoscOd);
                	Log.i("[Tab]O godzinie", przyszlaO);
                  	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    public String getNumber(String header)
    {
    	String [] tmp = header.split("-");
    	return tmp[1];
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
        bindService(new Intent(Tab.this, 
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
