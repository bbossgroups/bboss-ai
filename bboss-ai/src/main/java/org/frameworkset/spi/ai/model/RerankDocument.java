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

import java.io.Serializable;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/6
 */
public class RerankDocument implements Serializable {
    private String document;

    private double vectorScore;


    private double bm25Score;
    private Map<String,Object> metadata;

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
 
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }


    public double getVectorScore() {
        return vectorScore;
    }

    public void setVectorScore(double vectorScore) {
        this.vectorScore = vectorScore;
    }

    public double getBm25Score() {
        return bm25Score;
    }

    public void setBm25Score(double bm25Score) {
        this.bm25Score = bm25Score;
    }

}
