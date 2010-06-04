package android.pp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;


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
import android.widget.EditText;
import android.widget.Toast;


public class GanduClient extends Activity {

	public Toast toastGandu;
	String[] ip = null;
	private Button connectPhones;
	private EditText ggNumberEdit;
	private EditText ggPasswordEdit;
	private String serverIpAddress = "91.197.13.211";
	private boolean connected = false;

	// ------------------> OnCreate()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ggNumberEdit = (EditText) findViewById(R.id.EditText01);
		ggPasswordEdit = (EditText) findViewById(R.id.EditText02);
		connectPhones = (Button) findViewById(R.id.Button01);
		connectPhones.setText("Zaloguj...");
		connectPhones.setOnClickListener(connectListener);
		/*toastGandu = new Toast(this);
		toastGandu.setView(this.getCurrentFocus());
		toastGandu.makeText(this, "toastTest", 3000);*/
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
						"http://appmsg.gadu-gadu.pl/appsvc/appmsg_ver8.asp?fmnumber="+ggNumberEdit.getText().toString()+"&lastmsg=20429&version=10.0.0.10450");
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

                    	//GG_STATUS_AVAIL_DESCR	0x0004	Dostepny (z opisem)
						int numergg = Integer.parseInt(ggNumberEdit.getText().toString());
						String haslogg = ggPasswordEdit.getText().toString();
                    	Logowanie logowanie = new Logowanie(ziarno, haslogg, numergg, 0x0004, (byte)0xff, "Gandu szal:]");
                    	byte[] paczkalogowania = logowanie.pobraniePaczkiBajtow();
                    	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
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
                    		ggNumberEdit.post(new Runnable() 
                    		{
								@Override
								public void run() {
									ggNumberEdit.setText("Zalogowany");
								}
                    		});
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
				}
				socket.close();
				Log.d("ClientActivity", "C: Closed.");
			} catch (Exception e) {
				;
			}
		}
	}
}
