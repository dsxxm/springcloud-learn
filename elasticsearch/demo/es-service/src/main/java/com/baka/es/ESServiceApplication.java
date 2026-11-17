package com.baka.es;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;

/**
 * ESServiceApplication
 */
public class ESServiceApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(ESServiceApplication.class);
	private static final Logger SEPARATORLOGGER = LoggerFactory.getLogger("SEPARATOR_LOGGER");

	private static void log_separator(String title) {
		SEPARATORLOGGER.info("==================== {} ====================", title);
	}

	public static void main(String[] args) throws IOException {
		ElasticsearchClient client = ESClient.getInstance();

		// 索引操作
		// 查询所有索引
		log_separator("查询所有索引");
		IndicesResponse response = client.cat().indices();
		for (IndicesRecord record : response.indices()) {
			LOGGER.info("索引名称: " + record.index() + " 健康状态: " + record.health() + " 文档数量: " + record.docsCount());
		}
		// 创建索引 (会提前检查索引是否创建过)
		log_separator("创建索引");
		CreateIndexRequest createIndexRequest = CreateIndexRequest.of(i -> {
			return i.index("test");
		});
		ExistsRequest existsRequest = ExistsRequest.of(i -> {
			return i.index("test");
		});
		if (client.indices().exists(existsRequest).value()) {
			LOGGER.info("索引已存在 无需重复创建");
		} else {
			client.indices().create(createIndexRequest);
			LOGGER.info("已创建test索引");
		}
		// 删除索引同理 使用client.indices.delete()

		// 文档操作
		log_separator("文档操作");
		// 新增文档 检查是否存在同索引 只须额外加一个id 为了简洁就不再写一遍检查了
		// 不检查就是直接覆盖 因此修改文档也是这个api
		// 新增文档时 使用create api也就是client.create() 会原子性的防止重复创建,防止出现竞态问题
		Product testProduct = new Product(1L, "test", "200");
		IndexRequest<Product> indexRequest = IndexRequest.of(i -> {
			return i.index("test").id(String.valueOf(testProduct.getId())).document(testProduct);
		});
		client.index(indexRequest);
		LOGGER.info("新增文档成功");
		//新增一些其他文档留着查询用
		//-----------------------------------新增文档数据---------------------------------------
		Product testProduct1 = new Product(2L, "baka", "100");
		IndexRequest<Product> indexRequest1 = IndexRequest.of(i -> {
			return i.index("test").id(String.valueOf(testProduct1.getId())).document(testProduct1);
		});
		client.index(indexRequest1);
		LOGGER.info("新增文档成功");

		Product testProduct2 = new Product(3L, "bakaxxm", "300");
		IndexRequest<Product> indexRequest2 = IndexRequest.of(i -> {
			return i.index("test").id(String.valueOf(testProduct2.getId())).document(testProduct2);
		});
		client.index(indexRequest2);
		LOGGER.info("新增文档成功");

		Product testProduct3 = new Product(4L, "baka xxm", "100");
		IndexRequest<Product> indexRequest3 = IndexRequest.of(i -> {
			return i.index("test").id(String.valueOf(testProduct3.getId())).document(testProduct3);
		});
		client.index(indexRequest3);
		LOGGER.info("新增文档成功");
		//-----------------------------------新增文档数据---------------------------------------
		// 查询文档
		// 由于ES的索引刷新机制 写入成功后不一定能够立即被检索到
		// 可以手动强制刷新 或者在写入最后一个文档时调用.refresh(Refresh.WaitFor)
		SearchResponse<Product> searchResponse = client.search(
			i -> i.index("test").query(
				q -> q.term(t -> t.field("name").value("baka"))
			), Product.class);
		for (Hit<Product>hit : searchResponse.hits().hits()){
			Product product = hit.source();
			LOGGER.info("文档id: {}",hit.id());
			LOGGER.info("文档内容: id:{} name:{} price:{}", product.getId(),product.getName(), product.getPrice());
		}

		// 删除文档
		client.delete(i -> {
			return i.index("test").id(String.valueOf(1L));
		});
		LOGGER.info("删除文档成功");

		//关闭client
		client.close();
	}
}
