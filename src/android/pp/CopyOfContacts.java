package android.pp;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root
public class CopyOfContacts {
	
	@ElementList(required=false, inline=true)
	public List<CopyOfContact> Contacts;

}
