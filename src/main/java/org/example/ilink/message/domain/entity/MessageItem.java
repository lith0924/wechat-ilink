package org.example.ilink.message.domain.entity;

import lombok.Data;

@Data
public class MessageItem {
    private Integer type;
    private Long create_time_ms;
    private Long update_time_ms;
    private Boolean is_completed;
    private TextItem text_item;
}
