package org.example.ilink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ilink.entity.chat.ChatRecord;

/**
 * 聊天记录 Mapper
 */
@Mapper
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {
}
