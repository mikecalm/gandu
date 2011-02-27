package android.pp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class ArchiveSQLite {
	private static final String DATABASE_NAME = "archiveGG.db";
	private static final String DATABASE_TABLE = "ggmessage";
	private static final int DATABASE_VERSION = 1;
	
	//nazwy kolumn w tabeli i ich indeksy
	private static final String messageid = "messageid";
	private static final int messageidCol = 0;
	private static final String sender = "sender";
	private static final int senderCol = 1;
	private static final String recipient = "recipient";
	private static final int recipientCol = 2;
	private static final String messagetimestamp = "messagetimestamp";
	private static final int messagetimestampCol = 3;
	private static final String message = "message";
	private static final int messageCol = 4;
	private static final String unread = "unread";
	private static final int unreadCol = 5;
	private static final String conferenceMembers = "conferenceMembers";
	private static final int conferenceMembersCol = 6;
	//kolumny w tabeli
	
	//skrypt tworzacy baze
	private static final String DATABASE_CREATE = "CREATE TABLE "+DATABASE_TABLE+
		"("+messageid+" INTEGER PRIMARY KEY autoincrement, "+
		sender+" INTEGER, "+
		recipient+" INTEGER, "+
		messagetimestamp+" INTEGER, "+
		message+" TEXT, "+
		unread+" INTEGER, "+
		conferenceMembers+" TEXT);";
	//skrypt tworzacy baze
	
	//kontekst aplikacji uzywajacej bazy danych
	private Context context;
	//instancja archiveDbHelper do pobierania uchwytu do bazy
	private archiveDbHelper dbHelper;
	
	public ArchiveSQLite(Context _context)
	{
		context = _context;
		dbHelper = new archiveDbHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public long addMessage(int _sender, int _recipient, int _messagetimestamp, String _message, 
			int _unread, ArrayList<String> _conferenceMembers)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		long rezultat = -1;
		try
		{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues dodawaneWartosci = new ContentValues();
			dodawaneWartosci.put(sender, _sender);
			dodawaneWartosci.put(recipient, _recipient);
			dodawaneWartosci.put(messagetimestamp, _messagetimestamp);
			dodawaneWartosci.put(message, _message);
			dodawaneWartosci.put(unread, _unread);
			if(_conferenceMembers == null)
				dodawaneWartosci.putNull(conferenceMembers);
			else
			{
				_conferenceMembers.add(""+_sender);
				if(_recipient != -1)
					_conferenceMembers.add(""+_recipient);
				Collections.sort(_conferenceMembers);
				String uczestnicyKonferencjiPosortowani = "";
				for(int i=0; i<_conferenceMembers.size(); i++)
					uczestnicyKonferencjiPosortowani += _conferenceMembers.get(i)+";";
				//wyciecie srednika z konca ciagu
				uczestnicyKonferencjiPosortowani = uczestnicyKonferencjiPosortowani.substring(0, uczestnicyKonferencjiPosortowani.length()-1);
				Log.i("[ArchiveSQLite]Uczestnicy konferencji", uczestnicyKonferencjiPosortowani);
				dodawaneWartosci.put(conferenceMembers, uczestnicyKonferencjiPosortowani);
			}
			rezultat = db.insertOrThrow(DATABASE_TABLE, null, dodawaneWartosci);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public int setMessageAsRead(long _messageID)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		int rezultat = -1;
		try
		{
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues dodawaneWartosci = new ContentValues();
			dodawaneWartosci.put(unread, -1);
			String where = messageid+"="+_messageID;
			rezultat = db.update(DATABASE_TABLE, dodawaneWartosci, where, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public int setMessagesAsRead(int ggNum, long _lastMessageID)
	{
		int rezultat = -1;
		try
		{
			//ustawienie wszystkim odczytanym wiadomosc od numeru ggNum pola unread -1
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues zmienianeWartosci = new ContentValues();
			zmienianeWartosci.put(unread, -1);
			String where = sender+"="+ggNum+" AND "+messageid+" <= "+_lastMessageID+" AND "+conferenceMembers+" is null";
			db.update(DATABASE_TABLE, zmienianeWartosci, where, null);
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public int setMessagesAsReadConference(String _conferenceMembers, long _lastMessageID)
	{
		int rezultat = -1;
		try
		{
			//ustawienie wszystkim odczytanym wiadomosc od numeru ggNum pola unread -1
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues zmienianeWartosci = new ContentValues();
			zmienianeWartosci.put(unread, -1);
			String where = messageid+" <= "+_lastMessageID+" AND "+conferenceMembers+" LIKE '"+_conferenceMembers+"'";
			db.update(DATABASE_TABLE, zmienianeWartosci, where, null);
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public Cursor readAllNonConferenceGGNumbers()
	{
		Cursor rezultat = null;
		try
		{
			String[] kolumny = new String[] {sender};
			String where = conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(true, DATABASE_TABLE, null, where, null, null, null, null, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public ArrayList<String> showAllNonConferenceGGNumbers(Cursor _cursor)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = _cursor.getString(0);
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showAllNonConferenceGGNumbers:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public Cursor readAllConferenceVariations()
	{
		Cursor rezultat = null;
		try
		{
			String[] kolumny = new String[] {conferenceMembers};
			String where = conferenceMembers+" is not null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(true, DATABASE_TABLE, null, where, null, null, null, null, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public ArrayList<String> showAllConferenceVariations(Cursor _cursor)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = _cursor.getString(0);
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showAllConferenceVariations:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public Cursor readUnreadMessagesFrom(int ggNum)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			String where = unread + " = 1 AND "+sender+" = "+ggNum+ " AND "+conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" ASC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public Cursor readUnreadMessagesFromConference(String _conferenceMembers)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			//String where = unread + " = 1 AND "+sender+" = "+ggNum+ " AND "+conferenceMembers+" is null";
			String where = unread + " = 1 AND "+ conferenceMembers+" LIKE '"+_conferenceMembers+"'";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" ASC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public ArrayList<String> showUnreadMessages(Cursor _cursor, int ggNum)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		long lastID = -1;
		while(_cursor.moveToNext())
		{
			String builder = "";
			lastID = _cursor.getLong(messageidCol);
			long messagetimestamp = _cursor.getLong(messagetimestampCol);
			//long messagetimestamp = _cursor.getLong(1);
			String message = _cursor.getString(messageCol);
			//String message = _cursor.getString(2);
			builder += messagetimestamp+";";
			builder += message;
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadMessage:", builder);
		}
		_cursor.close();
		if(lastID != -1)
			this.setMessagesAsRead(ggNum, lastID);
		return wiadomosci;
	}
	
	public ArrayList<String> showUnreadMessagesConferece(Cursor _cursor, String _conferenceMembers)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		long lastID = -1;
		while(_cursor.moveToNext())
		{
			String builder = "";
			lastID = _cursor.getLong(messageidCol);
			long messagetimestamp = _cursor.getLong(messagetimestampCol);
			//long messagetimestamp = _cursor.getLong(1);
			String message = _cursor.getString(messageCol);
			String sender = _cursor.getString(senderCol);
			//String message = _cursor.getString(2);
			builder += messagetimestamp+";";
			builder += message+";";
			builder += sender;
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadMessage:", builder);
		}
		_cursor.close();
		if(lastID != -1)
			this.setMessagesAsReadConference(_conferenceMembers, lastID);
		return wiadomosci;
	}
	
	//pobiera z bazy wiadomosc z ostatnich lastTimeInSeconds sekund, wymieniane z ggNum (otrzymywane/wysylane)  
	public Cursor readLastMessagesFrom(int ggNum, int lastTimeInSeconds)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		long lastTime = (System.currentTimeMillis()/1000L) - lastTimeInSeconds;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			//String where = unread + " != 1 AND "+sender+" = "+ggNum+" AND "+messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";
			String where = unread + " != 1 AND ("+sender+" = "+ggNum+" OR "+recipient+" = "+ggNum+") AND "+
				messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" ASC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	//pobiera z bazy ostatnich X wiadomosc sprzed wiadomosci o ID lid, wymieniane z ggNum (otrzymywane/wysylane)  
	public Cursor readLastXMessagesFrom(int ggNum, int X, int lid)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			//String where = unread + " != 1 AND "+sender+" = "+ggNum+" AND "+messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";
			String where = unread + " != 1 AND ("+sender+" = "+ggNum+" OR "+recipient+" = "+ggNum+") AND "+
				messageid+" < "+lid+ " AND "+conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" DESC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy, ""+X);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	//pobranie id ostatniej wiadomosci z archiwum
	public int getLastArchiveMessageID()
	{
		int wynik = -1;
		try
		{
			String where = unread + " != 1";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" DESC";
			Cursor rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy, "1");
			while(rezultat.moveToNext())
			{
				wynik = rezultat.getInt(messageidCol);
			}
			rezultat.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return wynik;
		}
	}
	
	//pobranie id ostatniej wiadomosci wymienianej z ggNum (niekonferencyjnej)
	public int getLastMessageID(int ggNum)
	{
		int wynik = -1;
		try
		{
			String where = unread + " != 1 AND "+"("+sender+" = "+ggNum+" OR "+recipient+" = "+ggNum+") "+
				" AND "+conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" DESC";
			Cursor rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy, "1");
			while(rezultat.moveToNext())
			{
				wynik = rezultat.getInt(messageidCol);
			}
			rezultat.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return wynik;
		}
	}
	
	//pobranie id ostatniej wiadomosci wymienianej z konferentami _conferenceMembers
	public int getLastMessageIDConference(String _conferenceMembers)
	{
		int wynik = -1;
		try
		{			
			String where = unread + " != 1 AND "+conferenceMembers+" LIKE '"+_conferenceMembers+"'";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" DESC";
			Cursor rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy, "1");
			while(rezultat.moveToNext())
			{
				wynik = rezultat.getInt(messageidCol);
			}
			rezultat.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return wynik;
		}
	}
	
	//pobiera z bazy wiadomosc z ostatnich lastTimeInSeconds sekund, wymieniane z _conferenceMembers (otrzymywane/wysylane)  
	public Cursor readLastMessagesFromConference(String _conferenceMembers, int lastTimeInSeconds)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		long lastTime = (System.currentTimeMillis()/1000L) - lastTimeInSeconds;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			//String where = unread + " != 1 AND "+sender+" = "+ggNum+" AND "+messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";
			//String where = unread + " != 1 AND ("+sender+" = "+ggNum+" OR "+recipient+" = "+ggNum+") AND "+
			//	messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";			
			String where = unread + " != 1 AND "+
				messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" LIKE '"+_conferenceMembers+"'";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" ASC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	//pobiera z bazy X ostatnich wiadomosc sprzed wiadomosci o ID lid, wymieniane z _conferenceMembers (otrzymywane/wysylane)  
	public Cursor readLastXMessagesFromConference(String _conferenceMembers, int X, int lid)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			//String[] kolumny = new String[] {messageid, messagetimestamp, message};
			//String where = unread + " != 1 AND "+sender+" = "+ggNum+" AND "+messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";
			//String where = unread + " != 1 AND ("+sender+" = "+ggNum+" OR "+recipient+" = "+ggNum+") AND "+
			//	messagetimestamp+" > "+lastTime+ " AND "+conferenceMembers+" is null";			
			String where = unread + " != 1 AND "+
				messageid+" < "+lid+ " AND "+conferenceMembers+" LIKE '"+_conferenceMembers+"'";
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			String orderBy = messageid+" DESC";
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(DATABASE_TABLE, null, where, null, null, null, orderBy, ""+X);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public ArrayList<String> showLastMessages(Cursor _cursor, int ggNum)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = "";
			long messagetimestamp = _cursor.getLong(messagetimestampCol);
			//long messagetimestamp = _cursor.getLong(1);
			String message = _cursor.getString(messageCol);
			//String message = _cursor.getString(2);
			if(_cursor.getInt(senderCol) == ggNum)
			{
				builder += messagetimestamp+";";
				builder += message;
			}
			else
			{
				builder += "X"+messagetimestamp+";";
				builder += message;
			}
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadMessage:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public ArrayList<String> showLastXMessages(Cursor _cursor, int ggNum)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		if(_cursor.getCount() > 0)
		{
			_cursor.moveToLast();
			do
			{
				if(_cursor.isLast())
					wiadomosci.add(_cursor.getString(messageidCol));
				String builder = "";
				long messagetimestamp = _cursor.getLong(messagetimestampCol);
				String message = _cursor.getString(messageCol);
				if(_cursor.getInt(senderCol) == ggNum)
				{
					builder += messagetimestamp+";";
					builder += message;
				}
				else
				{
					builder += "X"+messagetimestamp+";";
					builder += message;
				}
				wiadomosci.add(builder);
				Log.i("ArchiveSQLite showUnreadMessage:", builder);
			}while(_cursor.moveToPrevious());
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public ArrayList<String> showLastMessagesConference(Cursor _cursor, String _conferenceMembers)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = "";
			long messagetimestamp = _cursor.getLong(messagetimestampCol);
			//long messagetimestamp = _cursor.getLong(1);
			String message = _cursor.getString(messageCol);
			String sender = _cursor.getString(senderCol);
			//String message = _cursor.getString(2);
			builder += messagetimestamp+";";
			builder += message+";";
			builder += sender;
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadMessage:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public ArrayList<String> showLastXMessagesConference(Cursor _cursor, String _conferenceMembers)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		if(_cursor.getCount() > 0)
		{
			_cursor.moveToLast();
			do
			{
				if(_cursor.isLast())
					wiadomosci.add(_cursor.getString(messageidCol));
				String builder = "";
				long messagetimestamp = _cursor.getLong(messagetimestampCol);
				//long messagetimestamp = _cursor.getLong(1);
				String message = _cursor.getString(messageCol);
				String sender = _cursor.getString(senderCol);
				//String message = _cursor.getString(2);
				builder += messagetimestamp+";";
				builder += message+";";
				builder += sender;
				wiadomosci.add(builder);
				Log.i("ArchiveSQLite showUnreadMessage:", builder);
			}while(_cursor.moveToPrevious());
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public Cursor readUnreadGGNumbers()
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			String[] kolumny = new String[] {sender};
			String where = unread + " = 1 AND "+conferenceMembers+" is null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(true, DATABASE_TABLE, kolumny, where, null, null, null, null, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public Cursor readUnreadGGNumbersConference()
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			String[] kolumny = new String[] {conferenceMembers};
			String where = unread + " = 1 AND "+conferenceMembers+" is not null";
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			//rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, orderBy);
			rezultat = db.query(true, DATABASE_TABLE, kolumny, where, null, null, null, null, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public ArrayList<String> showUnreadGGNumbers(Cursor _cursor)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = "";
			int ggNumber = _cursor.getInt(0);
			builder += ggNumber;
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadGGNumbers:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public ArrayList<String> showUnreadGGNumbersConference(Cursor _cursor)
	{
		ArrayList<String> wiadomosci = new ArrayList<String>();
		while(_cursor.moveToNext())
		{
			String builder = "";
			String _conference = _cursor.getString(0);
			builder += _conference;
			wiadomosci.add(builder);
			Log.i("ArchiveSQLite showUnreadGGNumbers:", builder);
		}
		_cursor.close();
		return wiadomosci;
	}
	
	public Cursor readMessage(long _messageid)
	{
		//jesli proba dodania rekordu sie powiedzie, to metoda zwroci
		//id nowo dodanej wiadomosci w bazie.
		//-1 bedzie oznaczac blad dodawania rekordu.
		Cursor rezultat = null;
		try
		{
			String[] kolumny = new String[] {messageid, sender, recipient, messagetimestamp, message, unread, conferenceMembers};
			String where = messageid + " = "+_messageid;
			SQLiteDatabase db = dbHelper.getReadableDatabase();			
			rezultat = db.query(DATABASE_TABLE, kolumny, where, null, null, null, null);
			//db.close();
		}
		catch(Exception exc)
		{
			Log.e("ArchiveSQLite", exc.getMessage());
		}
		finally
		{
			return rezultat;
		}
	}
	
	public void showMessage(Cursor _cursor)
	{
		StringBuilder builder = new StringBuilder();
		while(_cursor.moveToNext())
		{
			long id = _cursor.getLong(messageidCol);
			int sender = _cursor.getInt(senderCol);
			int recipient = _cursor.getInt(recipientCol);
			long messagetimestamp = _cursor.getLong(messagetimestampCol);
			String message = _cursor.getString(messageCol);
			int unread = _cursor.getInt(unreadCol);
			String conferenceMembers = _cursor.getString(conferenceMembersCol);
			builder.append(id).append(",").append(sender).append(",").append(recipient).append(",")
				.append(messagetimestamp).append(",").append(message).append(",").append(unread).append(",")
				.append(conferenceMembers);
			Log.i("ArchiveSQLite odczytana:", builder.toString());
		}
		_cursor.close();
	}

	public static class archiveDbHelper extends SQLiteOpenHelper{
		public archiveDbHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE);	
		}

		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
			_db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
			onCreate(_db);
		}		
	}
}
