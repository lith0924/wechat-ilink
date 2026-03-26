package org.example.ilink.controller;

import org.example.ilink.service.MessageService;
import org.example.ilink.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value ="/api")
public class MessageController {
    @Autowired
    private MessageService messageService;
    @PostMapping("/message/receive")
    public Result receiveMessage(){
        messageService.receiveMessage();
        return Result.success();
    }
}
