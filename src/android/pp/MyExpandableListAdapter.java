package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

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
    public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    	private Context mContext;
        private LayoutInflater mInflater;
        //private CopyOfContactBook listaKontaktow;
        private List<ViewableGroups> groups = new ArrayList<ViewableGroups>();
        private List<List<ViewableContacts>> children = new ArrayList<List<ViewableContacts>>();
        //private String[] groups = {};
        //private String[][] children = {{}};
        
        //Do kontruktora MyExpandableListAdapter przekazywany jest kontekst aplikacji
        //potrzebny do LayoutInflater, zeby moc wczytac layout wierszy i grup na liscie
        //z pliku xml
        public MyExpandableListAdapter(Context kontekst)
        {        	
        	this.mContext = kontekst;
        	this.mInflater = LayoutInflater.from(this.mContext);
        }
        
        //Ustawianie danych listy kontaktow
        //public void setAdapterData(String[] groups, String contacts[][], CopyOfContactBook lista)
        public void setAdapterData(List<ViewableGroups> groups, List<List<ViewableContacts>> contacts)
        {
        	//this.listaKontaktow = lista;
        	this.groups = groups;
        	this.children = contacts;
        	notifyDataSetChanged();
        }
        
        public Object getChild(int groupPosition, int childPosition) {
            //return children[groupPosition][childPosition];
        	return children.get(groupPosition).get(childPosition);
        	//return "cos: "+groupPosition+","+childPosition;
        }        

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            //return children[groupPosition].length;
        	return children.get(groupPosition).size();
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {  
        	//if(convertView == null)
            //{
	        	convertView = (LinearLayout)mInflater.inflate(R.layout.child_row, parent, false);
	            ((TextView)convertView.findViewById(R.id.username)).setText(((ViewableContacts)(getChild(groupPosition, childPosition))).showName);
	            if(((ViewableContacts)(getChild(groupPosition, childPosition))).showName.equals("Blip.pl"))
	            	((ImageView)convertView.findViewById(R.id.ImageView01)).setImageResource(R.drawable.notavailable);
            //}
            
            return convertView;
        }

        public Object getGroup(int groupPosition) {
            //return groups[groupPosition];
        	return groups.get(groupPosition);
        }

        public int getGroupCount() {
            //return groups.length;
        	return groups.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            /*TextView textView = getGenericView();
            textView.setText(getGroup(groupPosition).toString());
            return textView;*/
        	 //if(convertView == null)
             //{
                 convertView = (TextView)mInflater.inflate(R.layout.group_row, parent, false);
                 TextView tv = ((TextView)convertView.findViewById(R.id.groupname));
                 //tv.setText(mParentGroups.get(groupPosition).toString());
                 tv.setText(((ViewableGroups)getGroup(groupPosition)).name);
             //}
             return convertView;

        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }