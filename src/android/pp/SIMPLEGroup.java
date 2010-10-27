package android.pp;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name="Group")
public class SIMPLEGroup implements Comparable{
	
	//przedrostki A1, A2.. sa wymagane, poniewaz elementy
	//do pliku XML zapisywane sa wedlug kolejnosci alfabetycznej
	//nazw pol danej klasy.
	//Klient GG jest wrazliwy na kolejnosc pol, w przypadku
	//gdy elementy w pliku XML sa w innej kolejnosc to 
	//klient GG nie zaimportuje listy.
	@Element(name="Id")
	public String A1Id;
	@Element(name="Name")
	public String A2Name;
	
	@Element(required=false, name="IsExpanded")
	public Boolean A3IsExpanded;
	@Element(required=false, name="IsRemovable")
	public Boolean A4IsRemovable;
	
	public SIMPLEGroup()
	{
		this.A1Id = "Id";
		this.A2Name = "Name";
	}

	@Override
	public int compareTo(Object arg0) {
		String thisVal = this.A1Id;
		String anotherVal = ((SIMPLEGroup)arg0).A1Id;
		//return thisVal.compareTo(anotherVal);
		return thisVal.compareToIgnoreCase(anotherVal);
	}

}
