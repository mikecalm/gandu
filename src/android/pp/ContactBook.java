package android.pp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleExpandableListAdapter;

public class ContactBook extends ExpandableListActivity{

	boolean mIsBound;
	String gglista;
		
		List groupData; 
		List childData;
		SimpleExpandableListAdapter expListAdapter;
	
	/** Messenger for communicating with service. */
    Messenger mService = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contactbook);
		//zbindowanie aktywnosci do serwisu
		doBindService();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater=getMenuInflater();
		inflater.inflate(R.menu.contactbookmenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	switch (item.getItemId())
	{
		case R.id.item01:			
			//wyslanie do serwisu wiadomosci, ze importowana jest lista kontaktow    		
        	Message msg2 = Message.obtain(null,Common.CLIENT_GET_CONTACTBOOK, 0, 0);
    		try
    		{
    			mService.send(msg2);
    		}catch(Exception excMsg)
    		{
    			Log.e("Blad","Blad!!!!\n"+excMsg.getMessage());
    		}
			return true;
		//Moreitemsgohere(ifany)...
	}
	return false;
	}
	
	/**
	  * Creates the group list out of the groups[] array according to
	  * the structure required by SimpleExpandableListAdapter. The resulting
	  * List contains Maps. Each Map contains one entry with key "groupName" and
	  * value of an entry in the groups[] array.
	  */
		private List createGroupList(ArrayList<GroupContact> gcl) {
			ArrayList result = new ArrayList();
			for(GroupContact gc : gcl)
			{
				HashMap m = new HashMap();
			    m.put( "groupName",gc.gp.getName());
				result.add( m );
			}
		  return (List)result;
	    }

	/**
	  * Creates the child list out of the users[] array according to the
	  * structure required by SimpleExpandableListAdapter. The resulting List
	  * contains one list for each group. Each such second-level group contains
	  * Maps. Each such Map contains two keys: "contactname" is the name of the
	  * contact and "description" is the users description.
	  */
	  private List createChildList(ArrayList<GroupContact> gcl) {
		ArrayList result = new ArrayList();
		for(GroupContact gc : gcl)
		{
			ArrayList secList = new ArrayList();
			for(int i = 0; i < gc.ctt_list.size(); i++)
			{
				HashMap child = new HashMap();
				child.put( "username", gc.ctt_list.get(i).getShowName() );
				child.put( "description", gc.ctt_list.get(i).getGGNumber() );
				secList.add( child );
			}
			result.add( secList );
		}
		return result;
	  }
	
	//Funkcje potrzebne do zestawienia polaczenia aktywnosci z serwisem Gandu
	/**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i("ganduClient","Odebralem"+msg.what);
            switch (msg.what) {
                case Common.FLAG_ACTIVITY_REGISTER:
                	Log.i("ContactBook","Received: "+msg.what);
                	//wyslanie do serwisu wiadomosci, ze pobierana jest lista kontaktow
                	break;
                case Common.FLAG_CONTACTBOOK:
                	Bundle odebrany = msg.getData();
                	gglista = odebrany.getString("listaGG");
                	XMLContactBook  xcb = new XMLContactBook();
                	XMLParsedDataSet xpds = xcb.xmlparse(gglista);
                	groupData = createGroupList(xpds.GCList); 
        			childData = createChildList(xpds.GCList);
        			expListAdapter =
        				new SimpleExpandableListAdapter(
        					getApplicationContext(),
        					groupData,	// groupData describes the first-level entries
        					R.layout.group_row,	// Layout for the first-level entries
        					new String[] { "groupName" },	// Key in the groupData maps to display
        					new int[] { R.id.groupname },		// Data under "colorName" key goes into this TextView
        					childData,	// childData describes second-level entries
        					R.layout.child_row,	// Layout for second-level entries
        					new String[] { "username", "description" },	// Keys in childData maps to display
        					new int[] { R.id.username, R.id.description }	// Data under the keys above go into these TextViews
        				);
        			setListAdapter( expListAdapter );
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

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            //mCallbackText.setText("Attached.");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        Common.CLIENT_REGISTER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
            //Toast.makeText(Binding.this, R.string.remote_service_connected,
            //Toast.makeText(GanduClient.this, "remote_service_connected",
            //        Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            //mCallbackText.setText("Disconnected.");

            // As part of the sample, tell the user what happened.
            //Toast.makeText(Binding.this, R.string.remote_service_disconnected,
            //Toast.makeText(GanduClient.this, "remote_service_disconnected",
            //        Toast.LENGTH_SHORT).show();
        }
    };
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(ContactBook.this, 
                GanduService.class), mConnection, Context.BIND_AUTO_CREATE);
        		//GanduService.class), mConnection, Context.);
        
        mIsBound = true;
        //mCallbackText.setText("Binding.");
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                    		Common.CLIENT_UNREGISTER);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                  
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            //mCallbackText.setText("Unbinding.");
        }
    }
}
