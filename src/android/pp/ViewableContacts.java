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
	
	@Override
	public int compareTo(Object another) {
		String thisVal = this.showName;
		String anotherVal = ((ViewableContacts)another).showName;
		return thisVal.compareToIgnoreCase(anotherVal);
	}

}
