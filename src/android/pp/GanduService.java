package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

public class GanduService extends Service {
	String[] ip = null;
	private boolean connected = false;
	Socket socket;
	DataInputStream in;
	DataOutputStream out;
	String ggnum;
	String ggpass;
	byte[] skompresowanaLista = null;
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
    ArchiveSQLite archiveSQL;
    
  
   public Boolean getContactbook()
   {
	   int type = Integer.reverseBytes(Common.GG_USERLIST_REQUEST80);
	  byte contactbook_frame_type = Common.GG_USERLIST_GET;
	  //byte [] request ;
	  byte [] paczkaBajtow = null;
	  
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  DataOutputStream dos = new DataOutputStream(baos);
	  try
	  {
		  dos.writeInt(type);
		  dos.writeInt(Integer.reverseBytes(1));		
		  dos.write(contactbook_frame_type);
		  paczkaBajtow = baos.toByteArray();
		  out.write(paczkaBajtow);
		  out.flush();
		  Log.i("GanduService","Wykonalem getContactbook()");
	  }catch(Exception e)
	  {
		  return false;
	  }
	  return true;
}
   
   public Boolean setContactbook(String XMLList)
   {
	  int type = Integer.reverseBytes(Common.GG_USERLIST_REQUEST80);
	  byte contactbook_frame_type = Common.GG_USERLIST_PUT;
	  int dlugoscSkompresowana = 0;
	  Log.e("GanduService","Do serwisu trafila lista: "+XMLList);
	  //wyslanie pustej listy na serwer (nie dziala, ale to chyba jakis bug po stronie GG)
	  if(XMLList.trim().length() == 0)
	  {
		  //XMLList = " ";
		  //w dokumentacji jest napisane, ze aby wyslac pusta liste na serwer (usunac liste z sewera)
		  //nalezy wyslac jako liste kontaktow spacje (skompresowana deflate'em)
		  this.skompresowanaLista = new byte[]{(byte)0x78, (byte)0xda, (byte)0x53, 
				  (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x21, (byte)0x00, (byte)0x21};
		  dlugoscSkompresowana = this.skompresowanaLista.length;
		  Log.e("GanduService","Lista po zamianie przez if: "+XMLList);
	  }
	  else
	  {
		  //skompresowanaLista = deflateContactBook(XMLList);
		  dlugoscSkompresowana = deflateContactBook(XMLList);
	  }
	  byte [] paczkaBajtow = null;
	  
	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  DataOutputStream dos = new DataOutputStream(baos);
	  try
	  {
		  dos.writeInt(type);
		  //dos.writeInt(Integer.reverseBytes(1+skompresowanaLista.length));
		  dos.writeInt(Integer.reverseBytes(1+dlugoscSkompresowana));
		  dos.write(contactbook_frame_type);
		  dos.write(skompresowanaLista);
		  paczkaBajtow = baos.toByteArray();
		  out.write(paczkaBajtow);
		  out.flush();
		  Log.i("GanduService","Wykonalem setContactbook()");
		  Log.i("GanduService","Skompresowalem na: "+skompresowanaLista.length);
	  }catch(Exception e)
	  {
		  return false;	  	  
	  }
	  return true;	  	  
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
   public String inflateContactBook(byte [] paczkaBajtow)
   {
	   Log.i("Dlugosc skompresowanej listy kontaktow", ""+paczkaBajtow.length);
	   Inflater inf = new Inflater();
	   inf.setInput(paczkaBajtow,0,paczkaBajtow.length);
	   Log.i("GanduService", "Dlugosc skompresowana = "+paczkaBajtow.length);
	   byte [] result  = new byte [100000];
	   int resultLength;
	   String str = null;
	   try {
		   resultLength = inf.inflate(result);
		   inf.end();
		   str = new String(result, 0, resultLength, "UTF-8");
	    }catch (Exception e) {
	    	Log.e("GanduService","Blad Inflater"+e.getMessage());
	   }  
	  
	   return str;
   }
    
   public int deflateContactBook(String listaKontaktow)
   {
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
		   Log.i("GanduService","przed podaniem pustego output do deflate");
		   compressedDataLength = compresser.deflate(output);
		   Log.i("GanduService","po podaniu pustego output do deflate");
		   //wynik = output;
		   //this.skompresowanaLista = output;
		   ByteArrayOutputStream baos = new ByteArrayOutputStream();
		   DataOutputStream dos = new DataOutputStream(baos);
		   dos.write(output, 0, compressedDataLength);
		   this.skompresowanaLista = baos.toByteArray();
		   Log.i("GanduService","deflate dal wynik: "+compressedDataLength);
		} 
	   catch (Exception e) 
	   {
			// TODO Auto-generated catch block
			Log.e("Blad deflate!!", e.getMessage());
		}
	   
	   //return output;
	   return compressedDataLength;
   }
    
    public void wyloguj()
    {
    	;
    }
    
    public Boolean inicjujLogowanie(String numerGG)
    {
    	try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(
					"http://appmsg.gadu-gadu.pl/appsvc/appmsg_ver8.asp?fmnumber="+numerGG+"&lastmsg=20429&version=10.0.0.10450");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				while (instream.read(tmp) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp);
					BufferedReader br = new BufferedReader(new InputStreamReader(bais));
					String readline = br.readLine();
					Log.i("GANDU-TESTING", readline);
					ip = readline.split(" ");
				}
			}
			Log.i("ClientActivity", "Polaczenie....");

			String ipWyizolowany = ip[2].split(":")[0];
			//int portWyizolowany = Integer.parseInt(ip[2].split(":")[1]);
			int portWyizolowany = 443;
			socket = new Socket(ipWyizolowany, portWyizolowany);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			connected = true;
			//uruchom watek odbierajacy komunikaty z serwera GG
			Thread cThread = new Thread(new ReplyInterpreter());
			cThread.start();
    	}
    	catch(Exception excinit)
    	{
    		return false;
    	}
    	return true;
    }
    
    public Boolean wyslijPaczkeLogowania(String numerGG, String hasloGG, int ziarno)
    {
    	try
    	{
	    	int numergg = Integer.parseInt(numerGG);
	    	Logowanie logowanie = new Logowanie(ziarno, hasloGG, numergg, Common.GG_STATUS_AVAIL_DESCR, (byte)0xff, "http://code.google.com/p/gandu/");
	    	byte[] paczkalogowania = logowanie.pobraniePaczkiBajtow();
	    	out.write(paczkalogowania);
	    	out.flush();
			Toast.makeText(GanduService.this, "Zalogowany", Toast.LENGTH_SHORT).show();
    	}catch(Exception exclog2)
    	{
    		return false;
    	}
    	return true;
    }
    
    public Boolean wyslijWiadomoscOPustejLiscieKontaktow()
    {
    	try
    	{
			//wyslanie do serwera GG pakietu (o zerowej dlugosci) 
			//z informacja ze nie mamy nikogo na liscie kontaktow
			//#define GG_LIST_EMPTY 0x0012
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(baos2);
			dos2.writeInt(Integer.reverseBytes(Common.GG_LIST_EMPTY));
			dos2.writeInt(Integer.reverseBytes(0x0000));
			out.write(baos2.toByteArray());
    	}
    	catch(Exception excpustl)
    	{
    		return false;
    	}
    	return true;
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
                   
                    Message msg2 = Message.obtain(null,Common.FLAG_ACTIVITY_REGISTER, 0, 0);
                    
                    for(int i=0; i<mClients.size(); i++)
                    {
                    	try
                    	{
                    		mClients.get(i).send(msg2);
                    	}catch(Exception e)
                    	{
                    		Log.e("Blad", ""+e.getMessage());
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
                	
                	if(inicjujLogowanie(ggnum))
                	{
                		showNotification("Zalogowany "+ggnum);
                	}
                	break;
                case Common.CLIENT_GET_CONTACTBOOK:
                	if(getContactbook())
                	{
                		showNotification("Pobieram liste kontaktow...");
                	}
                	Log.i("GanduService","Pobieram liste kontaktow");
                	
                	break;
                case Common.CLIENT_SET_CONTACTBOOK:
                	Bundle odebranyXMLList = msg.getData();
                	String XMLList = odebranyXMLList.getString("listaGG");
                	if(setContactbook(XMLList))
                	{
                		showNotification("Wysylam liste kontaktow...");
                	}
                	Log.i("GanduService","Wyslalem liste kontaktow");
                	
                	break;
                case Common.CLIENT_SEND_MESSAGE:                	
                   	odebrany = msg.getData();
                	String text = odebrany.getString("text");
                	int ggnumber = odebrany.getInt("ggnumber");
					ChatMessage sm = new ChatMessage();
					try {
						int currentTime = (int)(System.currentTimeMillis() / 1000L);
						//byte[] paczka = sm.setMessage(text,ggnumber);
						byte[] paczka = sm.setMessage(text,ggnumber,currentTime);
						
						Log.i("[GanduService]START SQL","dodanie wysylanej wiadomosci do bazy");
						long idWiadomosci = archiveSQL.addMessage(Integer.parseInt(ggnum), ggnumber, currentTime, text, -1, "");
						Log.i("[GanduService]START SQL","["+idWiadomosci+"]dodanie wysylanej wiadomosci do bazy");
						
						out.write(paczka);
						Log.i("GanduService", "Wyslalem wiadomosc");
						out.flush();
					} catch (Exception e) {
						Log.e("GanduService", "SendingMessage Failed!");
					}
					break;
					
                case Common.CLIENT_GET_STATUSES:
                	Log.i("GanduService","Recived from Client");
                	odebrany = msg.getData();
                	byte[] paczka = odebrany.getByteArray("bytePackage");                	
                	try{
                		out.write(paczka);
                		Log.i("GanduService", "Wyslalem wiadomosc");
                		out.flush();
                	}
                	catch(Exception e) {
                		Log.e("GanduService","Error with GetStatuses");
                	}
                	break;
                	
                case Common.CLIENT_CHANGE_STATUS:                	
                   	odebrany = msg.getData();
                	String status = odebrany.getString("status");
                	String opisStatusu = odebrany.getString("opisStatusu");
                	StatusChangeMessage scm = new StatusChangeMessage();
					try {
						byte[] pack = scm.setStatus(status,opisStatusu);
						out.write(pack);
						Log.i("GanduService", "Ustawiam status");
						out.flush();
					} catch (Exception e) {
						Log.e("GanduService", "Status setting Failed!");
					}
				break;
                case Common.CLIENT_ADD_NEW_CONTACT:
                	odebrany = msg.getData();
                	String numerGG = odebrany.getString("numerGG");
                	Boolean ignorowany = odebrany.getBoolean("ingorowany");
                	try
                	{
						int numerGGint = Integer.parseInt(numerGG);
						byte [] paczkaBajtow = null;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(baos);
						dos.writeInt(Integer.reverseBytes(Common.GG_ADD_NOTIFY));
						dos.writeInt(Integer.reverseBytes(5));		
						dos.writeInt(Integer.reverseBytes(numerGGint));
						if(ignorowany)
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
                	Boolean kontaktIstnieje = odebrany.getBoolean("kontaktIstnieje");
                	try
                	{
						int numerGGint = Integer.parseInt(numerGGUsuwany);
						byte [] paczkaBajtow = null;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(baos);
						dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
						dos.writeInt(Integer.reverseBytes(5));		
						dos.writeInt(Integer.reverseBytes(numerGGint));
						//jesli kontakt zostal usuniety z ignorowani
						//to ten typ kontaktu zostanie usuniety z serwera
						if(ignorowanyUsuwany)
							dos.write(Common.GG_USER_BLOCKED);
						//jesli zwykly kontakt zostal usuniety
						//i nie ma go w zadnej innej grupie
						//to ten kontakt zostanie usuniety z serwera
						else
							dos.write(Common.GG_USER_NORMAL);
						paczkaBajtow = baos.toByteArray();
						out.write(paczkaBajtow);
						out.flush();
						//jesli zostal usuniety ignorowany kontakt
						//ale jest tez w innej grupie
						//to zostanie dodany normalny typ kontaktu na serwerze
						if(kontaktIstnieje)
						{
							baos = new ByteArrayOutputStream();
							dos = new DataOutputStream(baos);
							dos.writeInt(Integer.reverseBytes(Common.GG_ADD_NOTIFY));
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
                	try
                	{
						int numerGGint = Integer.parseInt(numerGGIgnorowany);
						byte [] paczkaBajtow = null;
						//usuniecie GG_USER_NORMAL
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(baos);
						dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
						dos.writeInt(Integer.reverseBytes(5));		
						dos.writeInt(Integer.reverseBytes(numerGGint));
						dos.write(Common.GG_USER_NORMAL);
						paczkaBajtow = baos.toByteArray();
						out.write(paczkaBajtow);
						out.flush();
						//dodanie GG_USER_BLOCKED
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
                	try
                	{
						int numerGGint = Integer.parseInt(numerGGOdignorowany);
						byte [] paczkaBajtow = null;
						//usuniecie GG_USER_BLOCKER
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutputStream dos = new DataOutputStream(baos);
						dos.writeInt(Integer.reverseBytes(Common.GG_REMOVE_NOTIFY));
						dos.writeInt(Integer.reverseBytes(5));		
						dos.writeInt(Integer.reverseBytes(numerGGint));
						dos.write(Common.GG_USER_BLOCKED);
						paczkaBajtow = baos.toByteArray();
						out.write(paczkaBajtow);
						out.flush();
						//dodanie GG_USER_NORMAL
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
    	
    	Toast.makeText(this, "Gandu Service - Start", Toast.LENGTH_SHORT).show();
    	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification("Witaj w Gandu");
        
        archiveSQL = new ArchiveSQLite(this.getApplicationContext());
    }

    @Override
    public void onDestroy() {
    	//zamkniecie socketa polaczonego z serwerem
    	//wyloguj();
    	try
    	{
    		socket.close();
    	}catch(Exception excclose) { ; }
    	
        // Cancel the persistent notification.
        //mNM.cancel(R.string.remote_service_started);
        mNM.cancel(111);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    	Toast.makeText(this, "Gandu Service - Stop", Toast.LENGTH_SHORT).show();
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification(String wiadomosc) {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        //CharSequence text = getText(R.string.remote_service_started);
    	CharSequence text = "Gandu";

        // Set the icon, scrolling text and timestamp
        //Notification notification = new Notification(R.drawable.stat_sample, text,
        //        System.currentTimeMillis());
    	Notification notification = new Notification(R.drawable.icon, wiadomosc,
    			System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, GanduClient.class), 0);

        // Set the info for the views that show in the notification panel.
        //notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
        notification.setLatestEventInfo(this, wiadomosc, text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        //mNM.notify(R.string.remote_service_started, notification);
        mNM.notify(111, notification);
    }
    
    //watek odbierajacy wiadomosci od serwera GG
    public class ReplyInterpreter implements Runnable {
		public void run() {
			Log.i("ReplyInterpreter", "Watek wystartowal");
			Bundle wysylany;
			while(connected)
			{
				try
				{
					int typWiadomosci = Integer.reverseBytes(in.readInt());
					int pobraneBajty = 0;
					switch(typWiadomosci){
					
					case Common.GG_WELCOME:
						
						Log.i("GanduService received: ", ""+typWiadomosci);
						int dlugoscDanych = Integer.reverseBytes(in.readInt());
						int ziarno = Integer.reverseBytes(in.readInt());
						wyslijPaczkeLogowania(ggnum, ggpass, ziarno);
						break;
						
					case Common.GG_LOGIN_OK80:
						int dlugoscOK = Integer.reverseBytes(in.readInt());
						byte[] zawartoscOK = new byte[dlugoscOK];
						int pobraneBajtyOK=0;
						while(pobraneBajtyOK != dlugoscOK)
							pobraneBajtyOK += in.read(zawartoscOK, pobraneBajtyOK, dlugoscOK-pobraneBajtyOK);
						Log.i("GanduService received: ", ""+typWiadomosci);
						wyslijWiadomoscOPustejLiscieKontaktow();
						Message msg = Message.obtain(null, Common.CLIENT_START_INTENT_CONTACTBOOK, 0 ,0 );
						//mClients.get(0).send(msg);
						for(int i=0; i<mClients.size(); i++)
	                    {
	                    	try
	                    	{
	                    		mClients.get(i).send(msg);
	                    	}catch(Exception e)
	                    	{
	                    		Log.e("GanduService", ""+e.getMessage());
	                    	}
	                    }
						Log.i("GanduService","Sent to Client "+msg.what);
						break;
						
					case Common.GG_LOGIN_FAILED:
						Log.i("Received: ", ""+typWiadomosci);
						connected = false;
						break;
						
					case Common.GG_USERLIST_REPLY80:
						Log.i("GanduService received: ", ""+typWiadomosci);
						int dlugoscListy = Integer.reverseBytes(in.readInt());
						int dlugoscSpakowana = dlugoscListy-1;
						byte typListaKont = in.readByte();
						if(typListaKont == 0x06)
							Log.i("typ replay import","0x06");
						else if(typListaKont == 0x00) //GG_USERLIST_PUT_REPLY 0x00/* pocz�tek eksportu listy */
						{
							Log.i("typ replay import","0x00");
							break;
						}
						else if(typListaKont == 0x02) //GG_USERLIST_PUT_MORE_REPLY 0x02/* kontynuacja */
						{
							Log.i("typ replay import","0x02");
							break;
						}
						else
						{
							Log.i("INNY!! typ replay import",""+typListaKont);
							break;
						}
						
						byte[] spakowanaSkompresowana = new byte[dlugoscSpakowana];
						int pobraneBajtyZeStosu=0;
						while(pobraneBajtyZeStosu != dlugoscSpakowana)
							pobraneBajtyZeStosu += in.read(spakowanaSkompresowana, pobraneBajtyZeStosu, dlugoscSpakowana-pobraneBajtyZeStosu);
						String lista = inflateContactBook(spakowanaSkompresowana);
						
						saveOnSDCard(lista);
						Message msg2 = Message.obtain(null,Common.FLAG_CONTACTBOOK, 0, 0);
						wysylany = new Bundle();
						wysylany.putString("listaGG", lista);
						msg2.setData(wysylany);
						for(int i=0; i<mClients.size(); i++)
	                    {
	                    	try
	                    	{
	                    		mClients.get(i).send(msg2);
	                    	}catch(Exception excMsg2)
	                    	{
	                    		Log.e("GanduService", ""+excMsg2.getMessage());
	                    	}
	                    }
						
						Log.i("Lista kontaktow", lista);
						break;
					case Common.GG_RECV_MSG80:
						Log.i("GanduService received message!: ", ""
								+ typWiadomosci);
						int dlugoscWiadomosci = Integer.reverseBytes(in
								.readInt());
						
						int sender = Integer.reverseBytes(in.readInt());
						int seq = Integer.reverseBytes(in.readInt());
						int time = Integer.reverseBytes(in.readInt());
						int classa = Integer.reverseBytes(in.readInt());
						int offset_plain = Integer.reverseBytes(in.readInt());
						int offset_attributes = Integer.reverseBytes(in.readInt());
						//dlugoscWiadomosci - 24 poniewaz 6 wczesniejszych pol jest
						//typu int, kazdy po 4 bajty (6*4 = 24)
						byte[] pozostalaCzescWiadomosci = new byte[dlugoscWiadomosci - 24];
						pobraneBajty=0;
						while(pobraneBajty != (dlugoscWiadomosci - 24))
							pobraneBajty += in.read(pozostalaCzescWiadomosci, pobraneBajty, (dlugoscWiadomosci - 24)-pobraneBajty);
						String trescCP1250 = new String(pozostalaCzescWiadomosci, offset_plain-24, offset_attributes-(offset_plain+1), "CP1250");
						//String tresc = new String(trescCP1250.getBytes("CP1250"),"UTF-8");
						String tresc =trescCP1250;
						Log.e("[GanduService]Odczytana wiadomosc: ", tresc);
						Log.e("[GanduService]Od numeru: ", "" + sender);
						wysylany = new Bundle();
						wysylany.putString("tresc",tresc);
						wysylany.putString("wiadomoscOd",""+sender);
						String czasNadejscia = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date (time*1000L));
						wysylany.putString("przyszlaO",czasNadejscia);
						
						Log.i("START SQL","dodanie i odczytanie wiadomosci z bazy");
						long idWiadomosci = archiveSQL.addMessage(sender, Integer.parseInt(ggnum), time, tresc, 1, "");
						wysylany.putLong("idSQL", idWiadomosci);
						//Cursor wynikSQL = archiveSQL.readMessage(idWiadomosci);
						//archiveSQL.showMessage(wynikSQL);
						Log.i("KONIEC SQL","dodanie i odczytanie wiadomosci z bazy");
						
						/*byte[] tresc = new byte[dlugoscWiadomosci];
						pobraneBajty = 0;
						while (pobraneBajty != dlugoscWiadomosci)
							pobraneBajty += in.read(tresc, pobraneBajty,
									dlugoscWiadomosci - pobraneBajty);
						Log.i("Odczytalem wiadomosc typu: ", "" + typWiadomosci);
						Log.i("Odczytalem wiadomosc o dlugosci: ", ""
								+ dlugoscWiadomosci);
						wysylany = new Bundle();
						wysylany.putByteArray("tresc",tresc);*/
						Message message_recived = Message.obtain(null, Common.CLIENT_RECV_MESSAGE, 0 ,0 );
						message_recived.setData(wysylany);
						for(int i=0; i<mClients.size(); i++)
	                    {
	                    	try
	                    	{
	                    		mClients.get(i).send(message_recived);
	                    	}catch(Exception e)
	                    	{
	                    		Log.e("GanduService", ""+e.getMessage());
	                    	}
	                    }
						
						break;
					case Common.GG_NOTIFY_REPLY80:
						Log.i("GanduService received: ", ""+typWiadomosci);
						int dlugosc = Integer.reverseBytes(in.readInt());
						Log.i("Odczytalem GG_NOTIFY_REPLY80 o dlugosci: ", ""+dlugosc);
						byte[] smiec = new byte[dlugosc];
						pobraneBajty=0;
						while(pobraneBajty != dlugosc){
							Log.i("GanduService","Jestem w petli: "+in.available());
							int uin = Integer.reverseBytes(in.readInt()); 
							Log.i("GanduService",""+uin);
							int status = Integer.reverseBytes(in.readInt()); 
							Log.i("GanduService",""+status);
							int features = Integer.reverseBytes(in.readInt()); 
							Log.i("GanduService",""+features);
							int remote_ip = Integer.reverseBytes(in.readInt()); 
							Log.i("GanduService",""+remote_ip);
							short remote_port = Short.reverseBytes(in.readShort()); 
							Log.i("GanduService",""+remote_port);
							byte image_size = in.readByte(); 
							Log.i("GanduService",""+image_size);
							byte unknown1 = in.readByte(); 
							Log.i("GanduService",""+unknown1);
							int flags = Integer.reverseBytes(in.readInt());
							Log.i("GanduService",""+flags);
							int description_size = Integer.reverseBytes(in.readInt()); 
							Log.i("GanduService",""+description_size);
							byte [] bufor = new byte [description_size];
							String description = null;
							pobraneBajty += 28;
							int tmp=0;
							if (description_size > 0)
							{
								Log.i("GanduService",""+pobraneBajty+" "+description_size + " "+ in.available());
								while (tmp!=description_size)
								tmp+=in.read(bufor, tmp , description_size-tmp); 
								pobraneBajty+=bufor.length;
								description = new String(bufor,"CP1250");
								Log.i("GanduService",""+description+" PobraneBajty: "+pobraneBajty);
								
							}
							
						}
							//pobraneBajty += in.read(smiec, pobraneBajty, dlugosc-pobraneBajty);
						
						break;
						
					default:
							Log.i("GanduService received default: ", ""+typWiadomosci);
							int dlugoscBadziewia = Integer.reverseBytes(in.readInt());
							byte[] smieci = new byte[dlugoscBadziewia];
							pobraneBajty=0;
							while(pobraneBajty != dlugoscBadziewia)
								pobraneBajty += in.read(smieci, pobraneBajty, dlugoscBadziewia-pobraneBajty);
							Log.i("Odczytalem cos typu: ", ""+typWiadomosci);
							Log.i("Odczytalem cos o dlugosci: ", ""+dlugoscBadziewia);
					}
				}
				catch(Exception excThread)
				{
					Log.e("GanduService: ", ""+excThread.getMessage());
					connected = false;
				}
			}
			Log.i("ReplyInterpreter", "Watek zakonczony");
			Log.i("GanduService", "wartosc connected = "+connected);
		}
    }
}
