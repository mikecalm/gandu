package android.pp;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MessagesAdapter extends BaseAdapter {
	 private LayoutInflater mInflater;
	 public ArrayList<MessageClass> messages;
	 public String myNum = "-1"; 
		
	 public MessagesAdapter(Context context) {
		 mInflater = LayoutInflater.from(context);
		 messages = new ArrayList<MessageClass>();
		 messages.add(new MessageClass(0,"","","Poka¿ starsze wiadomoœci.."));
	 }
	
	 public int getCount() {
		 return messages.size();
	 }
	
	 public Object getItem(int position) {
		 return position;
	 }
	
	 public long getItemId(int position) {
		 return position;
	 }
	
	 public View getView(int position, View convertView, ViewGroup parent) {
		 ViewHolder holder;
		 if (convertView == null) 
		 {
			 convertView = mInflater.inflate(R.layout.message_listview, null);
			 holder = new ViewHolder();
			 holder.sender = (TextView) convertView.findViewById(R.id.MessageUserName);
			 holder.time = (TextView) convertView.findViewById(R.id.MessageDateOverMessage);
			 holder.message = (TextView) convertView.findViewById(R.id.MessageBody);
			
			 convertView.setTag(holder);
		 } 
		 else 
		 {
			 holder = (ViewHolder) convertView.getTag();
		 }
		 //dla elementow nie ze szczytu listy (wiadomosci) ustaw odpowiednie tlo i wypelnij wszystkie pola.		 
		 if(position != 0)
		 {
			 if(messages.get(position).senderNum.equals(myNum))
			 {
				 holder.sender.setText("Ja");
				 holder.sender.setTextColor(Color.GREEN);
				 holder.message.setBackgroundResource(R.drawable.green);
			 }
			 else
			 {
				 holder.sender.setText(messages.get(position).senderName);
				 holder.sender.setTextColor(Color.YELLOW);
				 holder.message.setBackgroundResource(R.drawable.yellow);
			 }
			 
			 holder.time.setText(messages.get(position).incomingTime);
			 holder.sender.setVisibility(View.VISIBLE);
			 holder.time.setVisibility(View.VISIBLE);
			 holder.message.setText(messages.get(position).message);
			 holder.message.setGravity(Gravity.LEFT);
			 Linkify.addLinks(holder.message, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
		 }
		//Element na szczycie listy sluzy do pobrania starszych wiadomosci z archiwum
		 else
		 {
			 holder.message.setBackgroundResource(R.drawable.grey);
			 holder.sender.setVisibility(View.GONE);
			 holder.time.setVisibility(View.GONE);
			 holder.message.setText(messages.get(0).message);
			 holder.message.setGravity(Gravity.CENTER);
		 }				
		
		 return convertView;
	 }
	 
	 static class ViewHolder 
	 {
		 TextView sender;
		 TextView time;
		 TextView message;
	 }
	 
	 public void addItem(MessageClass msg)
	 {		 		 
		 messages.add(msg);
	 }	
	 
	 @SuppressWarnings("unchecked")
	public void addItemsToBeginning(ArrayList<MessageClass> rozmowyZArch)
	 {
		 int wstawianych = rozmowyZArch.size();
		 if(rozmowyZArch != null)
		 {
			 if(wstawianych > 0)
			 {				
				//this.messages.addAll(1, rozmowyZArch);
				rozmowyZArch.addAll(this.messages);
				this.messages = (ArrayList<MessageClass>)rozmowyZArch.clone();
				this.messages.remove(wstawianych);
				this.messages.add(0,new MessageClass(0,"","","Poka¿ starsze wiadomoœci.."));
			 }
		 }
	 }
	 
	 /*public void addItemsToBeginning(ArrayList<MessageClass> rozmowyZArch)
	 {	 
		 if(rozmowyZArch != null)
		 {
			 int wstawianych = rozmowyZArch.size();
			 if(wstawianych > 0)
			 {				
				 this.messages.addAll(1, rozmowyZArch);
			 }
		 }
	 }*/
 }