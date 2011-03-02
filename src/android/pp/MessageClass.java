package android.pp;

public class MessageClass {
	
	public String incomingTime;
	public long incomingTimeMSec;
	public String senderNum;
	public String senderName;
	public String message;
	
	public MessageClass(long timeMSec, String senderNu, String senderNa, String mess)
	{
		this.incomingTimeMSec = timeMSec;
		this.incomingTime = new java.text.SimpleDateFormat(" (dd/MM/yyyy HH:mm:ss) ").format(this.incomingTimeMSec);
		this.senderNum = senderNu;
		this.senderName = senderNa;
		if(senderNa.equals(""))
			this.senderName = this.senderNum;
		this.message = mess;
	}

}
