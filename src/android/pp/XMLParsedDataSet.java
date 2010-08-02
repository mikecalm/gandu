package android.pp;

import java.util.ArrayList;

public class XMLParsedDataSet {
	
	//public static ArrayList<GroupContact> GCList = new ArrayList<GroupContact>();
	public ArrayList<GroupContact> GCList = new ArrayList<GroupContact>();
	
	public void addGroup(Group gp)
	{
		GCList.add(new GroupContact(gp));
		
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
	
	public String toString()
	{
		String tmp = "";
		for(GroupContact gc : GCList)
		{
			tmp += gc.gp.getName()+"\n\n";
			for(int i = 0; i < gc.ctt_list.size(); i++)
			{
				tmp += gc.ctt_list.get(i).toString();
			}
		}	
		return tmp;
	}
}
