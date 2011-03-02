package android.pp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AndroidExplorer extends ListActivity {
	
	private List<String> item = null;
	private List<String> path = null;
	private String root="/";
	private TextView myPath;
	private String fileTo;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = this.getIntent().getExtras();
		if(b != null)
		{
			if(b.containsKey("fileTo"))
			{
				this.fileTo = b.getString("fileTo");
			}
		}
        setContentView(R.layout.fileexplorerlayout);
        myPath = (TextView)findViewById(R.id.path);
        getDir(root);
    }
    
    private void getDir(String dirPath)
    {
    	myPath.setText("Location: " + dirPath);
    	
    	item = new ArrayList<String>();
    	path = new ArrayList<String>();
    	
    	File f = new File(dirPath);
    	File[] files = f.listFiles();
    	
    	if(!dirPath.equals(root))
    	{

    		item.add(root);
    		path.add(root);
    		
    		item.add("../");
    		path.add(f.getParent());
            
    	}
    	
    	for(int i=0; i < files.length; i++)
    	{
    			File file = files[i];
    			path.add(file.getPath());
    			if(file.isDirectory())
    				item.add(file.getName() + "/");
    			else
    				//item.add(file.getName());
    			{
    				//jesli plik jest mniejszy niz 1000Bajtow
    				if(file.length() <= 1000)
    					item.add(file.getName()+" ["+file.length()+" B]");
    				//jesli plik wiekszy niz 1000Bajtow, to podaj rozmiar w KB
    				else if(file.length() > 1000 && file.length() <= 1000000)
    					item.add(file.getName()+" ["+file.length()/1000+" KB]");
    				//jesli plik wiekszy niz 1000KB to podaje rozmiar w MB 
    				else if(file.length() > 1000000)
    					item.add(file.getName()+" ["+file.length()/1000000+" MB]");
    			}
    	}

    	ArrayAdapter<String> fileList =
    		new ArrayAdapter<String>(this, R.layout.fileexplorerrow, item);
    	setListAdapter(fileList);
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		File file = new File(path.get(position));
		
		if (file.isDirectory())
		{
			if(file.canRead())
				getDir(path.get(position));
			else
			{
				new AlertDialog.Builder(this)
				.setTitle("[" + file.getName() + "] brak uprawnien do odczytu")
				.setPositiveButton("OK", 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
							}
						}).show();
			}
		}
		else
		{
			String pathString = myPath.getText().toString();
			int indeks = pathString.indexOf(" ");
			pathString = pathString.substring(indeks+2);
			if(!pathString.equals("") && !pathString.endsWith("/"))
				pathString += "/";			
			Intent odpowiedz = new Intent();
			if(!file.canRead())
			{
				new AlertDialog.Builder(this)
				.setTitle("[" + file.getName() + "]"+" brak uprawnien do odczytu")
				.setPositiveButton("OK", 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
							}
						}).show();
			}
			else
			{
	        	odpowiedz.putExtra("fileName", file.getName());
	        	odpowiedz.putExtra("filePath", pathString+file.getName());
	        	odpowiedz.putExtra("readable", file.canRead());
	        	odpowiedz.putExtra("fileTo", this.fileTo);
	        	setResult(RESULT_OK, odpowiedz);
	            finish();
			}
		}
	}
}