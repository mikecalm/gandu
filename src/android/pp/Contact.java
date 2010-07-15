package android.pp;

public class Contact {
	
	private String guid = null;
	private int ggnumber = 0;
	private String showName = null;
	private String CGroupId = null;
	
	private boolean flagBuddy = true;
	private boolean flagNormal = true;
	private boolean flagFriend = true;
	
	public String toString()
	{
		return guid+";"+ggnumber+";"+showName+";"+CGroupId+";"+flagBuddy+";"+flagNormal+";"+flagFriend;
	}

}
