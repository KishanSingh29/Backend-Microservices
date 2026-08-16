package userservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import userservice.entities.UserInfoDto;
import userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceConsumer
{

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "${spring.kafka.topic-json.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(UserInfoDto eventData) {
        System.out.println("Received: " + eventData);
        try{
            // Todo: Make it transactional, to handle idempotency and validate email, phoneNumber etc
            System.out.println("Saving user: " + eventData.getUserId());
            userService.createOrUpdateUser(eventData);
        }catch(Exception ex){
            ex.printStackTrace();
            System.out.println("Error: " + ex.getMessage());
            System.out.println("AuthServiceConsumer: Exception is thrown while consuming kafka event");
        }
    }

}