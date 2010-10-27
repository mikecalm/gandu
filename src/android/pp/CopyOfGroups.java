package android.pp;

import java.util.List;

import org.simpleframework.xml.ElementList;

public class CopyOfGroups {
	
	@ElementList(required=false, inline=true)
	public List<CopyOfGroup> Groups;

}
