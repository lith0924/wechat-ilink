package org.example.ilink.entity.message;

import lombok.Data;

import java.util.List;

@Data
public class MessageResponse {
    private List<Message> msgs;
    private String sync_buf;
    private String get_updates_buf;
}