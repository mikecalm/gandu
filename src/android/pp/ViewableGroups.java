package android.pp;

public class ViewableGroups implements Comparable{
	
	public String name;
	public String groupid;
	
	@Override
	public int compareTo(Object another) {
		String thisVal = this.groupid;
		String anotherVal = ((ViewableGroups)another).groupid;
		return thisVal.compareToIgnoreCase(anotherVal);
	}

}
