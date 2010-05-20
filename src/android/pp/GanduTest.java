package android.pp;

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.pp.GanduClient;

public class GanduTest extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Intent intent= new Intent(getApplicationContext(), GanduClient.class);
		startActivity(intent);
		
	}

	
}
