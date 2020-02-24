package com.webhook.persistence;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.webhook.model.Destination;
import com.webhook.model.Message;



public interface MessageRepository extends CrudRepository<Message, Long> {

	List<Message> findAllByDestinationOrderByIdAsc(Destination destination);

}
