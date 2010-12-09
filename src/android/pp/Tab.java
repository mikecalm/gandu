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
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class Tab extends Activity{
	/** Messenger for communicating with service. */
    Messenger mService = null;
    boolean mIsBound;
    EditText et;
    TextView tv;
    String ggnumber = "";
    ArrayList<String> konferenciGG = null;
    String konferenciWBazie = "";
    
    ArchiveSQLite archiveSQL;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.tab_content);
		Button btn = (Button) findViewById(R.id.ok);
		et = (EditText) findViewById(R.id.entry);
		tv = (TextView) findViewById(R.id.lblComments);
		//tv.setMovementMethod(ScrollingMovementMethod.getInstance());
		tv.setText("");
		tv.setTextSize(18);
		btn.setOnClickListener(listener);
		//doBindService();
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;
		//btn.setOnKeyListener(keyListener);
		//getParent().
		
		Bundle b = this.getIntent().getExtras();
		if(b != null)
		{
			if(b.containsKey("ggnumber"))
			{
				this.ggnumber = b.getString("ggnumber");
	    		Log.i("[Tab]onCreate, przyjalem ggnumber: ",this.ggnumber);
			}
			if(b.containsKey("konferenciGG") && b.containsKey("konferenciWBazie"))
			{
				konferenciGG = new ArrayList<String>();
				konferenciGG = b.getStringArrayList("konferenciGG");
				konferenciWBazie = b.getString("konferenciWBazie");
			}
		}
		
		archiveSQL = new ArchiveSQLite(this.getApplicationContext());
		//zakladka z rozmowa konferencyjna
		if(konferenciGG != null)
		{
			//TODO odczyt z bazy starych i nieprzeczytanych wiadomosci konferencyjnych
			;
		}
		//zakladka z rozmowa niekonferencyjna
		else
		{
	    	Log.i("[Tab"+ggnumber+"]START SQL","Odczyt nieprzeczytanych wiadomosci.");
	    	Cursor wynikSQLLast = archiveSQL.readLastMessagesFrom(Integer.parseInt(ggnumber),3600);
			ArrayList<String> ostatnie = archiveSQL.showLastMessages(wynikSQLLast,Integer.parseInt(ggnumber));
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
					tv.append(Html.fromHtml("<b><FONT COLOR=\"GREEN\">"+"Ja"+"</FONT></b>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
					tv.append(wiadomosc + "\n");
				}
				else
				{
					String wiadomoscIData = ostatnie.get(i);
					int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
					String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
					Long dataEpoch = Long.parseLong(data);
					String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1);
					tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+ggnumber+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
					tv.append(wiadomosc + "\n");
				}
			}
	    	
			Cursor wynikSQL = archiveSQL.readUnreadMessagesFrom(Integer.parseInt(ggnumber));
			ArrayList<String> nieprzeczytane = archiveSQL.showUnreadMessages(wynikSQL,Integer.parseInt(ggnumber));
			for(int i=0; i<nieprzeczytane.size(); i++)
			{
				String wiadomoscIData = nieprzeczytane.get(i);
				int indeksPierwszegoSrednika = wiadomoscIData.indexOf(";");
				String data = wiadomoscIData.substring(0, indeksPierwszegoSrednika);
				Long dataEpoch = Long.parseLong(data);
				String wiadomosc = wiadomoscIData.substring(indeksPierwszegoSrednika+1);
				tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+ggnumber+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(dataEpoch*1000L))+"<//FONT><br />"));
				tv.append(wiadomosc + "\n");
			}
			Log.i("[Tab"+ggnumber+"]KONIEC SQL","Odczyt nieprzeczytanych wiadomosci.");
		}
		
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
		public void onClick(View v) {
			Message msg = Message.obtain(null,Common.CLIENT_SEND_MESSAGE, 0, 0);
			Bundle wysylany = new Bundle();
			wysylany.putString("text", et.getText().toString());
			String ggnumber = Chat.tabHost.getCurrentTabTag();
		    wysylany.putInt("ggnumber", Integer.parseInt(getNumber(ggnumber)));
			
			//wysylany.putString("ggnumber",getNumber());
			//Calendar c = Calendar.getInstance();
			//tv.setBackgroundColor(R.color.conctactbookup);
			//tv.append(c.getTime().toString() + "\n" + et.getText().toString() + "\n");
		    tv.append(Html.fromHtml("<b><FONT COLOR=\"GREEN\">"+"Ja"+"</FONT></b><hr>"+"\n"));
			tv.append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(System.currentTimeMillis()) + "\n" + et.getText().toString() + "\n");			
			//wysylany.putString("hasloGG" , ggPasswordEdit.getText().toString());
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
					tabActiv.hiddenTabs.add(ggnumber);
					tabHost.getCurrentTabView().setVisibility(View.GONE);					
					TextView asd = (TextView)tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).findViewById(android.R.id.title);
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
						if(indeksZamykanej != 0)
							tabHost.setCurrentTab(0);
						else
							tabHost.setCurrentTab(1);
					}
				//}
				//else
				//	finish();		
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
                	if(!wiadomoscOd.equalsIgnoreCase(Tab.this.ggnumber))
                		break;
                	String tresc = odebrany.getString("tresc");
                	String przyszlaO = odebrany.getString("przyszlaO");
                	    	
                	//String tmp = tresc.toString();
                	//Log.i("Odebralem wiadomosc od Servicu", Integer.toString(num) + " " +Integer.toString(seq));
                	//Tab.this.tv.setBackgroundColor(R.color.conctactbookdown);
                	tv.append(Html.fromHtml("<FONT COLOR=\"RED\">"+wiadomoscOd+"</FONT>"+"<FONT COLOR=\"WHITE\">"+(new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(System.currentTimeMillis()))+"<//FONT><br />"));
                	tv.append(tresc+"\n");
                	//Tab.this.tv.append(""+przyszlaO + "\n" + tresc + "\n");
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
