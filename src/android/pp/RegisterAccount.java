package android.pp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class RegisterAccount extends Activity {
	
	Button rejestrujButton;
	EditText emailEdit;
	EditText haslo1Edit;
	EditText haslo2Edit;
	EditText kodZObrazkaEdit;
	//kontrolka wyswietlajaca pobrany token
	ImageView tokenPicture;	
	String tokenID;
	String nowyNumerGG;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.registeraccount);
	    //funkcja ponizej potrzebna, zeby okno zajmowalo cala szerokosc
	    //sam parametr fill_parent w android:layout_width nie wystarcza
	    getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    setTitle("Nowe konto GG");
	    
	    emailEdit = (EditText)findViewById(R.id.RegisterEmail);
	    haslo1Edit = (EditText)findViewById(R.id.RegisterPassword1);
	    haslo2Edit = (EditText)findViewById(R.id.RegisterPassword2);
	    tokenPicture = (ImageView)findViewById(R.id.RegisterToken);
	    kodZObrazkaEdit = (EditText)findViewById(R.id.RegisterTokenValue);
	    rejestrujButton = (Button)findViewById(R.id.RegisterButton);
	    rejestrujButton.setOnClickListener(registerClicked);
	    
	    //pobranie tokena, tokenID i wyswietlenie go w kontrolce
	    //TU BEDZIE
	    String tokenURL = getTokenURLAndTokenID();
	    Bitmap tokenBitmap = null;
	    //if((tokenBitmap = getImageBitmap("http://torrenty.org/banery/ikona.gif")) != null)
	    if((tokenBitmap = getImageBitmap(tokenURL)) != null)
	    	tokenPicture.setImageBitmap(tokenBitmap);
	}
	
	private OnClickListener registerClicked = new OnClickListener()
    {
        public void onClick(View v)
        {
        	// To send a result, simply call setResult() before your
            // activity is finished.
            //setResult(RESULT_OK, (new Intent()).setAction("Corky!"));
        	String email = emailEdit.getText().toString().trim();
        	String haslo1 = haslo1Edit.getText().toString().trim();
        	String haslo2 = haslo2Edit.getText().toString().trim();
        	String kodzObrazka = kodZObrazkaEdit.getText().toString().trim();
        	if(email.equals("") || haslo1.equals("") || haslo2.equals("") || kodzObrazka.equals(""))
        	{
        		Toast.makeText(getApplicationContext(), "Musisz podac e-mail, haslo i powtórzone haslo oraz lub kod z obrazka", Toast.LENGTH_LONG).show();
        		return;
        	}
        	if(!haslo1.equals(haslo2))
        	{
        		Toast.makeText(getApplicationContext(), "Niezgodne has³a", Toast.LENGTH_LONG).show();
        		return;
        	}
        	if(haslo1.length() < 6)
        	{
        		Toast.makeText(getApplicationContext(), "Has³o musi siê sk³adaæ z minimum 6 znaków", Toast.LENGTH_LONG).show();
        		return;
        	}
        	
        	//wyslanie i odebranie komunikatow rejestracji
        	String hash = makeEmailPassHash(email, haslo1);
        	if(sendRegisterMessage(hash, email, haslo1, kodzObrazka))
        	{
        		//Toast.makeText(getApplicationContext(), "Rejestracja udana! Nowy numer: "+nowyNumerGG, Toast.LENGTH_LONG);            	
            	Intent odpowiedz = new Intent();
            	odpowiedz.putExtra("numerGG", nowyNumerGG);
            	odpowiedz.putExtra("haslo", haslo1);
            	odpowiedz.putExtra("email", email);
            	setResult(RESULT_OK, odpowiedz);
                finish();
        	}
        	else
        		Toast.makeText(getApplicationContext(), "Nieudana próba rejestracji", Toast.LENGTH_LONG);
        }
    };
    
    private String getTokenURLAndTokenID()
    {
    	String tokenURL = "";
    	
    	try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(
					"http://register.gadu-gadu.pl/appsvc/regtoken.asp");
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				int wczytanych = 0;
				while ((wczytanych = instream.read(tmp)) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp, 0, wczytanych);
					BufferedReader br = new BufferedReader(new InputStreamReader(bais));
					String readline = br.readLine();
					tokenID = br.readLine();
					tokenURL = br.readLine()+"?tokenid="+tokenID;
				}
			}
    	} catch(Exception e)
    	{
    		Log.e("[RegisterAccount]", "Blad pobierania adresu i ID tokenu: "+e.getMessage());
    	}
    	
    	return tokenURL;
    }
    
    private Boolean sendRegisterMessage(String hash, String email, String haslo, String tokenValue)
    {
    	try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(
			//		"http://register.gadu-gadu.pl/appsvc/fmregister3.asp");
				"http://register.gadu-gadu.pl/fmregister.php");
			
			httppost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE,
			        false);
			
			StringEntity postEntity = new StringEntity("code="+hash+"&email="+email+"&pwd="+haslo+"&tokenid="+tokenID+"&tokenval="+tokenValue, "UTF-8");
			//StringEntity postEntity = new StringEntity("track=Berlin", "UTF-8");
	        postEntity.setContentType("application/x-www-form-urlencoded");
	        httppost.setEntity(postEntity);
			
			//HttpParams params = new BasicHttpParams();
	        //params = params.setParameter("code", hash);
	        //params = params.setParameter("email", email);
	        //params = params.setParameter("pwd", haslo);	        
	        //params = params.setParameter("tokenid", tokenID);	     
	        //params = params.setParameter("tokenval", tokenValue);	        
	        //httppost.setParams(params);
			
	        // Add data  
			//List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);  
			//nameValuePairs.add(new BasicNameValuePair("code", hash));  
			//nameValuePairs.add(new BasicNameValuePair("email", email));
			//nameValuePairs.add(new BasicNameValuePair("pwd", haslo));
			//nameValuePairs.add(new BasicNameValuePair("tokenid", tokenID));
			//nameValuePairs.add(new BasicNameValuePair("tokenval", tokenValue));
			//httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  

			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();	        

			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] tmp = new byte[2048];
				int wczytanych = 0;
				while ((wczytanych = instream.read(tmp)) != -1) {
					ByteArrayInputStream bais = new ByteArrayInputStream(tmp);
					BufferedReader br = new BufferedReader(new InputStreamReader(bais));
					String readline = br.readLine();
					Log.i("GANDU-TESTING", readline);
					if(readline.contains("reg_success:"))
					{
						nowyNumerGG = readline.substring(readline.lastIndexOf(":") + 1,wczytanych);
						return true;
					}
				}
			}

    	}catch(Exception e)
    	{
    		Log.e("[RegisterAccount]","Blad rejestracji: "+e.getMessage());
    	}
		return false;
    }
    
    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
       } catch (Exception e) {
           Log.e("[RegisterAccount]", "Blad pobierania obrazu tokenu: " + e.getMessage());
       }
       return bm;
    } 
    
    private String makeEmailPassHash(String email, String haslo)
    {
    	int a, c;
    	int b = -1;
    	String polaczoneEmailHaslo = email+haslo;
    	byte[] polaczoneByte = polaczoneEmailHaslo.getBytes();
    	//byte[] polaczoneByte = polaczoneEmailHaslo.getBytes("ASCII");
    	
    	//while ((c = (byte) polaczoneByte[i++]) != 0) {
    	for(int i=0; i<polaczoneByte.length; i++)
    	{
    		c = polaczoneByte[i];
			a = (c ^ b) + (c << 8);
			b = (a >> 24) | (a << 8);
    	}
    	
    	if(b < 0)
    		return ""+(new Long("4294967296") + b);  
    	return ""+b;
    	//return (b < 0 ? -b : b);
    }

}
