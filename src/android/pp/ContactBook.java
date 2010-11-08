package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ContactBook extends ExpandableListActivity{

	boolean mIsBound;
	String gglista;
	SIMPLEContactBookList contactBookFull;
	List<List<ViewableContacts>> contactsExpandableList;
	List<ViewableGroups> groupsExpandableList;
	private static final int DIALOG_STATUS = 1;
	
	AlertDialog alertDialog;

	//Adapter utrzymujacy dane z listy kontaktow
	MyExpandableListAdapter mAdapter;
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contactbook);
		//zbindowanie aktywnosci do serwisu
		doBindService();
		
        //Ustawienie adaptera z danymi listy kontaktow
        //mAdapter = new MyExpandableListAdapter(getApplicationContext());
		mAdapter = new MyExpandableListAdapter(getApplicationContext());
        setListAdapter(mAdapter);
		
		/* Wyswietl liste statusow */
		ImageButton statusButton = (ImageButton) findViewById(R.id.ImageButton01);
        statusButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_STATUS);
            	//int wybrany = alertDialog.getListView().getCheckedItemPosition();
            	//Toast.makeText(getApplicationContext(), "wybrano", wybrany).show();
            }
        });		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.contactbookmenu, menu);
		return true;
	}
	
	//zdarzenia zwiazane z wyborem opcji menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	switch (item.getItemId())
	{
		case R.id.Import01:			
			//wyslanie do serwisu wiadomosci, ze importowana jest lista kontaktow    		
        	Message msg2 = Message.obtain(null,Common.CLIENT_GET_CONTACTBOOK, 0, 0);
    		try
    		{
    			mService.send(msg2);
    		}catch(Exception excMsg)
    		{
    			Log.e("Blad","Blad!!!!\n"+excMsg.getMessage());
    		}
			return true;
		case R.id.End02:
			moveTaskToBack(true);
			break;
		//Moreitemsgohere(ifany)...
	}
	return false;
	}
	
	//zdarzenia realizowane po wywolaniu metody showDialog(id); 
	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_STATUS:
            //return new AlertDialog.Builder(ContactBook.this)
        	return alertDialog = new AlertDialog.Builder(ContactBook.this)
                .setTitle("Status")
                .setItems(new String[]{"Dostepny","Niewidoczny","Niedostepny"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        /* User clicked so do some stuff */
                        String[] items = new String[]{"Dostepny","Niewidoczny","Niedostepny"};
                        /*new AlertDialog.Builder(ContactBook.this)
                                .setMessage("Status: " + items[which])
                                .show();*/
                    }
                })
                .create();
            //w tym przypadku nie jest potrzebny break na koncu, bo w case jest zwracany obiekt
        	//break;
        }
        
        return null;
	}
	
	//akcja na klikniecie danego kontaktu na liscie
	@Override
	public boolean onChildClick(ExpandableListView parent,
            View v, int groupPosition, int childPosition,
            long id) {
		
		Toast.makeText(this.getApplicationContext(), "username: "+((TextView)v.findViewById(R.id.username)).getText()+" grupa: "+groupPosition+" podgrupa: "+childPosition, 2000).show();
		Intent intent = new Intent(this.getApplicationContext(), Chat.class);
		intent.putExtra("username",((TextView)v.findViewById(R.id.username)).getText());
		
		try
		{
			startActivity(intent);
		}
		catch(Exception e)
		{
			Log.i("ContactBook",""+e.getMessage());
		}
		return false;
    }

	public void prepareContactBook(String xmlList)
    {
    	Serializer serializer = new Persister();
		//false na ko�cu odpowiada za ignorowanie elementow w pliku XML ktorych
		//nie ma zadeklarowanych w klasie do ktorej wczytuje XMLa. Gdyby GG dorzucilo
		//jakies pole do listy kontaktow, to Gandu je zignoruje i wczyta te pola,
		//ktore ma zadeklarowane w klasie z lista kontaktow.
		try {
			this.contactBookFull = serializer.read(SIMPLEContactBookList.class, xmlList, false);
			sortContactListSIMPLE(this.contactBookFull);
			this.contactsExpandableList = new ArrayList<List<ViewableContacts>>();
			this.groupsExpandableList = new ArrayList<ViewableGroups>();
			createExpandableAdapter(this.contactBookFull, this.contactsExpandableList, this.groupsExpandableList);
		} catch (Exception excSimp) {
			Log.e("SIMPLE Error",excSimp.getMessage());
		}
    }
	
	//Posortowanie listy kontaktow przed jej prezentacja
	private void sortContactListSIMPLE(SIMPLEContactBookList unsortedList)
	{
	  	try
	  	{
	  		Collections.sort(unsortedList.A1Groupsy.Groups);
	  		Collections.sort(unsortedList.A2Contactsy.Contacts);
		}
		catch(Exception excSortG)
		{
			;
		}
	}
	
	private void createExpandableAdapter(SIMPLEContactBookList gcl, List<List<ViewableContacts>> kontaktyExp, List<ViewableGroups> grupyExp) 
	{
		for(int i=0; i<gcl.A1Groupsy.Groups.size(); i++)
		{
			ViewableGroups nowaGrupa = new ViewableGroups();
			nowaGrupa.name = gcl.A1Groupsy.Groups.get(i).A2Name;
			nowaGrupa.groupid = gcl.A1Groupsy.Groups.get(i).A1Id;
			grupyExp.add(nowaGrupa);
			
			kontaktyExp.add(new ArrayList<ViewableContacts>());
		}
		for(int i=0; i<gcl.A2Contactsy.Contacts.size(); i++)
		{
			for(int j=0; j<gcl.A2Contactsy.Contacts.get(i).AB5Groups.Groups.size(); j++)
			{
				String grupaDoKtorejDodacKontakt = gcl.A2Contactsy.Contacts.get(i).AB5Groups.Groups.get(j);
				ViewableGroups szukana = new ViewableGroups();
				szukana.groupid = grupaDoKtorejDodacKontakt;
				int indeksTab_kontaktyExp = Collections.binarySearch(grupyExp, szukana, null);
				ViewableContacts dodawany = new ViewableContacts();
				//kontakt GG musi miec podany conajmniej showName oraz
				//GGNumber lub MobilePhone lub HomePhone lub Email
				if(gcl.A2Contactsy.Contacts.get(i).AA2GGNumber != null)
				{
					try
					{
						dodawany.GGNumber = Integer.parseInt(gcl.A2Contactsy.Contacts.get(i).AA2GGNumber);
					}catch(Exception ExcAdd1)
					{
						Log.e("ExcAdd1", ExcAdd1.getMessage());
					}
				}
				if(gcl.A2Contactsy.Contacts.get(i).AA4MobilePhone != null)
					dodawany.MobilePhone = gcl.A2Contactsy.Contacts.get(i).AA4MobilePhone;
				if(gcl.A2Contactsy.Contacts.get(i).AA5HomePhone != null)
					dodawany.HomePhone = gcl.A2Contactsy.Contacts.get(i).AA5HomePhone;
				if(gcl.A2Contactsy.Contacts.get(i).AA6Email != null)
					dodawany.Email = gcl.A2Contactsy.Contacts.get(i).AA6Email;
				dodawany.showName = gcl.A2Contactsy.Contacts.get(i).AA3ShowName;
				kontaktyExp.get(indeksTab_kontaktyExp).add(dodawany);
			}
		}			
    }
	
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i("ganduClient","Odebralem"+msg.what);
            switch (msg.what) {
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("ContactBook","Received: "+msg.what);
                	//wyslanie do serwisu wiadomosci, ze pobierana jest lista kontaktow
                	break;
                case Common.FLAG_CONTACTBOOK:
                	Bundle odebrany = msg.getData();
                	gglista = odebrany.getString("listaGG");
                	prepareContactBook(gglista);
                	mAdapter.setAdapterData(groupsExpandableList, contactsExpandableList);
                	for(int parent=0; parent<groupsExpandableList.size(); parent++)
        				getExpandableListView().expandGroup(parent);
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
        bindService(new Intent(ContactBook.this, 
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
