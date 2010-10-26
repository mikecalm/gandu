package android.pp;

import java.util.List;

import org.simpleframework.xml.ElementList;

public class CopyOfContactGroups {
	
	@ElementList(entry="GroupId", inline=true)
	public List<String> Groups;

}
