package zjc.knowledgemanager.service.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import zjc.knowledgemanager.dto.*;
import zjc.knowledgemanager.entity.KnowledgeBase;
import zjc.knowledgemanager.entity.User;
import zjc.knowledgemanager.mapper.KnowledgeBaseMapper;
import zjc.knowledgemanager.mapper.UserMapper;
import zjc.knowledgemanager.service.DocumentService;
import zjc.knowledgemanager.service.KnowledgeBaseService;
import zjc.knowledgemanager.service.sql.NL2SQLService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class DocumentTool {

    private static final Logger log = LoggerFactory.getLogger(DocumentTool.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NL2SQLService nl2SQLService;

    private Long currentUserId = 1L;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId != null ? userId : 1L;
    }

    private boolean isAdmin() {
        User user = userMapper.findById(currentUserId);
        return user != null && user.isAdmin();
    }

    private boolean isNormalUser() {
        User user = userMapper.findById(currentUserId);
        return user != null && user.isNormalUser();
    }

    private String checkAdminPermission() {
        if (!isAdmin()) {
            return "抱歉，此操作需要管理员权限。";
        }
        return null;
    }

    @Tool(description = "创建新文档")
    public String create_document(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "文档内容") String content,
            @ToolParam(description = "文档描述") String description) {
        String permission = checkAdminPermission();
        if (permission != null) {
            return permission;
        }

        try {
            KnowledgeBase kb = knowledgeBaseMapper.findById(1L);
            if (kb == null) {
                kb = new KnowledgeBase();
                kb.setId(1L);
                kb.setName("默认知识库");
                knowledgeBaseMapper.insert(kb);
            }

            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            MultipartFile mockFile = createMockFile(title + ".txt", "text/plain", contentBytes);

            DocumentUploadResponse response = documentService.uploadDocument(currentUserId, kb.getId(), mockFile, description);
            return "文档创建成功！文档ID: " + response.getDocumentId() + ", 标题: " + title;
        } catch (Exception e) {
            log.error("创建文档失败", e);
            return "创建文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "编辑文档内容")
    public String edit_document(
            @ToolParam(description = "文档ID") String document_id,
            @ToolParam(description = "新的文档内容") String new_content,
            @ToolParam(description = "新的文档描述") String new_description) {
        String permission = checkAdminPermission();
        if (permission != null) {
            return permission;
        }

        try {
            if (document_id == null || document_id.trim().isEmpty()) {
                return "编辑文档需要提供文档ID";
            }
            Long docId = Long.parseLong(document_id.trim());
            boolean success = documentService.editDocument(docId, new_content, new_description, currentUserId);
            if (success) {
                return "文档编辑成功！文档ID: " + docId;
            } else {
                return "文档编辑失败，请检查文档ID是否正确";
            }
        } catch (NumberFormatException e) {
            return "文档ID格式不正确，应为数字";
        } catch (Exception e) {
            log.error("编辑文档失败", e);
            return "编辑文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取文档内容")
    public String get_document(
            @ToolParam(description = "文档ID") String document_id) {
        try {
            if (document_id == null || document_id.trim().isEmpty()) {
                return "获取文档需要提供文档ID";
            }
            Long docId = Long.parseLong(document_id.trim());
            DocumentPreviewResponse response = documentService.previewDocument(docId, currentUserId);
            if (response != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("文档标题: ").append(response.getOriginalFilename()).append("\n");
                sb.append("文档描述: ").append(response.getDescription()).append("\n");
                sb.append("文档内容:\n").append(response.getContent());
                return sb.toString();
            }
            return "未找到指定文档";
        } catch (NumberFormatException e) {
            return "文档ID格式不正确，应为数字";
        } catch (Exception e) {
            log.error("获取文档失败", e);
            return "获取文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "搜索文档列表")
    public String search_documents(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "返回数量") Integer top_k) {
        try {
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setQuery(query);
            request.setKnowledgeBaseIds(Collections.singletonList(1L));
            DocumentSearchResponse response = documentService.searchDocuments(currentUserId, request);

            List<DocumentSearchResponse.SearchResult> results = response.getResults();
            if (results != null && !results.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (DocumentSearchResponse.SearchResult result : results) {
                    if (count >= (top_k != null ? top_k : 5)) break;
                    sb.append("文档ID:").append(result.getDocumentId())
                      .append("|文档名:").append(result.getTitle())
                      .append("|所属知识库:").append(result.getKnowledgeBaseName())
                      .append("|匹配度:").append(String.format("%.2f", result.getScore()))
                      .append("\n");
                    count++;
                }
                return sb.toString();
            }
            return "未找到匹配的文档";
        } catch (Exception e) {
            log.error("搜索文档失败", e);
            return "搜索文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "搜索文档内容")
    public String search_document_content(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "返回数量") Integer top_k) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(top_k != null ? top_k : 5)
                            .build());

            if (!results.isEmpty()) {
                StringBuilder sb = new StringBuilder("文档内容搜索结果:\n");
                for (int i = 0; i < results.size(); i++) {
                    Document doc = results.get(i);
                    sb.append(i + 1).append(". 相关内容:\n")
                      .append(doc.getText().substring(0, Math.min(200, doc.getText().length())))
                      .append("...\n");
                }
                return sb.toString();
            }
            return "未找到相关文档内容";
        } catch (Exception e) {
            log.error("搜索文档内容失败", e);
            return "搜索文档内容失败: " + e.getMessage();
        }
    }

    @Tool(description = "列出所有文档")
    public String list_documents(
            @ToolParam(description = "页码") Integer page,
            @ToolParam(description = "每页数量") Integer size) {
        try {
            DocumentPageResponse response = documentService.getDocumentsPageWithFilter(
                    currentUserId, null, null,
                    page != null ? page : 1,
                    size != null ? size : 10,
                    null, null);

            List<DocumentPageResponse.Item> documents = response.getDocuments();
            if (documents != null && !documents.isEmpty()) {
                StringBuilder sb = new StringBuilder("文档列表 (共").append(response.getTotal()).append("个):\n");
                for (DocumentPageResponse.Item doc : documents) {
                    sb.append("- ID: ").append(doc.getId())
                      .append(", 标题: ").append(doc.getOriginalFilename())
                      .append(", 状态: ").append(doc.getStatus())
                      .append(", 上传时间: ").append(doc.getUploadTime()).append("\n");
                }
                return sb.toString();
            }
            return "暂无文档";
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return "获取文档列表失败: " + e.getMessage();
        }
    }

    @Tool(description = "删除文档")
    public String delete_document(
            @ToolParam(description = "文档ID") Long document_id) {
        String permission = checkAdminPermission();
        if (permission != null) {
            return permission;
        }

        try {
            boolean success = documentService.deleteDocument(currentUserId, document_id);
            if (success) {
                return "文档删除成功！文档ID: " + document_id;
            } else {
                return "文档删除失败，请检查文档ID是否正确";
            }
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return "删除文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "创建PDF文档")
    public String create_pdf_document(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "文档内容") String content,
            @ToolParam(description = "文档描述") String description) {
        String permission = checkAdminPermission();
        if (permission != null) {
            return permission;
        }

        try {
            KnowledgeBase kb = knowledgeBaseMapper.findById(1L);
            if (kb == null) {
                kb = new KnowledgeBase();
                kb.setId(1L);
                kb.setName("默认知识库");
                knowledgeBaseMapper.insert(kb);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                PDFont regularFont = PDType1Font.HELVETICA;
                PDFont boldFont = PDType1Font.HELVETICA_BOLD;

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                contentStream.setFont(boldFont, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(title);
                contentStream.endText();
                contentStream.close();

                String[] lines = content.split("\n");
                float y = 720;
                for (String line : lines) {
                    if (y < 50) {
                        page = new PDPage();
                        document.addPage(page);
                        y = 750;
                    }
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(regularFont, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, y);
                    contentStream.showText(line);
                    contentStream.endText();
                    contentStream.close();
                    y -= 20;
                }

                document.save(baos);
            }

            byte[] pdfBytes = baos.toByteArray();
            MultipartFile mockFile = createMockFile(title + ".pdf", "application/pdf", pdfBytes);

            DocumentUploadResponse response = documentService.uploadDocument(currentUserId, kb.getId(), mockFile, description);
            return "PDF文档创建成功！文档ID: " + response.getDocumentId() + ", 标题: " + title;
        } catch (Exception e) {
            log.error("创建PDF文档失败", e);
            return "创建PDF文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "创建Word文档")
    public String create_word_document(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "文档内容") String content,
            @ToolParam(description = "文档描述") String description) {
        String permission = checkAdminPermission();
        if (permission != null) {
            return permission;
        }

        try {
            KnowledgeBase kb = knowledgeBaseMapper.findById(1L);
            if (kb == null) {
                kb = new KnowledgeBase();
                kb.setId(1L);
                kb.setName("默认知识库");
                knowledgeBaseMapper.insert(kb);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (XWPFDocument document = new XWPFDocument()) {
                XWPFParagraph titleParagraph = document.createParagraph();
                titleParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun titleRun = titleParagraph.createRun();
                titleRun.setText(title);
                titleRun.setBold(true);
                titleRun.setFontSize(16);
                titleRun.addBreak();

                String[] paragraphs = content.split("\n\n");
                for (String paraText : paragraphs) {
                    XWPFParagraph paragraph = document.createParagraph();
                    paragraph.setFirstLineIndent(400);
                    XWPFRun run = paragraph.createRun();
                    run.setText(paraText.trim());
                    run.setFontSize(12);
                }

                document.write(baos);
            }

            byte[] docxBytes = baos.toByteArray();
            MultipartFile mockFile = createMockFile(title + ".docx", 
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);

            DocumentUploadResponse response = documentService.uploadDocument(currentUserId, kb.getId(), mockFile, description);
            return "Word文档创建成功！文档ID: " + response.getDocumentId() + ", 标题: " + title;
        } catch (Exception e) {
            log.error("创建Word文档失败", e);
            return "创建Word文档失败: " + e.getMessage();
        }
    }

    @Tool(description = "生成文档摘要")
    public String summarize_document(
            @ToolParam(description = "文档ID") String document_id,
            @ToolParam(description = "摘要长度（字数）") Integer summary_length) {
        try {
            if (document_id == null || document_id.trim().isEmpty()) {
                return "生成文档摘要需要提供文档ID";
            }
            Long docId = Long.parseLong(document_id.trim());
            DocumentPreviewResponse response = documentService.previewDocument(docId, currentUserId);
            if (response == null) {
                return "未找到指定文档";
            }

            String content = response.getContent();
            int targetLength = summary_length != null ? summary_length : 200;

            String summary = chatClient.prompt()
                    .user("请用" + targetLength + "字以内的中文为以下文档生成摘要：\n\n" + content)
                    .call()
                    .content();

            return "文档摘要：\n" + summary;
        } catch (NumberFormatException e) {
            return "文档ID格式不正确，应为数字";
        } catch (Exception e) {
            log.error("生成文档摘要失败", e);
            return "生成文档摘要失败: " + e.getMessage();
        }
    }

    @Tool(description = "知识库管理，支持创建、列出、删除操作")
    public String manage_knowledge_base(
            @ToolParam(description = "操作类型：create（创建）、list（列出）、delete（删除）") String operation,
            @ToolParam(description = "知识库名称（仅create操作需要）") String name,
            @ToolParam(description = "知识库描述（仅create操作需要）") String description,
            @ToolParam(description = "知识库ID（仅delete操作需要）") String knowledge_base_id) {
        try {
            switch (operation.toLowerCase()) {
                case "create":
                    String createPermission = checkAdminPermission();
                    if (createPermission != null) {
                        return createPermission;
                    }
                    if (name == null || name.isEmpty()) {
                        return "创建知识库需要提供知识库名称";
                    }
                    CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest();
                    request.setName(name);
                    request.setDescription(description);
                    Long kbId = knowledgeBaseService.createKnowledgeBase(currentUserId, request);
                    return "知识库创建成功！ID: " + kbId + ", 名称: " + name;

                case "list":
                    KnowledgeBaseListResponse listResponse = knowledgeBaseService.getKnowledgeBaseList(currentUserId, null, false);
                    List<KnowledgeBaseListResponse.KnowledgeBaseInfo> kbs = listResponse.getKnowledgeBases();
                    if (kbs != null && !kbs.isEmpty()) {
                        StringBuilder sb = new StringBuilder("知识库列表 (共").append(kbs.size()).append("个):\n");
                        for (KnowledgeBaseListResponse.KnowledgeBaseInfo kb : kbs) {
                            sb.append("- ID: ").append(kb.getId())
                              .append(", 名称: ").append(kb.getName())
                              .append(", 文档数: ").append(kb.getDocumentCount())
                              .append(", 创建时间: ").append(kb.getCreatedAt()).append("\n");
                        }
                        return sb.toString();
                    }
                    return "暂无知识库";

                case "delete":
                    String deletePermission = checkAdminPermission();
                    if (deletePermission != null) {
                        return deletePermission;
                    }
                    if (knowledge_base_id == null || knowledge_base_id.trim().isEmpty()) {
                        return "删除知识库需要提供知识库ID";
                    }
                    try {
                        Long kbDeleteId = Long.parseLong(knowledge_base_id.trim());
                        knowledgeBaseService.deleteKnowledgeBase(currentUserId, kbDeleteId);
                        return "知识库删除成功！ID: " + kbDeleteId;
                    } catch (NumberFormatException e) {
                        return "知识库ID格式不正确，应为数字";
                    }

                default:
                    return "不支持的操作类型，请使用 create、list 或 delete";
            }
        } catch (Exception e) {
            log.error("知识库管理操作失败", e);
            return "知识库管理操作失败: " + e.getMessage();
        }
    }

    @Tool(description = "在指定知识库中搜索文档")
    public String search_in_knowledge_base(
            @ToolParam(description = "知识库ID") Long knowledge_base_id,
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "返回数量") Integer top_k) {
        try {
            KnowledgeBase kb = knowledgeBaseMapper.findById(knowledge_base_id);
            if (kb == null) {
                return "未找到指定的知识库";
            }

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setQuery(query);
            request.setKnowledgeBaseIds(Collections.singletonList(knowledge_base_id));
            DocumentSearchResponse response = documentService.searchDocuments(currentUserId, request);

            List<DocumentSearchResponse.SearchResult> results = response.getResults();
            if (results != null && !results.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (DocumentSearchResponse.SearchResult result : results) {
                    if (count >= (top_k != null ? top_k : 5)) break;
                    sb.append("文档ID:").append(result.getDocumentId())
                      .append("|文档名:").append(result.getTitle())
                      .append("|所属知识库:").append(result.getKnowledgeBaseName())
                      .append("|匹配度:").append(String.format("%.2f", result.getScore()))
                      .append("\n");
                    count++;
                }
                return sb.toString();
            }
            return "在知识库「" + kb.getName() + "」中未找到匹配的文档";
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return "知识库搜索失败: " + e.getMessage();
        }
    }

    @Tool(description = "数据查询，支持自然语言查询数据库（仅管理员可用）")
    public String query_data(
            @ToolParam(description = "查询内容") String query) {
        try {
            return nl2SQLService.executeNL2SQL(query, currentUserId, isAdmin());
        } catch (Exception e) {
            log.error("数据查询失败", e);
            return "数据查询失败: " + e.getMessage();
        }
    }

    private MultipartFile createMockFile(String filename, String contentType, byte[] content) {
        return new MultipartFile() {
            @Override
            public String getName() { return "file"; }
            @Override
            public String getOriginalFilename() { return filename; }
            @Override
            public String getContentType() { return contentType; }
            @Override
            public boolean isEmpty() { return content.length == 0; }
            @Override
            public long getSize() { return content.length; }
            @Override
            public byte[] getBytes() { return content; }
            @Override
            public InputStream getInputStream() { return new ByteArrayInputStream(content); }
            @Override
            public void transferTo(java.io.File dest) throws IOException {
                java.nio.file.Files.write(dest.toPath(), content);
            }
        };
    }
}