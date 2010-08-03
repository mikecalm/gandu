package android.pp;

public class Contact implements Comparable{
	
	private String guid = null;
	private String ggnumber = null;
	private String showName = null;
	private String CGroupId = null;
	private boolean flagBuddy = true;
	private boolean flagNormal = true;
	private boolean flagFriend = true;
	private boolean flagIgnored = false;
	
	public int compareTo(Object o) 
	{
		String thisVal = this.showName;
		String anotherVal = ((Contact)o).showName;
		return thisVal.compareTo(anotherVal);
	}
	
	public String getflagBuddy()
	{
		return Boolean.toString(flagBuddy);
	}
	
	public String getflagFriend()
	{
		return Boolean.toString(flagFriend);
	}
	
	public String getflagNormal()
	{
		return Boolean.toString(flagNormal);
	}

	public String getflagIgnored(String data)
	{
		return Boolean.toString(flagIgnored);
	}
	
	public void setflagBuddy(String data)
	{
		this.flagBuddy = Boolean.getBoolean(data);
	}
	
	public void setflagNormal(String data)
	{
		this.flagNormal = Boolean.getBoolean(data);
	}
	
	public void setflagFriend(String data)
	{
		this.flagFriend= Boolean.getBoolean(data);
	}
	
	public void setflagIgnored(String data)
	{
		this.flagIgnored= Boolean.getBoolean(data);
	}
	
	public String getGuid()
	{
		return guid;
	}
	
	public void setGuid(String data)
	{
		this.guid = data;
	}
	
	public String getGroupId()
	{
		return CGroupId;
	}
	
	public void setGroupId(String data)
	{
		this.CGroupId = data;
	}
	
	public String getShowName()
	{
		return showName;
	}
	
	public void setShowName(String data)
	{
		this.showName = data;
	}
	
	public String getGGNumber()
	{
		return ggnumber;
	}
	public void setGGNumber(String data)
	{
		this.ggnumber = data;
	}
	
	public String toString()
	{
		return "\n"+"-> Nazwa:" +showName+"\nNumer GG: "+ ggnumber+"\nGUID: "+guid+ ";\nGroupId: "+CGroupId+";\n"+flagBuddy+";\n"+flagNormal+";\n"+flagFriend+"\n"+flagIgnored+"\n";
	}

}
