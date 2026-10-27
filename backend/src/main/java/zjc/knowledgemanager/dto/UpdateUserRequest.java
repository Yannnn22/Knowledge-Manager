package zjc.knowledgemanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * 更新用户请求DTO
 */
public class UpdateUserRequest {
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String realName;
    
    private String department;
    
    @NotNull(message = "状态不能为空")
    private Integer status;

    public UpdateUserRequest() {}

    public UpdateUserRequest(String email, String realName, String department, Integer status) {
        this.email = email;
        this.realName = realName;
        this.department = department;
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
} 