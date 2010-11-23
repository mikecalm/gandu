package android.pp;

import android.app.Activity;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class AddNewContact extends Activity {
	
	ToggleButton pokazWiecej;
	LinearLayout wiecejLayout;
	Button dodajButton;
	EditText numerGGE;
	EditText nazwaKontaktuE;
	EditText emailE;
	EditText komorkowyE;
	EditText stacjonarnyE;
	EditText stronaWWWE;
	String grupaID = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.add_new_contact);
	    //funkcja ponizej potrzebna, zeby okno zajmowalo cala szerokosc
	    //sam parametr fill_parent w android:layout_width nie wystarcza
	    getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    setTitle("Nowy kontakt");
	    
	    Bundle b = this.getIntent().getExtras();
		if(b != null)
		{
			if(b.containsKey("grupaID"))
			{
				this.grupaID = b.getString("grupaID");
	    		Log.i("[AddNewContact]onCreate, przyjalem grupaID: ",this.grupaID);
			}
		}
	    
	    numerGGE = (EditText)findViewById(R.id.NewGGNumber);
	    nazwaKontaktuE = (EditText)findViewById(R.id.NewShowName);
	    emailE = (EditText)findViewById(R.id.NewEmail);
	    komorkowyE = (EditText)findViewById(R.id.NewMobilePhone);
	    stacjonarnyE = (EditText)findViewById(R.id.NewHomePhone);
	    stronaWWWE = (EditText)findViewById(R.id.NewWwwAddress);
	    dodajButton = (Button)findViewById(R.id.NewDodajButton);
	    dodajButton.setOnClickListener(addClicked);
	    wiecejLayout = (LinearLayout)findViewById(R.id.NewWiecejLayout);	    
	    pokazWiecej = (ToggleButton)findViewById(R.id.NewMoreToggle);	    
	    pokazWiecej.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
				{
					wiecejLayout.setVisibility(View.VISIBLE);
				}
				else
				{
					wiecejLayout.setVisibility(View.GONE);
				}
			}
		});
	}
	
	private OnClickListener addClicked = new OnClickListener()
    {
        public void onClick(View v)
        {
        	// To send a result, simply call setResult() before your
            // activity is finished.
            //setResult(RESULT_OK, (new Intent()).setAction("Corky!"));
        	String numerGG = numerGGE.getText().toString().trim();        	
        	String nazwaKontaktu = nazwaKontaktuE.getText().toString().trim();
        	String email = emailE.getText().toString().trim();
        	String komorkowy = komorkowyE.getText().toString().trim();
        	String stacjonarny = stacjonarnyE.getText().toString().trim();
        	String stronaWWW = stronaWWWE.getText().toString().trim();
        	if(numerGG.equals("") && email.equals("") && komorkowy.equals("") && stacjonarny.equals(""))
        	{
        		Toast.makeText(getApplicationContext(), "Musisz podac numer GG, e-mail lub numer telefonu", Toast.LENGTH_LONG).show();
        		return;
        	}
        	if(!numerGG.equals(""))
        	{
	        	if(!numerGG.matches("[0-9]+"))
	        	{
	        		Toast.makeText(getApplicationContext(), "Nieprawidlowy format numeru GG", Toast.LENGTH_LONG).show();
	        		return;        	
	        	}
        	}
        	Intent odpowiedz = new Intent();
        	odpowiedz.putExtra("numerGG", numerGG);
        	odpowiedz.putExtra("nazwaKontaktu", nazwaKontaktu);
        	odpowiedz.putExtra("email", email);
        	odpowiedz.putExtra("komorkowy", komorkowy);
        	odpowiedz.putExtra("stacjonarny", stacjonarny);
        	odpowiedz.putExtra("stronaWWW", stronaWWW);
        	if(grupaID != null)
        		odpowiedz.putExtra("grupaID", grupaID);
        	setResult(RESULT_OK, odpowiedz);
            finish();
        }
    };

}
