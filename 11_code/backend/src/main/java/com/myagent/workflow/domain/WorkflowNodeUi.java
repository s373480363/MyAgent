package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 节点画布展示属性。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowNodeUi", description = "节点画布展示属性。")
public final class WorkflowNodeUi {

    /**
     * 画布坐标。
     */
    @Schema(description = "画布坐标。")
    private WorkflowNodeUiPosition position;

    /**
     * 宽度。
     */
    @Schema(description = "节点宽度。", example = "280")
    private Integer width;

    /**
     * 高度。
     */
    @Schema(description = "节点高度。", example = "160")
    private Integer height;

    public WorkflowNodeUiPosition getPosition() {
        return position;
    }

    public void setPosition(WorkflowNodeUiPosition position) {
        this.position = position;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
