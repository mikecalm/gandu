package android.pp;

import java.io.StringReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.util.Log;

public class XMLContactBook{
	
	public XMLParsedDataSet xmlparse(String tmp) {
	
		XMLParsedDataSet pds = new XMLParsedDataSet();
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try
		{
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			XMLHandler handler = new XMLHandler();
			xr.setContentHandler(handler);
			InputSource is = new InputSource(new StringReader(tmp));
			xr.parse(is);			
			pds = handler.getParsedData();			
			
		}catch(Exception e)
		{
			Log.e("XMLContactBook",e.getMessage());
		}
		// TODO Auto-generated constructor stub	
		return pds;
	}	
}
