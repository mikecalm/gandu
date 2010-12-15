package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import android.util.Log;

public class StatusChangeMessage {
	
	int status;				/* nowy status */
	int flags;              /* nowe flagi */
	int description_size = 0;   /* rozmiar opisu */
	byte description[];		/* opis (nie musi wyst¹piæ, bez \0) */
	
	public byte[] setStatus(String stat, String opis)
	{
		byte[] wynik = null;
		try
		{
			this.flags = Integer.reverseBytes(1);
			if(opis != "")
			{
				//START operacje niezbedne do obciecia opisu do maks 255 bajtow
				int dlugoscOpWStringu = opis.length();
				this.description = opis.getBytes("UTF-8");
				if(dlugoscOpWStringu > 255)
				{
					this.description = opis.substring(0, 255).getBytes("UTF-8");
					dlugoscOpWStringu = 255;
				}
				while(this.description.length > 255)
				{
					Log.i("StatusChangeMessage","Wszedlem do while dlugoscOpWStringu: "+dlugoscOpWStringu);
					Log.i("StatusChangeMessage","Teraz opis wyglada: "+opis);
					this.description = opis.substring(0, --dlugoscOpWStringu).getBytes("UTF-8");
				}
				//KONIEC operacje niezbedne do obciecia opisu do maks 255 bajtow
				
				this.description_size = Integer.reverseBytes(this.description.length);
			}
			
			if(this.description.length > 0)
			{
				Log.i("StatusChangeMessage","description_size>0: "+this.description.length);
				if(stat.equals("Niewidoczny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_INVISIBLE_DESCR);
				else if(stat.equals("Niedostepny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_NOT_AVAIL_DESCR);
				else if(stat.equals("Dostepny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_AVAIL_DESCR);
				else if(stat.equals("Zaraz wracam"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_BUSY_DESCR);
			}
			else
			{
				Log.i("StatusChangeMessage","description_size nie >0: "+this.description.length);
				if(stat.equals("Niewidoczny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_INVISIBLE);
				else if(stat.equals("Niedostepny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_NOT_AVAIL);
				else if(stat.equals("Dostepny"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_AVAIL);
				else if(stat.equals("Zaraz wracam"))
					this.status = Integer.reverseBytes(Common.GG_STATUS_BUSY);
			}
			
			byte[] resztaPaczki;
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(this.status);
			dos.writeInt(this.flags);
			dos.writeInt(this.description_size);
			if(this.description.length > 0)
				dos.write(this.description);			
			resztaPaczki = baos.toByteArray();
			
			//wyliczyc dlugoscResztyPakietu
			int dlugoscResztyPakietu = Integer.reverseBytes(resztaPaczki.length);
			ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(paczkaWTabBajtow);
			dos2.writeInt(Integer.reverseBytes(Common.GG_NEW_STATUS80));
			dos2.writeInt(dlugoscResztyPakietu);
			dos2.write(resztaPaczki);
			wynik = paczkaWTabBajtow.toByteArray();
		}
		catch(Exception e)
		{
			Log.e("SettingStatusError","Blad w klasie StatusChandeMessage w metodzie setStatus");
		}
		
		return wynik;
	}

}
