package zjc.knowledgemanager.service.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class NL2SQLService {

    private static final Logger log = LoggerFactory.getLogger(NL2SQLService.class);

    @Autowired
    private ChatClient chatClient;

    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
            "(?i)(DROP\\s+TABLE|DELETE\\s+FROM|UPDATE\\s+\\w+|INSERT\\s+INTO|TRUNCATE\\s+TABLE|ALTER\\s+TABLE|CREATE\\s+TABLE|DROP\\s+INDEX)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "(?i)^\\s*SELECT",
            Pattern.CASE_INSENSITIVE
    );

    private static final String TABLE_SCHEMA = """
            数据库表结构：
            
            1. document（文档表）
               - id: BIGINT, 主键
               - name: VARCHAR(255), 文档名称
               - knowledge_base_id: BIGINT, 所属知识库ID
               - file_path: VARCHAR(500), 文件路径
               - file_size: BIGINT, 文件大小
               - status: VARCHAR(50), 状态（processing/completed/failed）
               - uploader_id: BIGINT, 上传者ID
               - upload_time: DATETIME, 上传时间
               - is_deleted: INT, 是否删除（0/1）
            
            2. knowledge_base（知识库表）
               - id: BIGINT, 主键
               - name: VARCHAR(100), 知识库名称
               - description: VARCHAR(500), 知识库描述
               - owner_id: BIGINT, 所有者ID
               - document_count: INT, 文档数量
               - created_at: DATETIME, 创建时间
               - updated_at: DATETIME, 更新时间
               - is_deleted: INT, 是否删除（0/1）
            
            3. user（用户表）
               - id: BIGINT, 主键
               - username: VARCHAR(50), 用户名
               - real_name: VARCHAR(50), 真实姓名
               - department: VARCHAR(100), 部门
               - email: VARCHAR(100), 邮箱
               - status: INT, 状态（0禁用/1普通用户/2管理员）
               - is_deleted: INT, 是否删除（0/1）
            
            4. conversation（对话表）
               - id: BIGINT, 主键
               - chat_id: VARCHAR(64), 对话ID
               - user_id: BIGINT, 用户ID
               - title: VARCHAR(200), 对话标题
               - created_at: DATETIME, 创建时间
               - updated_at: DATETIME, 更新时间
               - is_deleted: INT, 是否删除（0/1）
            """;

    @Autowired
    private javax.sql.DataSource dataSource;

    public String executeNL2SQL(String userQuery, Long userId, boolean isAdmin) {
        if (!isAdmin) {
            return "抱歉，只有管理员可以使用数据查询功能。";
        }

        if (userQuery == null || userQuery.isBlank()) {
            return "请输入查询内容。";
        }

        try {
            String sql = generateSQL(userQuery);
            
            if (!isValidSQL(sql)) {
                return "抱歉，您的查询包含不安全的操作，无法执行。";
            }

            log.info("NL2SQL生成SQL: {}", sql);
            return executeSQL(sql);
        } catch (Exception e) {
            log.error("NL2SQL执行失败: {}", e.getMessage());
            return "查询执行失败: " + e.getMessage();
        }
    }

    private String generateSQL(String userQuery) {
        String prompt = """
                你是一个SQL生成专家，请根据用户的自然语言输入生成SQL查询语句。
                
                数据库表结构：
                %s
                
                要求：
                1. 只生成SELECT查询语句，禁止生成INSERT、UPDATE、DELETE、DROP等修改语句
                2. 使用适当的WHERE条件过滤数据，避免全表扫描
                3. 只查询is_deleted=0的有效数据
                4. 返回的SQL语句要可直接执行
                5. 如果无法生成有效的SQL，请返回空字符串
                6. 只返回SQL语句，不要包含其他任何内容
                
                用户输入：%s
                """.formatted(TABLE_SCHEMA, userQuery);

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim();

        return cleanSQL(result);
    }

    private String cleanSQL(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        
        int start = sql.indexOf("SELECT");
        int end = sql.lastIndexOf(";");
        
        if (start >= 0) {
            sql = sql.substring(start);
        }
        if (end >= 0) {
            sql = sql.substring(0, end);
        }
        
        return sql.trim();
    }

    private boolean isValidSQL(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        
        if (!SELECT_PATTERN.matcher(sql).find()) {
            log.warn("SQL不是SELECT语句: {}", sql);
            return false;
        }
        
        if (DANGEROUS_SQL_PATTERN.matcher(sql).find()) {
            log.warn("SQL包含危险操作: {}", sql);
            return false;
        }
        
        return true;
    }

    private String executeSQL(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }

            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            return formatResult(columns, rows);
        }
    }

    private String formatResult(List<String> columns, List<List<Object>> rows) {
        if (rows.isEmpty()) {
            return "未找到匹配的数据。";
        }

        StringBuilder sb = new StringBuilder();
        
        sb.append("查询结果:\n");
        sb.append(String.join("\t", columns)).append("\n");
        sb.append("-".repeat(100)).append("\n");

        for (List<Object> row : rows) {
            List<String> rowStrings = new ArrayList<>();
            for (Object obj : row) {
                rowStrings.add(obj != null ? obj.toString() : "NULL");
            }
            sb.append(String.join("\t", rowStrings)).append("\n");
        }

        sb.append("\n共 ").append(rows.size()).append(" 条记录");
        return sb.toString();
    }
}