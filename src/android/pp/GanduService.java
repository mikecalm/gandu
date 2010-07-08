package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class GanduService extends Service {
	//Toast tst = new Toast(this);
	String[] ip = null;
	private boolean connected = false;
	Socket socket;
	DataInputStream in;
	DataOutputStream out;
	String ggnum;
	String ggpass;
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;


    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;
    
    /**
     * Command to service to login user.
    */
   static final int MSG_LOGIN = 3;
    
    
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
			Log.d("ClientActivity", "Polaczenie....");

			String ipWyizolowany = ip[2].split(":")[0];
			//int portWyizolowany = Integer.parseInt(ip[2].split(":")[1]);
			int portWyizolowany = 443;
			socket = new Socket(ipWyizolowany, portWyizolowany);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			//Socket socket = new Socket(ipWyizolowany, portWyizolowany);
			//Socket socket = new Socket(ipWyizolowany, 443);
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
	    	//GG_STATUS_AVAIL_DESCR	0x0004	Dostepny (z opisem)
			int numergg = Integer.parseInt(numerGG);
	    	Logowanie logowanie = new Logowanie(ziarno, hasloGG, numergg, 0x0004, (byte)0xff, "Gandu sza³:]");
	    	byte[] paczkalogowania = logowanie.pobraniePaczkiBajtow();
	    	//DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	    	//out = new DataOutputStream(socket.getOutputStream());
	    	//wyslanie paczki logowania 
	    	//#define GG_LOGIN80 0x0031
	    	//do serwera
	    	out.write(paczkalogowania);
	    	out.flush();
			Toast.makeText(GanduService.this, "Zalogowany", Toast.LENGTH_LONG).show();
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
			//wyslanie do serwera GG pakietu (o zerowej d³ugoœci) 
			//z informacja ze nie mamy nikogo na liscie kontaktow
			//#define GG_LIST_EMPTY 0x0012
			ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(baos2);
			dos2.writeInt(Integer.reverseBytes(0x0012));
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
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_LOGIN:
                	Bundle odebrany = msg.getData();
                	ggnum = odebrany.getString("numerGG");
                	ggpass = odebrany.getString("hasloGG");
                	//jesli logowanie powiedzie sie
                	//if(zaloguj(ggnum, ggpass))
                	if(inicjujLogowanie(ggnum))
                	{
                		showNotification("Zalogowany "+ggnum);
                		//Toast.makeText(GanduService.this, "Zalogowany!!", Toast.LENGTH_LONG).show();
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
    	Toast.makeText(this, "Gandu Service - Start", Toast.LENGTH_LONG).show();
    	
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification("Witaj w Gandu");
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
    	Notification notification = new Notification(R.drawable.icon, text,
    			System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, GanduClient.class), 0);

        // Set the info for the views that show in the notification panel.
        //notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
        notification.setLatestEventInfo(this, text, wiadomosc, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        //mNM.notify(R.string.remote_service_started, notification);
        mNM.notify(111, notification);
    }
    
    //watek odbierajacy wiadomosci od serwera GG
    public class ReplyInterpreter implements Runnable {
		public void run() {
			Log.i("WSZEDLEM DO WATKU!!!!!", "WSZEDLEM DO WATKU!!!!!");
			while(connected)
			{
				try
				{
					int typWiadomosci = Integer.reverseBytes(in.readInt());
					switch(typWiadomosci){
					case 0x0001:
						Log.i("Odebrane: ", ""+typWiadomosci);
						int dlugoscDanych = Integer.reverseBytes(in.readInt());
						int ziarno = Integer.reverseBytes(in.readInt());
						wyslijPaczkeLogowania(ggnum, ggpass, ziarno);
						break;
					case 0x0035:
						Log.i("Odebrane: ", ""+typWiadomosci);
						wyslijWiadomoscOPustejLiscieKontaktow();
						break;
					case 0x0009:
						Log.i("Odebrane: ", ""+typWiadomosci);
						connected = false;
						break;
					case 0x002e:
						Log.i("Odebrane: ", ""+typWiadomosci);
						break;
					default:
							Log.i("Odebrane default: ", ""+typWiadomosci);
							int dlugoscBadziewia = Integer.reverseBytes(in.readInt());
							byte[] smieci = new byte[2048];
							//in.read(smieci, 0, dlugoscBadziewia);
							in.read(smieci, 0, dlugoscBadziewia);
							Log.i("Odczytalem smieci typu: ", ""+typWiadomosci);
							Log.i("Odczytalem smieci o dlugosci: ", ""+dlugoscBadziewia);
					}
				}
				catch(Exception excThread)
				{
					Log.i("WYPIEPRZYLEM SIE!!", ""+excThread.getMessage());
					connected = false;
				}
			}
			Log.i("WYSZEDLEM Z WATKU!!!!!", "WYSZEDLEM Z WATKU!!!!!");
			Log.i("wartosc connected = ", ""+connected);
		}
    }
}
