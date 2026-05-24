package org.frameworkset.spi.ai.store.db;
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

import com.frameworkset.common.poolman.handle.ValueExchange;
import com.frameworkset.util.BaseSimpleStringUtil;
import com.frameworkset.util.ColumnEditorInf;
import com.frameworkset.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.frameworkset.spi.ai.model.AIRuntimeException;
import org.frameworkset.spi.ai.model.TokenMetrics;
import org.frameworkset.util.annotations.wraper.ColumnWraper;

import java.sql.Clob;
import java.sql.SQLException;

/**
 * @author biaoping.yin
 * @Date 2026/4/6
 */
public class TokenMetricsEditor implements ColumnEditorInf {
    @Override
    public Object getValueFromObject(ColumnWraper columnWraper, Object fromValue) {
        if (fromValue == null) {
            return null;
        }
        String s = "";
        if(fromValue instanceof String){
            s = (String)fromValue;
        }
        else if(fromValue instanceof Clob){
            Clob clob = (Clob)fromValue;
            try {
                s = ValueExchange.getStringFromClob(clob);
            } catch (SQLException e) {
                throw new AIRuntimeException(e);
            }
        }

        if (BaseSimpleStringUtil.isNotEmpty(s)) {
            return JsonUtil.json2Object(s, TokenMetrics.class);
        }
        return null;
    }

    @Override
    public Object getValueFromString(ColumnWraper columnWraper, String fromValue) {
        return JsonUtil.json2Object(fromValue, TokenMetrics.class);
    }

    @Override
    public Object toColumnValue(ColumnWraper columnWraper, Object fromValue) {
        if(fromValue == null){
            return StringUtils.EMPTY;
        }
        if(fromValue instanceof String){
            return (String) fromValue;
        }
        return JsonUtil.object2json(fromValue);
    }

    @Override
    public Object toColumnValue(ColumnWraper columnWraper, String fromValue) {
        return fromValue;
    }

}
