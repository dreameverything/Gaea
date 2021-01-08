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
package com.bj58.spat.gaea.server.deploy.bytecode;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.bj58.spat.gaea.server.contract.context.Global;
import com.bj58.spat.gaea.server.contract.context.IProxyFactory;
import com.bj58.spat.gaea.server.contract.context.IProxyStub;
import com.bj58.spat.gaea.server.contract.log.ILog;
import com.bj58.spat.gaea.server.contract.log.LogFactory;
import com.bj58.spat.gaea.server.deploy.hotdeploy.DynamicClassLoader;

/**
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class CreateManager {
	
	private static ILog logger = LogFactory.getLogger(CreateManager.class);

	/**
	 * 如果serviceframe.xml配置文件存在的话，从此文件中加载类。
	 * 否则从参数serviceRootPath设定的位置中加载类。
	 * @param serviceRootPath {GARA_ROOT}/service/deploy/{gaea.service.name}
	 * @param classLoader 自定义的类加载器
	 * @return
	 * @throws Exception
	 */
	public IProxyFactory careteProxy(String serviceRootPath, DynamicClassLoader classLoader) throws Exception {
		
		String configPath = serviceRootPath + "/" + Constant.SERVICE_CONTRACT;
		File file = new File(configPath);
		//RPC接口和实现类的对应关系
		ContractInfo serviceContract = null;
		
		if (file != null && file.exists()) {
			serviceContract = ContractConfig.loadContractInfo(configPath, classLoader);
		} else {
			serviceContract = ScanClass.getContractInfo(serviceRootPath + "/", classLoader);
		}
		
		//time用于创建代理类时用于生成类的名称
		long time = System.currentTimeMillis();
		List<ClassFile> localProxyList = new ProxyClassCreater().createProxy(classLoader, serviceContract, time);
		logger.info("Proxy Class byte buffer finish");
		ClassFile cfProxyFactory = new ProxyFactoryCreater().createProxy(classLoader, serviceContract, time);
		logger.info("Proxy Factory byte buffer finish");

		//将java源文件输出到目录中
		File outputFile = new File(Global.getSingleton().getServiceFolderPath() + File.separator + "proxy-src");
		if (outputFile.exists()) {
			for (File childFile : outputFile.listFiles()) {
				if (childFile.exists()) {
					childFile.delete();
				}
			}
		}

		outputClassSourceCodeToFile( localProxyList );
		outputClassSourceCodeToFile( cfProxyFactory );
		List<IProxyStub> localProxyAry = new ArrayList<IProxyStub>();
		for(ClassFile cf : localProxyList) {
			Class<?> cls = classLoader.findClass(cf.getClsName(), cf.getClsByte(), null);
			logger.info("Dynamic load Proxy Class:" + cls.getName());
			localProxyAry.add((IProxyStub)cls.newInstance());
		}
		
		Class<?> proxyFactoryCls = classLoader.findClass(cfProxyFactory.getClsName(), cfProxyFactory.getClsByte(), null);
		logger.info("Dynamic load Proxy Factory Class:" + proxyFactoryCls.getName());
		Constructor<?> constructor = proxyFactoryCls.getConstructor(List.class);
		IProxyFactory pfInstance = (IProxyFactory)constructor.newInstance(localProxyAry);
		logger.info("Proxy Classes & Proxy Factory Class load finish!");
		return pfInstance;
	}

	private void outputClassSourceCodeToFile( List<ClassFile> classFileList ){
		for ( ClassFile classFile:
			 classFileList) {
			outputClassSourceCodeToFile( classFile );
		}
	}

	private void outputClassSourceCodeToFile( ClassFile classFile ){
		classFile.toFile( new File( Global.getSingleton().getServiceFolderPath() +File.separator+"proxy-src" ) );
	}
}