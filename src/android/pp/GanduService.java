package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.maps.ItemizedOverlay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.pp.MyLocation.LocationResult;
import android.preference.RingtonePreference;
import android.util.Log;
import android.widget.Toast;

public class GanduService extends Service {
	//klasa sluzaca do jednokrotnego pobrania mojej lokaliazacji
	MyLocation myLocation;
	GeoSynchronizedList geoSynchronizedList;
	
	String[] ip = null;
	private boolean connected = false;
	Thread cThread;

	Thread pingingThread;
	Socket socket;
	//SSLSocket socket;
	DataInputStream in;
	DataOutputStream out;
	String ggnum;
	String ggpass;
	String descriptionLast = "http://code.google.com/p/gandu/";
	String statusLast = "Dostepny";
	Boolean rozlaczenieNiePrzezUzytkownika = true;
	// String statusLast = "Niewidoczny";
	public byte[] skompresowanaLista = null;
	int licznik_serii_listy = 0;
	/** For showing and hiding our notification. */
	NotificationManager mNM;
	/** Keeps track of all current registered clients. */
	ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	/** Holds last value set by a client. */
	int mValue = 0;
	ArchiveSQLite archiveSQL;
	// variable which controls the ping thread
	private ConditionVariable mCondition;
	private ConditionVariable mTransfer;
	// private ConditionVariable mConditionPingExit;
	// private ConditionVariable mConditionGGExit;

	// START Zmienne potrzebne do foreground
	private static final Class[] mStartForegroundSignature = new Class[] {
			int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	// KONIEC Zmienne potrzebne do foreground

	ArrayList<String> numerShowName;
	ArrayList<String> numerIndex;

	public Files incomingFileTransfer = null;
	public Files outcomingFileTransfer = null;
	
	//sprawdzenie czy udostepniamy nasza lokaliazacji uzytkownikowi o danym numerze gg
	public boolean geoHavePermission(String ggnum)
	{
		SharedPreferences Geoprefs = getSharedPreferences("geofriends", 0);
		return Geoprefs.contains(ggnum);
	}
	
	//GEOtest
	public LocationResult locationResult = new LocationResult(){
	    @Override
	    public void gotLocation(final Location location){
	    	if(location != null)
	    	{
		        //Got the location!
		    	//Wyslij lokalizacje do wszystkich z listy oczekujacych na lokalizacje
		    	int currentTime = (int) (System.currentTimeMillis() / 1000L);
		    	geoSynchronizedList.sendLocalization(out, location, currentTime);
	    	}
        };
    };
    //GEOtest



	Handler someHandler = new Handler() {

		// this method will handle the calls from other threads.
		public void handleMessage(Message msg) {

			Toast.makeText(getBaseContext(),
					msg.getData().getString("SOMETHING"), Toast.LENGTH_SHORT)
					.show();
		}
	};

	public Boolean getContactbook() {
		int type = Integer.reverseBytes(Common.GG_USERLIST_REQUEST80);
		byte contactbook_frame_type = Common.GG_USERLIST_GET;
		// byte [] request ;
		byte[] paczkaBajtow = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(type);
			dos.writeInt(Integer.reverseBytes(1));
			dos.write(contactbook_frame_type);
			paczkaBajtow = baos.toByteArray();
			out.write(paczkaBajtow);
			out.flush();
			Log.i("GanduService", "Wykonalem getContactbook()");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public Boolean setContactbook(String XMLList) {
		int type = Integer.reverseBytes(Common.GG_USERLIST_REQUEST80);
		byte contactbook_frame_type = Common.GG_USERLIST_PUT;
		int dlugoscSkompresowana = 0;
		Log.e("GanduService", "Do serwisu trafila lista: " + XMLList);
		// wyslanie pustej listy na serwer (nie dziala, ale to chyba jakis bug
		// po stronie GG)
		if (XMLList.trim().length() == 0) {
			// XMLList = " ";
			// w dokumentacji jest napisane, ze aby wyslac pusta liste na serwer
			// (usunac liste z sewera)
			// nalezy wyslac jako liste kontaktow spacje (skompresowana
			// deflate'em)
			this.skompresowanaLista = new byte[] { (byte) 0x78, (byte) 0xda,
					(byte) 0x53, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x21, (byte) 0x00, (byte) 0x21 };
			dlugoscSkompresowana = this.skompresowanaLista.length;
			Log.e("GanduService", "Lista po zamianie przez if: " + XMLList);
		} else {
			// skompresowanaLista = deflateContactBook(XMLList);
			dlugoscSkompresowana = deflateContactBook(XMLList);
		}
		byte[] paczkaBajtow = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(type);
			// dos.writeInt(Integer.reverseBytes(1+skompresowanaLista.length));
			dos.writeInt(Integer.reverseBytes(1 + dlugoscSkompresowana));
			dos.write(contactbook_frame_type);
			dos.write(skompresowanaLista);
			paczkaBajtow = baos.toByteArray();
			out.write(paczkaBajtow);
			out.flush();
			Log.i("GanduService", "Wykonalem setContactbook()");
			Log.i("GanduService", "Skompresowalem na: "
					+ skompresowanaLista.length);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void saveOnInternalMemory(String tmp, String ggNum) {
		// String extStorageDirectory =
		// Environment.getDataDirectory().toString() ;
		// File file = new File(extStorageDirectory,
		// "contactBook"+"GGNumber"+".xml");

		try {
			// FileOutputStream fos = new FileOutputStream(file);
			// FileOutputStream fos =
			// openFileOutput("Kontakty_"+"GGNumber"+".xml",
			// Context.MODE_PRIVATE);
			FileOutputStream fos = openFileOutput("Kontakty_" + ggNum + ".xml",
					Context.MODE_PRIVATE);
			// FileOutputStream fos =
			// openFileOutput("Kontakty_"+"GGNumber"+".xml",
			// Context.MODE_WORLD_READABLE);
			byte[] buffer = tmp.getBytes("UTF-8");
			fos.write(buffer);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveOnSDCardBytes(byte[] tmp, String folder, String nazwaPliku) {
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
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;

		}
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();
		// File file = new File(extStorageDirectory, "tmp.xml");
		// File file = new File(extStorageDirectory+"/"+folder, nazwaPliku);
		File file = new File(extStorageDirectory, nazwaPliku);

		try {
			FileOutputStream fos = new FileOutputStream(file);
			// byte [] buffer = tmp.clone();
			// fos.write(buffer);
			fos.write(tmp);
			fos.flush();
			fos.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveOnSDCard(String tmp) {
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
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;

		}
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();
		File file = new File(extStorageDirectory, "tmp.xml");

		try {
			FileOutputStream fos = new FileOutputStream(file);
			byte[] buffer = tmp.getBytes("UTF-8");
			fos.write(buffer);
			fos.flush();
			fos.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String inflateContactBook(byte[] paczkaBajtow) {
		Log.i("Dlugosc skompresowanej listy kontaktow", ""
				+ paczkaBajtow.length);
		Inflater inf = new Inflater();
		inf.setInput(paczkaBajtow, 0, paczkaBajtow.length);
		Log.i("GanduService", "Dlugosc skompresowana = " + paczkaBajtow.length);
		byte[] result = new byte[1000000];
		int resultLength;
		String str = null;
		try {
			resultLength = inf.inflate(result);
			inf.end();
			str = new String(result, 0, resultLength, "UTF-8");
		} catch (Exception e) {
			Log.e("GanduService", "Blad Inflater" + e.getMessage());
		}

		return str;
	}

	public int deflateContactBook(String listaKontaktow) {
		// Encode a String into bytes
		byte[] input;
		byte[] output = null;
		int compressedDataLength = 0;
		try {
			input = listaKontaktow.getBytes("UTF-8");
			// Compress the bytes
			output = new byte[10000];
			Deflater compresser = new Deflater();
			compresser.setInput(input);
			compresser.finish();
			Log.i("GanduService", "przed podaniem pustego output do deflate");
			compressedDataLength = compresser.deflate(output);
			Log.i("GanduService", "po podaniu pustego output do deflate");
			// wynik = output;
			// this.skompresowanaLista = output;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.write(output, 0, compressedDataLength);
			this.skompresowanaLista = baos.toByteArray();
			Log.i("GanduService", "deflate dal wynik: " + compressedDataLength);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("Blad deflate!!", e.getMessage());
		}

		// return output;
		return compressedDataLength;
	}

	public Boolean inicjujLogowanie(String numerGG) {
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(
					"http://appmsg.gadu-gadu.pl/appsvc/appmsg_ver8.asp?fmnumber="
							+ numerGG + "&lastmsg=20429&version=10.0.0.10450");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				while (instream.read(tmp) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(bais));
					String readline = br.readLine();
					Log.i("GANDU-TESTING", readline);
					ip = readline.split(" ");
				}
			}
			Log.i("ClientActivity", "Polaczenie....");

			String ipWyizolowany = ip[2].split(":")[0];
			// int portWyizolowany = Integer.parseInt(ip[2].split(":")[1]);
			int portWyizolowany = 443;
			
			//javax.net.ssl.SSLSocketFactory.getDefault()
			//SSLContext sslContext = SSLContext.getInstance("TLS");
			//sslContext.init(null, null, null);
			//javax.net.SocketFactory socketFactory = javax.net.ssl.SSLSocketFactory.getDefault(); 
			//javax.net.SocketFactory socketFactory = sslContext.getSocketFactory();
			//socket = (javax.net.ssl.SSLSocket)socketFactory.createSocket(ipWyizolowany, portWyizolowany);
			//socket = (javax.net.ssl.SSLSocket)socketFactory.createSocket(ipWyizolowany, portWyizolowany);
			//socket.setSoTimeout(20000);
			//socket.setUseClientMode(true);
			
			socket = new Socket(ipWyizolowany, portWyizolowany);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			connected = true;
			// uruchom watek odbierajacy komunikaty z serwera GG
			// Thread cThread = new Thread(new ReplyInterpreter());
			cThread = new Thread(new ReplyInterpreter());
			cThread.start();
			// uruchom watek wysylajacy okresowo wiadomosc PING,
			// podtrzymujaca polaczenie z serwerem GG
			// Thread pingingThread = new Thread(null, pingTask,
			// "NotifyingService");
			pingingThread = new Thread(null, pingTask, "NotifyingService");
			mCondition = new ConditionVariable(false);
			// mConditionPingExit = new ConditionVariable(false);
			// mConditionGGExit = new ConditionVariable(false);
			pingingThread.start();
		}catch (ClientProtocolException cpexc){
			Log.e("[GanduService]inicjujLogowanie","ClientProtocolException: "+cpexc.getMessage());
		}
		catch (IllegalStateException isexc){
			Log.e("[GanduService]inicjujLogowanie","IllegalStateException: "+isexc.getMessage());
		}
		catch (IOException ioexc){
			Log.e("[GanduService]inicjujLogowanie","IOException: "+ioexc.getMessage());
		}
		catch (Exception excinit) {
			return false;
		}
		return true;
	}

	public int kodStatusuNaPodstawieStringa(String status, String opis) {
		int kodStatusu = Common.GG_STATUS_NOT_AVAIL;
		if (opis.equals("")) {
			if (status.equals("Dostepny"))
				kodStatusu = Common.GG_STATUS_AVAIL;
			else if (status.equals("Niewidoczny"))
				kodStatusu = Common.GG_STATUS_INVISIBLE;
			else if (status.equals("Zaraz wracam"))
				kodStatusu = Common.GG_STATUS_BUSY;
			else if (status.equals("Niedostepny"))
				kodStatusu = Common.GG_STATUS_NOT_AVAIL;
		} else {

			if (status.equals("Dostepny"))
				kodStatusu = Common.GG_STATUS_AVAIL_DESCR;
			else if (status.equals("Niewidoczny"))
				kodStatusu = Common.GG_STATUS_INVISIBLE_DESCR;
			else if (status.equals("Zaraz wracam"))
				kodStatusu = Common.GG_STATUS_BUSY_DESCR;
			else if (status.equals("Niedostepny"))
				kodStatusu = Common.GG_STATUS_NOT_AVAIL_DESCR;
		}
		Log.i("[GanduService]kodStatusuNaPodstawieStringa", "Kod Statusu: "
				+ kodStatusu + ", " + status);
		return kodStatusu;
	}

	public Boolean wyslijPaczkeLogowania(String numerGG, String hasloGG,
			int ziarno) {
		try {
			int numergg = Integer.parseInt(numerGG);
			// Logowanie logowanie = new Logowanie(ziarno, hasloGG, numergg,
			// Common.GG_STATUS_AVAIL_DESCR, (byte)0xff,
			// "http://code.google.com/p/gandu/");
			// Logowanie logowanie = new Logowanie(ziarno, hasloGG, numergg,
			// Common.GG_STATUS_AVAIL_DESCR, (byte)0xff, descriptionLast);
			Logowanie logowanie;
			if (statusLast.equals("Niedostepny")) {
				statusLast = "Dostepny";
				logowanie = new Logowanie(ziarno, hasloGG, numergg,
						Common.GG_STATUS_AVAIL_DESCR, (byte) 0xff,
						descriptionLast);
			} else
				logowanie = new Logowanie(ziarno, hasloGG, numergg,
						kodStatusuNaPodstawieStringa(statusLast,
								descriptionLast), (byte) 0xff, descriptionLast);
			byte[] paczkalogowania = logowanie.pobraniePaczkiBajtow();
			out.write(paczkalogowania);
			out.flush();
			// Toast.makeText(GanduService.this, "Zalogowany",
			// Toast.LENGTH_SHORT).show();
		} catch (Exception exclog2) {
			return false;
		}
		return true;
	}

	public Boolean wyslijWiadomoscOPustejLiscieKontaktow() {
		try {
			// wyslanie do serwera GG pakietu (o zerowej dlugosci)
			// z informacja ze nie mamy nikogo na liscie kontaktow
			// #define GG_LIST_EMPTY 0x0012
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(baos2);
			dos2.writeInt(Integer.reverseBytes(Common.GG_LIST_EMPTY));
			dos2.writeInt(Integer.reverseBytes(0x0000));
			out.write(baos2.toByteArray());
		} catch (Exception excpustl) {
			return false;
		}
		return true;
	}

	public String pobierzShowName(String numerGG) {
		String znalezionyShowName = numerGG;
		if (numerIndex != null) {
			int indeks = 0;
			if ((indeks = numerIndex.indexOf(numerGG)) != -1)
				znalezionyShowName = numerShowName.get(indeks);
		}
		return znalezionyShowName;
	}

	public void wylogujIZakoncz(String opis) {
		StatusChangeMessage scm = new StatusChangeMessage();
		try {
			rozlaczenieNiePrzezUzytkownika = false;
			byte[] pack = scm.setStatus("Niedostepny", opis);
			out.write(pack);
			Log.i("GanduService", "Ustawiam status niedostepny");
			out.flush();
		} catch (Exception e) {
			Log.e("GanduService", "Status setting Failed!");
		}
		// zakonczenie serwisu dopiero po zakonczeniu watku ping
		// while(!mConditionPingExit.block(1000))
		// ;
		// Log.e("[GanduService]wylogujIZakoncz", "koniec watku ping");
		// zakonczenie serwisu dopiero po zakonczeniu watku obslugujacego
		// polaczenie z serwerem GG
		// while(!mConditionGGExit.block(1000))
		// ;
		// Log.e("[GanduService]wylogujIZakoncz",
		// "koniec watku obslugujacego GG");
		// Log.i("[GanduService]wylogujIZakoncz","przed stopSelf");
		stopSelf();
		// Log.i("[GanduService]wylogujIZakoncz","po stopSelf");
		// metoda zabijajaca proces serwisu
		// int pid = android.os.Process.myPid();
		// android.os.Process.killProcess(pid);
	}

	String getInitialList() {
		return "<ContactBook><Groups><Group><Id>00000000-0000-0000-0000-000000000000</Id><Name>Moje kontakty</Name><IsExpanded>true</IsExpanded><IsRemovable>false</IsRemovable></Group><Group><Id>00000000-0000-0000-0000-000000000001</Id><Name>Ignorowani</Name><IsExpanded>false</IsExpanded><IsRemovable>false</IsRemovable></Group></Groups><Contacts><Contact><Guid>00000000-0000-0000-0000-000000000000</Guid><GGNumber>100</GGNumber><ShowName>Infobot</ShowName><Groups><GroupId>00000000-0000-0000-0000-000000000000</GroupId></Groups><Avatars/><FlagNormal>true</FlagNormal></Contact></Contacts></ContactBook>";
	}

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle odebrany;
			switch (msg.what) {
			case Common.CLIENT_REGISTER:
				mClients.add(msg.replyTo);
				Log.i("[GanduService]bindService", "Liczba zbindowanych: "
						+ mClients.size());

				Message msg2 = Message.obtain(null,
						Common.FLAG_ACTIVITY_REGISTER, 0, 0);

				for (int i = 0; i < mClients.size(); i++) {
					try {
						mClients.get(i).send(msg2);
					} catch (Exception e) {
						Log.e("Blad", "" + e.getMessage());
					}
				}
				break;
			case Common.CLIENT_UNREGISTER:
				mClients.remove(msg.replyTo);
				break;
			case Common.CLIENT_LOGIN:
				odebrany = msg.getData();
				ggnum = odebrany.getString("numerGG");
				ggpass = odebrany.getString("hasloGG");
				// pobranie opisu i statusu do logowania z preferencji
				descriptionLast = Prefs
						.getLoginDescriptionPref(getApplicationContext());
				statusLast = Prefs.getLoginStatusPref(getApplicationContext());

				rozlaczenieNiePrzezUzytkownika = true;
				// w przypadku nieudanej proby zainicjowania logowania.
				// Najprawdopodobniej brak polaczenia z internetem
				if (!inicjujLogowanie(ggnum)) {
					Message msg3 = Message.obtain(null,
							Common.CLIENT_INITIALIZE_FAILED, 0, 0);
					try {
						msg.replyTo.send(msg3);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						Log.e("[GanduService]",
								"Blad odsylania wiadomosci o nieudanym inicjowaniu logowania: "
										+ e.getMessage());
					}
					// showNotification("Zalogowany "+ggnum);
					/*
					 * showNotification("Lista kontaktow", "Gandu",
					 * "Zalogowany "+ggnum, R.drawable.icon, GanduClient.class,
					 * -1, false); mNM.cancel(-1);
					 */
				}
				break;
			case Common.CLIENT_GET_CONTACTBOOK:
				if (getContactbook()) {
					// showNotification("Pobieram liste kontaktow...");
					showNotification("Lista kontaktow", "Gandu",
							"Pobieram liste kontaktow...", R.drawable.icon,
							GanduClient.class, -1, false, false, false);
					mNM.cancel(-1);
				}
				Log.i("GanduService", "Pobieram liste kontaktow");

				// START test wyslania wiadomosci konferencyjnej z gandu2 do
				// gandu3 i do mnie
				/*
				 * int currentTime1 = (int)(System.currentTimeMillis() / 1000L);
				 * ChatMessage cm = new ChatMessage(); byte[] paczki =
				 * cm.setConferenceMessages("12345",new int[]{2522922,31841466},
				 * currentTime1); try { out.write(paczki);
				 * Log.i("[GanduService]Konferencja", "Wyslalem wiadomosci");
				 * out.flush(); } catch (IOException e) {
				 * Log.e("[GanduService]Konferencja"
				 * ,"Blad wysylania konferencji"); }
				 */
				// KONIEC test wyslania wiadomosci konferencyjnej z gandu2 do
				// gandu i do mnie
				break;
			case Common.CLIENT_SET_CONTACTBOOK:
				Bundle odebranyXMLList = msg.getData();
				String XMLList = odebranyXMLList.getString("listaGG");
				if (setContactbook(XMLList)) {
					// showNotification("Wysylam liste kontaktow...");
					showNotification("Lista kontaktow", "Gandu",
							"Wysylam liste kontaktow...", R.drawable.icon,
							GanduClient.class, -1, false, false, false);
					mNM.cancel(-1);
				}
				Log.i("GanduService", "Wyslalem liste kontaktow");

				break;
			case Common.CLIENT_SEND_MESSAGE:
				odebrany = msg.getData();
				String text = odebrany.getString("text");
				int ggnumber = odebrany.getInt("ggnumber");
				ChatMessage sm = new ChatMessage();
				try {
					int currentTime = (int) (System.currentTimeMillis() / 1000L);
					// byte[] paczka = sm.setMessage(text,ggnumber);
					byte[] paczka = sm.setMessage(text, ggnumber, currentTime);

					out.write(paczka);
					Log.i("GanduService", "Wyslalem wiadomosc");
					out.flush();

					// jesli nie powiedzie sie proba wyslania wiadomosci,
					// to nie zostanie ona dodana do bazy, bo program przejdzie
					// do
					// sekcji catch ponizej
					Log.i("[GanduService]START SQL",
							"dodanie wysylanej wiadomosci do bazy");
					long idWiadomosci = archiveSQL.addMessage(Integer
							.parseInt(ggnum), ggnumber, currentTime, text, -1,
							null);
					Log.i("[GanduService]START SQL", "[" + idWiadomosci
							+ "]dodanie wysylanej wiadomosci do bazy");
				} catch (Exception e) {
					Log.e("GanduService", "SendingMessage Failed!");
				}
				break;

			case Common.CLIENT_SEND_CONFERENCE_MESSAGE:
				odebrany = msg.getData();
				String textConference = odebrany.getString("text");
				ArrayList<String> konferenciGG = odebrany
						.getStringArrayList("konferenciGG");
				// int[] tablicaKonferentow = new int[konferenciGG.size()];
				int[] tablicaKonferentow = new int[konferenciGG.size() - 1];
				ArrayList<String> konferenciBezSiebie = new ArrayList<String>();
				for (int ii = 0; ii < konferenciGG.size(); ii++) {
					if (!konferenciGG.get(ii).equals(ggnum))
						konferenciBezSiebie.add(konferenciGG.get(ii));
				}
				// for(int i=0;i<konferenciGG.size();i++)
				// tablicaKonferentow[i] =
				// Integer.parseInt(konferenciGG.get(i));
				for (int i = 0; i < konferenciBezSiebie.size(); i++)
					tablicaKonferentow[i] = Integer
							.parseInt(konferenciBezSiebie.get(i));
				// START test wyslania wiadomosci konferencyjnej z gandu2 do
				// gandu3 i do mnie
				int currentTime1 = (int) (System.currentTimeMillis() / 1000L);
				ChatMessage cm = new ChatMessage();
				// byte[] paczki = cm.setConferenceMessages("12345",new
				// int[]{2522922,31841466}, currentTime1);
				byte[] paczki = cm.setConferenceMessages(textConference,
						tablicaKonferentow, currentTime1);
				try {
					out.write(paczki);
					Log.i("[GanduService]Konferencja", "Wyslalem wiadomosci");
					out.flush();
					// jesli nie powiedzie sie proba wyslania wiadomosci,
					// to nie zostanie ona dodana do bazy, bo program przejdzie
					// do
					// sekcji catch ponizej
					Log.i("[GanduService]START SQL",
							"dodanie wysylanej wiadomosci do bazy");
					//long idWiadomosciKonf = archiveSQL.addMessage(Integer
					//		.parseInt(ggnum), -1, currentTime1, textConference,
					//		-1, konferenciGG);
					long idWiadomosciKonf = archiveSQL.addMessage(Integer
							.parseInt(ggnum), -1, currentTime1, textConference,
							-1, konferenciBezSiebie);
					Log.i("[GanduService]START SQL", "[" + idWiadomosciKonf
							+ "]dodanie wysylanej wiadomosci do bazy");
				} catch (IOException e) {
					Log.e("[GanduService]Konferencja",
							"Blad wysylania lub zapisania w archiwum konferencji");
				}
				// KONIEC test wyslania wiadomosci konferencyjnej z gandu2 do
				// gandu i do mnie
				break;

			case Common.CLIENT_GET_STATUSES:
				Log.i("GanduService", "Recived from Client");
				odebrany = msg.getData();
				byte[] paczka = odebrany.getByteArray("bytePackage");
				try {
					out.write(paczka);
					Log.i("GanduService", "Wyslalem wiadomosc");
					out.flush();
				} catch (Exception e) {
					Log.e("GanduService", "Error with GetStatuses");
				}
				break;

			case Common.CLIENT_CHANGE_STATUS:
				odebrany = msg.getData();
				String status = odebrany.getString("status");
				String opisStatusu = odebrany.getString("opisStatusu");
				// jesli wczesniej wybrano w contactBook status niedostepny
				// i teraz zmienia ktos status na jakis inny niz niedostepny
				// to ponownie zaloguj na numer gg i haslo podane podczas
				// logowania
				if (statusLast.equals("Niedostepny")) {
					if (!ggnum.equals("") && !(status.equals("Niedostepny"))) {
						descriptionLast = opisStatusu;
						statusLast = status;
						rozlaczenieNiePrzezUzytkownika = true;
						if (inicjujLogowanie(ggnum)) {
							// showNotification("Zalogowany "+ggnum);
							//showNotification("Lista kontaktow", "Gandu",
							//		"Zalogowany " + ggnum, R.drawable.icon,
							//		GanduClient.class, -1, false, false);
							//mNM.cancel(-1);
						}
					}
				} else {
					if(status.equals("Niedostepny"))
						rozlaczenieNiePrzezUzytkownika = false;
					StatusChangeMessage scm = new StatusChangeMessage();
					try {
						byte[] pack = scm.setStatus(status, opisStatusu);
						out.write(pack);
						Log.i("GanduService", "Ustawiam status");
						out.flush();
						descriptionLast = opisStatusu;
						statusLast = status;
					} catch (Exception e) {
						Log.e("GanduService", "Status setting Failed!");
					}
				}
				break;
			case Common.CLIENT_ADD_NEW_CONTACT:
				odebrany = msg.getData();
				String numerGG = odebrany.getString("numerGG");
				String showName = odebrany.getString("showNameGG");
				if (numerIndex != null) {
					// sprawdzenie, czy nie ma juz danego numeru na liscie
					int miejsce = 0;
					if ((miejsce = numerIndex.indexOf(numerGG)) != -1) {
						numerIndex.remove(miejsce);
						numerShowName.remove(miejsce);
						numerShowName.add(showName);
						numerIndex.add(numerGG);
					}
					// jesli nie ma
					else {
						numerShowName.add(showName);
						numerIndex.add(numerGG);
					}
				}
				Boolean ignorowany = odebrany.getBoolean("ingorowany");
				try {
					int numerGGint = Integer.parseInt(numerGG);
					byte[] paczkaBajtow = null;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_ADD_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					if (ignorowany)
						dos.write(Common.GG_USER_BLOCKED);
					else
						dos.write(Common.GG_USER_NORMAL);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
				} catch (Exception e) {
					Log.e("GanduService", "Sending new contact info Failed!");
				}
				break;
			case Common.CLIENT_REMOVE_CONTACT:
				odebrany = msg.getData();
				String numerGGUsuwany = odebrany.getString("numerGG");
				Boolean ignorowanyUsuwany = odebrany.getBoolean("ingorowany");
				Boolean kontaktIstnieje = odebrany
						.getBoolean("kontaktIstnieje");
				try {
					int numerGGint = Integer.parseInt(numerGGUsuwany);
					byte[] paczkaBajtow = null;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					// jesli kontakt zostal usuniety z ignorowani
					// to ten typ kontaktu zostanie usuniety z serwera
					if (ignorowanyUsuwany)
						dos.write(Common.GG_USER_BLOCKED);
					// jesli zwykly kontakt zostal usuniety
					// i nie ma go w zadnej innej grupie
					// to ten kontakt zostanie usuniety z serwera
					else
						dos.write(Common.GG_USER_NORMAL);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
					// jesli zostal usuniety ignorowany kontakt
					// ale jest tez w innej grupie
					// to zostanie dodany normalny typ kontaktu na serwerze
					if (kontaktIstnieje) {
						baos = new ByteArrayOutputStream();
						dos = new DataOutputStream(baos);
						dos
								.writeInt(Integer
										.reverseBytes(Common.GG_ADD_NOTIFY));
						dos.writeInt(Integer.reverseBytes(5));
						dos.writeInt(Integer.reverseBytes(numerGGint));
						dos.write(Common.GG_USER_NORMAL);
						paczkaBajtow = baos.toByteArray();
						out.write(paczkaBajtow);
						out.flush();
					}
				} catch (Exception e) {
					Log.e("GanduService", "Sending new contact info Failed!");
				}
				break;
			case Common.CLIENT_IGNORE_CONTACT:
				odebrany = msg.getData();
				String numerGGIgnorowany = odebrany.getString("numerGG");
				try {
					int numerGGint = Integer.parseInt(numerGGIgnorowany);
					byte[] paczkaBajtow = null;
					// usuniecie GG_USER_NORMAL
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					dos.write(Common.GG_USER_NORMAL);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
					// dodanie GG_USER_BLOCKED
					baos = new ByteArrayOutputStream();
					dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_ADD_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					dos.write(Common.GG_USER_BLOCKED);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
				} catch (Exception e) {
					Log.e("GanduService", "Sending new contact info Failed!");
				}
				break;
			case Common.CLIENT_UNIGNORE_CONTACT:
				odebrany = msg.getData();
				String numerGGOdignorowany = odebrany.getString("numerGG");
				try {
					int numerGGint = Integer.parseInt(numerGGOdignorowany);
					byte[] paczkaBajtow = null;
					// usuniecie GG_USER_BLOCKER
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					dos.write(Common.GG_USER_BLOCKED);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
					// dodanie GG_USER_NORMAL
					baos = new ByteArrayOutputStream();
					dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_ADD_NOTIFY));
					dos.writeInt(Integer.reverseBytes(5));
					dos.writeInt(Integer.reverseBytes(numerGGint));
					dos.write(Common.GG_USER_NORMAL);
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
				} catch (Exception e) {
					Log.e("GanduService", "Sending new contact info Failed!");
				}
				break;
			case Common.CLIENT_CONTACTBOOK_OUT:
				//if (connected) {
				//	showNotification("", "Gandu", "Gandu", R.drawable.icon,
				//			ContactBook.class, -1, false, false);
				//}
				break;
			case Common.CLIENT_GG_NUM_SHOW_NAME:
				odebrany = msg.getData();
				// Log.i("[GanduService]","Otrzymalem CLIENT_GG_NUM_SHOW_NAME");
				if (odebrany.containsKey("ShowNameGGNumber")
						&& odebrany.containsKey("indexGGNumber")) {
					// Log.i("[GanduService]","Wewnatrz if CLIENT_GG_NUM_SHOW_NAME");
					// numerShowName = (HashMap<String,
					// String>)b.getParcelable("ShowNameGGNumber");
					numerShowName = odebrany
							.getStringArrayList("ShowNameGGNumber");
					// Log.i("[GanduService]","CLIENT_GG_NUM_SHOW_NAME numerShowName size: "+numerShowName.size());
					numerIndex = odebrany.getStringArrayList("indexGGNumber");
					// Log.i("[GanduService]","CLIENT_GG_NUM_SHOW_NAME numerIndex size: "+numerIndex.size());
				}
				break;
			case Common.CLIENT_GET_INITIAL_INFO:
				try {
					String zalogowanyNumer = ggnum;
					String ustawionyOpis = descriptionLast;
					String ustawionyStatus = statusLast;
					Message msg4 = Message.obtain(null,
							Common.CLIENT_SET_INITIAL_INFO, 0, 0);
					Bundle initialInfo = new Bundle();
					initialInfo.putString("mojNumer", zalogowanyNumer);
					initialInfo.putString("description", ustawionyOpis);
					initialInfo.putString("status", ustawionyStatus);
					msg4.setData(initialInfo);
					msg.replyTo.send(msg4);
				} catch (Exception excInitial) {
					Log.e("[GanduService]Common.CLIENT_GET_INITIAL_INFO",
							excInitial.getMessage());
				}
				break;
			case Common.CLIENT_EXIT_PROGRAM:
				odebrany = msg.getData();
				String opisPoWylogowaniu = odebrany.getString("opisStatusu");
				wylogujIZakoncz(opisPoWylogowaniu);
				break;
			case Common.CLIENT_SEND_FILE:
				try {
					odebrany = msg.getData();
					String wyslijPlikDo = odebrany.getString("numerGG");
					Log.i("Przesylanie pliku do ", wyslijPlikDo);
					String nazwaPliku = odebrany.getString("fileName");
					Log.i("Nazwa pliku", nazwaPliku);
					String sciezkaPliku = odebrany.getString("filePath");
					Log.i("Sciezka", sciezkaPliku);
					outcomingFileTransfer = new Files();
					outcomingFileTransfer.ggReceiverNumber = Integer
							.parseInt(wyslijPlikDo);
					outcomingFileTransfer.sendingFileName = nazwaPliku;
					outcomingFileTransfer.sendingFilePath = sciezkaPliku;
					// wyslanie do serwera GG zadania o 8-bajtowy (Long)
					// identyfikator
					// potrzebny do wymiany plikow
					byte[] zadanieID = outcomingFileTransfer.prepareIDRequest();
					out.write(zadanieID);
					out.flush();
				} catch (Exception excSendFile) {
					Log.e("[GanduService]CLIENT_SEND_FILE",
							"Blad wysylania zadania o id: "
									+ excSendFile.getMessage());
				}
				break;
			case Common.CLIENT_FILE_YES:
				Log.i("[GanduService]CLIENT_FILE_YES", "1");
				if (incomingFileTransfer != null) {
					Log.i("[GanduService]CLIENT_FILE_YES", "2");
					try {
						Log.i("[GanduService]CLIENT_FILE_YES", "3");
						/*
						 * byte[] komunikatOdrzuceniaPliku =
						 * incomingFileTransfer
						 * .rejectByReceiverRequest(incomingFileTransfer
						 * .id,Integer.parseInt(ggnum));
						 * out.write(komunikatOdrzuceniaPliku); out.flush();
						 */
						byte[] komunikatAkceptacjiPliku = incomingFileTransfer
								.acceptByReceiver(incomingFileTransfer.id,
										incomingFileTransfer.ggSenderNumber);
						out.write(komunikatAkceptacjiPliku);
						out.flush();

						Thread fileThread = new Thread(new receiveFileTask());
						fileThread.start();
						Log
								.i("[GanduService]CLIENT_FILE_YES",
										"Uruchomilem watek odierania pliku receiveFileTask");
						Log.i("[GanduService]CLIENT_FILE_YES", "4");
					} catch (Exception excRej) {
						Log.e("[GanduService]RejectFileError", excRej
								.getMessage());
					}
				}
				break;
			case Common.CLIENT_FILE_NO:
				Log.i("[GanduService]CLIENT_FILE_NO", "1");
				if (incomingFileTransfer != null) {
					Log.i("[GanduService]CLIENT_FILE_NO", "2");
					try {
						Log.i("[GanduService]CLIENT_FILE_NO", "3");
						byte[] komunikatOdrzuceniaPliku = incomingFileTransfer
								.rejectByReceiverRequest(
										incomingFileTransfer.id,
										incomingFileTransfer.ggSenderNumber);
						out.write(komunikatOdrzuceniaPliku);
						out.flush();
						Log.i("[GanduService]CLIENT_FILE_NO", "4");
					} catch (Exception excRej) {
						Log.e("[GanduService]RejectFileError", excRej
								.getMessage());
					}
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

	@Override
	public void onCreate() {

		//GEOtest
		geoSynchronizedList = new GeoSynchronizedList();
		/*if(geoHavePermission("2522922"))
			Toast.makeText(this,"2522922 Have geo permission", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(this,"2522922 DONT have geo permission", Toast.LENGTH_SHORT).show();
		if(geoHavePermission("100"))
			Toast.makeText(this,"100 Have geo permission", Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(this,"100 DONT have geo permission", Toast.LENGTH_SHORT).show();*/
		//GEOtest
			
		//Toast.makeText(this, "Gandu Service - Start", Toast.LENGTH_SHORT).show();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//START instrukcje potrzebne do setForeground/startForeground
		try {
	        mStartForeground = getClass().getMethod("startForeground",
	                mStartForegroundSignature);
	        mStopForeground = getClass().getMethod("stopForeground",
	                mStopForegroundSignature);
	    } catch (NoSuchMethodException e) {
	        // Running on an older platform.
	        mStartForeground = mStopForeground = null;
	    }
	    //KONIEC instrukcje potrzebne do setForeground/startForeground

		// pobranie opisu i statusu do logowania z preferencji
		descriptionLast = Prefs
				.getLoginDescriptionPref(getApplicationContext());
		statusLast = Prefs.getLoginStatusPref(getApplicationContext());

		// Display a notification about us starting.
		// showNotification("Witaj w Gandu");
		showNotification("Witaj w Gandu", "Gandu", "Witaj w Gandu",
				R.drawable.icon, GanduClient.class, -1, false, false, false);
		mNM.cancel(-1);

		archiveSQL = new ArchiveSQLite(this.getApplicationContext());

		numerShowName = new ArrayList<String>();
		numerIndex = new ArrayList<String>();
	}

	@Override
	public void onDestroy() {
		Log.i("[GanduService]onDestroy", "Wewnatrz onDestroy()");
		// zamkniecie socketa polaczonego z serwerem
		try {
			socket.close();
		} catch (Exception excclose) {
			;
		}

		// Cancel the persistent notification.
		// mNM.cancel(R.string.remote_service_started);
		mNM.cancel(111);

		// Tell the user we stopped.
		// Toast.makeText(this, R.string.remote_service_stopped,
		// Toast.LENGTH_SHORT).show();
		//Toast.makeText(this, "Gandu Service - Stop", Toast.LENGTH_SHORT).show();
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		super.onUnbind(intent);
		Log.i("[GanduService]onUnbind",
				"Wszedlem do onUbind, aktywnosci odlaczone!");
		return false;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private Notification showNotification(String wiadomosc, String tytul,
			String przeplywajacaWiadomosc, int ikona,
			Class<?> uruchamianaAktywnosc, int idWiadomosci,
			Boolean wstawNumerShowNameIMojNumer, Boolean setForeground, Boolean playSound) {
		Notification notification = new Notification(ikona,
				przeplywajacaWiadomosc, System.currentTimeMillis());

		Intent intent = new Intent(this, uruchamianaAktywnosc);
		if (wstawNumerShowNameIMojNumer) {
			intent.putExtra("mojNumer", ggnum);
			if (numerIndex != null) {
				Log.i("[GanduService]Notification", "numerShowName size:"
						+ numerShowName.size());
				Log.i("[GanduService]Notification", "numerIndex size:"
						+ numerIndex.size());
				intent.putStringArrayListExtra("ShowNameGGNumber",
						(ArrayList<String>) numerShowName.clone());
				intent.putStringArrayListExtra("indexGGNumber",
						(ArrayList<String>) numerIndex.clone());
			}
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		notification.setLatestEventInfo(this, tytul, wiadomosc, contentIntent);
		
		if(playSound)
		{
			if(Prefs.getMessageSoundPref(getApplicationContext()))
				//Ringtone rt = RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION, );
			{							
				try
				{
					Uri u = Uri.parse(Prefs.getRingtone(getApplicationContext()));
					notification.sound = u;
				}catch(Exception excRingtone)
				{
					Log.e("[GanduService]Dzwonek","Blad dzwonka");
				}
			}
		}

		// jesli funkcja zostala wywolana w celu ustawienia uslugi jako
		// foregroung
		// setForeground/startForeground, to funckja zwroci obiekt Notification
		// i nie wyswietli go.
		if (setForeground) {
			return notification;
		}

		if (idWiadomosci != -1)
			mNM.notify(idWiadomosci, notification);
		else
			mNM.notify(-1, notification);
		return null;
	}

	// watek odbierajacy wiadomosci od serwera GG
	public class ReplyInterpreter implements Runnable {
		public void run() {
			Log.i("ReplyInterpreter", "Watek wystartowal");
			Bundle wysylany;
			Boolean zalogowany = false;
			// START Wymuszenie stanu czuwania urzadzenia
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, "Gandu Serv");
			wl.acquire();
			// KONIEC Wymuszenie stanu czuwania urzadzenia
			//START zrobienie uslugi jako foreground
			Notification notify = showNotification(""+ggnum,
					"Gandu", "Zalogowany " + ggnum,
					R.drawable.icon, ContactBook.class, -5, false,
					true, false);
			startForegroundCompat(-5,notify);
			mNM.cancel(-1);
			//KONIEC zrobienie uslugi jako foreground
			// (zakonczone jest to poprzez wl.release();
			while (connected) {
				try {
					int typWiadomosci = Integer.reverseBytes(in.readInt());
					int pobraneBajty = 0;
					switch (typWiadomosci) {

					case Common.GG_WELCOME:

						Log.i("GanduService received: ", "" + typWiadomosci);
						int dlugoscDanych = Integer.reverseBytes(in.readInt());
						int ziarno = Integer.reverseBytes(in.readInt());
						wyslijPaczkeLogowania(ggnum, ggpass, ziarno);
						break;

					case Common.GG_LOGIN_OK80:
						int dlugoscOK = Integer.reverseBytes(in.readInt());
						byte[] zawartoscOK = new byte[dlugoscOK];
						int pobraneBajtyOK = 0;
						while (pobraneBajtyOK != dlugoscOK)
							pobraneBajtyOK += in.read(zawartoscOK,
									pobraneBajtyOK, dlugoscOK - pobraneBajtyOK);
						Log.i("GanduService received: ", "" + typWiadomosci);

						//showNotification("Lista kontaktow", "Gandu",
						//		"Zalogowany " + ggnum, R.drawable.icon,
						//		GanduClient.class, -1, false, false);
						//mNM.cancel(-1);
						zalogowany = true;

						wyslijWiadomoscOPustejLiscieKontaktow();
						Message msg = Message.obtain(null,
								Common.CLIENT_START_INTENT_CONTACTBOOK, 0, 0);
						// mClients.get(0).send(msg);
						for (int i = 0; i < mClients.size(); i++) {
							try {
								mClients.get(i).send(msg);
							} catch (Exception e) {
								Log.e("GanduService", "" + e.getMessage());
							}
						}
						Log.i("GanduService", "Sent to Client " + msg.what);
						break;

					case Common.GG_LOGIN_FAILED:
						Log.i("Received: ", "" + typWiadomosci);
						connected = false;
						break;

					case Common.GG_USERLIST_REPLY80:
						Log.i("GanduService received: ", "" + typWiadomosci);
						int dlugoscListy = Integer.reverseBytes(in.readInt());
						int dlugoscSpakowana = dlugoscListy - 1;
						byte typListaKont = in.readByte();
						String lista = null;

						if (typListaKont == 0x00) // GG_USERLIST_PUT_REPLY
													// 0x00/* poczï¿½tek
													// eksportu listy */
						{
							Log.i("typ replay import", "0x00");
							break;
						} else if (typListaKont == 0x02) // GG_USERLIST_PUT_MORE_REPLY
															// 0x02/*
															// kontynuacja */
						{
							Log.i("typ replay import", "0x02");
							break;
						}

						else if (typListaKont == 0x04
								&& licznik_serii_listy == 0) {
							Log.i("typ replay import", "Kontynuacja");
							skompresowanaLista = new byte[dlugoscSpakowana];
							licznik_serii_listy++;

							// byte[] spakowanaSkompresowana = new
							// byte[dlugoscSpakowana];
							int pobraneBajtyZeStosu = 0;
							while (pobraneBajtyZeStosu != dlugoscSpakowana)
								pobraneBajtyZeStosu += in.read(
										skompresowanaLista,
										pobraneBajtyZeStosu, dlugoscSpakowana
												- pobraneBajtyZeStosu);

							licznik_serii_listy++;
						} else if (typListaKont == 0x04
								&& licznik_serii_listy != 0) {
							Log.i("typ replay import",
									"Kontynuacja juz z jednym przebiegiem");
							byte[] bufor = skompresowanaLista;
							skompresowanaLista = new byte[skompresowanaLista.length
									+ dlugoscSpakowana];
							skompresowanaLista = bufor;

							int pobraneBajtyZeStosu = 0;
							while (pobraneBajtyZeStosu != dlugoscSpakowana)
								pobraneBajtyZeStosu += in.read(
										skompresowanaLista,
										pobraneBajtyZeStosu, dlugoscSpakowana
												- pobraneBajtyZeStosu);

							licznik_serii_listy++;
						} else if (typListaKont == 0x06) {
							Log.i("typ replay import", "Ostatnia seria");
							if (skompresowanaLista != null) {
								byte[] bufor = skompresowanaLista;
								skompresowanaLista = new byte[skompresowanaLista.length
										+ dlugoscListy];
								skompresowanaLista = bufor;
							} else {
								skompresowanaLista = new byte[dlugoscSpakowana];
							}

							int pobraneBajtyZeStosu = 0;
							while (pobraneBajtyZeStosu != dlugoscSpakowana)
								pobraneBajtyZeStosu += in.read(
										skompresowanaLista,
										pobraneBajtyZeStosu, dlugoscSpakowana
												- pobraneBajtyZeStosu);

							licznik_serii_listy = 0;
							lista = inflateContactBook(skompresowanaLista);
							skompresowanaLista = null;
						}

						if (!lista.contains("<Contact>"))
							lista = getInitialList();

						// saveOnSDCard(lista);
						// saveOnInternalMemory(lista);
						saveOnInternalMemory(lista, ggnum);
						Message msg2 = Message.obtain(null,
								Common.FLAG_CONTACTBOOK, 0, 0);
						wysylany = new Bundle();
						wysylany.putString("listaGG", lista);
						msg2.setData(wysylany);

						for (int i = 0; i < mClients.size(); i++) {
							try {
								mClients.get(i).send(msg2);
							} catch (Exception excMsg2) {
								Log
										.e("GanduService", ""
												+ excMsg2.getMessage());
							}
						}

						Log.i("Lista kontaktow", lista);
						break;
					case Common.GG_RECV_MSG80:
						//GEOtest
						myLocation.getLocation(getApplicationContext(), locationResult);

						/*double[] coord = geoMyCoordinates();
						if(coord != null)
							Log.e("GEO","MY coordinates: "+coord[0]+" "+coord[1]);
						else
							Log.e("GEO","NULL coordinates");*/
						//GEOtest
						Log.i("GanduService received message!: ", ""
								+ typWiadomosci);
						int dlugoscWiadomosci = Integer.reverseBytes(in
								.readInt());

						int sender = Integer.reverseBytes(in.readInt());
						int seq = Integer.reverseBytes(in.readInt());
						int time = Integer.reverseBytes(in.readInt());
						int classa = Integer.reverseBytes(in.readInt());
						int offset_plain = Integer.reverseBytes(in.readInt());
						int offset_attributes = Integer.reverseBytes(in
								.readInt());
						// dlugoscWiadomosci -(odjac) 24 poniewaz 6
						// wczesniejszych pol jest
						// typu int, kazdy po 4 bajty (6*4 = 24)
						byte[] pozostalaCzescWiadomosci = new byte[dlugoscWiadomosci - 24];
						pobraneBajty = 0;
						while (pobraneBajty != (dlugoscWiadomosci - 24))
							pobraneBajty += in.read(pozostalaCzescWiadomosci,
									pobraneBajty, (dlugoscWiadomosci - 24)
											- pobraneBajty);

						// wydzielenie tablicy bajtow atrybutow
						Log.i("[GanduService]Atrybuty",
								"Przed wydzieleniem atrybutow");
						byte[] atrybuty = new byte[dlugoscWiadomosci
								- offset_attributes];
						ByteArrayOutputStream atrybutyTemp = new ByteArrayOutputStream();
						DataOutputStream dosTemp = new DataOutputStream(
								atrybutyTemp);
						dosTemp.write(pozostalaCzescWiadomosci,
								offset_attributes - 24, dlugoscWiadomosci
										- offset_attributes);
						// dosTemp.close();
						atrybuty = atrybutyTemp.toByteArray();
						dosTemp.close();
						atrybutyTemp.close();
						Log.i("[GanduService]Atrybuty",
								"Po wydzieleniem atrybutow");
						// START przeczesanie atrybutow
						ArrayList<String> konferenci = new ArrayList<String>();
						if (atrybuty.length != 0) {
							ByteArrayInputStream odczyAtryb = new ByteArrayInputStream(
									atrybuty);
							DataInputStream dosOdczytAtryb = new DataInputStream(
									odczyAtryb);
							switch (dosOdczytAtryb.readByte()) {
							// sprawdzenie, czy to wiadomosc konferencyjna
							case 0x01:
								Log.i("[GanduService]Atrybuty",
										"wiadomosc konferencyjna!!");
								// odczytanie liczby pozostalych uczestnikow
								// konferencji(poza nadawca)
								int liczbaPozosyalychUczest = Integer
										.reverseBytes(dosOdczytAtryb.readInt());
								// konferenci = new
								// int[liczbaPozosyalychUczest];
								// pobranie numerow pozostalych uczestnikow
								for (int pozostali = 0; pozostali < liczbaPozosyalychUczest; pozostali++) {
									// konferenci[pozostali] =
									// Integer.reverseBytes(dosOdczytAtryb.readInt());
									konferenci
											.add(""
													+ Integer
															.reverseBytes(dosOdczytAtryb
																	.readInt()));
									// Log.i("[GanduService]Konferenci",
									// pozostali+". "+konferenci[pozostali]);
									Log.i("[GanduService]Konferenci", pozostali
											+ ". " + konferenci.get(pozostali));
								}
								break;
							default:
								;
							}
						}
						// KONIEC przeczesanie atrybutow
						// for(int bajty=0; bajty<atrybuty.length; bajty++)
						// Log.i("[GanduService]Atrybuty", ""+Integer.toString(
						// ( atrybuty[bajty] & 0xff ) + 0x100, 16).substring( 1
						// ));
						// wydzielenie tablicy bajtow atrybutow

						String trescCP1250 = new String(
								pozostalaCzescWiadomosci, offset_plain - 24,
								offset_attributes - (offset_plain + 1),
								"CP1250");
						// String tresc = new
						// String(trescCP1250.getBytes("CP1250"),"UTF-8");
						String tresc = trescCP1250;
						tresc = tresc.replace("\r", "");
						Log.e("[GanduService]Odczytana wiadomosc: ", tresc);
						Log.e("[GanduService]Od numeru: ", "" + sender);
						wysylany = new Bundle();
						wysylany.putString("tresc", tresc);
						wysylany.putString("wiadomoscOd", "" + sender);
						String wiadomoscOdSN = "" + sender;
						if (numerIndex != null) {
							int indeksSN = 0;
							// if((indeksSN = numerIndex.indexOf(sender)) != -1)
							if ((indeksSN = numerIndex.indexOf("" + sender)) != -1) {
								wiadomoscOdSN = numerShowName.get(indeksSN);
							}
						}
						wysylany.putString("wiadomoscOdName", wiadomoscOdSN);
						String czasNadejscia = new java.text.SimpleDateFormat(
								"dd/MM/yyyy HH:mm:ss")
								.format(new java.util.Date(time * 1000L));
						wysylany.putString("przyszlaO", czasNadejscia);

						Log.i("START SQL",
								"dodanie i odczytanie wiadomosci z bazy");
						long idWiadomosci;
						if (konferenci.size() == 0)
							idWiadomosci = archiveSQL.addMessage(sender,
									Integer.parseInt(ggnum), time, tresc, 1,
									null);
						else {
							idWiadomosci = archiveSQL.addMessage(sender,
									Integer.parseInt(ggnum), time, tresc, 1,
									(ArrayList<String>) konferenci.clone());
							konferenci.add("" + sender);
							wysylany.putStringArrayList("konferenciGG",
									(ArrayList<String>) konferenci.clone());
							konferenci.add(ggnum);
							Collections.sort(konferenci);
							String uczestnicyKonferencjiPosortowani = "";
							for (int i = 0; i < konferenci.size(); i++)
								uczestnicyKonferencjiPosortowani += konferenci
										.get(i)
										+ ";";
							// wyciecie srednika z konca ciagu
							uczestnicyKonferencjiPosortowani = uczestnicyKonferencjiPosortowani
									.substring(0,
											uczestnicyKonferencjiPosortowani
													.length() - 1);

							wysylany.putString("konferenci",
									uczestnicyKonferencjiPosortowani);
						}
						wysylany.putLong("idSQL", idWiadomosci);
						// Cursor wynikSQL =
						// archiveSQL.readMessage(idWiadomosci);
						// archiveSQL.showMessage(wynikSQL);
						Log.i("KONIEC SQL",
								"dodanie i odczytanie wiadomosci z bazy");

						/*
						 * byte[] tresc = new byte[dlugoscWiadomosci];
						 * pobraneBajty = 0; while (pobraneBajty !=
						 * dlugoscWiadomosci) pobraneBajty += in.read(tresc,
						 * pobraneBajty, dlugoscWiadomosci - pobraneBajty);
						 * Log.i("Odczytalem wiadomosc typu: ", "" +
						 * typWiadomosci);
						 * Log.i("Odczytalem wiadomosc o dlugosci: ", "" +
						 * dlugoscWiadomosci); wysylany = new Bundle();
						 * wysylany.putByteArray("tresc",tresc);
						 */

						// START show notification
						String senderName = pobierzShowName("" + sender);
						if (konferenci.size() == 0) {
							// showNotification(tresc, ""+sender,
							// ""+sender+": "+tresc, R.drawable.icon,
							showNotification(tresc, "" + senderName, ""
									+ senderName + ": " + tresc,
									R.drawable.icon, Chat.class, sender, true,
									false, true);
							// ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(300);

						} else {
							// showNotification(tresc, ""+sender,
							// "[Konferencja]"+sender+": "+tresc,
							// R.drawable.icon,
							showNotification(
									tresc,
									"" + senderName,
									"[Konferencja]" + senderName + ": " + tresc,
									R.drawable.icon, Chat.class, sender, true,
									false, true);
							// ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(300);

						}
						// jesli w ustawieniach jest, ze ma vibrowac, to niech
						// wibruje;)
						if (Prefs
								.getMessageVibrationPref(getApplicationContext()))
						{
							((Vibrator) getSystemService(VIBRATOR_SERVICE))
									.vibrate(300);
						}
						// KONIEC show notification
						Message message_recived = Message.obtain(null,
								Common.CLIENT_RECV_MESSAGE, 0, 0);
						message_recived.setData(wysylany);
						for (int i = 0; i < mClients.size(); i++) {
							try {
								mClients.get(i).send(message_recived);
							} catch (Exception e) {
								Log.e("GanduService", "" + e.getMessage());
							}
						}
						mNM.cancel(-1);
						break;
					case Common.GG_STATUS80:
					case Common.GG_NOTIFY_REPLY80:
						Log.i("GanduService received: ", "" + typWiadomosci);
						int dlugosc = Integer.reverseBytes(in.readInt());
						Log.i("Odczytalem GG_NOTIFY_REPLY80 o dlugosci: ", ""
								+ dlugosc);
						byte[] smiec = new byte[dlugosc];
						pobraneBajty = 0;
						while (pobraneBajty != dlugosc) {
							Log.i("GanduService", "Jestem w petli: "
									+ in.available());
							int uin = Integer.reverseBytes(in.readInt());
							Log.i("GanduService", "" + uin);
							int status = Integer.reverseBytes(in.readInt());
							Log.i("GanduService", "" + status);
							int features = Integer.reverseBytes(in.readInt());
							Log.i("GanduService", "" + features);
							int remote_ip = Integer.reverseBytes(in.readInt());
							Log.i("GanduService", "" + remote_ip);
							short remote_port = Short.reverseBytes(in
									.readShort());
							Log.i("GanduService", "" + remote_port);
							byte image_size = in.readByte();
							Log.i("GanduService", "" + image_size);
							byte unknown1 = in.readByte();
							Log.i("GanduService", "" + unknown1);
							int flags = Integer.reverseBytes(in.readInt());
							Log.i("GanduService", "" + flags);
							int description_size = Integer.reverseBytes(in
									.readInt());
							Log.i("GanduService", "" + description_size);
							byte[] bufor = new byte[description_size];
							String description = null;
							pobraneBajty += 28;
							int tmp = 0;
							if (description_size > 0) {
								Log.i("GanduService", "" + pobraneBajty + " "
										+ description_size + " "
										+ in.available());
								while (tmp != description_size)
									tmp += in.read(bufor, tmp, description_size
											- tmp);
								pobraneBajty += bufor.length;
								description = new String(bufor, "UTF-8");
							} else
								description = "";
							Log.i("GanduService", "" + description
									+ " PobraneBajty: " + pobraneBajty);
							try {
								Message msg3 = Message.obtain(null,
										Common.CLIENT_SET_STATUSES, 0, 0);
								wysylany = new Bundle();
								wysylany.putString("description", description);
								wysylany.putInt("ggnumber", uin);
								wysylany.putInt("status", status);
								msg3.setData(wysylany);
								for (int i = 0; i < mClients.size(); i++) {
									try {
										mClients.get(i).send(msg3);
									} catch (Exception e) {
										Log.e("GanduService", ""
												+ e.getMessage());
									}
								}
							} catch (Exception e) {
								;
							}

							// }

						}
						// pobraneBajty += in.read(smiec, pobraneBajty,
						// dlugosc-pobraneBajty);

						break;

					case Common.GG_DCC7_ID_REPLY:
						int dlugoscFile = Integer.reverseBytes(in.readInt());
						int transferType = Integer.reverseBytes(in.readInt());
						Long transferID = Long.reverseBytes(in.readLong());
						// outcomingFileTransfer nie powinno byc null,
						// bo ten obiekt jest inicjowany w momencie odebrania
						// od ContactBook polecenia wyslania pliku
						if (outcomingFileTransfer == null) {
							outcomingFileTransfer = new Files();
							Log
									.e("[GanduService]GG_DCC7_ID_REPLY",
											"outcomingFileTransfer byl null, a nie powinien");
						}
						outcomingFileTransfer.id = transferID;
						Log.i("[GanduService]outcomingFileID", ""
								+ outcomingFileTransfer.id);
						// teraz rozpocznij procedure wysylania pliku pod numer
						// GG
						// zapisany w outcomingFileTransfer.ggReceiverNumber
						// byte[] wyslijPlik =
						// outcomingFileTransfer.prepareSendFileRequest(Integer.parseInt(ggnum),
						// outcomingFileTransfer.ggReceiverNumber,
						// "data/data/android.pp/files/slij.txt", "slij.txt");
						byte[] wyslijPlik = outcomingFileTransfer
								.prepareSendFileRequest(
										Integer.parseInt(ggnum),
										outcomingFileTransfer.ggReceiverNumber);
						try {
							out.write(wyslijPlik);
							out.flush();
						} catch (Exception excSend) {
							Log.e("[GanduService]GG_DCC7_NEW",
									"Blad wyslania powiadomienia o checi przeslania pliku: "
											+ excSend.getMessage());
						}
						break;

					// odbiorca pliku zgodzil sie na odebranie
					case Common.GG_DCC7_ACCEPT:
						int dlugoscAccept = Integer.reverseBytes(in.readInt());
						int numerPrzyjmujacego = Integer.reverseBytes(in
								.readInt());
						Long idFile = Long.reverseBytes(in.readLong());
						Long offset = Long.reverseBytes(in.readLong());
						Log.i("[GanduService]GG_DCC7_ACCEPT", ""
								+ numerPrzyjmujacego + " zaakceptowal plik");
						Log.i("[GanduService]GG_DCC7_ACCEPT", "ma juz: "
								+ offset + " bajt(ow)");
						// jesli offset jest liczba rozna od 0, to wysylamy plik
						// nie
						// od poczatku, tylko od bajtu numer offset
						// if(offset != 0)
						// Toast.makeText(GanduService.this,
						// "Odbiorca zgodzi³ siê na odebranie pliku.\nWysy³am plik.",
						// Toast.LENGTH_SHORT).show();
						// create the message for the handler
						Message statuss = someHandler.obtainMessage();
						Bundle data = new Bundle();
						data
								.putString("SOMETHING",
										"Odbiorca zgodzi³ siê na odebranie pliku.\nWysy³am plik.");
						statuss.setData(data);
						someHandler.sendMessage(statuss);
						// uruchom watek wysylajacy plik do odbiorcy
						Thread fileThread = new Thread(new sendFileTask());
						fileThread.start();
						Log
								.i("[GanduService]GG_DCC7_ACCEPT",
										"Uruchomilem watek wysylania pliku sendFileTask");
						break;

					case Common.GG_DCC7_REJECT:
						int dlugoscReject = Integer.reverseBytes(in.readInt());
						int numerOdrzucajacego = Integer.reverseBytes(in
								.readInt());
						Long idRejectedFile = Long.reverseBytes(in.readLong());
						int powodOdrzucenia = Integer
								.reverseBytes(in.readInt());
						if (outcomingFileTransfer != null)
							outcomingFileTransfer = null;
						// Toast.makeText(GanduService.this,
						// "Odbiorca nie zgodzi³ siê na odebranie pliku",
						// Toast.LENGTH_SHORT).show();
						Message statuss2 = someHandler.obtainMessage();
						Bundle data2 = new Bundle();
						data2.putString("SOMETHING",
								"Odbiorca nie zgodzi³ siê na odebranie pliku");
						statuss2.setData(data2);
						someHandler.sendMessage(statuss2);

						Log.i("[GanduService]GG_DCC7_REJECT",
								"Odbiorca odrzucil plik. Powod: "
										+ powodOdrzucenia);
						break;

					case Common.GG_DCC7_NEW:
						int dlugoscDccNew = Integer.reverseBytes(in.readInt());
						Long idpolaczenia = Long.reverseBytes(in.readLong());
						int numerNad = Integer.reverseBytes(in.readInt());
						int numerOdbior = Integer.reverseBytes(in.readInt());
						int typPol = Integer.reverseBytes(in.readInt());
						// jesli ktos chce nam przeslac plik
						if (typPol == 4) {
							int nazwaPlik = 255;
							int pobraneBajtyFN = 0;
							byte[] nazwaPlikuByte = new byte[255];
							while (pobraneBajtyFN != nazwaPlik)
								pobraneBajtyFN += in.read(nazwaPlikuByte,
										pobraneBajtyFN, nazwaPlik
												- pobraneBajtyFN);
							int znakZeroWNazwie = 255;
							for (int szukZZ = 0; szukZZ < nazwaPlikuByte.length; szukZZ++) {
								if (nazwaPlikuByte[szukZZ] == 0) {
									znakZeroWNazwie = szukZZ;
									break;
								}
							}
							String nazwaPliku = new String(nazwaPlikuByte, 0,
									znakZeroWNazwie, "UTF-8");
							Long rozmiarPliku = Long
									.reverseBytes(in.readLong());
							byte[] nieuzywanyHash = new byte[20];
							pobraneBajtyFN = 0;
							while (pobraneBajtyFN != 20)
								pobraneBajtyFN += in.read(nieuzywanyHash,
										pobraneBajtyFN, 20 - pobraneBajtyFN);
							Log.i("[GanduService]Przyszedl plik",
									"nieuzywanyHash wczytany");
							incomingFileTransfer = new Files();
							incomingFileTransfer.id = idpolaczenia;
							incomingFileTransfer.ggReceiverNumber = numerOdbior;
							incomingFileTransfer.ggSenderNumber = numerNad;
							incomingFileTransfer.sendingFileName = nazwaPliku;
							incomingFileTransfer.sendingFilePath = "Gandu/IncomingFiles/";
							incomingFileTransfer.fileSize = rozmiarPliku;
							// wyslanie pytania do uzytkownika, czy chce odebrac
							// plik
							try {
								Message msg3 = Message.obtain(null,
										Common.CLIENT_FILE_QUESTION, 0, 0);
								wysylany = new Bundle();
								wysylany.putString("nazwaPliku",
										incomingFileTransfer.sendingFileName);
								wysylany.putInt("plikOd", numerNad);
								wysylany.putLong("rozmiarPliku", rozmiarPliku);
								msg3.setData(wysylany);
								for (int i = 0; i < mClients.size(); i++) {
									try {
										mClients.get(i).send(msg3);
									} catch (Exception e) {
										Log.e("GanduService", ""
												+ e.getMessage());
									}
								}
							} catch (Exception e) {
								;
							}
						}
						// np rozmowa audio, ktorej nie obslugujemy
						else {
							// - 20, bo idpolaczenia(1xLong(8B))
							// numerNad,numerOdbior,typPol(3xInt(4B)) = 20
							int pozostaleDlugosc = dlugoscDccNew - 20;
							int pobraneB = 0;
							byte[] reszta = new byte[pozostaleDlugosc];
							while (pobraneB != pozostaleDlugosc)
								pobraneB += in.read(reszta, pobraneB,
										pozostaleDlugosc - pobraneB);
						}
						break;

					default:
						Log.i("GanduService received default: ", ""
								+ typWiadomosci);
						int dlugoscBadziewia = Integer.reverseBytes(in
								.readInt());
						byte[] smieci = new byte[dlugoscBadziewia];
						pobraneBajty = 0;
						while (pobraneBajty != dlugoscBadziewia)
							pobraneBajty += in.read(smieci, pobraneBajty,
									dlugoscBadziewia - pobraneBajty);
						Log.i("Odczytalem cos typu: ", "" + typWiadomosci);
						Log.i("Odczytalem cos o dlugosci: ", ""
								+ dlugoscBadziewia);
					}
				} catch (Exception excThread) {
					Log.e("GanduService: ", "" + excThread.getMessage());
					connected = false;
				}
			}
			try {
				// START Zakonczenie wymuszania stanu czuwania urzadzenia
				wl.release();
				// KONIEC Zakonczenie wymuszania stanu czuwania urzadzenia
				//START wylaczenie uslugi jako foreground
				stopForegroundCompat(-5);
				//KONIEC wylaczenie uslugi jako foreground
				mCondition.open();
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				if (socket != null)
					socket.close();
				mNM.cancel(-1);
				if (zalogowany)
					showNotification("Nast¹pi³o roz³¹czenie z serwerem",
							"Gandu", "Nast¹pi³o roz³¹czenie z serwerem",
							R.drawable.icon, GanduClient.class, -1, false,
							false, false);
				else {
					showNotification("Nieudana próba logowania", "Gandu",
							"Nieudana próba logowania", R.drawable.icon,
							GanduClient.class, -1, false, false,false);
					// wyslanie do informacji o nieudanej probie logowania.
					// ganduClient po tej informacji bedzie wiadzialo, zeby
					// zamknac
					// okno z progress barem logowania
					Message msg3 = Message.obtain(null,
							Common.CLIENT_LOGIN_FAILED, 0, 0);
					for (int i = 0; i < mClients.size(); i++) {
						try {
							mClients.get(i).send(msg3);
						} catch (Exception e) {
							Log.e("GanduService", "" + e.getMessage());
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("[GanduService]Blad zamykania gniazda oraz in/out", e
						.getMessage());
			}
			Log.i("ReplyInterpreter", "Watek zakonczony");
			Log.i("GanduService", "wartosc connected = " + connected);
			// mConditionGGExit.open();
			if(zalogowany && rozlaczenieNiePrzezUzytkownika)
			{
				new Thread(reLoginTask).start();
			}
		}
	}
	
	// watek ponownego logowania
	private Runnable reLoginTask = new Runnable() {
		public void run() {			
			int numerProby = 1;
			mNM.cancel(-1);
			showNotification("["+numerProby+". próba] Trwa logowanie "+ggnum+"..","Gandu", "Trwa logowanie " + ggnum,
					R.drawable.icon, ContactBook.class, -5, false,false,false);
			//proba ponownego zalogowania co 5 sekund
			while(!inicjujLogowanie(ggnum) && rozlaczenieNiePrzezUzytkownika)
			{
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.e("[GanduService]reLoginTask","Blad sleep: "+e.getMessage());
				}
				numerProby++;
				showNotification("["+numerProby+". próba] Trwa logowanie "+ggnum+"..","Gandu", "Trwa logowanie " + ggnum,
						R.drawable.icon, ContactBook.class, -5, false,false,false);
			}
			if(!rozlaczenieNiePrzezUzytkownika)
				mNM.cancel(-5);
		}
	};

	// watek pingowy
	private Runnable pingTask = new Runnable() {
		public void run() {
			Log.i("[GanduService]pingTask", "Start watku pingTask");
			// wyslanie ping co 4 minuty
			// Zakonczenie watku jesli mCondition zostanie otwarte
			// (mCondition.open)
			while (!mCondition.block(4 * 60 * 1000)) {
				try {
					byte[] paczkaBajtow;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream dos = new DataOutputStream(baos);
					dos.writeInt(Integer.reverseBytes(Common.GG_PING));
					dos.writeInt(Integer.reverseBytes(0));
					paczkaBajtow = baos.toByteArray();
					out.write(paczkaBajtow);
					out.flush();
					Log.i("[GanduService]pingTask", "Wyslalem Ping do serwera");
				} catch (Exception excPing) {
					Log.e("[GanduService]pingTask",
							"pingTask watek rzucil blad: "
									+ excPing.getMessage());
				}
			}
			Log.i("[GanduService]pingTask", "Stop watku pingTask");
			// mConditionPingExit.open();
		}
	};

	// watek wyslania pliku
	public class sendFileTask implements Runnable {
		public void run() {
			Log.i("[GanduService]sendFileTask", "1. Start watku");
			Socket socketRelay = null;
			DataInputStream inRealy = null;
			DataOutputStream outRealy = null;
			// wys³anie GG_DCC7_RELAY_REQUEST z req_type równym
			// GG_DCC7_RELAY_TYPE_SERVER na relay.gadu-gadu.pl:80
			try {
				Log
						.i(
								"[GanduService]sendFileTask",
								"2. GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_SERVER na relay.gadu-gadu.pl:80");
				socketRelay = new Socket("relay.gadu-gadu.pl", 80);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());
				byte[] request1 = outcomingFileTransfer
						.prepareSendRelayRequest(Common.GG_DCC7_RELAY_TYPE_SERVER);
				outRealy.write(request1);
				outRealy.flush();
				Log
						.i("[GanduService]sendFileTask",
								"3. GG_DCC7_RELAY_REQUEST wyslalem GG_DCC7_RELAY_TYPE_SERVER");

				// proba odebrania odpowiedzi z adresem serwera proxy
				// jesli serwer nie odpowie, to sie rozlaczy, zostanie wywolany
				// catch i watek sie zakonczy
				int typWiadomosci = Integer.reverseBytes(inRealy.readInt());
				int packetSize = Integer.reverseBytes(inRealy.readInt());
				int count = Integer.reverseBytes(inRealy.readInt());
				// struktur jest:
				// (packetSize-(type(int4) + packetSize(int4) + count(int4) =
				// 12))/(ip(int4)+port(short2)+family(bajt1)=7)
				int liczbaStrukturZAdresamiProxy = (packetSize - 12) / 7;
				for (int iProxy = 0; iProxy < liczbaStrukturZAdresamiProxy; iProxy++) {
					int ip = inRealy.readInt();
					short port = Short.reverseBytes(inRealy.readShort());
					byte family = inRealy.readByte();
					// tu najczesciej podawane sa dwa razy te same adresy proxy
					// tylko z dwoma roznymi portami (najczesciej 80 i 443).
					// Nam wystarczy pierwszy adres proxy z pierwszym portem
					if (iProxy == 0) {
						outcomingFileTransfer.proxyIP1 = ip;
						outcomingFileTransfer.proxyPort1 = port;
					}
				}
				Log.i("[GanduService]sendFileTask",
						"3(1). GG_DCC7_RELAY_REQUEST odebralem Proxy: "
								+ outcomingFileTransfer.proxyIP1 + ":"
								+ outcomingFileTransfer.proxyPort1);
			} catch (Exception excRequest1) {
				Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
						"GG_DCC7_RELAY_REQUEST nie odpowiedzial adresem proxy");
			} finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}

			// wys³anie GG_DCC_INFO z typem GG_DCC7_TYPE_P2P
			try {
				Log.i("[GanduService]sendFileTask",
						"4. wys³anie GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
				// byte[] gg_dcc_info =
				// outcomingFileTransfer.prepareSendDCCInfo(Integer.parseInt(ggnum),
				// Common.GG_DCC7_TYPE_P2P);
				byte[] gg_dcc_info = outcomingFileTransfer
						.prepareSendDCCInfo(Common.GG_DCC7_TYPE_P2P);
				out.write(gg_dcc_info);
				out.flush();
				Log.i("[GanduService]sendFileTask",
						"4(1). wyslalem GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
			} catch (Exception excRequest1) {
				Log.e("[GanduService]GG_DCC_INFO",
						"GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
			}

			// wys³anie GG_DCC7_RELAY_REQUEST z req_type równym
			// GG_DCC7_RELAY_TYPE_PROXY
			// na adres serwera otrzymany w pierwszym GG_DCC7_RELAY_REQUEST
			// (Mo¿e siê zdarzyæ, ¿e ¿aden z serwerów nie odpowie na pierwsze
			// ¿¹danie,
			// wtedy jako adres drugiego ¿¹dania bierzemy znowu
			// relay.gadu-gadu.pl)
			try {
				Log
						.i("[GanduService]sendFileTask",
								"5. wys³anie GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_PROXY");
				if (outcomingFileTransfer.proxyIP1 != -1)
					socketRelay = new Socket(InetAddress.getByName(""
							+ outcomingFileTransfer.proxyIP1),
							outcomingFileTransfer.proxyPort1);
				else
					socketRelay = new Socket("relay.gadu-gadu.pl", 80);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());
				byte[] request1 = outcomingFileTransfer
						.prepareSendRelayRequest(Common.GG_DCC7_RELAY_TYPE_PROXY);
				outRealy.write(request1);
				outRealy.flush();
				Log
						.i("[GanduService]sendFileTask",
								"6. wyslalem GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_PROXY");

				// proba odebrania odpowiedzi z adresem serwera proxy
				// jesli serwer nie odpowie, to sie rozlaczy, zostanie wywolany
				// catch i watek sie zakonczy
				int typWiadomosci = Integer.reverseBytes(inRealy.readInt());
				int packetSize = Integer.reverseBytes(inRealy.readInt());
				int count = Integer.reverseBytes(inRealy.readInt());
				// struktur jest:
				// (packetSize-(type(int4) + packetSize(int4) + count(int4) =
				// 12))/(ip(int4)+port(short2)+family(bajt1)=7)
				int liczbaStrukturZAdresamiProxy = (packetSize - 12) / 7;
				for (int iProxy = 0; iProxy < liczbaStrukturZAdresamiProxy; iProxy++) {
					int ip = inRealy.readInt();
					short port = Short.reverseBytes(inRealy.readShort());
					byte family = inRealy.readByte();
					if (iProxy == 0) {
						outcomingFileTransfer.proxyIP2 = ip;
						outcomingFileTransfer.proxyPort2 = port;
					}
				}
				Log.i("[GanduService]sendFileTask", "6(1). Odebralem proxy: "
						+ outcomingFileTransfer.proxyIP2 + ":"
						+ outcomingFileTransfer.proxyPort2);
			} catch (Exception excRequest1) {
				Log.e("[GanduService]proxyIPRequest1Task",
						"GG_DCC7_RELAY_REQUEST nie odpowiedzial adresem proxy");
			}

			finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}

			// wyslanie pakietu GG_DCC7_INFO z polem type równym
			// GG_DCC7_TYPE_SERVER
			// i polem info w postaci: GGidCHrand
			// oraz wyslanie powitania i pliku przez serer proxy
			try {
				Log
						.i("[GanduService]sendFileTask",
								"7. wyslanie pakietu GG_DCC7_INFO z polem type równym GG_DCC7_TYPE_SERVER");
				if (outcomingFileTransfer.proxyIP2 != -1) {
					// byte[] gg_dcc_info =
					// outcomingFileTransfer.prepareSendDCCInfo(Integer.parseInt(ggnum),
					// Common.GG_DCC7_TYPE_SERVER);
					byte[] gg_dcc_info = outcomingFileTransfer
							.prepareSendDCCInfo(Common.GG_DCC7_TYPE_SERVER);
					out.write(gg_dcc_info);
					out.flush();
					Log
							.i("[GanduService]sendFileTask",
									"8. wyslalem pakietu GG_DCC7_INFO z polem type równym GG_DCC7_TYPE_SERVER");
				} else {
					Log.e("[GanduService]GG_DCC7_INFO",
							"Adres proxy2 jest -1, a nie powinien byc!");
					return;
				}

				socketRelay = new Socket(InetAddress.getByName(""
						+ outcomingFileTransfer.proxyIP2),
						outcomingFileTransfer.proxyPort2);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());

				// wyslanie pakietu powitalnego do serwera posredniczacego
				Log
						.i("[GanduService]sendFileTask",
								"9. wyslanie pakietu powitalnego do serwera posredniczacego");
				byte[] welcome = outcomingFileTransfer.prepareWelcomeProxy();
				outRealy.write(welcome);
				outRealy.flush();
				Log
						.i("[GanduService]sendFileTask",
								"10. wyslalem pakiet powitalnego do serwera posredniczacego");
				// serwer proxy powinien odpowiedziec tym samym powitaniem
				int reWelcome = Integer.reverseBytes(inRealy.readInt());
				Long reID = Long.reverseBytes(inRealy.readLong());
				Log.i("[GanduService]sendFileTask",
						"11. odebralem pakiet powitalny: " + reWelcome + " ID:"
								+ reID);
				// wyslanie bajtow pliku
				Log
						.i("[GanduService]sendFileTask",
								"12. wyslanie bajtow pliku");
				byte[] plik = outcomingFileTransfer.prepareFileBytes();
				outRealy.write(plik);
				outRealy.flush();
				Log.i("[GanduService]sendFileTask", "13. wyslalem bajty pliku");
				// Toast.makeText(GanduService.this, "Plik zosta³ wys³any",
				// Toast.LENGTH_SHORT).show();
				Message statuss2 = someHandler.obtainMessage();
				Bundle data2 = new Bundle();
				data2.putString("SOMETHING", "Plik zosta³ wys³any");
				statuss2.setData(data2);
				someHandler.sendMessage(statuss2);
			} catch (Exception excRequest1) {
				Log.e("[GanduService]proxy2", "cos z polaczeniem do proxy2");
				// Toast.makeText(GanduService.this,
				// "Wyst¹pi³ b³¹d podczas wysy³ania pliku",
				// Toast.LENGTH_SHORT).show();
				Message statuss2 = someHandler.obtainMessage();
				Bundle data2 = new Bundle();
				data2.putString("SOMETHING",
						"Wyst¹pi³ b³¹d podczas wysy³ania pliku");
				statuss2.setData(data2);
				someHandler.sendMessage(statuss2);
			}

			finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}
			Log.i("[GanduService]sendFileTask", "14. Koniec watku");
		}
	}

	// watek odbierania pliku
	public class receiveFileTask implements Runnable {
		public void run() {
			Log.i("[GanduService]receiveFileTask", "1. Start watku");
			Socket socketRelay = null;
			DataInputStream inRealy = null;
			DataOutputStream outRealy = null;
			// wys³anie GG_DCC7_RELAY_REQUEST z req_type równym
			// GG_DCC7_RELAY_TYPE_SERVER na relay.gadu-gadu.pl:80
			try {
				Log.i("[GanduService]receiveFileTask",
						"2. GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_SERVER na relay.gadu-gadu.pl:80");
				socketRelay = new Socket("relay.gadu-gadu.pl", 80);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());
				byte[] request1 = incomingFileTransfer
						.prepareSendRelayRequest(Common.GG_DCC7_RELAY_TYPE_SERVER);
				outRealy.write(request1);
				outRealy.flush();
				Log.i("[GanduService]receiveFileTask",
						"3. GG_DCC7_RELAY_REQUEST wyslalem GG_DCC7_RELAY_TYPE_SERVER");

				// proba odebrania odpowiedzi z adresem serwera proxy
				// jesli serwer nie odpowie, to sie rozlaczy, zostanie wywolany
				// catch i watek sie zakonczy
				int typWiadomosci = Integer.reverseBytes(inRealy.readInt());
				int packetSize = Integer.reverseBytes(inRealy.readInt());
				int count = Integer.reverseBytes(inRealy.readInt());
				// struktur jest:
				// (packetSize-(type(int4) + packetSize(int4) + count(int4) =
				// 12))/(ip(int4)+port(short2)+family(bajt1)=7)
				int liczbaStrukturZAdresamiProxy = (packetSize - 12) / 7;
				for (int iProxy = 0; iProxy < liczbaStrukturZAdresamiProxy; iProxy++) {
					int ip = inRealy.readInt();
					short port = Short.reverseBytes(inRealy.readShort());
					byte family = inRealy.readByte();
					// tu najczesciej podawane sa dwa razy te same adresy proxy
					// tylko z dwoma roznymi portami (najczesciej 80 i 443).
					// Nam wystarczy pierwszy adres proxy z pierwszym portem
					if (iProxy == 0) {
						incomingFileTransfer.proxyIP1 = ip;
						incomingFileTransfer.proxyPort1 = port;
					}
				}
				Log.i("[GanduService]receiveFileTask",
						"3(1). GG_DCC7_RELAY_REQUEST odebralem Proxy: "
								+ incomingFileTransfer.proxyIP1 + ":"
								+ outcomingFileTransfer.proxyPort1);
			} catch (Exception excRequest1) {
				Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
						"GG_DCC7_RELAY_REQUEST nie odpowiedzial adresem proxy");
			} finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}

			// wys³anie GG_DCC_INFO z typem GG_DCC7_TYPE_P2P
			try {
				Log.i("[GanduService]receiveFileTask",
						"4. wys³anie GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
				// byte[] gg_dcc_info =
				// outcomingFileTransfer.prepareSendDCCInfo(Integer.parseInt(ggnum),
				// Common.GG_DCC7_TYPE_P2P);
				byte[] gg_dcc_info = incomingFileTransfer
						.prepareSendDCCInfo(Common.GG_DCC7_TYPE_P2P);
				out.write(gg_dcc_info);
				out.flush();
				Log.i("[GanduService]receiveFileTask",
						"4(1). wyslalem GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
			} catch (Exception excRequest1) {
				Log.e("[GanduService]GG_DCC_INFO",
						"GG_DCC_INFO z typem GG_DCC7_TYPE_P2P");
			}

			// wys³anie GG_DCC7_RELAY_REQUEST z req_type równym
			// GG_DCC7_RELAY_TYPE_PROXY
			// na adres serwera otrzymany w pierwszym GG_DCC7_RELAY_REQUEST
			// (Mo¿e siê zdarzyæ, ¿e ¿aden z serwerów nie odpowie na pierwsze
			// ¿¹danie,
			// wtedy jako adres drugiego ¿¹dania bierzemy znowu
			// relay.gadu-gadu.pl)
			try {
				Log.i("[GanduService]receiveFileTask",
						"5. wys³anie GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_PROXY");
				if (incomingFileTransfer.proxyIP1 != -1)
					socketRelay = new Socket(InetAddress.getByName(""
							+ incomingFileTransfer.proxyIP1),
							incomingFileTransfer.proxyPort1);
				else
					socketRelay = new Socket("relay.gadu-gadu.pl", 80);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());
				byte[] request1 = incomingFileTransfer
						.prepareSendRelayRequest(Common.GG_DCC7_RELAY_TYPE_PROXY);
				outRealy.write(request1);
				outRealy.flush();
				Log.i("[GanduService]receiveFileTask",
						"6. wyslalem GG_DCC7_RELAY_REQUEST z req_type równym GG_DCC7_RELAY_TYPE_PROXY");

				// proba odebrania odpowiedzi z adresem serwera proxy
				// jesli serwer nie odpowie, to sie rozlaczy, zostanie wywolany
				// catch i watek sie zakonczy
				int typWiadomosci = Integer.reverseBytes(inRealy.readInt());
				int packetSize = Integer.reverseBytes(inRealy.readInt());
				int count = Integer.reverseBytes(inRealy.readInt());
				// struktur jest:
				// (packetSize-(type(int4) + packetSize(int4) + count(int4) =
				// 12))/(ip(int4)+port(short2)+family(bajt1)=7)
				int liczbaStrukturZAdresamiProxy = (packetSize - 12) / 7;
				for (int iProxy = 0; iProxy < liczbaStrukturZAdresamiProxy; iProxy++) {
					int ip = inRealy.readInt();
					short port = Short.reverseBytes(inRealy.readShort());
					byte family = inRealy.readByte();
					if (iProxy == 0) {
						incomingFileTransfer.proxyIP2 = ip;
						incomingFileTransfer.proxyPort2 = port;
					}
				}
				Log.i("[GanduService]receiveFileTask",
						"6(1). Odebralem proxy: "
								+ incomingFileTransfer.proxyIP2 + ":"
								+ incomingFileTransfer.proxyPort2);
			} catch (Exception excRequest1) {
				Log.e("[GanduService]proxyIPRequest1Task",
						"GG_DCC7_RELAY_REQUEST nie odpowiedzial adresem proxy");
			}

			finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}

			// wyslanie pakietu GG_DCC7_INFO z polem type równym
			// GG_DCC7_TYPE_SERVER
			// i polem info w postaci: GGidCHrand
			// oraz wyslanie powitania i pliku przez serer proxy
			try {
				Log.i("[GanduService]receiveFileTask",
						"7. wyslanie pakietu GG_DCC7_INFO z polem type równym GG_DCC7_TYPE_SERVER");
				if (incomingFileTransfer.proxyIP2 != -1) {
					// byte[] gg_dcc_info =
					// outcomingFileTransfer.prepareSendDCCInfo(Integer.parseInt(ggnum),
					// Common.GG_DCC7_TYPE_SERVER);
					byte[] gg_dcc_info = incomingFileTransfer
							.prepareSendDCCInfo(Common.GG_DCC7_TYPE_SERVER);
					out.write(gg_dcc_info);
					out.flush();
					Log.i("[GanduService]receiveFileTask",
							"8. wyslalem pakietu GG_DCC7_INFO z polem type równym GG_DCC7_TYPE_SERVER");
				} else {
					Log.e("[GanduService]GG_DCC7_INFO",
							"Adres proxy2 jest -1, a nie powinien byc!");
					return;
				}

				socketRelay = new Socket(InetAddress.getByName(""
						+ incomingFileTransfer.proxyIP2),
						incomingFileTransfer.proxyPort2);
				inRealy = new DataInputStream(socketRelay.getInputStream());
				outRealy = new DataOutputStream(socketRelay.getOutputStream());

				// wyslanie pakietu powitalnego do serwera posredniczacego
				Log.i("[GanduService]receiveFileTask",
						"9. wyslanie pakietu powitalnego do serwera posredniczacego");
				byte[] welcome = incomingFileTransfer.prepareWelcomeProxy();
				outRealy.write(welcome);
				outRealy.flush();
				Log.i("[GanduService]receiveFileTask",
						"10. wyslalem pakiet powitalnego do serwera posredniczacego");
				// serwer proxy powinien odpowiedziec tym samym powitaniem
				int reWelcome = Integer.reverseBytes(inRealy.readInt());
				Long reID = Long.reverseBytes(inRealy.readLong());
				Log.i("[GanduService]receiveFileTask",
						"11. odebralem pakiet powitalny: " + reWelcome + " ID:"
								+ reID);
				// oderanie bajtow pliku
				Log.i("[GanduService]receiveFileTask",
						"12. odebranie bajtow pliku");
				int dlugoscPliku = (int) incomingFileTransfer.fileSize
						.intValue();
				byte[] plik = new byte[incomingFileTransfer.fileSize.intValue()];
				int pobraneBajty = 0;
				while (pobraneBajty != dlugoscPliku)
					pobraneBajty += inRealy.read(plik, pobraneBajty,
							dlugoscPliku - pobraneBajty);
				Log.i("[GanduService]receiveFileTask",
						"13. odebralem bajty pliku");
				saveOnSDCardBytes(plik, "gandu/download/",
						incomingFileTransfer.sendingFileName);
				// Toast.makeText(GanduService.this,
				// "Plik zosta³ zapisany na karcie pamiêci",
				// Toast.LENGTH_SHORT).show();
				Message statuss2 = someHandler.obtainMessage();
				Bundle data2 = new Bundle();
				data2.putString("SOMETHING",
						"Plik zosta³ zapisany na karcie pamiêci");
				statuss2.setData(data2);
				someHandler.sendMessage(statuss2);
				Log.i("[GanduService]receiveFileTask",
						"14. zapisalem plik na karcie");
				// outRealy.write(plik);
				// outRealy.flush();
				// Log.i("[GanduService]receiveFileTask",
				// "15. wyslalem bajty pliku");
			} catch (Exception excRequest1) {
				Log.e("[GanduService]proxy2", "cos z polaczeniem do proxy2");
				// Toast.makeText(GanduService.this,
				// "Wyst¹pi³ b³¹d podczas odbierania pliku",
				// Toast.LENGTH_SHORT).show();
				Message statuss2 = someHandler.obtainMessage();
				Bundle data2 = new Bundle();
				data2.putString("SOMETHING",
						"Wyst¹pi³ b³¹d podczas odbierania pliku");
				statuss2.setData(data2);
				someHandler.sendMessage(statuss2);
			}

			finally {
				try {
					if (socketRelay != null)
						socketRelay.close();
					if (inRealy != null)
						inRealy.close();
					if (outRealy != null)
						outRealy.close();
				} catch (IOException e) {
					Log.e("[GanduService]GG_DCC7_RELAY_REQUEST",
							"Blad zamykania socketRelay, inRealy, outRealy");
				}
			}
			Log.i("[GanduService]receiveFileTask", "15. Koniec watku");
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			}
			return;
		}

		// Fall back on the old API.
		setForeground(true);
		mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			}
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		setForeground(false);
	}
}
