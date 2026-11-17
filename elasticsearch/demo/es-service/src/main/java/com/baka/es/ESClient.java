package com.baka.es;

import java.io.File;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.TransportUtils;

/**
 * ESClient
 */
public class ESClient {

	private static volatile ElasticsearchClient esClient;
	private static final Logger LOGGER = LoggerFactory.getLogger(ESClient.class);

	public static ElasticsearchClient getInstance() {
		if (esClient == null) {
			synchronized (ESClient.class) {
				if (esClient == null) {
					try {
						File cert = new File("certs/http_ca.crt");
						SSLContext sslContext;
						sslContext = TransportUtils.sslContextFromHttpCaCrt(cert);
						esClient = ElasticsearchClient
								.of(b -> b.host("https://localhost:9200").usernameAndPassword("bakaxxm", "20031228w")
										.sslContext(sslContext));
						LOGGER.info("ESClient初始化成功");
					} catch (Exception exception) {
						throw new IllegalStateException("ES客户端初始化失败" + exception.getMessage(), exception);
					}
				}
			}
		}

		return esClient;
	}

	private ESClient() {
	}

}
