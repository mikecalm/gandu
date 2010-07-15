package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XMLParsedDataSet {
	
	public static ArrayList<GroupContact> GCList = new ArrayList<GroupContact>();
	public GroupContact GCItem;

	public String getGroupID()
	{
		return GCItem.gp.getGroupId();
	}
	
	public void setGroupID(GroupContact GCItem,String data)
	{
		GCItem.gp.setGroupId(data);
	
	}
	
	public String getName()
	{
		return GCItem.gp.getName();
	}
	public void setName(GroupContact GCItem,String data)
	{
		GCItem.gp.setName(data);
	}
	public String toString()
	{
		String tmp = null;
		for (int i =0; i < GCList.size(); i++)
		{
			tmp += GCList.get(i).gp.getName()+"\n";
		}
		
		return tmp;
	}
}
