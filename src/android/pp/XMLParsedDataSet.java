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
	
	
	public void addContact(Contact ct)
	{
		for (GroupContact gc : GCList)
		{			
				if(gc.gp.getGroupId().equals(ct.getGroupId()))
				{
					gc.ctt_list.add(ct);
				}
			
		}
	}
	
	public String getName()
	{
		return GCItem.gp.getName();
	}
	public void setName(GroupContact GCItem,String data)
	{
		GCItem.gp.setName(data);
	}
	//CONTACTS 
	
	public void setGGNumber(String data)
	{
		;
	}
	public String toString()
	{
		String tmp = "";
		for(GroupContact gc : GCList)
		{
			tmp += gc.gp.getName()+"\n";
			for(int i = 0; i < gc.ctt_list.size(); i++)
			{
				tmp += gc.ctt_list.get(i).toString() + " , ";
			}
		}
	
		return tmp;
	}
}
