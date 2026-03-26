package org.example.ilink.entity.message;

import lombok.Data;

import java.util.List;

@Data
public class Message {
    private Integer seq;
    private Long message_id;
    private String from_user_id;
    private String to_user_id;
    private String client_id;
    private Long create_time_ms;
    private Long update_time_ms;
    private Long delete_time_ms;
    private String session_id;
    private String group_id;
    private Integer message_type;
    private Integer message_state;
    private List<MessageItem> item_list;
    private String context_token;
}