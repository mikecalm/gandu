package android.pp;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="ContactBook")
public class SIMPLEContactBookList{
	
	//przedrostki A1, A2.. sa wymagane, poniewaz elementy
	//do pliku XML zapisywane sa wedlug kolejnosci alfabetycznej
	//nazw pol danej klasy.
	//Klient GG jest wrazliwy na kolejnosc pol, w przypadku
	//gdy elementy w pliku XML sa w innej kolejnosc to 
	//klient GG nie zaimportuje listy.
	@Element(required=false, name="Groups")
	public SIMPLEGroups A1Groupsy;
	@Element(required=false, name="Contacts")
	public SIMPLEContacts A2Contactsy;	

}
