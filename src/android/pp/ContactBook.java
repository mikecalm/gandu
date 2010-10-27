package android.pp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.Transformer;

import android.R.integer;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ContactBook extends ExpandableListActivity{

	boolean mIsBound;
	String gglista;
	SIMPLEContactBookList contactBookFull;
	List<List<ViewableContacts>> contactsExpandableList;
	List<ViewableGroups> groupsExpandableList;
	private static final int DIALOG_STATUS = 1;
		
	//List groupData; 
	//List childData;
	//SimpleExpandableListAdapter expListAdapter;
	AlertDialog alertDialog;

	//Adapter utrzymujacy dane z listy kontaktow	
	//MyExpandableListAdapter mAdapter;
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
		case R.id.item01:			
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
        return false;
    }
	
	/**
	  * Creates the group list out of the groups[] array according to
	  * the structure required by SimpleExpandableListAdapter. The resulting
	  * List contains Maps. Each Map contains one entry with key "groupName" and
	  * value of an entry in the groups[] array.
	  */
		/*private List createGroupList(ArrayList<GroupContact> gcl) {
			ArrayList result = new ArrayList();
			for(GroupContact gc : gcl)
			{
				HashMap m = new HashMap();
			    m.put( "groupName",gc.gp.getName());
				result.add( m );
			}
		  return (List)result;
	    }*/
		
		/*private String[] createGroupArray(ArrayList<GroupContact> gcl) {
			String[] result = new String[gcl.size()];
			for(int i=0; i<result.length; i++)
			{
				result[i] = gcl.get(i).gp.getName();
			}
		  return result;
	    }*/

	/**
	  * Creates the child list out of the users[] array according to the
	  * structure required by SimpleExpandableListAdapter. The resulting List
	  * contains one list for each group. Each such second-level group contains
	  * Maps. Each such Map contains two keys: "contactname" is the name of the
	  * contact and "description" is the users description.
	  */
	  /*private List createChildList(ArrayList<GroupContact> gcl) {
		ArrayList result = new ArrayList();
		for(GroupContact gc : gcl)
		{
			ArrayList secList = new ArrayList();
			for(int i = 0; i < gc.ctt_list.size(); i++)
			{
				HashMap child = new HashMap();
				child.put( "username", gc.ctt_list.get(i).getShowName() );
				child.put( "description", gc.ctt_list.get(i).getGGNumber() );
				secList.add( child );
			}
			result.add( secList );
		}
		return result;
	  }*/
	  
	  /*private String[][] createChildArray(ArrayList<GroupContact> gcl) {
			String[][] result = new String[gcl.size()][];
			for(int i=0; i<result.length; i++)
			{
				result[i] = new String[gcl.get(i).ctt_list.size()];
				for(int j=0; j<result[i].length; j++)
				{
					result[i][j] = gcl.get(i).ctt_list.get(j).getShowName();
				}
			}
			return result;
		  }*/
	  
	//Posortowanie listy kontaktow przed jej prezentacja
	/*private void sortContactList(XMLParsedDataSet unsortedList)
	{
	  	try
	  	{
	  		Collections.sort(unsortedList.GCList);
	  		for (GroupContact gc : unsortedList.GCList)
	  		{
	  			gc.sortContacts();
	  		}
		}
		catch(Exception excSortG)
		{
			;
		}
	}*/

	public void prepareContactBook(String xmlList)
    {
    	Serializer serializer = new Persister();
		//false na koñcu odpowiada za ignorowanie elementow w pliku XML ktorych
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
			;
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
				dodawany.GGNumber = Integer.parseInt(gcl.A2Contactsy.Contacts.get(i).AA2GGNumber);
				dodawany.showName = gcl.A2Contactsy.Contacts.get(i).AA3ShowName;
				kontaktyExp.get(indeksTab_kontaktyExp).add(dodawany);
			}
		}			
    }
	/*
	private CopyOfViewableContacts[][] createChildArray_expandable(CopyOfContactBook gcl) {
		CopyOfViewableContacts[][] result = new CopyOfViewableContacts[gcl.A1Groupsy.Groups.size()][];
		for(int i=0; i<result.length; i++)
		{
			//result[i] = new Array();
			List<CopyOfViewableContacts> resultList = new ArrayList<CopyOfViewableContacts>();
			for(int j=0; j<result[i].length; j++)
			{
				result[i][j] = gcl.get(i).ctt_list.get(j).getShowName();
			}
		}
		return result;
	  }*/	
	
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
                	
                	//!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!
                	//saveOnSDCard(gglista); 
                	prepareContactBook(gglista);
                	//!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!
                	
                	//XMLContactBook  xcb = new XMLContactBook();
                	//XMLParsedDataSet xpds = xcb.xmlparse(gglista);
                	
                	//Posortowanie listy kontaktow przed jej prezentacja
                	//sortContactList(xpds);
                	//String[] grupy = createGroupArray(xpds.GCList);
                	//String[][] kontakty = createChildArray(xpds.GCList);
        			//mAdapter.setAdapterData(grupy, kontakty);
                	mAdapter.setAdapterData(groupsExpandableList, contactsExpandableList);
                	//mAdapter.prepareContactBook(gglista);
        			//mAdapter.notifyDataSetChanged();
        			//for(int parent=0; parent<grupy.length; parent++)
                	for(int parent=0; parent<groupsExpandableList.size(); parent++)
        				getExpandableListView().expandGroup(parent);
        			
        			//getExpandableListAdapter()
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
        		//GanduService.class), mConnection, Context.);
        
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
    
    public void saveOnSDCard(String tmp)
    {
 	   boolean mExternalStorageAvailable = false;
 	   boolean mExternalStorageWriteable = false;
 	   String state = Environment.getExternalStorageState();

 	   if (Environment.MEDIA_MOUNTED.equals(state)) {
 	       // We can read and write the media
 	       mExternalStorageAvailable = mExternalStorageWriteable = true;
 	   } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
 	       // We can only read the media
 	       mExternalStorageAvailable = true;
 	       mExternalStorageWriteable = false;
 	   } else {
 	       // Something else is wrong. It may be one of many other states, but all we need
 	       //  to know is we can neither read nor write
 	       mExternalStorageAvailable = mExternalStorageWriteable = false;
 	      
 		
 	   }
 	   String extStorageDirectory = Environment.getExternalStorageDirectory().toString() ;
 	   File file = new File(extStorageDirectory, "tmp.xml");
 	   
 	   if(mExternalStorageWriteable)
 	   {
 		   //!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!
 		   
 		   	/*Serializer serializer = new Persister();
 		   	CopyOfContact example = new CopyOfContact();
 		   	CopyOfGroup exampleGroup = new CopyOfGroup();
		   	CopyOfContactBook examplefinal = new CopyOfContactBook();
		   	examplefinal.A2Contactsy = new CopyOfContacts();
		   	examplefinal.A2Contactsy.Contacts = new ArrayList<CopyOfContact>();
		   	examplefinal.A1Groupsy = new CopyOfGroups();
		   	examplefinal.A1Groupsy.Groups = new ArrayList<CopyOfGroup>();
		   	examplefinal.A2Contactsy.Contacts.add(example);
		   	examplefinal.A2Contactsy.Contacts.add(example);
		   	examplefinal.A1Groupsy.Groups.add(exampleGroup);
		   	examplefinal.A1Groupsy.Groups.add(exampleGroup);
	      	File result = new File(extStorageDirectory, "result.xml");*/

			try {
				//serializer.write(examplefinal, result);
				
				Serializer serializer = new Persister();

				//false na koñcu odpowiada za ignorowanie elementow w pliku XML ktorych
				//nie ma zadeklarowanych w klasie do ktorej wczytuje XMLa. Gdyby GG dorzucilo
				//jakies pole do listy kontaktow, to Gandu je zignoruje i wczyta te pola,
				//ktore ma zadeklarowane w klasie z lista kontaktow.
				//CopyOfContactBook nowy = serializer.read(CopyOfContactBook.class, result, false);
				/*
				CopyOfContactBook nowy2 = serializer.read(CopyOfContactBook.class, tmp, false);
				sortContactListSIMPLE(nowy2);
				List<List<CopyOfViewableContacts>> kontaktyExp = new ArrayList<List<CopyOfViewableContacts>>();
				List<CopyOfViewableGroups> grupyExp = new ArrayList<CopyOfViewableGroups>();
				createExpandableAdapter(nowy2, kontaktyExp, grupyExp);
				*/
				
				//Wyszukiwanie kontaktu na liscie nowy2.A2Contactsy.Contacts
				//po showName kontaktu.
				//Jesli kontaktu o danym showName nie ma na liscie, to zostanie zwrocona
				//wartosc ujemna (-(X)-1), gdzie X to indeks na liscie pod ktorym
				//moglby zostac umieszczony element tak, aby lista ciagle pozostawala
				//posortowana rosnaco.
				//UWAGA
				//Przed wykonaniem Collections.binarySearch lista nowy2.A2Contactsy.Contacts
				//musi zostac posortowana rosnaco wedlug showName: 
				//sortContactListSIMPLE(CopyOfContactBook kontakty);
				//W przeciwnym wypadku wynik funkcji binarySearch nie gwarantuje poprawnego wyniku.
				//UWAGA
				//Zrodlo:
				//http://download.oracle.com/javase/6/docs/api/java/util/Collections.html#binarySearch%28java.util.List,%20T,%20java.util.Comparator%29
				
				/*
				CopyOfContact szukany = new CopyOfContact();
				szukany.AA3ShowName = "Blip.pl";
				int miejsce = Collections.binarySearch(nowy2.A2Contactsy.Contacts,szukany,null);
				String numerznalezionego = null;
				if(miejsce>0)
					numerznalezionego = nowy2.A2Contactsy.Contacts.get(miejsce).AA2GGNumber;
				
				//Collections.binarySearch(nowy2.A2Contactsy.Contacts, "Blip");
				//nowy2.A2Contactsy.Contacts
				File result2 = new File(extStorageDirectory, "pobranaIZinterpretowana.xml");
				//serializer2.write(nowy2,result2);
				serializer.write(nowy2,result2);
				*/
				//nowy.Avatars.Avatars.add("ADAD");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!!TEST SIMPLE!
 		   
	 	   try {
		 		FileOutputStream fos = new FileOutputStream(file);
		 		byte [] buffer = tmp.getBytes("UTF-8");
		 		fos.write(buffer);
		 		fos.flush();
		 		fos.close();
	 		
		 	} catch (Exception e) {
		 		// TODO Auto-generated catch block
		 		e.printStackTrace();
		 	}
 	   }
    }
}
