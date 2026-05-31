package com.myagent.settings.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量更新系统设置请求。
 */
public final class UpdateSettingsRequest {

    /**
     * 待更新设置项。
     */
    @Valid
    @NotEmpty(message = "items 不能为空。")
    private List<UpdateSettingsItemRequest> items;

    /**
     * 返回待更新设置项。
     *
     * @return 待更新设置项
     */
    public List<UpdateSettingsItemRequest> getItems() {
        return items;
    }

    /**
     * 设置待更新设置项。
     *
     * @param items 待更新设置项
     */
    public void setItems(List<UpdateSettingsItemRequest> items) {
        this.items = items;
    }
}
