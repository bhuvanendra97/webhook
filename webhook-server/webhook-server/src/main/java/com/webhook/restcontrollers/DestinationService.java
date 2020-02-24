package com.webhook.restcontrollers;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.webhook.events.MessageReceivedEvent;
import com.webhook.model.Destination;
import com.webhook.model.Message;
import com.webhook.persistence.DestinationRepository;
import com.webhook.persistence.MessageRepository;



@RestController
public class DestinationService implements ApplicationEventPublisherAware {

	private static final Logger logger = LoggerFactory.getLogger(DestinationService.class);

	@Autowired
	private DestinationRepository destinationRepository;

	@Autowired
	private MessageRepository messageRepository;

	// Event publisher
	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Registrar uma url para envio da notificacao
	 * (http://localhost:9090/servico/xpto)
	 * 
	 * @param url
	 * @return
	 */
	@PostMapping("/destinations")
	public Long registerNewDestination(@RequestParam("url") String url) {
		validateParam(url, "url");

		Destination destination = destinationRepository.save(new Destination(url));

		logger.debug("Received Destination {%s}", url);

		return destination.getId();
	}

	/**
	 * Lista as urls destinos cadastradas
	 * 
	 * @return
	 */
	@GetMapping("/destinations")
	public Iterable<Destination> listDestinations() {
		logger.debug("Listing Destinations");

		return destinationRepository.findAll();
	}

	/**
	 * Apagar uma url cadastrada
	 * 
	 * @param id
	 */
	@DeleteMapping("/destinations/{id}")
	public void deleteDestination(@PathVariable("id") Long id) {
		Destination destination = getDestination(id);

		destinationRepository.deleteById(id);

		logger.debug("Deleted Destination {}", destination.getUrl());
	}

	/**
	 * Grava e notifica um destinatario da mensagem
	 * 
	 * @param id
	 * @param body
	 * @param contentType
	 */
	@PostMapping("/destinations/{id}/message")
	public void postMessageToDestination(@PathVariable("id") Long id, @RequestBody String body,
			@RequestHeader("Content-Type") String contentType) {
		validateParam(body, "body");

		Destination destination = getDestination(id);

		Message message = messageRepository.save(new Message(body, contentType, destination));

		logger.info("Received Message {} for Destination {}", message.getId(), message.getDestinationUrl());

		// Notifica destinatario
		applicationEventPublisher.publishEvent(new MessageReceivedEvent(this, message));
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	private Destination getDestination(Long id) throws NoSuchElementException {

		Destination destination = destinationRepository.findOne(id);
		
		if (destination == null) {
			throw new NoSuchElementException("Destinatario nao encontrado " + id);
		}

		return destination;
	}

	/**
	 * valida se tem conteudo
	 * 
	 * @param param
	 * @param paramName
	 * @throws IllegalArgumentException
	 */
	private void validateParam(String param, String paramName) throws IllegalArgumentException {

		if (param == null || param.isEmpty()) {
			throw new IllegalArgumentException("O '" + paramName + "' nao pode ser vazio");
		}
	}

}
