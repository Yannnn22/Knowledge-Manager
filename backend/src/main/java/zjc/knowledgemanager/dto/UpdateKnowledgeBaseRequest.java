package zjc.knowledgemanager.dto;

/**
 * 更新知识库请求 DTO
 */
public class UpdateKnowledgeBaseRequest {

    private String name;
    private String description;
    private String department;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}


