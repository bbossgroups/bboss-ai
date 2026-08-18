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

import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.ElasticSearchBoot;
import org.frameworkset.spi.remote.http.HttpRequestProxy;
import org.junit.Before;
import org.junit.Test;

/**
 * @author biaoping.yin
 * @Date 2025/5/11
 */
public class TestKnowledgeEmbedding {
	public static void main(String args[]){
		TestKnowledgeEmbedding testTextEmbedding = new TestKnowledgeEmbedding();
		testTextEmbedding.init();
		testTextEmbedding.testEmbedding();
	}
	@Before
	public void init(){
        ElasticSearchBoot.boot("application-stream.properties");
		HttpRequestProxy.startHttpPools("application-stream.properties");
	}
    @Test
    public void testEmbedding(){
        //Elasticsearch KNN search参考文档：https://www.elastic.co/docs/solutions/search/vector/knn#knn-search-filter-example
		KnowledgeEmbeddingService knowledgeEmbedding = new KnowledgeEmbeddingService();
        knowledgeEmbedding.createKnowledgeChunksIndex();
//        knowledgeEmbedding.searchVectorAndRerank("React Compiler的增量采用是什么意思？");
        
        knowledgeEmbedding.searchVectorAndRerank("为什么 React 中的代码重复可能会带来维护问题？");
//        knowledgeEmbedding.searchVectorAndRerank("从大多数后端或 REST 风格 API 获取数据时，React 建议使用哪些库？");
    }
 
    
   
     
}
