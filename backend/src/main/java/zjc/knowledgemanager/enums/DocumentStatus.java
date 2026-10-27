package zjc.knowledgemanager.enums;

public enum DocumentStatus {
    
    UPLOADING(0, "uploading", "上传中"),
    PROCESSING(1, "processing", "处理中"),
    COMPLETED(2, "completed", "完成"),
    FAILED(3, "failed", "失败"),
    ARCHIVED(4, "archived", "已归档");

    private final int code;
    private final String value;
    private final String description;

    DocumentStatus(int code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static DocumentStatus fromCode(int code) {
        for (DocumentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PROCESSING;
    }

    public static DocumentStatus fromValue(String value) {
        if (value == null) {
            return PROCESSING;
        }
        for (DocumentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return PROCESSING;
    }
}