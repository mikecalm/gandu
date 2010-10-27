package android.pp;

import java.util.ArrayList;
import java.util.Collections;

public class GroupContact implements Comparable{

	public Group gp = new Group();
	public ArrayList<Contact> ctt_list = new ArrayList<Contact>();

	public void sortContacts()
	{
		try
		{
			Collections.sort(ctt_list);
		}
		catch(Exception excSortC)
		{
			;
		}
	}
	
	public int compareTo(Object o) 
	{
		String thisVal = this.gp.getName();
		String anotherVal = ((GroupContact)o).gp.getName();
		return thisVal.compareTo(anotherVal);
	}
	
	public GroupContact(Group tmp)
	{
		this.gp = tmp;
	}
	
}
