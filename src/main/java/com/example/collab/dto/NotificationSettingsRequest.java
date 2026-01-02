package com.example.collab.dto;

public class NotificationSettingsRequest {
    private Boolean editEnabled;
    private Boolean commentEnabled;
    private Boolean taskEnabled;
    private Boolean shareEnabled;

    public Boolean getEditEnabled() {
        return editEnabled;
    }

    public void setEditEnabled(Boolean editEnabled) {
        this.editEnabled = editEnabled;
    }

    public Boolean getCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(Boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public Boolean getTaskEnabled() {
        return taskEnabled;
    }

    public void setTaskEnabled(Boolean taskEnabled) {
        this.taskEnabled = taskEnabled;
    }

    public Boolean getShareEnabled() {
        return shareEnabled;
    }

    public void setShareEnabled(Boolean shareEnabled) {
        this.shareEnabled = shareEnabled;
    }
}
