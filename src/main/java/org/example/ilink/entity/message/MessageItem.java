package org.example.ilink.entity.message;

import lombok.Data;

@Data
public class MessageItem {
    private Integer type;
    private Long create_time_ms;
    private Long update_time_ms;
    private Boolean is_completed;
    private TextItem text_item;
}