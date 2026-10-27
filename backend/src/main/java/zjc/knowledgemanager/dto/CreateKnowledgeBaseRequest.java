package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建知识库请求DTO
 */
public class CreateKnowledgeBaseRequest {
    
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
    private String name;
    
    private String description;
    
    @NotBlank(message = "部门不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50个字符")
    private String department;

    public CreateKnowledgeBaseRequest() {}

    public CreateKnowledgeBaseRequest(String name, String description, String department) {
        this.name = name;
        this.description = description;
        this.department = department;
    }

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