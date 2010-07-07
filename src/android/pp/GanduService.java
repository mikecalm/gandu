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
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;
    
    /**
     * Command to service to login user.
    */
   static final int MSG_LOGIN = 4;
    
    
    public void wyloguj()
    {
    	;
    }
    
	public Boolean zaloguj(String numerGG, String hasloGG)
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
			int portWyizolowany = Integer.parseInt(ip[2].split(":")[1]);
			socket = new Socket(ipWyizolowany, portWyizolowany);
			//Socket socket = new Socket(ipWyizolowany, portWyizolowany);
			//Socket socket = new Socket(ipWyizolowany, 443);
			
			connected = true;
			try {
				//DataInputStream in = new DataInputStream(socket.getInputStream());
				in = new DataInputStream(socket.getInputStream());
				int typKom = Integer.reverseBytes(in.readInt());
				int dlugoscDanych = Integer.reverseBytes(in.readInt());
				int ziarno = Integer.reverseBytes(in.readInt());
				Log.i("GANDU-TESTING typ komunikatu: ", "" + typKom);
				Log.i("GANDU-TESTING dlugosc danych: ", ""
						+ dlugoscDanych);
				Log.i("GANDU-TESTING ziarno: ", "" + ziarno);
				/*
				 * po podsluchaniu wiresharkiem i wychwyceniu paczki z
				 * ziarnem
				 * i przeliczeniu go z systemu szesnastkowego na
				 * dziesietny
				 * wartosc ziarna zgadza sie z ta uzyskana w zmiennej
				 * int ziarno
				 * wiec jest dobrze, teraz mozna dalej kombinowac ze
				 * skompletowaniem
				 * paczki ktora pozniej trzeba odeslac;)
				*/

            	//GG_STATUS_AVAIL_DESCR	0x0004	Dostepny (z opisem)
				int numergg = Integer.parseInt(numerGG);
            	Logowanie logowanie = new Logowanie(ziarno, hasloGG, numergg, 0x0004, (byte)0xff, "Gandu sza³:]");
            	byte[] paczkalogowania = logowanie.pobraniePaczkiBajtow();
            	//DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            	out = new DataOutputStream(socket.getOutputStream());
            	//wyslanie paczki logowania 
            	//#define GG_LOGIN80 0x0031
            	//do serwera
            	out.write(paczkalogowania);
            	out.flush();
            	//po prawidlowym zalogowaniu sie serwer zawraca pakiet typu
            	//#define GG_LOGIN80_OK 0x0035
            	//w przypadku bledy autoryzacji otrzymamy pakiet
            	//#define GG_LOGIN80_FAILED 0x0043
            	int typOdpSerw = Integer.reverseBytes(in.readInt());
            	int dlugoscOdpSerw = Integer.reverseBytes(in.readInt());
            	int poleUnknownOdpSerw = 0;
            	if(dlugoscOdpSerw != 0)
            		poleUnknownOdpSerw = Integer.reverseBytes(in.readInt());
            	if(typOdpSerw == 0x00000035)
        		{
            		Log.i("Zalogowany", "Pole unknown: "+poleUnknownOdpSerw);
            		Toast.makeText(GanduService.this, "Zalogowany", Toast.LENGTH_LONG).show();
            		//wyslanie do serwera GG pakietu (o zerowej d³ugoœci) 
            		//z informacja ze nie mamy nikogo na liscie kontaktow
            		//#define GG_LIST_EMPTY 0x0012
            		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        			DataOutputStream dos2 = new DataOutputStream(baos2);
        			dos2.writeInt(Integer.reverseBytes(0x0012));
        			dos2.writeInt(Integer.reverseBytes(0x0000));
            		out.write(baos2.toByteArray());
            		//odebranie odpowiedzi serwera
            		int typOdpSerw2 = Integer.reverseBytes(in.readInt());
                	int dlugoscOdpSerw2 = Integer.reverseBytes(in.readInt());
                	byte[] resztaOdp = new byte[dlugoscOdpSerw2];
                	if(dlugoscOdpSerw2 != 0)
                		in.read(resztaOdp, 0, dlugoscOdpSerw2);
        		}
            	else
            	{
            		Log.i("Blad autoryzacji", "Typ bledu: "+typOdpSerw);
        		}
			} catch (Exception e) {
				Log.e("ClientActivity", "S: Error", e);
				connected = false;
			}
			//socket.close();
		} catch (Exception e) {
			;
		}
		finally{
			if(connected)
				//jesli udalo sie polaczyc z serwerem funkcja zwraca true
				return true;
			//w przypadku nieudanej proby zalogowania funkcja zwraca false
			return false;
		}
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
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    if(msg.arg1==0)
                    {
                    	Bundle odebrany = msg.getData();
                    	Log.i("S_odebrane: ",odebrany.getString("widomoscString"));
                    	Log.i("S_odebrane: ",""+odebrany.getInt("widomoscInt"));
                    	for (int i=mClients.size()-1; i>=0; i--) {
	                        try {
	                            mClients.get(i).send(Message.obtain(null,
	                                    MSG_SET_VALUE, mValue, 0));
	                        } catch (RemoteException e) {
	                            // The client is dead.  Remove it from the list;
	                            // we are going through the list from back to front
	                            // so this is safe to do inside the loop.
	                            mClients.remove(i);
	                        }
	                    }
                    }
                    else
                    {
	                    for (int i=mClients.size()-1; i>=0; i--) {
	                        try {
	                            mClients.get(i).send(Message.obtain(null,
	                                    MSG_SET_VALUE, mValue, 0));
	                        } catch (RemoteException e) {
	                            // The client is dead.  Remove it from the list;
	                            // we are going through the list from back to front
	                            // so this is safe to do inside the loop.
	                            mClients.remove(i);
	                        }
	                    }
                    }
                    break;
                case MSG_LOGIN:
                	Bundle odebrany = msg.getData();
                	String ggnum = odebrany.getString("numerGG");
                	String ggpass = odebrany.getString("hasloGG");
                	//jesli logowanie powiedzie sie
                	if(zaloguj(ggnum, ggpass))
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
}
