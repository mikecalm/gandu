package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XMLParsedDataSet {

	public GroupContact GCItem;
	
	XMLParsedDataSet() {
		GCItem = new GroupContact(); 
	}
	
	
	public String getGroupID()
	{
		return GCItem.gp.getGroupId();
	}
	
	public void setGroupID(String data)
	{
		this.GCItem.gp.setGroupId(data);
		//this.groupId.add(data);
	}
	
	public String getName()
	{
		return GCItem.gp.getName();
	}
	public void setName(String data)
	{
		this.GCItem.gp.setName(data);
	}
	
	public String toString(){
		String tmp = null; 
		for (int i = 0 ; i<= GCItem.size(); i++)
		{
			tmp +=GCItem.gp.getGroupId()+"\n "+GCItem.gp.getName()+"\n";
		}

		return tmp;
	}

}
