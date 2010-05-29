/*
     * Pole features jest mapa bitowa informujaca serwer, ktore z funkcji protokolu obslugujemy. 
     * Do minimalnej zgodnosci z protokolem Nowego Gadu-Gadu niezbedna jest co najmniej wartosc 0x00000007.
     * Bit	Wartosc		Znaczenie
     * 0	0x00000001	Rodzaj pakietu informujacego o zmianie stanu kontaktow (patrz bit 2)
     * 					0 — GG_STATUS77, GG_NOTIFY_REPLY77
     * 					1 — GG_STATUS80BETA, GG_NOTIFY_REPLY80BETA
     * 1	0x00000002	Rodzaj pakietu z otrzymaja wiadomoscia
     * 					0 — GG_RECV_MSG
     * 					1 — GG_RECV_MSG80
     * 2	0x00000004	Rodzaj pakietu informujacego o zmianie stanu kontaktow (patrz bit 0)
     * 					0 — wybrany przez bit 0
     * 					1 — GG_STATUS80, GG_NOTIFY_REPLY80
     * 4	0x00000010	Klient obsluguje statusy "nie przeszkadzac" i "poGGadaj ze mna"
     * 5	0x00000020	Klient obsluguje statusy graficzne i GG_STATUS_DESCR_MASK (patrz Zmiana stanu)
     * 6	0x00000040	Znaczenie nie jest znane, ale klient otrzyma w przypadku blednego hasla 
     * 					pakiet GG_LOGIN80_FAILED zamiast GG_LOGIN_FAILED
     * 7	0x00000100	Znaczenie nie jest znane, ale jest uzywane przez nowe klienty
     * 9	0x00000200	Klient obsluguje dodatkowe informacje o liscie kontaktow
     * 10	0x00000400	Klient wysyla potwierdzenia odebrania wiadomosci
     * 13	0x00002000	Klient obsluguje powiadomienia o pisaniu
     * 
     * Pole flags:
     * Mozliwe flagi to:
     * Bit	Wartosc	Znaczenie
     * 0	0x00000001	Nieznane, zawsze wystepuje
     * 1	0x00000002	Klient obsluguje wideorozmowy
     * 20	0x00100000	Klient mobilny (ikona telefonu komorkowego)
     * 23	0x00800000	Klient chce otrzymywac linki od nieznajomych
     */

/*
 * Przyklad zamiany Stringa na ciag bajtow i odwrotnie:
	String napis = "napis";
	Charset cset = Charset.forName("UTF-8");
	byte[] tabbyte = napis.getBytes("UTF-8");
	ByteBuffer bbuf = ByteBuffer.wrap(tabbyte);
	CharBuffer cbuf = cset.decode(bbuf);
	String odzyskane = cbuf.toString();
*/

package android.pp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class Logowanie {
	//#define GG_LOGIN80 0x0031
	int typKomunikatu = Integer.reverseBytes(0x0031);
	int dlugoscResztyPakietu;
	
	int uin;              				/* numer Gadu-Gadu */
    byte language[] = new byte[2];    	/* jezyk: "pl" */
    byte hash_type;       				/* rodzaj funkcji skrotu hasla */
    byte hash[] = new byte[64];        	/* skrot hasla dopelniony \0 */
    int status;           				/* poczatkowy status polaczenia */
    int flags;            				/* poczatkowe flagi polaczenia */
    int features;         				/* opcje protokolu (0x00000367)*/
    int local_ip = 0;         			/* lokalny adres polaczen bezposrednich (nieuzywany) */
    short local_port = 0;     			/* lokalny port polaczen bezposrednich (nieuzywany) */
    int external_ip = 0;      			/* zewnetrzny adres (nieuzywany) */
    short external_port = 0;  			/* zewnetrzny port (nieuzywany) */
    byte image_size;      				/* maksymalny rozmiar grafiki w KB */
    byte unknown1 = 0x64;  				/* 0x64 */
    int version_len;					/* dlugosc ciagu z wersja (0x23) */
    byte version[];       				/* "Gadu-Gadu Client build 10.0.0.10450" (bez \0) */
    int description_size; 				/* rozmiar opisu */
    byte description[];   				/* opis (nie musi wystapic, bez \0) */
    
    public Logowanie(int ziarno, String haslo, int numerGG, int status, byte rozmiarGrafiki, String opis)
    {
    	try 
    	{
    		this.uin = Integer.reverseBytes(numerGG);
			this.language = "pl".getBytes("UTF-8");
			//Skrot hasla SHA1
			this.hash_type = 0x02;
			
	    	//wyliczenie skrotu hasla w polaczeniu z ziarnem
	    	//i wpisanie otrzymanej wartosci do pola this.hash (dopelnic \0)
			MessageDigest alg = MessageDigest.getInstance("SHA-1");
			byte buf[] = new byte[4];
            ByteBuffer bb = ByteBuffer.wrap(buf);
            bb.putInt(Integer.reverseBytes(ziarno));
			alg.update(bb.array());
			alg.update(haslo.getBytes("UTF-8"));
			//bb.clear();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			/* skrot hasla */
			dos.write(alg.digest());
			//bb.put(alg.digest());
			/* dopelniony \0 */
			dos.write(0);
			//bb.put((byte)0);
			//this.hash = bb.array();         
			this.hash = baos.toByteArray();
			
			this.status = Integer.reverseBytes(status);
			//0x00000001	Nieznane, zawsze wystepuje
			this.flags = Integer.reverseBytes(1);
			//Do minimalnej zgodnosci z protokolem Nowego Gadu-Gadu niezbedna jest co najmniej wartosc 0x00000007
			this.features = Integer.reverseBytes(0x00000007);
			this.image_size = rozmiarGrafiki;
			this.version = "Gadu-Gadu Client build 10.0.0.10450".getBytes("UTF-8");
			//powinna byc 35 bajtow
			this.version_len = Integer.reverseBytes(this.version.length);
			this.description = opis.getBytes("UTF-8");
			this.description_size = Integer.reverseBytes(this.description.length);
			
		} catch (Exception e) 
		{
			;
		}	
    }
    //w razie wystapienia bledu funkcja zwraca null
    public byte[] pobraniePaczkiBajtow()
    {	
    	byte wynik[];
    	try
    	{
	    	byte[] resztaPaczki;
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeInt(this.uin);
			dos.write(this.language);
			dos.write(this.hash_type);
			dos.write(this.hash);
			dos.writeInt(this.status);
			dos.writeInt(this.flags);
			dos.writeInt(this.features);
			dos.writeInt(this.local_ip);
			dos.writeInt(this.local_port);
			dos.writeInt(this.external_ip);
			dos.writeInt(this.external_port);
			dos.write(this.image_size);
			dos.write(this.unknown1);
			dos.writeInt(this.version_len);
			dos.write(this.version);
			dos.writeInt(this.description_size);
			dos.write(this.description);
			resztaPaczki = baos.toByteArray();
			
	    	//wyliczyc this.dlugoscResztyPakietu
			this.dlugoscResztyPakietu = Integer.reverseBytes(resztaPaczki.length);
			ByteArrayOutputStream paczkaWTabBajtow = new ByteArrayOutputStream();
			DataOutputStream dos2 = new DataOutputStream(paczkaWTabBajtow);
			dos2.writeInt(this.typKomunikatu);
			dos2.writeInt(this.dlugoscResztyPakietu);
			dos2.write(resztaPaczki);
			wynik = paczkaWTabBajtow.toByteArray();
    	}
    	catch(Exception e)
    	{
    		wynik = null;
    	}
    	return wynik;
    }
}
