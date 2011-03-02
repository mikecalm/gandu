package android.pp;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class StatusListAdapter extends SimpleAdapter {
	
	private Context localContext;
	private ArrayList<HashMap<String, Object>> localList;

	public StatusListAdapter(Context context,
			ArrayList<HashMap<String, Object>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		// TODO Auto-generated constructor stub
		localContext = context;
		localList = data;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (null == convertView) 
		{
		    LayoutInflater inflater = (LayoutInflater) localContext
		            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    convertView = inflater.inflate(R.layout.status_row, parent, false);
		}
		TextView name = (TextView) convertView.findViewById(R.id.statusName);
		name.setText((String)localList.get(position).get("Name"));
		ImageView icon = (ImageView) convertView.findViewById(R.id.statusImage);	
		icon.setImageResource((Integer)localList.get(position).get("ResID"));
	
		return convertView;
	}


}
