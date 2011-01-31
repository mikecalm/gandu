package android.pp;

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
	public int status;
	public Boolean blocked = false;
	
	@Override
	public int compareTo(Object another) {
		String thisVal = this.showName;
		//jesli kontakt ma status niedostepny, niedostepny z opisem lub ignorowany
		if(this.status == 1 || this.status == 15 || this.status == 6)
			thisVal = "2"+thisVal;
		//jesli kontakt jest dostepny, zaraz wracam, poggadaj itp.
		else
			thisVal = "1"+thisVal;
		String anotherVal = ((ViewableContacts)another).showName;
		//jesli kontakt ma status niedostepny, niedostepny z opisem lub ignorowany
		if(((ViewableContacts)another).status == 1 || ((ViewableContacts)another).status == 15 || ((ViewableContacts)another).status == 6)
			anotherVal = "2"+anotherVal;
		//jesli kontakt jest dostepny, zaraz wracam, poggadaj itp.
		else
			anotherVal = "1"+anotherVal;
		return thisVal.compareToIgnoreCase(anotherVal);
	}

}
