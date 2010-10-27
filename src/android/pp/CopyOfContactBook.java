package android.pp;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="ContactBook")
public class CopyOfContactBook{
	
	//przedrostki A1, A2.. sa wymagane, poniewaz elementy
	//do pliku XML zapisywane sa wedlug kolejnosci alfabetycznej
	//nazw pol danej klasy.
	//Klient GG jest wrazliwy na kolejnosc pol, w przypadku
	//gdy elementy w pliku XML sa w innej kolejnosc to 
	//klient GG nie zaimportuje listy.
	@Element(required=false, name="Groups")
	public CopyOfGroups A1Groupsy;
	@Element(required=false, name="Contacts")
	public CopyOfContacts A2Contactsy;	

}
