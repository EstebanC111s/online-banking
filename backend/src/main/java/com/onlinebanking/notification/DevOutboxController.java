package com.onlinebanking.notification;

import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev/outbox")
public class DevOutboxController {

    private final OutboxRepository outbox;
    private final AppProperties props;

    public DevOutboxController(OutboxRepository outbox, AppProperties props) {
        this.outbox = outbox;
        this.props = props;
    }

    @GetMapping
    public List<OutboxMessage> latest() {
        if (!props.devOutbox()) {
            throw new NotFoundException("Recurso");
        }
        return outbox.findTop50ByOrderByIdDesc();
    }
}
