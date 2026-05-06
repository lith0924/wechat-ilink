package org.example.ilink.chat.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ilink.chat.domain.entity.ChatReply;

@Mapper
public interface ChatReplyMapper extends BaseMapper<ChatReply> {
}
