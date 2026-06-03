package com.org.ai.intent;

import lombok.Data;

@Data
public class ChatIntent {
    private IntentType intent;
    private String username;
    private String date;        // yyyy-MM-dd
    private String deploymentId;
}
