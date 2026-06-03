package com.metertracking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyPlanCreated() {
        messagingTemplate.convertAndSend("/topic/plans", "PLAN_CREATED");
    }

    public void notifyPlanUpdated() {
        messagingTemplate.convertAndSend("/topic/plans", "PLAN_UPDATED");
    }

    public void notifyPlanDeleted() {
        messagingTemplate.convertAndSend("/topic/plans", "PLAN_DELETED");
    }

    public void notifyTaskCompleted() {
        messagingTemplate.convertAndSend("/topic/tasks", "TASK_COMPLETED");
    }

    public void notifyTaskUpdated() {
        messagingTemplate.convertAndSend("/topic/tasks", "TASK_UPDATED");
    }
    
    public void notifyUserCreated() {
        messagingTemplate.convertAndSend("/topic/users", "USER_CREATED");
    }
    
    public void notifyUserDeleted() {
        messagingTemplate.convertAndSend("/topic/users", "USER_DELETED");
    }
}
