package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class ChatMessage {
		
	int recipient_sender = 0;		/* numer odbiorcy */
	int seq = 0;					/* numer sekwencyjny */
	int message_class = 0;			/* klasa wiadomości */
	int offset_plain = 0;			/* położenie treści czystym tekstem */
	int offset_attributes = 0;		/* położenie atrybutów */
	byte html_message[] = null;		/* treść w formacie HTML (zakończona \0) */
	byte plain_message[] = null;	/* treść czystym tekstem (zakończona \0) */
	byte attributes[] = null;				/* atrybuty wiadomości */
	
	public byte [] setMessage(String text, int ggnumber, int _seq){
		byte[] wynik = null;
		try
		{
			this.recipient_sender=Integer.reverseBytes(ggnumber);
			this.seq = Integer.reverseBytes(_seq);
			this.message_class = Integer.reverseBytes(0x00000008);
			String msg = new String("<span style=\"color:#000000; font-family:'MS Shell Dlg 2'; font-size:9pt; \">"+text+"</span>"); 
			this.html_message= (msg +"\0").getBytes();
			msg = text+"\0";
			int polozeniePlain = this.html_message.length+20;
			this.offset_plain = Integer.reverseBytes(polozeniePlain);
			this.offset_attributes = Integer.reverseBytes(polozeniePlain+msg.length());
			this.plain_message = msg.getBytes("CP1250");
			//informacje o tym, ze tekst ma kolor czarny
			this.attributes = new byte[]{0x02,0x06,0x00,0x00,0x00,0x08,0x00,0x00,0x00};
			
			byte[] resztaPaczki;
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(this.recipient_sender);
			dos.writeInt(this.seq);
			dos.writeInt(this.message_class);
			dos.writeInt(this.offset_plain);
			dos.writeInt(this.offset_attributes);
			dos.write(this.html_message);
			dos.write(this.plain_message);
		    dos.write(this.attributes);
			resztaPaczki = baos.toByteArray();
			
	    	//wyliczyc this.dlugoscResztyPakietu
			int dlugoscResztyPakietu = Integer.reverseBytes(resztaPaczki.length);
			ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(paczkaWTabBajtow);
			dos2.writeInt(Integer.reverseBytes(Common.GG_SEND_MSG80));
			dos2.writeInt(dlugoscResztyPakietu);
			dos2.write(resztaPaczki);
			wynik = paczkaWTabBajtow.toByteArray();
			
		}
		catch(Exception e)
		{
			Log.e("SendingMessageError","ZONK");
		}
		return wynik;
		
	}
	
	public byte [] setConferenceMessages(String text, int[] conferenceNumbers, int _seq){
		byte[] wynikKonferencji = null;
		byte[][] wyniki = new byte[conferenceNumbers.length][];
		try
		{
			int[] recipient_senders = new int[conferenceNumbers.length];
			for(int i=0; i<conferenceNumbers.length; i++)
				recipient_senders[i] = Integer.reverseBytes(conferenceNumbers[i]);
			this.seq = Integer.reverseBytes(_seq);
			message_class = Integer.reverseBytes(0x00000008);
			String msg = new String("<span style=\"color:#000000; font-family:'MS Shell Dlg 2'; font-size:9pt; \">"+text+"</span>"); 
			this.html_message= (msg +"\0").getBytes();
			msg = text+"\0";
			int polozeniePlain = this.html_message.length+20;
			this.offset_plain = Integer.reverseBytes(polozeniePlain);
			this.offset_attributes = Integer.reverseBytes(polozeniePlain+msg.length());
			this.plain_message = msg.getBytes("CP1250");
			//informacje o tym, ze tekst ma kolor czarny
			this.attributes = new byte[]{0x02,0x06,0x00,0x00,0x00,0x08,0x00,0x00,0x00};
			byte[][] conferenceAttributesRecipients = new byte[conferenceNumbers.length][];
			for(int i=0; i<conferenceNumbers.length; i++)
			{
				byte[] allAtrributes = null;
				ByteArrayOutputStream conferenceBaos = new ByteArrayOutputStream();
				DataOutputStream conferenceDos = new DataOutputStream(conferenceBaos);
				conferenceDos.write(conferenceAttribute(conferenceNumbers,i));
				conferenceDos.write(this.attributes);
				allAtrributes = conferenceBaos.toByteArray();
				conferenceAttributesRecipients[i] = allAtrributes.clone();
			}
			
			for(int i=0; i<conferenceNumbers.length; i++)
			{
				byte[] wynik = null;
				byte[] resztaPaczki;
		    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeInt(recipient_senders[i]);
				dos.writeInt(this.seq);
				dos.writeInt(this.message_class);
				dos.writeInt(this.offset_plain);
				dos.writeInt(this.offset_attributes);
				dos.write(this.html_message);
				dos.write(this.plain_message);
				dos.write(conferenceAttributesRecipients[i]);
				resztaPaczki = baos.toByteArray();
				
		    	//wyliczyc this.dlugoscResztyPakietu
				int dlugoscResztyPakietu = Integer.reverseBytes(resztaPaczki.length);
				ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
				DataOutputStream dos2 = new DataOutputStream(paczkaWTabBajtow);
				dos2.writeInt(Integer.reverseBytes(Common.GG_SEND_MSG80));
				dos2.writeInt(dlugoscResztyPakietu);
				dos2.write(resztaPaczki);
				wynik = paczkaWTabBajtow.toByteArray();
				wyniki[i] = wynik.clone();
			}
			
			ByteArrayOutputStream konferencjaAll = new ByteArrayOutputStream();
			DataOutputStream dos3 = new DataOutputStream(konferencjaAll);
			for(int i=0; i<wyniki.length; i++)	
				dos3.write(wyniki[i]);
			wynikKonferencji = konferencjaAll.toByteArray();
		}
		catch(Exception e)
		{
			Log.e("SendingMessageError","ZONK");
		}
		return wynikKonferencji;			
	}
	
	public byte [] conferenceAttribute(int[] ggNumbers, int odbiorca)
	{			
		byte[] wynik = null;

		try 
		{
			byte flagaKonferencji = 0x01;
			int liczbaOdbiorcow = Integer.reverseBytes(ggNumbers.length-1);
			
			ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(paczkaWTabBajtow);
			dos.write(flagaKonferencji);			
			dos.writeInt(liczbaOdbiorcow);
			for(int i=0; i<ggNumbers.length; i++)
			{
				if(i!=odbiorca)
					dos.writeInt(Integer.reverseBytes(ggNumbers[i]));
			}
			wynik = paczkaWTabBajtow.toByteArray();
		} 
		catch (IOException e) 
		{
			Log.e("[ChatMessage]Konferencja", e.getMessage());
		}
			
		return wynik;
	}
}
