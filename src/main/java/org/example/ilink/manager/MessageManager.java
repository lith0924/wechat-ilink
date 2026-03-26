package org.example.ilink.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageManager {
    private String ilinkUserId;
    private String ilinkBotId;
    private String clientId;
    private String contextToken;
}
