package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Arrays;

public class Files {
	public Long id;
	public int ggReceiverNumber;
	public int proxyIP;
	public short proxyPort;
	
	public byte[] prepareIDRequest()
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_ID_REQUEST));
			//dlugosc paczki
			//int(4) = 4
			dos.writeInt(Integer.reverseBytes(4));
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_TYPE_FILE));
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excPrep)
		{
			return null;
		}
	}
	
	public byte[] prepareSendFileRequest(int from, int to, String filePath, String fileName)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			File plik = new File(filePath);
			Long rozmiarPliku = plik.length();
			byte[] nazwaPliku = fileName.getBytes("UTF-8");
			int liczbaDopelnianychZer = 255 - nazwaPliku.length;
			byte[]dopelnienie = new byte[liczbaDopelnianychZer];
			Arrays.fill(dopelnienie, (byte)0);
			byte[] hash = new byte[20];
			Arrays.fill(hash, (byte)0);
			
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_NEW));
			//dlugosc paczki
			//long(8) + 3*int(4)+255*byte(1)+long(8)+20*byte(1) = 303
			dos.writeInt(Integer.reverseBytes(303));
			dos.writeLong(Long.reverseBytes(this.id));
			dos.writeInt(Integer.reverseBytes(from));
			dos.writeInt(Integer.reverseBytes(to));
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_TYPE_FILE));
			dos.write(nazwaPliku);
			dos.write(dopelnienie);
			dos.writeLong(Long.reverseBytes(rozmiarPliku));
			dos.write(hash);
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excSend)
		{
			return null;
		}
	}
	
	public byte[] abortBySenderRequest(int from, int to)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_ABORT));
			//dlugosc paczki
			//long(8) + 2*int(4) = 16
			dos.writeInt(Integer.reverseBytes(16));
			dos.writeLong(Long.reverseBytes(this.id));
			dos.writeInt(Integer.reverseBytes(from));
			dos.writeInt(Integer.reverseBytes(to));
			
			wynik = baos.toByteArray(); 
			return wynik;
		}catch(Exception excAbort)
		{
			return null;
		}
	}
	
	public byte[] rejectByReceiverRequest(Long id, int myGGNumber)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_REJECT));
			//dlugosc paczki
			//long(8) + 2*int(4) = 16
			dos.writeInt(Integer.reverseBytes(16));
			dos.writeInt(Integer.reverseBytes(myGGNumber));
			dos.writeLong(Long.reverseBytes(id));
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_REJECT_USER));
			
			wynik = baos.toByteArray(); 
			return wynik;
		}catch(Exception excAbort)
		{
			return null;
		}
	}

}
