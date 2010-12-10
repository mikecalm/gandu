package android.pp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
     * A simple adapter which maintains an ArrayList of photo resource Ids. 
     * Each photo is displayed as an image. This adapter supports clearing the
     * list of photos and adding a new photo.
     *
     */
    public class MyExpandableListAdapter extends BaseExpandableListAdapter 
    {
    	private Context mContext;
        private LayoutInflater mInflater;
        private List<ViewableGroups> groups = new ArrayList<ViewableGroups>();
        private List<List<ViewableContacts>> children = new ArrayList<List<ViewableContacts>>();
        
        //Do konstruktora MyExpandableListAdapter przekazywany jest kontekst aplikacji
        //potrzebny do LayoutInflater, zeby moc wczytac layout wierszy i grup na liscie
        //z pliku xml
        public MyExpandableListAdapter(Context kontekst)
        {        	
        	this.mContext = kontekst;
        	this.mInflater = LayoutInflater.from(this.mContext);
        }
        
        //Ustawianie danych listy kontaktow
        public void setAdapterData(List<ViewableGroups> groups, List<List<ViewableContacts>> contacts)
        {
        	this.groups = groups;
        	this.children = contacts;
        	notifyDataSetChanged();
        }
        
        public Object getChild(int groupPosition, int childPosition) 
        {
        	return children.get(groupPosition).get(childPosition);
        }        

        public long getChildId(int groupPosition, int childPosition) 
        {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) 
        {
        	return children.get(groupPosition).size();
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) 
        {
	        	convertView = (LinearLayout)mInflater.inflate(R.layout.child_row, parent, false);
	            ((TextView)convertView.findViewById(R.id.username)).setText(((ViewableContacts)(getChild(groupPosition, childPosition))).showName);
	            ((TextView)convertView.findViewById(R.id.description)).setText(((ViewableContacts)(getChild(groupPosition, childPosition))).description);
	            //if(((ViewableContacts)(getChild(groupPosition, childPosition))).showName.equals("Blip.pl"))
	            //	((ImageView)convertView.findViewById(R.id.ImageView01)).setImageResource(R.drawable.notavailable);
            
            return convertView;
        }

        public Object getGroup(int groupPosition) 
        {
        	return groups.get(groupPosition);
        }

        public int getGroupCount() 
        {
        	return groups.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) 
        {
             convertView = (TextView)mInflater.inflate(R.layout.group_row, parent, false);
             TextView tv = ((TextView)convertView.findViewById(R.id.groupname));
             tv.setText(((ViewableGroups)getGroup(groupPosition)).name);
             return convertView;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) 
        {
            return true;
        }

        public boolean hasStableIds() 
        {
            return true;
        }

    }