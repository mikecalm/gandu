package android.pp;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

public class Chat extends TabActivity{
	public TabHost tabHost;
	public TabSpec firstTabSpec;
	public SharedPreferences prefs;
	public  SharedPreferences.Editor editor;
	
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);	
		Toast.makeText(getApplicationContext(), "onCreate()", Toast.LENGTH_LONG).show();
		
		prefs = getPreferences(0);
		editor = prefs.edit();
		tabHost = (TabHost)findViewById(android.R.id.tabhost);	
    	
}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Toast.makeText(getApplicationContext(), "onResume()", Toast.LENGTH_LONG).show();
        String odz = prefs.getString("text", null);
        if (odz != null)
        {
        	Bundle b = this.getIntent().getExtras();
            firstTabSpec = tabHost.newTabSpec(b.getString("username"));        

            /** TabSpec setIndicator() is used to set name for the tab. */
            /** TabSpec setContent() is used to set content for a particular tab. */
            
            firstTabSpec.setIndicator(b.getString("username")).setContent(new Intent(this,Tab.class));
           
            /** Add tabSpec to the TabHost to display. */
        	tabHost.addTab(firstTabSpec); 
        	
        	String [] tab = odz.split("~");
        	for (String s: tab)
        	{
        		firstTabSpec = tabHost.newTabSpec(s);
            	firstTabSpec.setIndicator(s).setContent(new Intent(this,Tab.class));
            	tabHost.addTab(firstTabSpec);
        	}
        	editor.remove("text");
    		editor.commit();
        }  
        if (odz == null)
        {
        	/** TabSpec used to create a new tab.
             * By using TabSpec only we can able to setContent to the tab.
             * By using TabSpec setIndicator() we can set name to tab. */

            /** tid1 is firstTabSpec Id. Its used to access outside. */
    		
    		Bundle b = this.getIntent().getExtras();
            firstTabSpec = tabHost.newTabSpec(b.getString("username"));        

            /** TabSpec setIndicator() is used to set name for the tab. */
            /** TabSpec setContent() is used to set content for a particular tab. */
            
            firstTabSpec.setIndicator(b.getString("username")).setContent(new Intent(this,Tab.class));
           
            /** Add tabSpec to the TabHost to display. */
        	tabHost.addTab(firstTabSpec); 
        }
             
        
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Toast.makeText(getApplicationContext(), "onPause()", Toast.LENGTH_LONG).show();
		//preferencje
		String tabs = "";
        for (int i =0 ; i<tabHost.getChildCount() ; i++)
        {
        	tabHost.setCurrentTab(i);
        	tabs += tabHost.getCurrentTabTag()+"~";                
        }
        Toast.makeText(getApplicationContext(), tabs, Toast.LENGTH_LONG).show();
        editor.putString("text", tabs);
        editor.commit();
        
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Toast.makeText(getApplicationContext(), "onDestroy()", Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		tabHost.clearAllTabs();
		Toast.makeText(getApplicationContext(), "onStop()", Toast.LENGTH_LONG).show();
	}

}
