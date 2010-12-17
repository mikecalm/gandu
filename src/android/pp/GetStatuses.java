package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class GetStatuses {

	public List<GetStatusesStruct> StatusesPairs = new ArrayList<GetStatusesStruct>();
	public byte [] preparePackage()
	{
		byte[] resztaPaczki;
		byte[] wynik;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int size = StatusesPairs.size(); //TO DO if > 400 new packet
		try
		{
			for(int i=0; i<size; i++)
			{
				dos.writeInt(this.StatusesPairs.get(i).uin);
				dos.write(this.StatusesPairs.get(i).type);
			}
			resztaPaczki = baos.toByteArray();
	    	//wyliczyc this.dlugoscResztyPakietu
			int dlugoscResztyPakietu = Integer.reverseBytes(resztaPaczki.length);
			ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(paczkaWTabBajtow);
			dos2.writeInt(Integer.reverseBytes(Common.GG_NOTIFY_LAST));
			dos2.writeInt(dlugoscResztyPakietu);
			dos2.write(resztaPaczki);
			return wynik = paczkaWTabBajtow.toByteArray();
		}
		catch(Exception e)
		{
			Log.e("GetStatuses","Byte Package NOT Created!");
			return null;
		}				
	}
}
