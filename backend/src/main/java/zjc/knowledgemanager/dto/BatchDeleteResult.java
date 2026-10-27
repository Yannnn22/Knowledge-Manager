package zjc.knowledgemanager.dto;

import java.util.List;

public class BatchDeleteResult {
    
    private List<Long> successIds;
    private List<String> failed;

    public BatchDeleteResult() {}

    public BatchDeleteResult(List<Long> successIds, List<String> failed) {
        this.successIds = successIds;
        this.failed = failed;
    }

    public List<Long> getSuccessIds() {
        return successIds;
    }

    public void setSuccessIds(List<Long> successIds) {
        this.successIds = successIds;
    }

    public List<String> getFailed() {
        return failed;
    }

    public void setFailed(List<String> failed) {
        this.failed = failed;
    }
}