package android.pp;

import java.util.Hashtable;

public class ViewableContacts implements Comparable{
	
	public String showName;
	public String GGNumber;
	public String MobilePhone;
	public String HomePhone;
	public String Email;
	public String description;
	//prawdopodobnie pole avatar bedzie musialo byc typu bitmap.
	//Poki co jest String jako np URL do avatara
	public String avatar;
	public int status = 1;
	public Boolean blocked = false;
	
	@Override
	public int compareTo(Object another) {
		String thisVal = this.showName;
		thisVal = Common.wyciecieOgonkow(thisVal);
		//jesli kontakt ma status niedostepny, niedostepny z opisem lub ignorowany
		if(this.status == Common.GG_STATUS_NOT_AVAIL || this.status == Common.GG_STATUS_NOT_AVAIL_DESCR || this.status == Common.GG_STATUS_BLOCKED)
			thisVal = "2"+thisVal;
		//jesli kontakt jest dostepny, zaraz wracam, poggadaj itp.
		else
			thisVal = "1"+thisVal;
		String anotherVal = ((ViewableContacts)another).showName;
		anotherVal = Common.wyciecieOgonkow(anotherVal);
		//jesli kontakt ma status niedostepny, niedostepny z opisem lub ignorowany
		if(((ViewableContacts)another).status == Common.GG_STATUS_NOT_AVAIL || ((ViewableContacts)another).status == Common.GG_STATUS_NOT_AVAIL_DESCR || ((ViewableContacts)another).status == Common.GG_STATUS_BLOCKED)
			anotherVal = "2"+anotherVal;
		//jesli kontakt jest dostepny, zaraz wracam, poggadaj itp.
		else
			anotherVal = "1"+anotherVal;
		return thisVal.compareToIgnoreCase(anotherVal);
	}	

}
