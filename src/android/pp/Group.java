package android.pp;

public class Group {

	private String groupId = null;
	private String name = null;
	private boolean isExpanded = true;
	private boolean isRemovable = true;
	
	public void setGroupId(String data)
	{
		this.groupId = data;
	}
	public String getGroupId()
	{
		return groupId;
	}
	
	public void setName(String data)
	{
		this.name = data;
	}
	public String getName()
	{
		return name;
	}
	public void setIsExpanded(boolean data)
	{
		this.isExpanded = data;
	}
	public void setIsRemovable(boolean data)
	{
		this.isRemovable = data;
	}
	
	public String toString()
	{
		return groupId+";"+name+";"+isRemovable+";"+isExpanded;
	}
}
