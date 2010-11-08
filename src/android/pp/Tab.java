package android.pp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Tab extends Activity{
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		/* First Tab Content */
		setContentView(R.layout.tabcontent);

	}
}
