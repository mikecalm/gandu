package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class GanduClient extends Activity {

	String[] ip = null;
	private Button connectPhones;
	private String serverIpAddress = "91.197.13.211";
	private boolean connected = false;

	// ------------------> OnCreate()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		connectPhones = (Button) findViewById(R.id.Button01);
		connectPhones.setText("Zaloguj...");
		connectPhones.setOnClickListener(connectListener);

	}

	private OnClickListener connectListener = new OnClickListener() {
		public void onClick(View v) {
			if (!connected && serverIpAddress != "") {
				Thread cThread = new Thread(new ClientThread());
				cThread.start();

			}
		}
	};

	public class ClientThread implements Runnable {
		public void run() {
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(
						"http://appmsg.gadu-gadu.pl/appsvc/appmsg_ver8.asp?fmnumber=6063724&lastmsg=0&version=10.1.1.11119");
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
				// ------------------> Tworzenie skrótu SHA1 wraz z ziarnem otrzymanym od serwera
				Mac mac = Mac.getInstance("HmacSHA1");

				String key = "123";
				String data = "456";

				SecretKeySpec spec = new SecretKeySpec(key.getBytes(),"HmacSHA1");
				mac.init(spec);
				byte[] byteHMAC = null;
				byteHMAC = mac.doFinal(data.getBytes());

				String oauth = Base64.encodeBytes(byteHMAC);
				Log.i("GANDU-TESTING", oauth);
				Log.d("ClientActivity", "Polaczenie....");

				String ipWyizolowany = ip[2].split(":")[0];
				int portWyizolowany = Integer.parseInt(ip[2].split(":")[1]);
				Socket socket = new Socket(ipWyizolowany, portWyizolowany);
				
				connected = true;
				while (connected) {
					try {
						// BufferedReader in = new BufferedReader(new
						// InputStreamReader(sslsocket.getInputStream()));
						// BufferedReader in = new BufferedReader(new
						// InputStreamReader(socket.getInputStream()));
						DataInputStream in = new DataInputStream(socket
								.getInputStream());
						int typKom = Integer.reverseBytes(in.readInt());
						int dlugoscDanych = Integer.reverseBytes(in.readInt());
						int ziarno = Integer.reverseBytes(in.readInt());
						Log.i("GANDU-TESTING typ komunikatu: ", "" + typKom);
						Log.i("GANDU-TESTING dlugosc danych: ", ""
								+ dlugoscDanych);
						Log.i("GANDU-TESTING ziarno: ", "" + ziarno);
						// po podsluchaniu wiresharkiem i wychwyceniu paczki z
						// ziarnem
						// i przeliczeniu go z systemu szesnastkowego na
						// dziesietny
						// wartosc ziarna zgadza sie z ta uzyskana w zmiennej
						// int ziarno
						// wiec jest dobrze, teraz mozna dalej kombinowac ze
						// skompletowaniem
						// paczki ktora pozniej trzeba odeslac;)

						// to jakas proba wysylanie czegokolwiek do socketa
						/*
						 * DataOutputStream out = new
						 * DataOutputStream(socket.getOutputStream());
						 * out.writeByte(8); out.flush(); out.writeByte(9);
						 * out.flush();
						 */
					} catch (Exception e) {
						Log.e("ClientActivity", "S: Error", e);
						connected = false;
					}
				}
				socket.close();
				Log.d("ClientActivity", "C: Closed.");
			} catch (Exception e) {
				;
			}
		}
	}
}
