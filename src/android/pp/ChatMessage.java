package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import android.util.Log;

public class ChatMessage {
		
		int recipient_sender = 0;		/* numer odbiorcy */
		int seq = 0;					/* numer sekwencyjny */
		int message_class = 0;			/* klasa wiadomości */
		int offset_plain = 0;			/* położenie treści czystym tekstem */
		int offset_attributes = 0;		/* położenie atrybutów */
		byte html_message[] = null;		/* treść w formacie HTML (zakończona \0) */
		byte plain_message[] = null;	/* treść czystym tekstem (zakończona \0) */
		int attributes = 0;				/* atrybuty wiadomości */
		
		public byte [] setMessage(String text, int ggnumber){
			byte[] wynik = null;
			try
			{
				this.recipient_sender=Integer.reverseBytes(ggnumber);
				this.seq = Integer.reverseBytes((int) (System.currentTimeMillis() / 1000L));
				this.message_class = 0x08;
				//this.offset_plain = 0x6b;
				//this.offset_attributes = 0x70;
				String msg = new String("<span style=\"color:#000000; font-family:'MS Shell Dlg 2'; font-size:9pt; \">"+text+"</span>");
				//this.html_message =msg.getBytes("UTF-8"); 
				this.html_message= (msg +"\0").getBytes();
				//msg = "\0";
				//String textCP1250 = new String(text.getBytes("UTF-8"),"CP1250");
				//String textCP1250 = text;
				msg = text+"\0";
				this.offset_plain = Integer.reverseBytes(this.html_message.length+20);
				this.offset_attributes = Integer.reverseBytes(this.offset_plain+msg.length());
				//this.plain_message = msg.getBytes("UTF-8");
				this.plain_message = msg.getBytes("CP1250");
				//String attr = "00000206";
				this.attributes = Integer.reverseBytes(0x00000206);
				
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


}
