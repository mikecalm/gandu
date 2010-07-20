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
	
	private boolean x = false;
	public GroupContact GCItem = new GroupContact();
	public Contact ctt = new Contact();
	
	public  XMLParsedDataSet pds = new XMLParsedDataSet();

	
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
		//GROUPS
		else if (localName.equals("Groups"))
		{
			if(!x)
			{
				this.inGroups = true;
			}
			else if(x)
			{
				this.inCGroups = true;
			}
		}
		else if(localName.equals("Group"))
		{
			this.inGroup = true;
			GCItem = new GroupContact();
		}
		else if (localName.equals("Id"))
		{
			this.inId = true;
		}
		else if (localName.equals("Name"))
		{
			this.inName = true;			
		}
		else if (localName.equals("IsExpanded"))
		{
			this.inIsExpanded = true;
		}
		else if (localName.equals("IsRemovable"))
		{
			this.inIsRemovable = true;
		}
		
		//CONTACTS
		else if( localName.equals("Contacts"))
		{
			this.inContacts = true;
		}
		else if(localName.equals("Contact"))
		{
			this.inContact = true;
			ctt = new Contact();
			
		}
		
		else if (localName.equals("Guid"))
		{
			this.inGuid = true;
		}
		else if (localName.equals("GGNumber"))
		{
			this.inGGNumber = true;
		}
		else if (localName.equals("ShowName"))
		{
			this.inShowName = true;
		}
		else if (localName.equals("Groups"))
		{
			this.inCGroups = true;
			
		}
		else if (localName.equals("GroupId"))
		{
			this.inGroupId = true;
		}
		else if (localName.equals("Avatars"))
		{
			this.inAvatars = true;
		}
		else if (localName.equals("FlagBuddy"))
		{
			this.inFlagBuddy = true;
		}
		else if (localName.equals("FlagNormal"))
		{
			this.inFlagNormal = true;
		}
		else if (localName.equals("FlagFriend"))
		{
			this.inFlagFriend = true;
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
			if(x)
			{
				this.inCGroups = false;
			}
			if(!x)
			{
				this.inGroups = false;
				x = true;
			}
			
		}
		else if(localName.equals("Group"))
		{
			this.inGroup = false;
			XMLParsedDataSet.GCList.add(GCItem);	
		}
		else if (localName.equals("Id"))
		{
			this.inId = false;
		}
		else if (localName.equals("Name"))
		{
			this.inName = false;
			
		}
		else if (localName.equals("IsExpanded"))
		{
			this.inIsExpanded = false;
		}
		else if (localName.equals("IsRemovable"))
		{
			this.inIsRemovable = false;
		}
		
		//CONTACTS
		
		else if( localName.equals("Contacts"))
		{
			this.inContacts = false;
		}
		else if(localName.equals("Contact"))
		{
			this.inContact = false;
			
		}
		
		else if (localName.equals("Guid"))
		{
			this.inGuid = false;
		}
		else if (localName.equals("GGNumber"))
		{
			this.inGGNumber = false;
		}
		else if (localName.equals("ShowName"))
		{
			this.inShowName = false;
		}
		/*else if (localName.equals("Groups"))
		{
			this.inCGroups = false;
		}*/
		else if (localName.equals("GroupId"))
		{
			this.inGroupId = false;
		}
		else if (localName.equals("Avatars"))
		{
			this.inAvatars = false;
		}
		else if (localName.equals("FlagBuddy"))
		{
			this.inFlagBuddy = false;
		}
		else if (localName.equals("FlagNormal"))
		{
			this.inFlagNormal = false;
		}
		else if (localName.equals("FlagFriend"))
		{
			this.inFlagFriend = false;
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
						pds.setGroupID(GCItem, new String(ch, start, length));
					}
					else if(this.inName)
					{
						pds.setName(GCItem, new String(ch, start, length));
					}
					
				}
				
			}
			else if(this.inContacts)
			{
				
				if(this.inContact)
				{
					if(this.inGuid)
					{
						ctt.setGuid(new String(ch, start, length));
					}
					else if(this.inGGNumber)
					{
						ctt.setGGNumber(new String(ch, start,length));
					}
					else if(this.inShowName)
					{
						ctt.setShowName(new String(ch, start, length));
					}
					/*else if(this.inShowName)
					{
						ShowName = new String(ch, start, length);
					}*/
					else if(this.inCGroups)
					{
						if(this.inGroupId)
						{							
							ctt.setGroupId(new String(ch, start, length));
							pds.addContact(ctt);
						}
					}
					else if(this.inAvatars)
					{
						;
					}
					else if(this.inFlagBuddy)
					{
						;
					}
					else if(this.inFlagNormal)
					{
						;
					}
					else if(this.inFlagFriend)
					{
						;
					}
					
				}
			}
		}
	}
	
}
