package org.frameworkset.spi.ai.embedding;
/**
 * Copyright 2025 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.frameworkset.util.JsonUtil;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.frameworkset.elasticsearch.entity.MetaMap;
import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.model.*;
import org.frameworkset.tran.DataImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2025/5/11
 */
public class KnowledgeEmbeddingService {
    private static Logger logger = LoggerFactory.getLogger(KnowledgeEmbeddingService.class);
  

    /**
     * 数据向量化处理方法
     * @param text
     * @return
     */
    private float[] text2embedding(String text){
        EmbeddingMessage embeddingMessage = new EmbeddingMessage();
		embeddingMessage.setInput(text);//设置将要向量化的数据
		embeddingMessage.setModel("bge-m3");
		embeddingMessage.setMaas("embedding_model");
//        params.put("model", "bge-large-zh-v1.5");//指定Xinference向量模型id
        //                   {"input": ["\\u5411\\u91cf\\u8f6c\\u6362"], "model": "custom-bge-large-zh-v1.5", "encoding_format": "base64"}
//                   String params = "{\"input\": [\"\\u5411\\u91cf\\u8f6c\\u6362\"], \"model\": \"custom-bge-large-zh-v1.5\", \"encoding_format\": \"base64\"}";
//                   Headers({'host': '172.24.176.18:9997', 'accept-encoding': 'gzip, deflate, br', 'connection': 'keep-alive', 
//                   'accept': 'application/json', 'content-type': 'application/json', 'user-agent': 'OpenAI/Python 1.35.13', 
//                   'x-stainless-lang': 'python', 'x-stainless-package-version': '1.35.13', 'x-stainless-os': 'Windows', 
//                   'x-stainless-arch': 'other:amd64', 'x-stainless-runtime': 'CPython', 'x-stainless-runtime-version': '3.10.14', 
//                   'authorization': '[secure]', 'x-stainless-async': 'false', 
//                   'openai-organization': '', 'content-length': '105'})

        //调用向量服务，对数据进行向量化
		AIAgent agent = new AIAgent();
		float[] embedding = agent.embedding(embeddingMessage);
		if(embedding == null){
			throw new AIRuntimeException("change LOG_CONTENT to vector failed:XinferenceResponse is null");
		}
		return embedding;
//        XinferenceResponse result = HttpRequestProxy.sendJsonBody("embedding_model", params, "/v1/embeddings", XinferenceResponse.class);
//        if (result != null) {
//            float[] embedding = result.embedding();
//            return embedding;
//        } else {
//            throw new DataImportException("change LOG_CONTENT to vector failed:XinferenceResponse is null");
//        }
    }
      

    public void searchVectorAndRerank(){
        ClientInterface clientUtil = ElasticSearchHelper.getConfigRestClientUtil("esmapper/knowledge.xml");
        String query = "Spring AOP";
        
        // ============ 方案1: 使用 ES 原生混合检索 (推荐) ============
		 /*
        logger.info("========== 开始混合检索 (向量 + BM25) ==========");
        Map hybridParams = new LinkedHashMap();
        hybridParams.put("query", query);
        hybridParams.put("embedding", text2embedding(query));
        hybridParams.put("k", 50);
        hybridParams.put("size", 20);
        hybridParams.put("similarity", 0.8);
        hybridParams.put("is_active", true);
        
        ESDatas<MetaMap> hybridDatas = clientUtil.searchList("/knowledge_chunks/_search", "searchHybrid", hybridParams, MetaMap.class);
        logger.info("混合检索返回总数: {}", hybridDatas.getTotalSize());
        
        List<MetaMap> allResults = hybridDatas.getDatas();
         */
        
        // ============ 方案2: 分别检索后手动合并 (备选) ============
        // 如果需要更灵活的控制，可以分别执行向量检索和 BM25 检索，然后手动合并
        
        // 1. 向量检索
        Map vectorParams = new LinkedHashMap();
        vectorParams.put("embedding", text2embedding(query));
        vectorParams.put("k", 100);
        vectorParams.put("size", 50);
        vectorParams.put("similarity", 0.5);
        ESDatas<MetaMap> vectorDatas = clientUtil.searchList("/knowledge_chunks/_search", "searchWithScore", vectorParams, MetaMap.class);
		List<MetaMap> vectorResults = vectorDatas.getDatas();
      
        
        // 2. BM25 检索
        Map bm25Params = new LinkedHashMap();
        bm25Params.put("query", query);
        bm25Params.put("size", 50);
        bm25Params.put("is_active", true);
        ESDatas<MetaMap> bm25Datas = clientUtil.searchList("/knowledge_chunks/_search", "searchBM25", bm25Params, MetaMap.class);
		List<MetaMap> bm25Results = bm25Datas.getDatas();
		
		RerankDocument rerankDocument = null;
        // 3. 合并结果 (去重)
        Map<String, RerankDocument> mergedMap = new LinkedHashMap<>();
        if (vectorResults != null) {
			logger.info("向量检索返回: {} 条", vectorResults.size());
            for (MetaMap metaMap : vectorResults) {
                String chunkId = (String) metaMap.get("chunk_id");
				rerankDocument = new RerankDocument();
				rerankDocument.setDocument((String) metaMap.get("content"));				
				rerankDocument.setVectorScore(metaMap.getScore());
				rerankDocument.setMetadata(metaMap);
                mergedMap.put(chunkId, rerankDocument);
            }
        }
        if (bm25Results != null) {
			logger.info("BM25检索返回: {} 条", bm25Results.size());
            for (MetaMap metaMap : bm25Results) {
                String chunkId = (String) metaMap.get("chunk_id");
				rerankDocument = mergedMap.get(chunkId);
                if (rerankDocument == null) {
					rerankDocument = new RerankDocument();
					rerankDocument.setDocument((String) metaMap.get("content"));
					
					rerankDocument.setBm25Score(metaMap.getScore());
					rerankDocument.setMetadata(metaMap);
					mergedMap.put(chunkId, rerankDocument);
                }
				else{
					rerankDocument.setBm25Score(metaMap.getScore());
				}
            }
        }
		// ============ 准备 Rerank 数据 ============
		if(mergedMap != null && mergedMap.size() > 0){
			List<RerankDocument> rerankDatas = new ArrayList<>(mergedMap.values());
			logger.info("合并后去重总数: {}", rerankDatas.size());
         
        
        
			RerankMessage rerankMessage = new RerankMessage();
            // ============ 调用 Rerank 服务 ============
            logger.info("========== 开始 Rerank 排序 ==========");
//			rerankMessage.setMaas("embedding_model");
//			rerankMessage.setModel("bge-reranker-large");  // 使用项目规范的 rerank 模型
			
			rerankMessage.setMaas("aigw");
			rerankMessage.setModel("10086/bge-reranker-v2-m3");  // 使用项目规范的 rerank 模型
            rerankMessage.setRerankDocuments(rerankDatas);
            rerankMessage.setQuery(query);
			rerankMessage.setReturnDocuments(false);  // 如需返回原始文本可开启
			AIAgent aiAgent = new AIAgent();
			List<RerankedDocument> rerankedDocuments = aiAgent.rerank(rerankMessage);
            
            if(rerankedDocuments != null) {
				logger.info("Rerank 响应: {}", JsonUtil.object2jsonPretty( rerankedDocuments));
			}
            
        } else {
            logger.warn("未检索到任何结果");
        }
    }
	
	public void createKnowledgeChunksIndex() {
		//创建加载配置文件的客户端工具，用来检索文档，单实例多线程安全
		ClientInterface clientUtil = ElasticSearchHelper.getConfigRestClientUtil("esmapper/knowledge.xml");
		try {
			clientUtil.dropIndice("knowledge_chunks");
		}
		catch (Exception e){
			
		}
		clientUtil.createIndiceMapping("knowledge_chunks","createKnowledgeChunksIndex");
	}


//        String requestBody =
//                "{\"model\": \"bge-reranker-large\", " +
//                        "\"documents\": [\"A man is eating food.\",  " +
//                        "\"A man is eating a piece of bread.\",  " +
//                        "\"The girl is carrying a baby.\", " +
//                        "\"A man is riding a horse.\",  " +
//                        "\"A woman is playing violin.\"],  " +
//                        "\"query\": \"A man is eating pasta.\"}";
}
