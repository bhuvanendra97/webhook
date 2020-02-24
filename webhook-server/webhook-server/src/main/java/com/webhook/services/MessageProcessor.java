package com.webhook.services;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.webhook.events.MessageReceivedEvent;
import com.webhook.model.Destination;
import com.webhook.model.Message;
import com.webhook.persistence.DestinationRepository;
import com.webhook.persistence.MessageRepository;



@Service
public class MessageProcessor {

	private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private DestinationRepository destinationRepository;

	private final RestTemplate restTemplate;

	public MessageProcessor(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();
	}

	/**
	 * Listener para eventos de mensagens recebidas
	 */
	@Async
	@EventListener
	public void messageReceivedListener(MessageReceivedEvent messageReceivedEvent) {
		Message message = messageReceivedEvent.getMessage();

		logger.info("Listening Event for Message {}", message.getId());

		processMessagesForDestination(message.getDestination());
	}

	/**
	 * Agendamento para processamentod as mensagens do banco
	 */
	@Scheduled(cron = "0 0 */6 * * *") // Run at minute 0 past every 6th hour.
	public void scheduledMessagesProcessor() {

		logger.info("Executing scheduled message processor at {}", new Date(System.currentTimeMillis()));

		destinationRepository.findAll().forEach(destination -> processMessagesForDestination(destination));
	}

	private void processMessagesForDestination(Destination destination) {

		logger.info("Processing messages for Destination {}", destination.getUrl());
		
		try {

			destinationRepository.setDestinationOnline(destination.getId());

			List<Message> messages = messageRepository.findAllByDestinationOrderByIdAsc(destination);
			for (Message message : messages) {
				if (message.isMessageTimeout()) {
					deleteMessage(message);
				} else {
					sendMessage(message);
				}
			}

		} catch (MessageProcessorException ex) {
			logger.error("processMessagesForDestination caught an exception: {}", ex.getMessage());
		}
	}

	/**
	 * Envio da mensagem para url cadastrada no destino
	 * @param message
	 * @throws MessageProcessorException
	 */
	private void sendMessage(Message message) throws MessageProcessorException {
		
		logger.info("Sending Message {} to Destination {}", message.getId(), message.getDestinationUrl());
		
		try {
			
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.CONTENT_TYPE, message.getContentType());
			HttpEntity<String> request = new HttpEntity<>(message.getMessageBody(), headers);

			Thread.sleep(500); // espera 0,5 segundos para enviar a mensagem

			ResponseEntity<String> entity = restTemplate.postForEntity(message.getDestinationUrl(), request,
					String.class);

			//verifica se o destino retornou o codigo de sucesso 200
			if (entity.getStatusCode().equals(HttpStatus.OK)) {
				onSendMessageSuccess(message);
			} else {
				throw new MessageProcessorException("Erro nao recebeu o codigo 200!");
			}
		} catch (Exception ex) {
			
			logger.error("sendMessage exception: {}", ex.getMessage());

			onSendMessageError(message);
			throw new MessageProcessorException(ex.getMessage());
		}
	}

	private void onSendMessageSuccess(Message message) {
		logger.info("Sent Message {}", message.getId());

		deleteMessage(message);
	}

	private void onSendMessageError(Message message) {
		logger.debug("Unsent Message {}", message.getId());

		destinationRepository.setDestinationOffline(message.getDestinationId());
	}

	private void deleteMessage(Message message) {
		messageRepository.deleteById(message.getId());

		logger.debug("Deleted Message {}", message.getId());
	}

}
