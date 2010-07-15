package android.pp;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLHandler extends DefaultHandler{
	
	/**
	 * Names of marks in .XML file with contactbook
	 */
	private boolean inContactBook = false;
	private boolean inGroups = false;
	private boolean inGroup = false;
	private boolean inId = false;
	private boolean inName = false;
	private boolean inIsExpanded = false;
	private boolean inIsRemovable = false;
	
	private boolean inContacts = false;
	private boolean inContact = false;
	private boolean inGuid = false;
	private boolean inGGNumber = false;
	private boolean inShowName = false;
	private boolean inCGroups = false;
	private boolean inGroupId = false;
	private boolean inAvatars = false;
	private boolean inFlagBuddy = false;
	private boolean inFlagNormal = false;
	private boolean inFlagFriend =false;
	
	private XMLParsedDataSet pds = new XMLParsedDataSet();
	
	public  XMLParsedDataSet getParsedData() {
		return this.pds;
	}
	
	@Override
	public void startDocument() throws SAXException
	{
		this.pds = new XMLParsedDataSet();
	}
	
	@Override
	public void endDocument() throws SAXException
	{
		//  ntd
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
	{
		if (localName.equals("ContactBook"))
		{
			this.inContactBook = true;
		}
		else if (localName.equals("Groups"))
		{
			this.inGroups = true;
		}
		else if(localName.equals("Group"))
		{
			this.inGroup = true;
		}
		else if (localName.equals("Id"))
		{
			this.inId = true;
		}
		else if (localName.equals("Name"))
		{
			this.inName = true;
			
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException
	{
		if (localName.equals("ContactBook"))
		{
			this.inContactBook = false;
		}
		else if (localName.equals("Groups"))
		{
			this.inGroups = false;
		}
		else if(localName.equals("Group"))
		{
			this.inGroup = false;
		}
		else if (localName.equals("Id"))
		{
			this.inId = false;
		}
		else if (localName.equals("Name"))
		{
			this.inName = false;
			
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length)
	{
		if (this.inContactBook)
		{
			if (this.inGroups)
			{
				if (this.inGroup)
				{
					if(this.inId)
					{
						pds.setGroupID(new String(ch, start, length));
					}
					else if(this.inName)
					{
						pds.setName(new String(ch, start, length));
					}
				}
				
			}
		}
	}
	
}
