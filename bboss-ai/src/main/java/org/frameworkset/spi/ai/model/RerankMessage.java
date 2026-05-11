package org.frameworkset.spi.ai.model;
/**
 * Copyright 2026 bboss
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

import java.util.List;

/**
 * @author biaoping.yin
 * @Date 2026/1/4
 */
public class RerankMessage<T extends RerankMessage> extends AgentMessage<T> {
    private List<RerankDocument> rerankDocuments;
    private String query;
    private boolean returnDocuments;
    /**
     * The relevance score of the query to the document.
     * 只召回相似度大于relevanceScore的文档
     * @return
     */
    private Double relevanceScore;
    /**
     * The number of top documents to return.
     * 最多只召回topK个文档
     * @return
     */
    private Integer topK;


    public List<RerankDocument> getRerankDocuments() {
        return rerankDocuments;
    }
    
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    public T setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
        return (T)this;
    }

    public Integer getTopK() {
        return topK;
    }

    public T setTopK(Integer topK) {
        this.topK = topK;
        return (T)this;
    }

    public List<String> convertDocuments(){
        List<String> documents = null;
        if(rerankDocuments != null && rerankDocuments.size() > 0){
            documents = new java.util.ArrayList<String>();
            for(RerankDocument rerankDocument : rerankDocuments){
                documents.add(rerankDocument.getDocument());
            }
        }
        return documents;
    }

    public void setRerankDocuments(List<RerankDocument> rerankDocuments) {
        this.rerankDocuments = rerankDocuments;
    }

    public boolean isReturnDocuments() {
        return returnDocuments;
    }

    public T setReturnDocuments(boolean returnDocuments) {
        this.returnDocuments = returnDocuments;
        return (T)this;
    }

   

    public String getQuery() {
        return query;
    }
    public T setQuery(String query) {
        this.query = query;
        return (T)this;
    }
}
