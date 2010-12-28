package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URLEncodedUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class ContactBook extends ExpandableListActivity{

	boolean mIsBound;
	boolean connectedToGGServer;
	String mojNumer = "";
	String gglista = "";
	SIMPLEContactBookList contactBookFull;
	List<List<ViewableContacts>> contactsExpandableList;
	List<ViewableGroups> groupsExpandableList;
	public SharedPreferences prefs;
	public SharedPreferences.Editor editor;
	private static final int DIALOG_STATUS = 1;
	EditText statusDescription;
	ImageButton statusButton;
	int ustawionyStatus = 0;
	static final private int NEW_CONTACT_ACTIVITY_RESULT = 0;
	static final private int NEW_ANDROID_EXPLORER_ACTIVITY_RESULT = 1;
	public GetStatuses gs = new GetStatuses();
	ArrayList<String> itemsy = new ArrayList();
	
	AlertDialog alertDialog;
	
	public NotificationManager mNM;

	//Adapter utrzymujacy dane z listy kontaktow
	MyExpandableListAdapter mAdapter;
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    
    //variable which controls the ping thread and initialThread
    private ConditionVariable mCondition;
    private ConditionVariable mInitial;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		itemsy.add("Dostepny");
		itemsy.add("Zaraz wracam");
		itemsy.add("Niewidoczny");
		itemsy.add("Niedostepny");
        mCondition = new ConditionVariable(false);
        mInitial = new ConditionVariable(false);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancel(-1);
		/*Bundle b = this.getIntent().getExtras();
		if(b != null)
		{
			if(b.containsKey("mojNumer"))
			{
				this.mojNumer = b.getString("mojNumer");
			}
		}*/
        
		setContentView(R.layout.contactbook);
		//zbindowanie aktywnosci do serwisu
		//doBindService();
		Intent intent = new Intent(getApplicationContext(), GanduService.class);
		getApplicationContext().bindService(intent,mConnection,1);
		mIsBound = true;
		
        //Ustawienie adaptera z danymi listy kontaktow
		mAdapter = new MyExpandableListAdapter(getApplicationContext());
        setListAdapter(mAdapter);
        
        registerForContextMenu(this.getExpandableListView());
        
        //prefs = getPreferences(0);
        prefs = getSharedPreferences("otwarteZakladki", 0);
		editor = prefs.edit();
		
		/* Wyswietl liste statusow */
		statusDescription = (EditText) findViewById(R.id.EditText01);
		
		statusButton = (ImageButton) findViewById(R.id.ImageButton01);
		
		//dodanie akcji na wcisniecie przycisku "Done" po wpisaniu tekstu
		//w pole opisu
		statusDescription.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// TODO Auto-generated method stub
				if(actionId == EditorInfo.IME_ACTION_DONE)
				{
					Message msg2 = Message.obtain(null,Common.CLIENT_CHANGE_STATUS, 0, 0);
					String[] items = new String[]{"Dostepny","Zaraz wracam","Niewidoczny","Niedostepny"};
                    Bundle wysylany = new Bundle();
        			wysylany.putString("status", items[ustawionyStatus]);
        			if(statusDescription.getText() != null)
        				wysylany.putString("opisStatusu", statusDescription.getText().toString());
        			else
        				wysylany.putString("opisStatusu", "");
        			msg2.setData(wysylany);
    	    		try
    	    		{
    	    			mService.send(msg2);
    	    		}catch(Exception exc)
    	    		{
    	    			Log.e("ContactBook","Blad przypisania akcji DONE edittextowi z opisem");
    	    		}
				}
				return false;
			}
		});
		
        statusButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	//wygenerowanie akcja na zdarzenie wybrania przycisku zmiany statusu
                showDialog(DIALOG_STATUS);
            	//int wybrany = alertDialog.getListView().getCheckedItemPosition();
            	//Toast.makeText(getApplicationContext(), "wybrano", wybrany).show();
            }
        });
        
		//uruchom watek oczekujacy na polaczenie z serwisem, 
		//i pobierajacym z niego mojNumer, aktualny status i opis
		Thread initialThread = new Thread(null, initialTask, "initialService");
        mInitial = new ConditionVariable(false);
        initialThread.start();
        //zaladowanie listy kontaktow z pliku, jesli taki istnieje
        //jest w IncomingHandler w Common.CLIENT_SET_INITIAL_INFO.
        //takie info od serwisu przychodzi w reakcji na instrukcje powyzej
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
        }
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub		
		super.onStop();
	}
	
	 /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to
     *                    startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode,
		Intent data) {
        // You can use the requestCode to select between multiple child
        // activities you may have started.  Here there is only one thing
        // we launch.
        if (requestCode == NEW_CONTACT_ACTIVITY_RESULT) {

            // This is a standard resultCode that is sent back if the
            // activity doesn't supply an explicit result.  It will also
            // be returned if the activity failed to launch.
            if (resultCode == RESULT_CANCELED) {
                Log.i("NewContactResult", "RESULT_CANCELED");

            // Our protocol with the sending activity is that it will send
            // text in 'data' as its result.
            }
            //RESULT_OK
            else 
            {
            	Log.i("NewContactResult", Integer.toString(resultCode));
                if (data != null) {
                    //text.append(data.getAction());
                	String new_numerGG = data.getStringExtra("numerGG");
                	String new_nazwaKontaktu = data.getStringExtra("nazwaKontaktu");
                	String new_email = data.getStringExtra("email");
                	String new_komorkowy = data.getStringExtra("komorkowy");
                	String new_stacjonarny = data.getStringExtra("stacjonarny");
                	String new_stronaWWW = data.getStringExtra("stronaWWW");
                	String new_groupID = data.getStringExtra("grupaID");
                	//START Jesli wyedytowano kontakt
                	Boolean edit_edited = data.getBooleanExtra("edited",false);
                	String edit_oldNumerGG = data.getStringExtra("poprzedniNumer");
                	String edit_oldNazwaKontaktu = data.getStringExtra("poprzedniaNazwaKontaktu");
                	//KONIEC Jesli wyedytowano kontakt
                	
                	SIMPLEContact nowy = new SIMPLEContact();
                	if(!new_nazwaKontaktu.equals(""))
                	{
                		nowy.AA3ShowName = new_nazwaKontaktu; 
	                	//START Nowy kontakt musial miec ustawiony albo numergg albo email albo telefon(kom./stac.)
	                	if(!new_numerGG.equals(""))
	                	{
	                		nowy.AA1Guid = new_numerGG;
	                	}
	                	else if(!new_email.equals(""))
	                		nowy.AA1Guid = new_email;
	                	else if(!new_komorkowy.equals(""))
	                		nowy.AA1Guid = new_komorkowy;
	                	else
	                		nowy.AA1Guid = new_stacjonarny;
	                	//KONIEC Nowy kontakt musial miec ustawiony albo numergg albo email albo telefon(kom./stac.)
                	}
                	else
                	{                		
                		//START Nowy kontakt musial miec ustawiony albo numergg albo email albo telefon(kom./stac.)
	                	if(!new_numerGG.equals(""))
	                	{
	                		nowy.AA1Guid = new_numerGG; 
	                		nowy.AA3ShowName = new_numerGG;
	                	}
	                	else if(!new_email.equals(""))
	                	{
	                		nowy.AA1Guid = new_email;
	                		nowy.AA3ShowName = new_email;
	                	}
	                	else if(!new_komorkowy.equals(""))
	                	{
	                		nowy.AA1Guid = new_komorkowy;
	                		nowy.AA3ShowName = new_komorkowy;
	                	}
	                	else
	                	{
	                		nowy.AA1Guid = new_stacjonarny;
	                		nowy.AA3ShowName = new_stacjonarny;
	                	}
	                	//KONIEC Nowy kontakt musial miec ustawiony albo numergg albo email albo telefon(kom./stac.)
                	}
                	SIMPLEContact kopiaKontaktu = null;
                	int indeksKontaktu = 0;
                	//jesli wyedytowana kontakt, to stworz kopie pierwotnego i zapisz jego indeks
                	//ktory mial na liscie this.contactBookFull.A2Contactsy.Contacts
                	if(edit_edited)
                	{
                		kopiaKontaktu = new SIMPLEContact();
                		SIMPLEContact wyszukiwanyKontakt = new SIMPLEContact();
                		wyszukiwanyKontakt.AA3ShowName = edit_oldNazwaKontaktu;
                		indeksKontaktu = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, wyszukiwanyKontakt, null);
                		kopiaKontaktu = this.contactBookFull.A2Contactsy.Contacts.get(indeksKontaktu);
                		this.contactBookFull.A2Contactsy.Contacts.remove(indeksKontaktu);
                	}
                	//sprawdzenie, czy nie ma na liscie kontaktu o podanej nazwie
                	//GG nie dopuszcza istnienia kontaktow na liscie o takiej samej nazwie
                	if(this.contactBookFull != null)
                		if(this.contactBookFull.A2Contactsy != null)
                			if(this.contactBookFull.A2Contactsy.Contacts != null)
                				//if(this.contactBookFull.A2Contactsy.Contacts.contains(nowy))
                				if(Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, nowy, null) >= 0)
                				{
                					//jesli po wyedytowaniu i tymczasowym usunieciu pierwotnego kontaktu
                					//z listy, znaleziono kontakt o nowo podanej (wyedytowanej) nazwie
                					//to przywroc na liste pierwotny kontakt
                					if(edit_edited)
                                	{
                						this.contactBookFull.A2Contactsy.Contacts.add(indeksKontaktu, kopiaKontaktu);
                                	}
                					Toast.makeText(getApplicationContext(), "Zmien nazwe kontaktu\n"+
                							nowy.AA3ShowName+" jest juz na liscie kontaktow", Toast.LENGTH_LONG).show();
                					return;
                				}
                	
                	//tu juz mozna porzucic pierwotny obiekt kontaktu
                	kopiaKontaktu = null;
                	if(!new_numerGG.equals(""))
                		nowy.AA2GGNumber = new_numerGG;
                	else
                		nowy.AA2GGNumber = "";
                	if(!new_email.equals(""))
                		nowy.AA6Email = new_email;
                	if(!new_komorkowy.equals(""))
                		nowy.AA4MobilePhone = new_komorkowy;
                	if(!new_stacjonarny.equals(""))
                		nowy.AA5HomePhone = new_stacjonarny;
                	if(!new_stronaWWW.equals(""))
                		nowy.AA7WwwAddress = new_stronaWWW;
                	if(new_groupID != null)
                		if(new_groupID.equals("00000000-0000-0000-0000-000000000001"))
                			nowy.AC3FlagIgnored = true;
                	nowy.AC1FlagNormal = true;
                	nowy.AB7Avatars = new SIMPLEAvatars();
                	nowy.AB7Avatars.Avatars = new ArrayList<String>();
                	nowy.AB7Avatars.Avatars.add("");
    				SIMPLEContactGroups scg = new SIMPLEContactGroups();
    				ArrayList<String> grupy = new ArrayList<String>();
    				//grupy.add(this.contactBookFull.A1Groupsy.Groups.get(0).A1Id);
    				if(new_groupID == null)
    					grupy.add("00000000-0000-0000-0000-000000000000");
    				else
    					grupy.add(new_groupID);
    				scg.Groups = grupy;
    				//nowy.AB5Groups = new 
    				nowy.AB5Groups = scg;
    				//jesli zostal przekazany new_grupaName, tzn ze dodawanie
    				//nowego kontaktu zostalo wywolane z menu kontekstowego grupy
    				//czyli lista na pewno nie jest pusta
    				//if(new_grupaName == null)
    				if(new_groupID == null)    				
    				{
	    				//START jesli lista kontaktow jest pusta, 
	    				//to najpierw nalezy utworzyc pusta liste
	    				//kontaktow z grupa glowna "Moje kontakty" o ID "00000000-0000-0000-0000-000000000000"
	    				if(this.contactBookFull == null)
	    					this.contactBookFull = new SIMPLEContactBookList();
	    				if(this.contactBookFull.A1Groupsy == null)
	    					this.contactBookFull.A1Groupsy = new SIMPLEGroups();
	    				if(this.contactBookFull.A1Groupsy.Groups == null)
	    				{
	    					this.contactBookFull.A1Groupsy.Groups = new ArrayList<SIMPLEGroup>();
	    					SIMPLEGroup mojeKontakty = new SIMPLEGroup();
	    					mojeKontakty.A1Id = "00000000-0000-0000-0000-000000000000";
	    					mojeKontakty.A2Name = "Moje kontakty";
	    					mojeKontakty.A3IsExpanded = true;
	    					mojeKontakty.A4IsRemovable = false;
	    					this.contactBookFull.A1Groupsy.Groups.add(mojeKontakty);
	    				}
	    				if(this.contactBookFull.A2Contactsy == null)
	    					this.contactBookFull.A2Contactsy = new SIMPLEContacts();
	    				if(this.contactBookFull.A2Contactsy.Contacts == null)
	    					this.contactBookFull.A2Contactsy.Contacts = new ArrayList<SIMPLEContact>();    				
	    				//KONIEC jesli lista kontaktow jest pusta
    				}
    				addContactToContactBook(nowy);
    				//Jesli jestesmy polaczeni z serwerem, to
    				//po dodaniu kontaktu nalezy wyslac do serwera GG pakiet
    				//GG_ADD_NOTIFY
    				//z informacja o nowo dodanym kontakcie, aby serwer
    				//informowal nas o dostepnosci kontaktu
    				if(!new_numerGG.equals(""))
    				{
    					//jesli kontakt po wyedytowaniu ma ten sam numer GG co przed edycja,
    					//to nie trzeba wysylac do serwera GG informacji o nowym kontakcie
    					if(edit_oldNumerGG != null && edit_oldNumerGG == new_numerGG)
    						return;
	    	        	Message msg3 = Message.obtain(null,Common.CLIENT_ADD_NEW_CONTACT, 0, 0);	        
	    	    		try
	    	    		{
	    		    		Bundle wysylany = new Bundle();
	    					wysylany.putString("numerGG", new_numerGG);
	    					wysylany.putString("showNameGG", nowy.AA3ShowName);
	    					//jesli nowy kontakt dodawany jest do grupy ignorowanych
	    					//to trzeba wyslac do serwisu wiadomosc o tym, ze kontakt
	    					//jest ignorowany. Serwer GG nie bedzie przysylal nam wiadomosci
	    					//od ignorowanego kontaktu
	    					if(new_groupID != null)
	                    		if(new_groupID.equals("00000000-0000-0000-0000-000000000001"))
	                    			wysylany.putBoolean("ingorowany", true);
	    					msg3.setData(wysylany);
	    	    			mService.send(msg3);
	    	    		}catch(Exception excMsg)
	    	    		{
	    	    			Log.e("ContactBook","Blad wyslania info do serwisu o nowo dodanym kontakcie:\n"+
	    	    					excMsg.getMessage());
	    	    		}
    				}
                }
            }
        }
        else if (requestCode == NEW_ANDROID_EXPLORER_ACTIVITY_RESULT) {
        	if (resultCode == RESULT_CANCELED) {
                Log.i("NewAndroidExplorerResult", "RESULT_CANCELED");
        	}
        	else if(resultCode == RESULT_OK)
        	{
        		if (data != null) 
        		{
        			if(data.hasExtra("fileName") && data.hasExtra("filePath") && data.hasExtra("fileTo"))
        			{
	                    //text.append(data.getAction());
	                	String fileName = data.getStringExtra("fileName");
	                	String filePath = data.getStringExtra("filePath");
	                	String fileTo = data.getStringExtra("fileTo");
	                	Boolean readable = data.getBooleanExtra("readable", false);
	                	if(readable)
	                	{
		                	//wyslanie do serwisu informacji o checi wyslania pliku na na podany numer GG
			        		Message msg3 = Message.obtain(null,Common.CLIENT_SEND_FILE, 0, 0);	        
		    	    		try
		    	    		{
		    		    		Bundle wysylany = new Bundle();
		    					wysylany.putString("numerGG", fileTo);
		    					wysylany.putString("fileName", fileName);
		    					wysylany.putString("filePath", filePath);
		    					msg3.setData(wysylany);
		    	    			mService.send(msg3);
		    	    		}catch(Exception excMsg)
		    	    		{
		    	    			Log.e("ContactBook","Blad wyslania info do serwisu o wysylaniu pliku:\n"+
		    	    					excMsg.getMessage());
		    	    		}
	                	}
        			}
        		}
        	}
        }
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
    	if(v.getId()==this.getExpandableListView().getId())
    	{
    		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)menuInfo;
    		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
    		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
    		//long click na kontakt
    		if(child != -1)
    		{
    			ViewableContacts pobrany = this.contactsExpandableList
    				.get(group)
    				.get(child);
	    		menu.setHeaderTitle(pobrany.showName);
	    		SIMPLEContact szukanyPrzytrzymany = new SIMPLEContact();
	    		szukanyPrzytrzymany.AA3ShowName = pobrany.showName;
	    		String[] menuItems = {"Edytuj","Usun","Lokalizuj","Lokalizuj mnie"};
	    		if(!pobrany.GGNumber.equals(""))
	    		{
	    			//menuItems = new String[]{"Edytuj","Usun","Lokalizuj","Lokalizuj mnie","Ignoruj"};
	    			//menuItems = new String[]{"Edytuj","Usun","Lokalizuj","Lokalizuj mnie","Ignoruj","Wyslij plik"};
	    			menuItems = new String[]{"Edytuj","Usun","Lokalizuj","Lokalizuj mnie","Otwórz link z opisu","Ignoruj","Wyslij plik"};
		    		//sprawdzenie, czy przytrzymany kontakt jest ignorowany	    	
		    		int indeksSzukanegoPrzytrzymanego = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, szukanyPrzytrzymany, null);
		    		if(this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoPrzytrzymanego).AC3FlagIgnored != null)
		    		{
		    			//jesli jest, to zamiast opcji ignoruj, bedzie opcja "Nie ignoruj"
		    			if(this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoPrzytrzymanego).AC3FlagIgnored == true)
		    				menuItems[5] = "Nie ignoruj";
		    		}
	    		}
	    		for (int i = 0; i<menuItems.length; i++) {
	    			menu.add(Menu.NONE, i, i, menuItems[i]);
	    		}
    		}
    		//long click na grupe
    		else
    		{
    			ViewableGroups pobrany = this.groupsExpandableList
				.get(group);
	    		menu.setHeaderTitle(pobrany.name);
	    		String[] menuItems = {"Dodaj kontakt"};
	    		for (int i = 0; i<menuItems.length; i++) {
	    			menu.add(Menu.NONE, i, i, menuItems[i]);
	    		}
    		}
    	}
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo)item.getMenuInfo();
		String menuItemName = item.getTitle().toString();
		int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
		//akcja dla kontaktu
		if(child != -1)
		{
			ViewableContacts pobrany = this.contactsExpandableList
				.get(group)
				.get(child);
			ViewableGroups pobranyG = this.groupsExpandableList
			.get(group);
			switch(item.getItemId())
			{
				//akcja edytuj
				case 0:
					Intent intent = new Intent(this.getApplicationContext(), AddNewContact.class);
					intent.putExtra("grupaID", pobranyG.groupid);
					intent.putExtra("edited", true);
					if(pobrany.GGNumber != null)
						if(!pobrany.GGNumber.equals(""))
							intent.putExtra("poprzedniNumer", pobrany.GGNumber);
					if(pobrany.showName != null)
						if(!pobrany.showName.equals(""))
							intent.putExtra("poprzedniaNazwaKontaktu", pobrany.showName);
					if(pobrany.Email != null)
						if(!pobrany.Email.equals(""))
							intent.putExtra("poprzedniaEmail", pobrany.Email);
					if(pobrany.MobilePhone != null)
						if(!pobrany.MobilePhone.equals(""))
							intent.putExtra("poprzedniaKomorka", pobrany.MobilePhone);
					if(pobrany.HomePhone != null)
						if(!pobrany.HomePhone.equals(""))
							intent.putExtra("poprzedniaStacjonarny", pobrany.HomePhone);
					String oldWWW = null;
					SIMPLEContact poszuk = new SIMPLEContact();
					poszuk.AA3ShowName = pobrany.showName;
					int indeksNaLiscieSimple = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, poszuk, null);
					SIMPLEContact kopiakontaktSimple = this.contactBookFull.A2Contactsy.Contacts.get(indeksNaLiscieSimple);
					oldWWW = kopiakontaktSimple.AA7WwwAddress;
					if(oldWWW != null)
						if(!oldWWW.equals(""))
							intent.putExtra("poprzedniaStrona", oldWWW);
					startActivityForResult(intent,NEW_CONTACT_ACTIVITY_RESULT);
					break;
				//akcja usun
				case 1:
					String nazwaUsuwanego = pobrany.showName;
					String usuwanaGrupaID = this.groupsExpandableList.get(group).groupid;
					Boolean usuwanyKontaktJestTezWInnejGrupie = false;
					SIMPLEContact sc = new SIMPLEContact();
					sc.AA3ShowName = nazwaUsuwanego;
					int indeksSzukanegoKontaktu = Collections.binarySearch(contactBookFull.A2Contactsy.Contacts, 
							sc, null);
					//jesli usuwany kontakt nalezy tylko do jednej grupy, to zostanie w calosci usuniety ze struktury
					//this.contactBookFull.A2Contactsy.Contacts
					if(this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoKontaktu).AB5Groups.Groups.size() == 1)
						this.contactBookFull.A2Contactsy.Contacts.remove(indeksSzukanegoKontaktu);
					//w przeciwnym razie z danych kontaktu zostanie usunieta grupa z ktorej jest usuwany 
					else
					{
						usuwanyKontaktJestTezWInnejGrupie = true;
						//jesli kontakt usuwany jest z grupy ignorowani,
						//to nalezy ustawic mu odpowiednie flagi
						if(usuwanaGrupaID.equals("00000000-0000-0000-0000-000000000001"))
						{
							this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoKontaktu).AC3FlagIgnored = false;
							this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoKontaktu).AC1FlagNormal = true;
						}
						int indeksSzukanejGrupyWKontakcie = contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoKontaktu)
																	.AB5Groups.Groups.indexOf(usuwanaGrupaID);
						this.contactBookFull.A2Contactsy.Contacts.get(indeksSzukanegoKontaktu).AB5Groups.Groups.remove(
								indeksSzukanejGrupyWKontakcie);
					}
					//usuniecie kotantu z adaptera expandableListView
					this.contactsExpandableList.get(group).remove(child);
					//zapis listy kontaktow do pliku w pam. wewnetrznej
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						new Persister().write(this.contactBookFull, baos);
						this.gglista = baos.toString("UTF-8");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	saveOnInternalMemory(this.gglista,mojNumer);
		        	mAdapter.notifyDataSetChanged();
					//jesli usuwany kontakt ma wpisany numer GG
					//to trzeba wyslac do serwera informacje o
					//usunieciu kontaktu z listy, zeby nie przysylal
					//nam informacji o dostepnosci kontaktu
		        	if(pobrany.GGNumber != null)
		        	{
						if(!pobrany.GGNumber.equals(""))
						{
							//jesli kontakt usuwany jest z grupy ignorowani ale jest tez w innej grupie,
							//to informujemy serwis, zeby powiadomij serwer,
							//ze ten kontakt nie jest juz ignorowany
							if(usuwanaGrupaID.equals("00000000-0000-0000-0000-000000000001") && usuwanyKontaktJestTezWInnejGrupie)
							{							
								Message msg3 = Message.obtain(null,Common.CLIENT_REMOVE_CONTACT, 0, 0);	        
			    	    		try
			    	    		{
			    		    		Bundle wysylany = new Bundle();
			    					wysylany.putString("numerGG", pobrany.GGNumber);
			                    	wysylany.putBoolean("ingorowany", true);
			                    	wysylany.putBoolean("kontaktIstnieje", true);
			    					msg3.setData(wysylany);
			    	    			mService.send(msg3);
			    	    		}catch(Exception excMsg)
			    	    		{
			    	    			Log.e("ContactBook","Blad wyslania info do serwisu o usuwanym(ignorowanym i istniejacym) kontakcie:\n"+
			    	    					excMsg.getMessage());
			    	    		}
							}
							//jesli kontakt usuwany jest z grupy ignorowani i nie ma go w innej grupie,
							//to informujemy serwis, zeby powiadomij serwer,
							//ze ten kontakt nie jest juz ignorowany i nie mamy tego kontaktu na liscie
							//(zeby nie przysylal wiadomosci o dostepnosci kontaktu)
							else if(usuwanaGrupaID.equals("00000000-0000-0000-0000-000000000001"))
							{
								Message msg3 = Message.obtain(null,Common.CLIENT_REMOVE_CONTACT, 0, 0);	        
			    	    		try
			    	    		{
			    		    		Bundle wysylany = new Bundle();
			    					wysylany.putString("numerGG", pobrany.GGNumber);
			                    	wysylany.putBoolean("ingorowany", true);
			                    	wysylany.putBoolean("kontaktIstnieje", false);
			    					msg3.setData(wysylany);
			    	    			mService.send(msg3);
			    	    		}catch(Exception excMsg)
			    	    		{
			    	    			Log.e("ContactBook","Blad wyslania info do serwisu o usuwanym(ignorowanym i nieistniejacym) kontakcie:\n"+
			    	    					excMsg.getMessage());
			    	    		}
							}
							//w tej sytuacji nie wysylamy do serwisu zadnej informacji,
							//bo dalej chcemy otrzymywac wiadomosci o dostepnosci kontaktu
							else if(usuwanyKontaktJestTezWInnejGrupie)
								;
							//w tej sytuacji informujemy serwis, zeby powiadomij serwer,
							//ze nie mamy juz tego kontaktu na liscie
							//(zeby nie przysylal wiadomosci o dostepnosci kontaktu)
							else
							{
								Message msg3 = Message.obtain(null,Common.CLIENT_REMOVE_CONTACT, 0, 0);	        
			    	    		try
			    	    		{
			    		    		Bundle wysylany = new Bundle();
			    					wysylany.putString("numerGG", pobrany.GGNumber);
			                    	wysylany.putBoolean("ingorowany", false);
			                    	wysylany.putBoolean("kontaktIstnieje", false);
			    					msg3.setData(wysylany);
			    	    			mService.send(msg3);
			    	    		}catch(Exception excMsg)
			    	    		{
			    	    			Log.e("ContactBook","Blad wyslania info do serwisu o usuwanym(nieignorowanym i nieistniejacy) kontakcie:\n"+
			    	    					excMsg.getMessage());
			    	    		}
							}
						}
		        	}
					break;
				case 2: //Lokalizuj
					Geo geo = new Geo();
					GeoPoint g = geo.getFix(pobrany.GGNumber);
					if(g != null)
					{
						Intent i = new Intent(getApplicationContext(),Maps.class);
						i.putExtra("latitude", g.getLatitudeE6());
						i.putExtra("longitude", g.getLongitudeE6());
						startActivity(i);
					}
					else 
					{
						Toast.makeText(getApplicationContext(), "U¿ytkownik nie udostêpnia \nswojej lokalizacji", Toast.LENGTH_SHORT).show();
					}
					
					break;
				case 3: //Lokalizuj mnie
					Intent j = new Intent(getApplicationContext(), Maps.class);
					j.putExtra("FromDevice", true);
					j.putExtra("source", mojNumer);
					j.putExtra("ggnumber",pobrany.GGNumber);
					startActivity(j);
					break;
				case 4: //link
						String adres =null;			
						Pattern pattern = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\@?^=%&amp;/~\\+#])?");
						Matcher matcher = pattern.matcher(pobrany.description);
						while(matcher.find())
						{
								if(matcher.group().startsWith("www") || matcher.group().length()> 2)
								{
									adres = matcher.group();		
								}								
						}
						if(adres!=null)
						{
							Intent k = new Intent(getApplicationContext(), WWW.class);
							k.putExtra("adres", adres);
							startActivity(k);
						}
						else
						{
							Toast.makeText(getApplicationContext(),"Brak URL w opisie", Toast.LENGTH_SHORT).show();
						}
					    
					
					break;
					
				//akcja ignoruj/nie ignoruj
				case 5:
					//jesli wybrano ignoruj
					if(menuItemName.equalsIgnoreCase("ignoruj"))
					{
						SIMPLEContact wyszukiwanyKontakt = new SIMPLEContact();
		        		wyszukiwanyKontakt.AA3ShowName = pobrany.showName;
		        		pobrany.blocked = true;
		        		int indeksKontaktu = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, wyszukiwanyKontakt, null);
		        		SIMPLEContact kopiaKontaktu = this.contactBookFull.A2Contactsy.Contacts.get(indeksKontaktu);
		        		kopiaKontaktu.AC3FlagIgnored = true;
		        		kopiaKontaktu.AB5Groups.Groups.add("00000000-0000-0000-0000-000000000001");
		        		ViewableGroups szukanaIgnorowani = new ViewableGroups();
		        		szukanaIgnorowani.groupid = "00000000-0000-0000-0000-000000000001";
		        		int indeksGrupyIgnorowani = Collections.binarySearch(this.groupsExpandableList, szukanaIgnorowani, null);
		        		//wyznaczenie indeksu pod jakim powinien zostac dodany kontakt w grupie Ignorowani
		        		int indeksKontaktuWIgnorowanych = Collections.binarySearch(this.contactsExpandableList.get(indeksGrupyIgnorowani), pobrany, null);
		        		indeksKontaktuWIgnorowanych = -(indeksKontaktuWIgnorowanych+1);
		        		this.contactsExpandableList.get(indeksGrupyIgnorowani).add(indeksKontaktuWIgnorowanych, pobrany);
		        		//zapis listy kontaktow do pliku w pam. wewnetrznej
						ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
						try {
							new Persister().write(this.contactBookFull, baos2);
							this.gglista = baos2.toString("UTF-8");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Log.e("ContactBook blad ignorowania",e.getMessage());
						}
			        	saveOnInternalMemory(this.gglista,mojNumer);
		        		this.mAdapter.notifyDataSetChanged();
		        		//wyslanie do serwisu informacji, zeby powiadomij serwer GG o ignorowaniu kontaktu
		        		Message msg3 = Message.obtain(null,Common.CLIENT_IGNORE_CONTACT, 0, 0);	        
	    	    		try
	    	    		{
	    		    		Bundle wysylany = new Bundle();
	    					wysylany.putString("numerGG", pobrany.GGNumber);
	    					msg3.setData(wysylany);
	    	    			mService.send(msg3);
	    	    		}catch(Exception excMsg)
	    	    		{
	    	    			Log.e("ContactBook","Blad wyslania info do serwisu o usuwanym(ignorowanym i istniejacym) kontakcie:\n"+
	    	    					excMsg.getMessage());
	    	    		}
					}
					//jesli wybrano nie ignoruj
					else
					{
						SIMPLEContact wyszukiwanyKontakt = new SIMPLEContact();
		        		wyszukiwanyKontakt.AA3ShowName = pobrany.showName;
		        		pobrany.blocked = false;
		        		int indeksKontaktu = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, wyszukiwanyKontakt, null);
		        		SIMPLEContact kopiaKontaktu = this.contactBookFull.A2Contactsy.Contacts.get(indeksKontaktu);
		        		kopiaKontaktu.AC3FlagIgnored = null;
		        		kopiaKontaktu.AC1FlagNormal = true;		        		
		        		kopiaKontaktu.AB5Groups.Groups.remove("00000000-0000-0000-0000-000000000001");
		        		//jesli nie chcemy wiecej ignorowac kontaktu, ale nie bylo go w zadnej innej
		        		//grupie poza ignorowanymi, to doda go do standardowej grupy "Moje kontakty"
		        		if(kopiaKontaktu.AB5Groups.Groups.size() < 1)
		        		{
		        			kopiaKontaktu.AB5Groups.Groups.add("00000000-0000-0000-0000-000000000000");
			        		ViewableGroups szukanaMojeKontakty = new ViewableGroups();
			        		szukanaMojeKontakty.groupid = "00000000-0000-0000-0000-000000000000";
			        		int indeksGrupyMojeKontakty = Collections.binarySearch(this.groupsExpandableList, szukanaMojeKontakty, null);
			        		//wyznaczenie indeksu pod jakim powinien zostac dodany kontakt w grupie Moje kontakty
			        		int indeksKontaktuWMojeKontakty = Collections.binarySearch(this.contactsExpandableList.get(indeksGrupyMojeKontakty), pobrany, null);
			        		indeksKontaktuWMojeKontakty = -(indeksKontaktuWMojeKontakty+1);
			        		this.contactsExpandableList.get(indeksGrupyMojeKontakty).add(indeksKontaktuWMojeKontakty, pobrany);
		        		}
		        		//usuniecie kontaktu z grupy Ignorowani
		        		ViewableGroups szukanaIgnorowani = new ViewableGroups();
		        		szukanaIgnorowani.groupid = "00000000-0000-0000-0000-000000000001";
		        		int indeksGrupyIgnorowani = Collections.binarySearch(this.groupsExpandableList, szukanaIgnorowani, null);
		        		ViewableContacts kontaktWIgnorowanych = new ViewableContacts();
		        		kontaktWIgnorowanych.showName = pobrany.showName;
		        		int indeksKontaktuWIgnorowanych = Collections.binarySearch(this.contactsExpandableList.get(indeksGrupyIgnorowani), kontaktWIgnorowanych, null);
		        		this.contactsExpandableList.get(indeksGrupyIgnorowani).remove(indeksKontaktuWIgnorowanych);
		        		
		        		//zapis listy kontaktow do pliku w pam. wewnetrznej
						ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
						try {
							new Persister().write(this.contactBookFull, baos2);
							this.gglista = baos2.toString("UTF-8");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Log.e("ContactBook blad ignorowania",e.getMessage());
						}
						saveOnInternalMemory(this.gglista,mojNumer);
		        		this.mAdapter.notifyDataSetChanged();
		        		//wyslanie do serwisu informacji, zeby powiadomij serwer GG o ignorowaniu kontaktu
		        		Message msg3 = Message.obtain(null,Common.CLIENT_UNIGNORE_CONTACT, 0, 0);	        
	    	    		try
	    	    		{
	    		    		Bundle wysylany = new Bundle();
	    					wysylany.putString("numerGG", pobrany.GGNumber);
	    					msg3.setData(wysylany);
	    	    			mService.send(msg3);
	    	    		}catch(Exception excMsg)
	    	    		{
	    	    			Log.e("ContactBook","Blad wyslania info do serwisu o usuwanym(ignorowanym i istniejacym) kontakcie:\n"+
	    	    					excMsg.getMessage());
	    	    		}
					}
					break;
					//akcja wyslij plik
					case 6:
						Intent intentExplorer = new Intent(this.getApplicationContext(), AndroidExplorer.class);
						intentExplorer.putExtra("fileTo", pobrany.GGNumber);
						startActivityForResult(intentExplorer,NEW_ANDROID_EXPLORER_ACTIVITY_RESULT);
						//wyslanie do serwisu informacji o checi wyslania pliku na na podany numer GG
		        		/*Message msg3 = Message.obtain(null,Common.CLIENT_SEND_FILE, 0, 0);	        
	    	    		try
	    	    		{
	    		    		Bundle wysylany = new Bundle();
	    					wysylany.putString("numerGG", pobrany.GGNumber);
	    					//"data/data/android.pp/files/slij.txt", "slij.txt"
	    					//wysylany.putString("fileName", "slij.txt");
	    					wysylany.putString("fileName", "SSH.apk");
	    					//wysylany.putString("filePath", "data/data/android.pp/files/slij.txt");
	    					wysylany.putString("filePath", "sdcard/SSH.apk");
	    					msg3.setData(wysylany);
	    	    			mService.send(msg3);
	    	    		}catch(Exception excMsg)
	    	    		{
	    	    			Log.e("ContactBook","Blad wyslania info do serwisu o wysylaniu pliku:\n"+
	    	    					excMsg.getMessage());
	    	    		}*/
						break;
			}
		}
		//akcja dla grupy
		else
		{
			ViewableGroups pobrany = this.groupsExpandableList
			.get(group);
			switch(item.getItemId())
			{
				//dodaj kontakt (do wybranej grupy)
				case 0:
					Intent intent = new Intent(this.getApplicationContext(), AddNewContact.class);
					intent.putExtra("grupaID", pobrany.groupid);
					startActivityForResult(intent,NEW_CONTACT_ACTIVITY_RESULT);
					break;
			}					
		}
		return true;
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
				String odz = prefs.getString("text", null);
				editor.remove("text");
	    		editor.commit();
	    		Message msgExit = Message.obtain(null, Common.CLIENT_EXIT_PROGRAM, 0, 0);
	    		try
	    		{
		    		Bundle wysylanyExit = new Bundle();	    	
        			if(statusDescription.getText() != null)
        				wysylanyExit.putString("opisStatusu", statusDescription.getText().toString());
        			else
        				wysylanyExit.putString("opisStatusu", "");
					msgExit.setData(wysylanyExit);
	    			mService.send(msgExit);
		    		finish();
	    		}catch(Exception exExit)
	    		{
	    			Log.e("]ContactBook]Wyloguj i zakoncz", exExit.getMessage());
	    		}
			 	//moveTaskToBack(true);
			 	break;
			case R.id.Export03:			
				//wyslanie do serwisu wiadomosci, ze eksportowana jest lista kontaktow    		
	        	Message msg3 = Message.obtain(null,Common.CLIENT_SET_CONTACTBOOK, 0, 0);	        
	    		try
	    		{
		    		Bundle wysylany = new Bundle();
					wysylany.putString("listaGG", gglista);
					msg3.setData(wysylany);
	    			mService.send(msg3);
	    		}catch(Exception excMsg)
	    		{
	    			Log.e("Blad","Blad!!!!\n"+excMsg.getMessage());
	    		}
				return true;
			case R.id.AddContact04:
				Intent intent = new Intent(this.getApplicationContext(), AddNewContact.class);
				startActivityForResult(intent,NEW_CONTACT_ACTIVITY_RESULT);
			 	break;
			case R.id.GoToConversations05:
				Intent intent1 = new Intent(this.getApplicationContext(), Chat.class);
				intent1.putExtra("mojNumer", this.mojNumer);
				
				prepareGGNumShowNameForIntent(intent1, null);
				
				try
				{
					startActivity(intent1);
				}
				catch(Exception e)
				{
					Log.i("ContactBook",""+e.getMessage());
				}
				break;
			//Moreitemsgohere(ifany)...
		}
		return false;
	}
	
	//zdarzenia realizowane po wywolaniu metody showDialog(id); 
	@Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        //metoda uruchamiana po wybraniu przycisku zmiany statusu
        case DIALOG_STATUS:
        	ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("Name", "Dostepny");
            map.put("ResID", R.drawable.available);
            list.add(map);

            map = new HashMap<String, Object>();
            map.put("Name", "Zaraz wracam");
            map.put("ResID", R.drawable.away);
            list.add(map);

            map = new HashMap<String, Object>();
            map.put("Name", "Niewidoczny");
            map.put("ResID", R.drawable.offline);
            list.add(map);
            
            map = new HashMap<String, Object>();
            map.put("Name", "Niedostepny");
            map.put("ResID", R.drawable.notavailable);
            list.add(map);            
            
            StatusListAdapter adapter = new StatusListAdapter(ContactBook.this, list,
                    0, new String[]{},
                    new int[] {});
            
            return alertDialog = new AlertDialog.Builder(ContactBook.this)
            	.setTitle("Status")
            	.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
            
                        /* User clicked so do some stuff */
                        String[] items = new String[]{"Dostepny","Zaraz wracam","Niewidoczny","Niedostepny"};
                        Message msg2 = Message.obtain(null,Common.CLIENT_CHANGE_STATUS, 0, 0);
                        Bundle wysylany = new Bundle();
            			wysylany.putString("status", items[which]);
            			if(statusDescription.getText() != null)
            				wysylany.putString("opisStatusu", statusDescription.getText().toString());
            			else
            				wysylany.putString("opisStatusu", "");
            			msg2.setData(wysylany);
        	    		try
        	    		{
        	    			mService.send(msg2);
        	    			ustawionyStatus = which;
        	    			switch(which){
        	    				case 0:
        	    					statusButton.setImageResource(R.drawable.available);
        	    					break;
        	    				case 1:
        	    					statusButton.setImageResource(R.drawable.away);
        	    					break;
        	    				case 2:
        	    					statusButton.setImageResource(R.drawable.offline);
        	    					break;
        	    				case 3:
        	    					statusButton.setImageResource(R.drawable.notavailable);
        	    					break;
        	    			}
        	    		}catch(Exception excMsg)
        	    		{
        	    			Log.e("Blad","Blad!!!!\n"+excMsg.getMessage());
        	    		}
                    }
                }).create();
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
		//alternatywna metoda pobrania numeru kliknietego goscia
		ViewableContacts pobrany = this.contactsExpandableList.get(groupPosition).get(childPosition);
		Log.i("[Metoda2]kontakt z numerem: ", ""+pobrany.GGNumber);		
		
		if(pobrany.GGNumber == null)
			return false;
		if(pobrany.GGNumber.equals(""))
			return false;
		
		Intent intent = new Intent(this.getApplicationContext(), Chat.class);
		intent.putExtra("username",((TextView)v.findViewById(R.id.username)).getText());
		intent.putExtra("ggnumber", pobrany.GGNumber);
		intent.putExtra("mojNumer", this.mojNumer);
		
		prepareGGNumShowNameForIntent(intent, null);
		
		try
		{
			startActivityForResult(intent,999);
		}
		catch(Exception e)
		{
			Log.i("ContactBook",""+e.getMessage());
		}
		return false;
    }
	
	public void prepareGGNumShowNameForIntent(Intent intent, Bundle bundle)
	{
		if(contactBookFull != null)
		{
			if(contactBookFull.A2Contactsy != null)
			{
				if(contactBookFull.A2Contactsy.Contacts != null)
				{
					ArrayList<String> numerIndex = new ArrayList<String>();
					ArrayList<String> numerShowName = new ArrayList<String>();
					for(int i=0; i<contactBookFull.A2Contactsy.Contacts.size(); i++)
					{
						numerShowName.add(contactBookFull.A2Contactsy.Contacts.get(i).AA3ShowName);
						numerIndex.add(contactBookFull.A2Contactsy.Contacts.get(i).AA2GGNumber);
					}
					if(intent != null)
					{
						intent.putStringArrayListExtra("ShowNameGGNumber", numerShowName);
						intent.putStringArrayListExtra("indexGGNumber", numerIndex);
					}
					else if (bundle != null)
					{
						bundle.putStringArrayList("ShowNameGGNumber", numerShowName);
						bundle.putStringArrayList("indexGGNumber", numerIndex);
					}
				}
			}
		}
	}
	
	public void prepareContactBook(String xmlList)
    {
    	Serializer serializer = new Persister();
		//false na koï¿½cu odpowiada za ignorowanie elementow w pliku XML ktorych
		//nie ma zadeklarowanych w klasie do ktorej wczytuje XMLa. Gdyby GG dorzucilo
		//jakies pole do listy kontaktow, to Gandu je zignoruje i wczyta te pola,
		//ktore ma zadeklarowane w klasie z lista kontaktow.
		try {
			this.contactBookFull = serializer.read(SIMPLEContactBookList.class, xmlList, false);
			sortContactListSIMPLE(this.contactBookFull);
			this.contactsExpandableList = new ArrayList<List<ViewableContacts>>();
			this.groupsExpandableList = new ArrayList<ViewableGroups>();
			createExpandableAdapter(this.contactBookFull, this.contactsExpandableList, this.groupsExpandableList);
			statusDescription.setSelection(0, statusDescription.getText().length());
		} catch (Exception excSimp) {
			Log.e("SIMPLE Error",excSimp.getMessage());
		}
    }
	
	public void addContactToContactBook(SIMPLEContact addedContact)
    {
    	Serializer serializer = new Persister();
		//false na koï¿½cu odpowiada za ignorowanie elementow w pliku XML ktorych
		//nie ma zadeklarowanych w klasie do ktorej wczytuje XMLa. Gdyby GG dorzucilo
		//jakies pole do listy kontaktow, to Gandu je zignoruje i wczyta te pola,
		//ktore ma zadeklarowane w klasie z lista kontaktow.
		try {
			//wyszukuje najpierw indeks pod ktorym nalezy dodac nowy kontakt na liste,
			//zeby po dodaniu lista kontaktow dalej byla posortowana
			int wynikBinarySearch = Collections.binarySearch(this.contactBookFull.A2Contactsy.Contacts, addedContact, null);
			int miejsceWstawieniaKontaktu = 0;
			if(wynikBinarySearch >= 0)
			{
				Log.i("Adding contact","dodawany kontakt juz jest na liscie kontaktow");
				Toast.makeText(this, "Zmien nazwe kontaktu.\n"+
						"Kontakt o podanej nazwie juz istnieje", Toast.LENGTH_SHORT).show();
				return;
			}
			else
				miejsceWstawieniaKontaktu = -(wynikBinarySearch+1);
			this.contactBookFull.A2Contactsy.Contacts.add(miejsceWstawieniaKontaktu,addedContact);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.write(this.contactBookFull, baos);
			this.gglista = baos.toString("UTF-8");
			saveOnInternalMemory(this.gglista,mojNumer);
			this.contactsExpandableList = new ArrayList<List<ViewableContacts>>();
			this.groupsExpandableList = new ArrayList<ViewableGroups>();
			createExpandableAdapter(this.contactBookFull, this.contactsExpandableList, this.groupsExpandableList);
			mAdapter.setAdapterData(this.groupsExpandableList, this.contactsExpandableList);
        	for(int parent=0; parent<this.groupsExpandableList.size(); parent++)
				getExpandableListView().expandGroup(parent);
		} catch (Exception excAdd) {
			Log.e("Dodawanie kontaktu Error",excAdd.getMessage());
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
		gs = new GetStatuses();
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
				dodawany.GGNumber = "";
				if(gcl.A2Contactsy.Contacts.get(i).AA2GGNumber != null)
				{
					try
					{
						//dodawany.GGNumber = Integer.parseInt(gcl.A2Contactsy.Contacts.get(i).AA2GGNumber);
						dodawany.GGNumber = gcl.A2Contactsy.Contacts.get(i).AA2GGNumber;
						
						byte type = (byte)0x03;
						if(gcl.A2Contactsy.Contacts.get(i).AC3FlagIgnored != null)
							if(gcl.A2Contactsy.Contacts.get(i).AC3FlagIgnored)
								type = (byte)0x04;	
						gs.StatusesPairs.add(new GetStatusesStruct(Integer.reverseBytes(Integer.parseInt(dodawany.GGNumber)), type));
						
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
				if(gcl.A2Contactsy.Contacts.get(i).AC3FlagIgnored != null)
					dodawany.blocked = gcl.A2Contactsy.Contacts.get(i).AC3FlagIgnored;				
				dodawany.showName = gcl.A2Contactsy.Contacts.get(i).AA3ShowName;
				kontaktyExp.get(indeksTab_kontaktyExp).add(dodawany);
			}
		}
		//uruchom watek oczekujacy na , 
		//podtrzymujaca polaczenie z serwerem GG
		Thread statusesThread = new Thread(null, statusesTask, "statusesService");
        mCondition = new ConditionVariable(false);
        statusesThread.start();
    }
	
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Bundle odebrany ;
            switch (msg.what) {
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("Zarejestrowano ContactBook","Received: "+msg.what);
                	//wyslanie do serwisu wiadomosci, ze pobierana jest lista kontaktow
                	break;
                case Common.FLAG_CONTACTBOOK:
                	odebrany = msg.getData();
                	gglista = odebrany.getString("listaGG");
                	prepareContactBook(gglista);
                	Message msg3 = Message.obtain(null,Common.CLIENT_GG_NUM_SHOW_NAME, 0, 0);	        
    	    		try
    	    		{
    		    		Bundle wysylany = new Bundle();
    					prepareGGNumShowNameForIntent(null, wysylany);
    					msg3.setData(wysylany);
    	    			mService.send(msg3);
    	    		}catch(Exception excMsg)
    	    		{
    	    			Log.e("ContactBook","gg num -> show name:\n"+
    	    					excMsg.getMessage());
    	    		}
                	mAdapter.setAdapterData(groupsExpandableList, contactsExpandableList);
                	for(int parent=0; parent<groupsExpandableList.size(); parent++)
        				getExpandableListView().expandGroup(parent);
                	break;
                case Common.CLIENT_SET_STATUSES:
                	odebrany = msg.getData();
                	updateSD(odebrany.getInt("ggnumber"), odebrany.getInt("status"), odebrany.getString("description"));
                	//TODO dodac do listy
                	break;
                case Common.CLIENT_SET_INITIAL_INFO:
                	odebrany = msg.getData();
                	mojNumer = odebrany.getString("mojNumer");
                	String opisOdSer = odebrany.getString("description");
                	String statusOdSer = odebrany.getString("status");
                	statusDescription.setText(opisOdSer);
                	int statusOstatni = itemsy.indexOf(statusOdSer);
                	ustawionyStatus = statusOstatni;
                	switch(statusOstatni)
                	{
	                	case 0:
	    					statusButton.setImageResource(R.drawable.available);
	    					break;
	    				case 1:
	    					statusButton.setImageResource(R.drawable.away);
	    					break;
	    				case 2:
	    					statusButton.setImageResource(R.drawable.offline);
	    					break;
	    				case 3:
	    					statusButton.setImageResource(R.drawable.notavailable);
	    					break;
                		default:
                			ustawionyStatus = 0;
                			statusButton.setImageResource(R.drawable.notavailable);
                	}
                	//zaladowanie listy kontaktow z pliku, jesli taki istnieje
                	gglista = readFromInternalMemory("Kontakty_"+mojNumer+".xml");
                    if(!gglista.equals(""))
                    {
            	    	prepareContactBook(gglista);
            	    	mAdapter.setAdapterData(groupsExpandableList, contactsExpandableList);
            	    	for(int parent=0; parent<groupsExpandableList.size(); parent++)
            				getExpandableListView().expandGroup(parent);
                    }
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
        bindService(new Intent(ContactBook.this, 
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
    
    public void saveOnInternalMemory(String tmp, String ggNum)
    {  
	   try {
		   FileOutputStream fos = openFileOutput("Kontakty_"+ggNum+".xml", Context.MODE_PRIVATE);
			byte [] buffer = tmp.getBytes("UTF-8");
			fos.write(buffer);
			fos.flush();
			fos.close(); 		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public String readFromInternalMemory(String fileName)
    {
    	String result = "";    
    	try
    	{
    		String linia = "";
    		FileInputStream inFile = openFileInput(fileName);
    		InputStreamReader isr = new	InputStreamReader(inFile);
    		BufferedReader bfr = new BufferedReader(isr);
    		while((linia = bfr.readLine()) != null)
    		{
    			result += linia;
    		}
    	} 
    	catch( IOException ex )
    	{
    		System.out.println("B³¹d przy operacji na pliku: "+ex);
    	}
		return result;
    }
    
    public void sendStatusesInfoToService()
    {
    	Message m =  Message.obtain(null,Common.CLIENT_GET_STATUSES, 0, 0);
		Bundle wysylany = new Bundle();
		wysylany.putByteArray("bytePackage", gs.preparePackage());
		m.setData(wysylany);
		Log.i("ContactBook","Wyslalem do serwisu");
		try {
			mService.send(m);
		} catch (Exception e) {
			Log.e("ContactBook","wysylanie"+e.getMessage());
		}
    }
    public void sendGGNumShowNameInfoToService()
    {
    	Message msg3 = Message.obtain(null,Common.CLIENT_GG_NUM_SHOW_NAME, 0, 0);	        
		try
		{
    		Bundle wysylany = new Bundle();
			prepareGGNumShowNameForIntent(null, wysylany);
			msg3.setData(wysylany);
			mService.send(msg3);
		}catch(Exception excMsg)
		{
			Log.e("ContactBook","gg num -> show name:\n"+
					excMsg.getMessage());
		}
    }
    public void sendInitialInfoRequestToService()
    {
    	Message m =  Message.obtain(null,Common.CLIENT_GET_INITIAL_INFO, 0, 0);
    	m.replyTo = mMessenger;
		Log.i("ContactBook","Wyslalem do serwisu");
		try {
			mService.send(m);
		} catch (Exception e) {
			Log.e("ContactBook","wysylanie"+e.getMessage());
		}
    }
  //watek wysylania listy kontaktow do serwisu w celu pobrania statusow
    private Runnable initialTask = new Runnable() {
        public void run() {
        	Log.i("[ContactBook]initialTask", "Start watku initialTask");
        	//jesli polaczenie z serwisem juz zostalo nawiazane, to od razu
        	//mozna wyslac do serwisu zadanie pobrania statusow kontaktow z listy
        	if(mService == null)
        	{
	        	//Wyjscie z petli i zakonczenie watku jesli mCondition 
        		//zostanie otwarte (mCondition.open).
        		//mCondition jest otwierane w metodzie onServiceConnected
        		//obiektu mConnection, czyli po polaczeniu ContactBook z GanduService
	        	while(!mInitial.block(4 * 60 * 1000))
	        		;
        	}
        	sendInitialInfoRequestToService();
        	Log.i("[ContactBook]initialTask", "Stop watku initialTask");
        }
    };
    
  //watek wysylania listy kontaktow do serwisu w celu pobrania statusow
    private Runnable statusesTask = new Runnable() {
        public void run() {
        	Log.i("[ContactBook]statusesTask", "Start watku statusesTask");
        	//jesli polaczenie z serwisem juz zostalo nawiazane, to od razu
        	//mozna wyslac do serwisu zadanie pobrania statusow kontaktow z listy
        	if(mService == null)
        	{
	        	//Wyjscie z petli i zakonczenie watku jesli mCondition 
        		//zostanie otwarte (mCondition.open).
        		//mCondition jest otwierane w metodzie onServiceConnected
        		//obiektu mConnection, czyli po polaczeniu ContactBook z GanduService
	        	while(!mCondition.block(4 * 60 * 1000))
	        		;
        	}
        	sendStatusesInfoToService();
        	sendGGNumShowNameInfoToService();
        	Log.i("[ContactBook]statusesTask", "Stop watku statusesTask");
        }
    };
    
    public void updateSD(int ggnumber, int status , String description) //aktualizuje status, opis kontaktu na liscie kontaktow
    {
    	if(this.contactsExpandableList != null)
    	{
	    	for (int i =0 ; i<=(this.contactsExpandableList.size()-1); i++)
	    	{
	    		for (int j =0; j<=(this.contactsExpandableList.get(i).size()-1); j++)
	    		{
	    			if(this.contactsExpandableList.get(i).get(j).GGNumber != null)
	    			{
	    				if(!this.contactsExpandableList.get(i).get(j).GGNumber.equals(""))
	    				{
			    			int z = Integer.parseInt(this.contactsExpandableList.get(i).get(j).GGNumber) ;
			    			if (z == (ggnumber))
			    			{
				    			this.contactsExpandableList.get(i).get(j).description = description;
				    			this.contactsExpandableList.get(i).get(j).status = status;
			    			}
	    				}
	    			}
	    			
	    		}
	    
	    	}
	    	mAdapter.notifyDataSetChanged();
    	}
    }
}
