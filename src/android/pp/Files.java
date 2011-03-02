package android.pp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Random;

import android.util.Log;

public class Files {
	public Long id;
	public String sendingFilePath;
	public String sendingFileName;
	public int ggReceiverNumber;
	public int ggSenderNumber;
	public Long fileSize;
	public int proxyIP1 = -1;
	public short proxyPort1 = -1;
	public int proxyIP2 = -1;
	public short proxyPort2 = -1;
	
	public void Files()
	{
		this.proxyIP1 = -1;
		this.proxyPort1 = -1;
		this.proxyIP2 = -1;
		this.proxyPort2 = -1;		
	}
	
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
	
	//public byte[] prepareSendFileRequest(int from, int to, String filePath, String fileName)
	public byte[] prepareSendFileRequest(int from, int to)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			//File plik = new File(filePath);
			File plik = new File(this.sendingFilePath);
			Long rozmiarPliku = plik.length();
			//byte[] nazwaPliku = fileName.getBytes("UTF-8");
			byte[] nazwaPliku = this.sendingFileName.getBytes("UTF-8");
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
			
			//this.sendingFilePath = filePath;
			//this.sendingFileName = fileName;
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excSend)
		{
			return null;
		}
	}
	
	public byte[] prepareSendRelayRequest(short req_type)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{	
			dos.writeInt(Integer.reverseBytes(0x0a));
			//dlugosc calej paczki
			//2*int(4)+long(8)+short(2)+2*byte(1) = 20
			dos.writeInt(Integer.reverseBytes(20));
			dos.writeLong(Long.reverseBytes(this.id));
			dos.writeShort(Short.reverseBytes(req_type));
			dos.write(0x02);
			dos.write(0x00);
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excSend)
		{
			return null;
		}
	}
	
	//public byte[] prepareSendDCCInfo(int uin, int type)
	public byte[] prepareSendDCCInfo(int type)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{	
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_INFO));
			//dlugosc paczki
			//2*int(4)+long(8)+2*32*byte(1) = 80
			dos.writeInt(Integer.reverseBytes(80));
			//dos.writeInt(Integer.reverseBytes(uin));
			dos.writeInt(Integer.reverseBytes(this.ggReceiverNumber));
			dos.writeInt(Integer.reverseBytes(type));
			dos.writeLong(Long.reverseBytes(this.id));
			//wyznaczenie pol info i cookie
			if(type == Common.GG_DCC7_TYPE_SERVER)
			{
				byte[] info = ("GG"+this.id+"CH"+(new Random().nextInt(10000))).getBytes("UTF-8");
				//pole coockie ma same 0, dlatego 64 - info.length, bo dopelnienie 
				//wypelnie tez pole cookie
				int liczbaDopelnianychZer = 64 - info.length;
				byte[]dopelnienie = new byte[liczbaDopelnianychZer];
				Arrays.fill(dopelnienie, (byte)0);
				dos.write(info);
				dos.write(dopelnienie);
			}
			else if (type == Common.GG_DCC7_TYPE_P2P)
			{
				//pole info bedzie zawieralo jakies oszukany IP i numer portu
				//bo wysylanie plikow bedzie sie odbywalo tylko przez serwer posredniczacy
				byte[] info = "192.168.0.2 30000".getBytes("UTF-8");
				int liczbaDopelnianychZer = 32 - info.length;
				byte[]dopelnienie = new byte[liczbaDopelnianychZer];
				Arrays.fill(dopelnienie, (byte)0);
				
				//pole coockie:
				//int cookie = adres + port * rand();
				//192.168.1.2 -> (int)-962068478
				byte[] cookie = (""+((-962068478)+30000*(new Random().nextInt(10000)))).getBytes("UTF-8");
				int liczbaDopelnianychZerCookie = 32 - cookie.length;
				byte[]dopelnienieCookie = new byte[liczbaDopelnianychZerCookie];
				Arrays.fill(dopelnienieCookie, (byte)0);
				
				dos.write(info);
				dos.write(dopelnienie);
				dos.write(cookie);
				dos.write(dopelnienieCookie);
			}
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excSend)
		{
			return null;
		}
	}
	
	public byte[] prepareWelcomeProxy()
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{	
			dos.writeInt(Integer.reverseBytes(0xc0debabe));
			dos.writeLong(Long.reverseBytes(this.id));
			
			wynik = baos.toByteArray();
			return wynik;
		}catch(Exception excSend)
		{
			return null;
		}
	}
	
	public byte[] prepareFileBytes()
	{
		byte[] wynik = null;
		try
		{	
			File plik = new File(this.sendingFilePath);
			Long wielkoscPliku = plik.length();
			if(plik.canRead())
			{
				FileInputStream fis = new FileInputStream(plik);
				BufferedInputStream bis = new BufferedInputStream(fis);
				if(wielkoscPliku > Integer.MAX_VALUE)
					return null;
				int tmp = 0;
				int wielkoscInt = Integer.parseInt(wielkoscPliku.toString());
				wynik = new byte[wielkoscInt];
				while (tmp!=wielkoscInt)
					tmp+=bis.read(wynik, tmp , wielkoscInt-tmp);
				fis.close();
				bis.close();
			}
			else
				Log.e("[Files]","Nie mozna czytac pliku: "+this.sendingFilePath);
			
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
	
	public byte[] acceptByReceiver(Long id, int myGGNumber)
	{
		byte[] wynik;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try
		{
			dos.writeInt(Integer.reverseBytes(Common.GG_DCC7_ACCEPT));
			//dlugosc paczki
			//2*long(8) + int(4) = 20
			dos.writeInt(Integer.reverseBytes(20));
			dos.writeInt(Integer.reverseBytes(myGGNumber));
			dos.writeLong(Long.reverseBytes(id));
			dos.writeLong(Long.reverseBytes(0));
			
			wynik = baos.toByteArray(); 
			return wynik;
		}catch(Exception excAbort)
		{
			return null;
		}
	}

}
