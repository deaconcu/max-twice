package com.twicemax.web.v2.handler;

import com.twicemax.shared.domain.exception.StatusCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防漏配测试：每个新增 StatusCode 必须在 {@link StatusCodeHttpMapper} 里显式声明。
 *
 * <p>OK 是唯一例外（成功路径不走异常处理器，无需映射）。
 */
class StatusCodeHttpMapperTest {

    @Test
    void everyStatusCodeMustHaveExplicitHttpMapping() {
        List<StatusCode> missing = new ArrayList<>();
        for (StatusCode code : StatusCode.values()) {
            if (code == StatusCode.OK) continue;
            if (!StatusCodeHttpMapper.hasMapping(code)) {
                missing.add(code);
            }
        }
        assertThat(missing)
                .as("以下 StatusCode 未在 StatusCodeHttpMapper 中配置 HTTP 映射，请补充：%s", missing)
                .isEmpty();
    }
}
