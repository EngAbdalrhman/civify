package com.app.civify.entity.logs;

import com.app.civify.dto.token.TokenUsageDto;
import com.app.civify.mapper.CommonMapper;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TokenUsageLogMapper extends CommonMapper<TokenUsageDto, TokenUsageLog> {
    }