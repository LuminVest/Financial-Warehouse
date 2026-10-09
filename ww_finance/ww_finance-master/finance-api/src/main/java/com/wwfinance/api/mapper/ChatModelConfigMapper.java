package com.wwfinance.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wwfinance.api.entity.ChatModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatModelConfigMapper extends BaseMapper<ChatModelConfig> {

    @Update("UPDATE chat_model_config SET is_default = 0 WHERE is_deleted = 0")
    int clearDefault();

    @Update("UPDATE chat_model_config SET is_default = 1 WHERE id = #{id} AND is_deleted = 0")
    int setDefault(Long id);
}
