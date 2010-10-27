package android.pp;

public class CopyOfViewableGroups implements Comparable{
	
	public String name;
	public String groupid;
	
	@Override
	public int compareTo(Object another) {
		String thisVal = this.groupid;
		String anotherVal = ((CopyOfViewableGroups)another).groupid;
		//return thisVal.compareTo(anotherVal);
		return thisVal.compareToIgnoreCase(anotherVal);
	}

}
