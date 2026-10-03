package org.example.QueueAdaptor.RMQ.tut1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rmq")
public class RmqController {

    private final RmqProducer rmqProducer;

    public RmqController(RmqProducer rmqProducer) {
        this.rmqProducer = rmqProducer;
    }

    // GET /rmq/send?message=YourMessage
    @GetMapping("/send")
    public String sendMessage(@RequestParam(value = "message", defaultValue = "Hello from Pongpin RMQ!") String message) {
        rmqProducer.sendMessage(message);
        return "Message published to RabbitMQ successfully: " + message;
    }
}
