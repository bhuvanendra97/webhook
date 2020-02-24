package com.webhook.events;

import org.springframework.context.ApplicationEvent;

import com.webhook.model.Message;



public class MessageReceivedEvent extends ApplicationEvent {
	
	private static final long serialVersionUID = 5530059221702191330L;
	
	private Message message;

	public MessageReceivedEvent(Object source, Message message) {
		super(source);
		
		System.out.println("MessageReceivedEvent - construtor");
		
		this.message = message;
	}

	public Message getMessage() {
		
		System.out.println("MessageReceivedEvent - getMessage");
		
		return message;
	}
	
}
