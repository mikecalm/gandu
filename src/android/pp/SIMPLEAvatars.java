package android.pp;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root
public class SIMPLEAvatars {
	
	@ElementList(required=false, inline=true, entry="URL")
	   List<String> Avatars;

}
