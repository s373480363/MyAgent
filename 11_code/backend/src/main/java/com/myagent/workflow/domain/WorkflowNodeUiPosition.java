package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 节点画布坐标。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowNodeUiPosition", description = "节点画布坐标。")
public final class WorkflowNodeUiPosition {

    /**
     * X 坐标。
     */
    @Schema(description = "X 坐标。", example = "0")
    private Integer x;

    /**
     * Y 坐标。
     */
    @Schema(description = "Y 坐标。", example = "0")
    private Integer y;

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
