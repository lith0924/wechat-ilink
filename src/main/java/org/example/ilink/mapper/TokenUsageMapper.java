package org.example.ilink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.ilink.entity.chat.TokenUsage;

/**
 * Token 消耗 Mapper
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsage> {
}
