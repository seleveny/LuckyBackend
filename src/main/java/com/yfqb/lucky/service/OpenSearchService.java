package com.yfqb.lucky.service;

import com.yfqb.lucky.basic.IResult;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch 搜索服务
 *
 * @author xuchengcheng
 * @since 2026-04-23
 */
@Service
public class OpenSearchService {

    private final OpenSearchClient client;

    public OpenSearchService(OpenSearchClient client) {
        this.client = client;
    }

    /**
     * 创建索引
     */
    public Mono<IResult<Boolean>> createIndex(String indexName) {
        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            if (exists) {
                return IResult.error("索引已存在: " + indexName);
            }
            CreateIndexRequest request = CreateIndexRequest.of(r -> r.index(indexName));
            client.indices().create(request);
            return IResult.success(true, "索引创建成功");
        } catch (IOException e) {
            return IResult.error("创建索引失败: " + e.getMessage());
        }
    }

    /**
     * 创建索引（带 mapping）
     */
    public Mono<IResult<Boolean>> createIndex(String indexName, String mappingJson) {
        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            if (exists) {
                return IResult.error("索引已存在: " + indexName);
            }
            try (org.opensearch.client.opensearch.generic.Response response = client.generic()
                    .execute(org.opensearch.client.opensearch.generic.Requests.builder()
                            .endpoint("/" + indexName)
                            .method("PUT")
                            .json(mappingJson)
                            .build())) {
                return IResult.success(true, "索引创建成功");
            }
        } catch (IOException e) {
            return IResult.error("创建索引失败: " + e.getMessage());
        }
    }

    /**
     * 判断索引是否存在
     */
    public Mono<IResult<Boolean>> indexExists(String indexName) {
        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            return IResult.success(exists);
        } catch (IOException e) {
            return IResult.error("查询索引失败: " + e.getMessage());
        }
    }

    /**
     * 删除索引
     */
    public Mono<IResult<Boolean>> deleteIndex(String indexName) {
        try {
            boolean exists = client.indices().exists(r -> r.index(indexName)).value();
            if (!exists) {
                return IResult.error("索引不存在: " + indexName);
            }
            client.indices().delete(DeleteIndexRequest.of(r -> r.index(indexName)));
            return IResult.success(true, "索引删除成功");
        } catch (IOException e) {
            return IResult.error("删除索引失败: " + e.getMessage());
        }
    }

    /**
     * 新增文档
     */
    public Mono<IResult<String>> indexDocument(String indexName, String id, Object document) {
        try {
            IndexResponse response = client.index(r -> r
                    .index(indexName)
                    .id(id)
                    .document(document)
            );
            String docId = response.id();
            if (response.result() == Result.Created) {
                return IResult.success(docId, "文档创建成功");
            }
            return IResult.success(docId, "文档更新成功");
        } catch (IOException e) {
            return IResult.error("索引文档失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 获取文档
     */
    public <T> Mono<IResult<T>> getDocument(String indexName, String id, Class<T> clazz) {
        try {
            GetResponse<T> response = client.get(r -> r.index(indexName).id(id), clazz);
            if (response.found()) {
                return IResult.success(response.source());
            }
            return IResult.error("文档不存在");
        } catch (IOException e) {
            return IResult.error("查询文档失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    public Mono<IResult<Boolean>> deleteDocument(String indexName, String id) {
        try {
            DeleteResponse response = client.delete(r -> r.index(indexName).id(id));
            return IResult.success(true, "文档删除成功");
        } catch (IOException e) {
            return IResult.error("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文档
     */
    public Mono<IResult<List<Map<String, Object>>>> search(String indexName, String queryText, int from, int size) {
        try {
            SearchResponse<Map> response = client.search(r -> r
                            .index(indexName)
                            .from(from)
                            .size(size)
                            .query(q -> q
                                    .multiMatch(m -> m
                                            .query(queryText)
                                    )
                            ),
                    Map.class
            );
            List<Map<String, Object>> hits = response.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = hit.source();
                        if (source != null) {
                            source.put("_id", hit.id());
                            source.put("_score", hit.score());
                        }
                        return source;
                    })
                    .toList();
            return IResult.success(hits);
        } catch (IOException e) {
            return IResult.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 批量索引文档
     */
    public Mono<IResult<Integer>> bulkIndex(String indexName, List<Map.Entry<String, Object>> documents) {
        try {
            List<BulkOperation> operations = documents.stream()
                    .map(entry -> BulkOperation.of(b -> b
                            .index(IndexOperation.of(i -> i
                                    .index(indexName)
                                    .id(entry.getKey())
                                    .document(entry.getValue())
                            ))
                    ))
                    .toList();

            BulkResponse response = client.bulk(r -> r.operations(operations));
            int successCount = (int) response.items().stream()
                    .filter(item -> item.error() == null)
                    .count();
            return IResult.success(successCount, "批量索引完成");
        } catch (IOException e) {
            return IResult.error("批量索引失败: " + e.getMessage());
        }
    }
}
