/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zjc.knowledgemanager.controller.ai;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import zjc.knowledgemanager.config.ai.MilvusAvailableCondition;
import zjc.knowledgemanager.service.chunk.ChunkStrategyFactory;
import zjc.knowledgemanager.service.retrieval.HybridRetriever;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件上传和向量存储管理控制器
 * 支持多种文件格式的上传、向量化存储、检索和删除操作
 */
@RestController
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true", matchIfMissing = true)
@Conditional(MilvusAvailableCondition.class)
@RequestMapping("/ingest")
@Tag(name = "文件上传与向量存储", description = "文件上传、向量化存储、检索和删除相关接口")
public class FileIngestController {

    private static final Logger logger = LoggerFactory.getLogger(FileIngestController.class);

    private final VectorStore vectorStore;
    private final ChunkStrategyFactory chunkStrategyFactory;
    private final HybridRetriever hybridRetriever;

    private static final int EMBEDDING_BATCH_LIMIT = 5;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    public FileIngestController(VectorStore vectorStore, 
                                ChunkStrategyFactory chunkStrategyFactory,
                                HybridRetriever hybridRetriever) {
        this.vectorStore = vectorStore;
        this.chunkStrategyFactory = chunkStrategyFactory;
        this.hybridRetriever = hybridRetriever;
    }

    /**
     * 上传文件并入库：PDF 用分页 Reader，其它用 Tika；统一 Token 切分，写入向量库。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "上传文件并向量化存储",
        description = "支持PDF、Word、Excel、TXT等多种文件格式，自动进行文本提取、分块和向量化存储"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "文件上传成功",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "successfully inserted 10 text fragments into vector store, fileId: abc123-def456"))
        ),
        @ApiResponse(responseCode = "400", description = "文件为空或格式不支持"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<String> upload(
        @Parameter(description = "要上传的文件", required = true)
        @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("必须上传非空的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body("文件大小超过限制，最大支持10MB");
        }

        try {
            List<Document> docs;

            String filename = file.getOriginalFilename();
            logger.info("开始上传文件: {}, 大小: {} bytes", filename, file.getSize());
            
            if (StringUtils.hasText(filename) && filename.toLowerCase().endsWith(".pdf")) {
                docs = new PagePdfDocumentReader(file.getResource()).get();
            } else {
                docs = new TikaDocumentReader(file.getResource()).get();
            }
            
            logger.info("文件解析完成，共 {} 个文档", docs.size());

            List<Document> splitDocs = chunkStrategyFactory.smartChunk(docs);
            
            logger.info("智能分块完成，共 {} 个片段", splitDocs.size());

            if (splitDocs.size() > 100) {
                return ResponseEntity.badRequest().body("文件内容过多，分块后超过100个片段，请尝试上传更小的文件或精简内容");
            }

            String fileId = UUID.randomUUID().toString();
            
            for (Document doc : splitDocs) {
                doc.getMetadata().put("fileId", fileId);
                doc.getMetadata().put("originalFilename", filename);
            }

            int total = splitDocs.size();
            int successCount = 0;
            int failCount = 0;
            
            for (int i = 0; i < total; i += EMBEDDING_BATCH_LIMIT) {
                int end = Math.min(i + EMBEDDING_BATCH_LIMIT, total);
                List<Document> batch = splitDocs.subList(i, end);
                try {
                    hybridRetriever.addDocuments(batch);
                    successCount += batch.size();
                    logger.info("ingested batch {} ({} - {}) for fileId={}", (i / EMBEDDING_BATCH_LIMIT) + 1, i, end - 1, fileId);
                } catch (Exception e) {
                    failCount += batch.size();
                    logger.warn("batch {} failed: {}", (i / EMBEDDING_BATCH_LIMIT) + 1, e.getMessage());
                }
                System.gc();
            }

            String msg = String.format("successfully inserted %d text fragments into vector store, fileId: %s",
                    successCount, fileId);
            if (failCount > 0) {
                msg += String.format(" (failed %d fragments)", failCount);
            }
            return ResponseEntity.ok(msg);
        } catch (OutOfMemoryError e) {
            logger.error("文件上传失败：内存不足", e);
            return ResponseEntity.internalServerError().body("服务器内存不足，请尝试上传更小的文件");
        } catch (Exception e) {
            logger.error("file upload failed", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("20015")) {
                return ResponseEntity.badRequest().body("文件内容解析失败：内容可能包含特殊字符或过长，请尝试上传更小的文件");
            }
            return ResponseEntity.internalServerError().body("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 基于 fileId 过滤检索。
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "向量相似度搜索",
        description = "根据查询文本在指定文件中进行向量相似度搜索，返回最相关的文档片段"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "搜索成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Document.class))
        ),
        @ApiResponse(responseCode = "400", description = "查询参数无效")
    })
    public ResponseEntity<List<Document>> search(
        @Parameter(description = "搜索查询文本", required = true, example = "Spring AI")
        @RequestParam("q") String query,
        @Parameter(description = "文件ID，用于过滤搜索结果", required = true, example = "abc123-def456")
        @RequestParam("fileId") String fileId,
        @Parameter(description = "返回结果数量，默认5", example = "5")
        @RequestParam(value = "topK", defaultValue = "5") int topK
    ) {
        if (!StringUtils.hasText(query)) {
            return ResponseEntity.badRequest().build();
        }
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq("fileId", fileId).build();
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).filterExpression(expression).build());
        return ResponseEntity.ok(results);
    }

    /**
     * 删除指定 fileId 的向量数据。
     */
    @PostMapping("/delete")
    @Operation(
        summary = "删除文件向量数据",
        description = "根据文件ID删除对应的向量存储数据"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "删除成功",
            content = @Content(mediaType = "text/plain", schema = @Schema(example = "successfully deleted"))
        )
    })
    public ResponseEntity<String> delete(
        @Parameter(description = "要删除的文件ID", required = true, example = "abc123-def456")
        @RequestParam("fileId") String fileId
    ) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.eq("fileId", fileId).build();
        vectorStore.delete(expression);
        return ResponseEntity.ok("successfully deleted");
    }
}


