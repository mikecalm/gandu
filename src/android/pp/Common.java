package android.pp;

import java.util.Hashtable;

public class Common {
	
	public static String wyciecieOgonkow(String str)
	{
		Hashtable<String, String> odpowiedniki = new Hashtable<String, String>();
		odpowiedniki.put("π", "a");odpowiedniki.put("Ê", "c");odpowiedniki.put("Í", "e");odpowiedniki.put("≥", "l");
		odpowiedniki.put("Ò", "n");odpowiedniki.put("Û", "o");odpowiedniki.put("ú", "s");odpowiedniki.put("ü", "z");
		odpowiedniki.put("ø", "z");
		odpowiedniki.put("•", "a");odpowiedniki.put("∆", "c");odpowiedniki.put(" ", "e");odpowiedniki.put("£", "l");
		odpowiedniki.put("—", "n");odpowiedniki.put("”", "o");odpowiedniki.put("å", "s");odpowiedniki.put("è", "z");
		odpowiedniki.put("Ø", "z");
		
		String wynik = str;
		char[] znaki = str.toCharArray();
		for(int i=0; i<znaki.length; i++)
		{
			if(odpowiedniki.containsKey(""+znaki[i]))
				znaki[i] = odpowiedniki.get(""+znaki[i]).charAt(0);
		}
		wynik = new String(znaki);
		return wynik;
	}
	
	/**
     * REQUESTS TO SERVER GG
    */
    static final int GG_USERLIST_REQUEST80 = 0x002f;
    static final int GG_LOGIN80 = 0x0031;
    static final int GG_LIST_EMPTY = 0x0012;
    static final int GG_SEND_MSG80 = 0x002d;
    static final int GG_NEW_STATUS80 = 0x0038;
    static final int GG_ADD_NOTIFY = 0x000d;
    static final int GG_REMOVE_NOTIFY = 0x000e;
    static final int GG_NOTIFY_FIRST = 0x000f;
    static final int GG_NOTIFY_LAST = 0x0010;
    static final int GG_PING = 0x0008;
    static final int GG_DCC7_ID_REQUEST = 0x0023;
    static final int GG_DCC7_NEW = 0x0020;
    static final int GG_DCC7_ABORT = 0x0025;
    static final int GG_DCC7_REJECT = 0x0022;
    static final int GG_DCC7_ACCEPT = 0x0021;
    
    /**
     * REQUESTS TO relay.gadu-gadu.pl SERVER
    */
    static final int GG_DCC7_RELAY_REQUEST = 0x0a;
    static final int GG_DCC7_INFO = 0x001f;
    
    /**
     * GG_DCC7_RELAY_REQUEST req_type
     */
    static final short GG_DCC7_RELAY_TYPE_PROXY = 0x01; /* adresy proxy, na ktÛre sie ≥πczyÊ */
    static final short GG_DCC7_RELAY_TYPE_SERVER = 0x08; /* adres serwera, na ktÛry spytaÊ o proxy */
    
    /**
     * GG_DCC7_INFO type
     */
    static final int GG_DCC7_TYPE_P2P = 0x00000001;	/* po≥πczenie bezpoúrednie */
    static final int GG_DCC7_TYPE_SERVER = 0x00000002;	/* po≥πczenie przez serwer */

    
    /**
     * STATES 
     */
    static final int GG_STATUS_AVAIL = 0x0002;
    static final int GG_STATUS_AVAIL_DESCR = 0x0004;    
    static final int GG_STATUS_NOT_AVAIL = 0x0001;
    static final int GG_STATUS_NOT_AVAIL_DESCR = 0x0015;
    static final int GG_STATUS_INVISIBLE = 0x0014;
    static final int GG_STATUS_INVISIBLE_DESCR = 0x0016;
    static final int GG_STATUS_BUSY = 0x0003;
    static final int GG_STATUS_BUSY_DESCR = 0x0005;
    static final int GG_STATUS_BLOCKED = 0x0006;
    /**
     * REPLIES FROM SERVER GG
    */
    static final int GG_WELCOME = 0x0001;
    static final int GG_USERLIST_REPLY80 = 0x0030;
    static final int GG_LOGIN_OK80 = 0x0035;
    static final int GG_LOGIN_FAILED = 0x0009;
    static final int GG_RECV_MSG80 = 0x002e;
    static final int GG_STATUS80 = 0x0036;
    static final int GG_NOTIFY_REPLY80 = 0x0037;
    static final int GG_DCC7_ID_REPLY = 0x0023;
    
    /**
     * REPLIES FROM relay.gadu-gadu.pl SERVER
    */
    static final int GG_DCC7_RELAY_REPLY = 0x0b;

    /**
     * CONTACT TYPES
     */
    static final byte GG_USER_OFFLINE = 0x01;	//UÔøΩytkownik, dla ktÔøΩrego bÔøΩdziemy niedostÔøΩpni, ale mamy go w liÔøΩcie kontaktÔøΩw
    static final byte GG_USER_NORMAL = 0x03;		//ZwykÔøΩy uÔøΩytkownik dodany do listy kontaktÔøΩw
    static final byte GG_USER_BLOCKED = 0x04;	//UÔøΩytkownik, ktÔøΩrego wiadomoÔøΩci nie chcemy otrzymywaÔøΩ

    /**
     * CLASS FLAGS
    */
    static final int FLAG_ACTIVITY_REGISTER = 10;
    static final int FLAG_CONTACTBOOK = 11;
    
    /**
     * IMPORT/EXPORT CONTACTBOOK TYPES
    */
    static final byte GG_USERLIST_GET=  0x02; 	//import listy
    static final byte GG_USERLIST_GET_MORE_REPLY = 0x04;
    static final byte GG_USERLIST_GET_REPLY = 0x06;
    static final byte GG_USERLIST_PUT= 0x00; 	//poczatek eksportu listy
    
    /**
     * TYPE RODZJA TRANSMISJI
     */
    static final int GG_DCC7_TYPE_VOICE = 0x00000001; /* rozmowa g≥osowa (juø nieuøywane) */
    static final int GG_DCC7_TYPE_FILE = 0x00000004;	/* przesy≥anie plikÛw */
    
    /**
     * REASON powod odmowy przyjecia pliku
     */
    static final int GG_DCC7_REJECT_USER = 0x00000002;	/* uøytkownik odrzuci≥ po≥πczenie */
    
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int CLIENT_REGISTER = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int CLIENT_UNREGISTER = 2;
    
    /**
     * Command to service to login user.
    */
   static final int CLIENT_LOGIN = 3;
   /**
    * Command to service to download a contactbook from server.
   */
   static final int CLIENT_GET_CONTACTBOOK = 4;
   
   static final int CLIENT_START_INTENT_CONTACTBOOK = 5;
   static final int CLIENT_SET_CONTACTBOOK = 6;
   
   static final int CLIENT_SEND_MESSAGE = 7;
   static final int CLIENT_RECV_MESSAGE = 8;
   
   static final int CLIENT_CHANGE_STATUS = 9;
   
   static final int CLIENT_ADD_NEW_CONTACT = 10;
   static final int CLIENT_REMOVE_CONTACT = 11;
   static final int CLIENT_IGNORE_CONTACT = 12;
   static final int CLIENT_UNIGNORE_CONTACT = 13;
   static final int CLIENT_GET_STATUSES = 14;
   
   static final int CLIENT_SET_STATUSES = 15;
   static final int CLIENT_SEND_CONFERENCE_MESSAGE = 16;
   static final int CLIENT_CONTACTBOOK_OUT = 17;
   static final int CLIENT_GG_NUM_SHOW_NAME = 18;
   static final int CLIENT_GET_INITIAL_INFO = 20;
   static final int CLIENT_SET_INITIAL_INFO = 21;
   static final int CLIENT_EXIT_PROGRAM = 22;
   static final int CLIENT_SEND_FILE = 23;
   static final int CLIENT_LOGIN_FAILED = 24;
   static final int CLIENT_INITIALIZE_FAILED = 25;
   static final int CLIENT_FILE_QUESTION = 26;
   static final int CLIENT_FILE_YES = 27;
   static final int CLIENT_FILE_NO = 28;
}
