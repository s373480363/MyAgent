package com.myagent.workflow.web.dto;

/**
 * 发布工作流草稿请求。
 */
public final class PublishWorkflowDraftRequest {

    /**
     * 发布说明。
     */
    private String publishMessage;

    public String getPublishMessage() {
        return publishMessage;
    }

    public void setPublishMessage(String publishMessage) {
        this.publishMessage = publishMessage;
    }
}
