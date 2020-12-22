/*
 *  Copyright Beijing 58 Information Technology Co.,Ltd.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package com.bj58.spat.gaea.server.core.proxy;

import com.bj58.spat.gaea.server.contract.context.ExecFilterType;
import com.bj58.spat.gaea.server.contract.context.Global;
import com.bj58.spat.gaea.server.contract.context.GaeaContext;
import com.bj58.spat.gaea.server.contract.filter.IFilter;
import com.bj58.spat.gaea.server.contract.log.ILog;
import com.bj58.spat.gaea.server.contract.log.LogFactory;

/**
 * sync service invoke handle
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class SyncInvokerHandle extends InvokerBase {

	private static ILog logger = LogFactory.getLogger(SyncInvokerHandle.class);

	/**
	 * <pre>
	 * create protocol and invoke service proxy
	 * 参数传入的上下文context会添加到ThreadLoal中，这个上下文
	 * 还会经过多个Request Filter过滤处理之后，添加序列化后的数据，
	 * 执行时，会将context传入执行业务数据，最后会经过一系列的
	 * Response Filter过滤处理后，将此上下文的数据响应给客户端。
	 * </pre>
	 */
	@Override
	public void invoke(GaeaContext context) throws Exception {
		Global.getSingleton().getThreadLocal().set(context);
		try {
			/*
			默认的gaea.filter.global.request配置的是：
			com.bj58.spat.gaea.server.filter.ProtocolParseFilter
			com.bj58.spat.gaea.server.filter.HandclaspFilter
			com.bj58.spat.gaea.server.filter.ExecuteMethodFilter
			*/
			for (IFilter f : Global.getSingleton().getGlobalRequestFilterList()) {
				if (context.getExecFilter() == ExecFilterType.All || context.getExecFilter() == ExecFilterType.RequestOnly) {
					f.filter(context);
				}
			}

			if (context.isDoInvoke()) {
				doInvoke(context);
			}

			logger.debug("begin response filter");
			for (IFilter f : Global.getSingleton().getGlobalResponseFilterList()) {
				if (context.getExecFilter() == ExecFilterType.All || context.getExecFilter() == ExecFilterType.ResponseOnly) {
					f.filter(context);
				}
			}
			context.getServerHandler().writeResponse(context);
		} catch (Exception ex) {
			context.setError(ex);
			context.getServerHandler().writeResponse(context);
			logger.error("in Sync(同步) messageReceived", ex);
		} finally {
			Global.getSingleton().getThreadLocal().remove();
		}
	}
}