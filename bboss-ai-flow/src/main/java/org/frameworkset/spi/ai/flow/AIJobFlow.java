package org.frameworkset.spi.ai.flow;
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

import org.frameworkset.tran.jobflow.JobFlow;

/**
 * @author biaoping.yin
 * @Date 2026/4/20
 */
public class AIJobFlow extends JobFlow {
    private AIPlanAgent planAgent;
    public AIJobFlow(AIPlanAgent planAgent){
        this.planAgent = planAgent;
    }
//    @Override
//    public void execute() {
//        logger.info("Execute {} begin.",jobInfo );
//
//        startEndScheduleThread(new ScheduleEndCall() {
//            @Override
//            public void call(boolean scheduled) {
//                stop(true);
//            }
//        });
//
//
//        reset();
//        this.jobFlowExecuteContext = new DefaultJobFlowExecuteContext(this);
//        jobFlowMetrics.addTotalCount();
//        if(CollectionUtils.isNotEmpty(this.jobFlowListeners)){
//            for(JobFlowListener jobFlowListener:jobFlowListeners){
//                jobFlowListener.beforeExecute(jobFlowExecuteContext);
//            }
//        }
//        try {
//            JobFlowNodeExecuteContext jobFlowNodeExecuteContext = this.startJobFlowNode.buildJobFlowNodeExecuteContext();
//            jobFlowNodeExecuteContext.setContainerJobFlowExecuteContext(this.jobFlowExecuteContext);
//            this.startJobFlowNode.execute(  jobFlowNodeExecuteContext);
//
//
//            logger.info("Execute {} end.",jobInfo);
//        }
//        catch (RuntimeException e){
//            throw e;
//        }
//        catch (Exception e){
//            throw new JobFlowException(e);
//        }
//        catch (Throwable e){
//            throw new JobFlowException(e);
//        }
//        finally {
//
//        }
//    }
}
