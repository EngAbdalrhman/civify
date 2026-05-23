package com.app.civify.service.ai.credential;

import com.app.civify.entity.enums.ByokMode;
import lombok.Value;

import java.util.List;

@Value
public class ResolvedCredentialChain {
    List<ResolvedAiCall> calls;
    ByokMode byokMode;

    public boolean isStrict() {
        return byokMode == ByokMode.STRICT;
    }
}
