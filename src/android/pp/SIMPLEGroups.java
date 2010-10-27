package android.pp;

import java.util.List;

import org.simpleframework.xml.ElementList;

public class SIMPLEGroups {
	
	@ElementList(required=false, inline=true)
	public List<SIMPLEGroup> Groups;

}
