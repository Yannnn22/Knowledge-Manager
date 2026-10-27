package zjc.knowledgemanager.dto;

/**
 * 文档状态响应DTO
 */
public class DocumentStatusResponse {
    
    private Integer status;
    private Integer progress;
    private String currentStep;

    public DocumentStatusResponse() {}

    public DocumentStatusResponse(Integer status, Integer progress, String currentStep) {
        this.status = status;
        this.progress = progress;
        this.currentStep = currentStep;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
} 