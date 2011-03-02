package android.pp;

import android.app.Activity;
import android.content.Intent;
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
	Boolean edited = false;
	String oldNumerGG = null;
	String oldNazwaKontaktu = null;
	String oldEmail = null;
	String oldKomorka = null;
	String oldStacjonarny = null;
	String oldStrona = null;
	
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
			//pola zwiazane z edycja kontaktu
			if(b.containsKey("edited"))
				this.edited = true;
			if(b.containsKey("poprzedniNumer"))
				this.oldNumerGG = b.getString("poprzedniNumer");
			if(b.containsKey("poprzedniaNazwaKontaktu"))
				this.oldNazwaKontaktu = b.getString("poprzedniaNazwaKontaktu");
			if(b.containsKey("poprzedniaEmail"))
				this.oldEmail = b.getString("poprzedniaEmail");
			if(b.containsKey("poprzedniaKomorka"))
				this.oldKomorka = b.getString("poprzedniaKomorka");
			if(b.containsKey("poprzedniaStacjonarny"))
				this.oldStacjonarny = b.getString("poprzedniaStacjonarny");
			if(b.containsKey("poprzedniaStrona"))
				this.oldStrona = b.getString("poprzedniaStrona");
		}
	    
	    numerGGE = (EditText)findViewById(R.id.NewGGNumber);
	    if(this.oldNumerGG != null)
	    	numerGGE.setText(this.oldNumerGG);
	    nazwaKontaktuE = (EditText)findViewById(R.id.NewShowName);
	    if(this.oldNazwaKontaktu != null)
	    	nazwaKontaktuE.setText(this.oldNazwaKontaktu);
	    emailE = (EditText)findViewById(R.id.NewEmail);
	    if(this.oldEmail != null)
	    	emailE.setText(this.oldEmail);
	    komorkowyE = (EditText)findViewById(R.id.NewMobilePhone);
	    if(this.oldKomorka != null)
	    	komorkowyE.setText(this.oldKomorka);
	    stacjonarnyE = (EditText)findViewById(R.id.NewHomePhone);
	    if(this.oldStacjonarny != null)
	    	stacjonarnyE.setText(this.oldStacjonarny);
	    stronaWWWE = (EditText)findViewById(R.id.NewWwwAddress);
	    if(this.oldStrona != null)
	    	stronaWWWE.setText(this.oldStrona);
	    dodajButton = (Button)findViewById(R.id.NewDodajButton);
	    if(this.edited)
	    	dodajButton.setText("Zapisz");
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
        	if(edited)
        	{
        		odpowiedz.putExtra("edited", true);
	        	if(oldNumerGG != null)
	        		odpowiedz.putExtra("poprzedniNumer", oldNumerGG);
	        	if(oldNazwaKontaktu != null)
	        		odpowiedz.putExtra("poprzedniaNazwaKontaktu", oldNazwaKontaktu);
        	}
        	if(grupaID != null)
        		odpowiedz.putExtra("grupaID", grupaID);
        	setResult(RESULT_OK, odpowiedz);
            finish();
        }
    };

}
