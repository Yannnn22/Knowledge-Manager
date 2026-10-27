package zjc.knowledgemanager.dto;

public class AssistantStatsDTO {

    private Integer todayChatCount;
    private Integer todayMessageCount;
    private Integer totalChatCount;
    private Integer monthChatCount;
    private Integer accuracyRate;
    private Integer satisfactionRate;
    private Integer taskCompleted;
    private Integer taskInProgress;

    public AssistantStatsDTO() {}

    public Integer getTodayChatCount() {
        return todayChatCount;
    }

    public void setTodayChatCount(Integer todayChatCount) {
        this.todayChatCount = todayChatCount;
    }

    public Integer getTodayMessageCount() {
        return todayMessageCount;
    }

    public void setTodayMessageCount(Integer todayMessageCount) {
        this.todayMessageCount = todayMessageCount;
    }

    public Integer getTotalChatCount() {
        return totalChatCount;
    }

    public void setTotalChatCount(Integer totalChatCount) {
        this.totalChatCount = totalChatCount;
    }

    public Integer getMonthChatCount() {
        return monthChatCount;
    }

    public void setMonthChatCount(Integer monthChatCount) {
        this.monthChatCount = monthChatCount;
    }

    public Integer getAccuracyRate() {
        return accuracyRate;
    }

    public void setAccuracyRate(Integer accuracyRate) {
        this.accuracyRate = accuracyRate;
    }

    public Integer getSatisfactionRate() {
        return satisfactionRate;
    }

    public void setSatisfactionRate(Integer satisfactionRate) {
        this.satisfactionRate = satisfactionRate;
    }

    public Integer getTaskCompleted() {
        return taskCompleted;
    }

    public void setTaskCompleted(Integer taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    public Integer getTaskInProgress() {
        return taskInProgress;
    }

    public void setTaskInProgress(Integer taskInProgress) {
        this.taskInProgress = taskInProgress;
    }
}