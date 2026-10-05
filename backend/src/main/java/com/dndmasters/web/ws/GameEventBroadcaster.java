package com.dndmasters.web.ws;

import com.dndmasters.service.CampaignEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Broadcasts recorded game events to the campaign's STOMP topic. */
@Component
public class GameEventBroadcaster {

    private final SimpMessagingTemplate messaging;

    public GameEventBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCampaignEvent(CampaignEvent event) {
        messaging.convertAndSend("/topic/campaign/" + event.campaignId(), event.event());
    }
}
