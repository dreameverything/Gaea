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
package com.bj58.spat.gaea.server.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.bj58.spat.gaea.serializer.serializer.ObjectSerializer;
import sun.misc.Signal;

import com.bj58.spat.gaea.server.bootstrap.signal.OperateSignal;
import com.bj58.spat.gaea.server.contract.context.Global;
import com.bj58.spat.gaea.server.contract.context.IProxyFactory;
import com.bj58.spat.gaea.server.contract.context.ServiceConfig;
import com.bj58.spat.gaea.server.contract.filter.IFilter;
import com.bj58.spat.gaea.server.contract.init.IInit;
import com.bj58.spat.gaea.server.contract.log.ILog;
import com.bj58.spat.gaea.server.contract.log.Log4jConfig;
import com.bj58.spat.gaea.server.contract.log.LogFactory;
import com.bj58.spat.gaea.server.contract.log.SystemPrintStream;
import com.bj58.spat.gaea.server.contract.server.IServer;
import com.bj58.spat.gaea.server.deploy.filemonitor.FileMonitor;
import com.bj58.spat.gaea.server.deploy.filemonitor.HotDeployListener;
import com.bj58.spat.gaea.server.deploy.filemonitor.NotifyCount;
import com.bj58.spat.gaea.server.deploy.hotdeploy.DynamicClassLoader;
import com.bj58.spat.gaea.server.deploy.hotdeploy.GlobalClassLoader;
import com.bj58.spat.gaea.server.deploy.hotdeploy.ProxyFactoryLoader;

/**
 * serive frame entry
 * 
 * @author Service Platform Architecture Team (spat@58.com)
 */
public class Main extends DynamicClassLoader {

	private static ILog logger = null;

	private static ExecutorService executorService = Executors.newSingleThreadExecutor();

	/**
	 * start server
	 * @param args : service name
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw new IllegalArgumentException("usage: -Dgaea.service.name=<service-name> [<other-gaea-config>]");
		}

		String userDir = System.getProperty("user.dir");
		String rootPath = userDir + "/../";
		String serviceName = null;
		Map<String, String> argsMap = new HashMap<String, String>();
		Global.getSingleton().setRootPath(rootPath);
		
		for(int i=0; i<args.length; i++) {
			if(args[i].startsWith("-D")) {
				String[] aryArg = args[i].split("=");
				if(aryArg.length == 2) {
					if(aryArg[0].equalsIgnoreCase("-Dgaea.service.name")) {
						serviceName = aryArg[1];
					}
					argsMap.put(aryArg[0].replaceFirst("-D", ""), aryArg[1]);
				}
			}
		}
		
		if(serviceName == null){
			throw new Exception("no service name please set it");
		}
		
		String serviceFolderPath = rootPath + "service/deploy/" + serviceName;
		String serviceLibPath = rootPath + "service/lib";
		String gaeaLibPath = rootPath + "lib";
		String gaeaConfigDefaultPath = rootPath + "conf/gaea_config.xml";
		String gaeaConfigPath = serviceFolderPath + "/gaea_config.xml";
		String log4jConfigDefaultPath = rootPath + "conf/gaea_log4j.xml";
		String log4jConfigPath = serviceFolderPath + "/gaea_log4j.xml";

		Global.getSingleton().setServiceFolderPath( serviceFolderPath );

		// load log4j
		loadLog4jConfig(log4jConfigPath, log4jConfigDefaultPath);
		logger = LogFactory.getLogger(Main.class);
		
		logger.info("+++++++++++++++++++++ starting +++++++++++++++++++++\n");
		
		logger.info("user.dir: " + userDir);
		logger.info("rootPath: " + rootPath);
		logger.info("service gaea_config.xml: " + gaeaConfigPath);
		logger.info("default gaea_config.xml: " + gaeaConfigDefaultPath);
		logger.info("service gaea_log4j.xml: " + log4jConfigPath);
		logger.info("default gaea_log4j.xml: " + log4jConfigDefaultPath);
		
		// load service config
		logger.info("load service config...");
		ServiceConfig serviceConfig = loadServiceConfig(gaeaConfigDefaultPath, gaeaConfigPath);
		Set<String> keySet = argsMap.keySet();
		for(String key : keySet) {
			logger.info(key + ": " + argsMap.get(key));
			serviceConfig.set(key, argsMap.get(key));
		}
		if(serviceConfig.getString("gaea.service.name") == null || serviceConfig.getString("gaea.service.name").equalsIgnoreCase("")) {
			logger.info("gaea.service.name:" + serviceName);
			serviceConfig.set("gaea.service.name", serviceName);
		}
		Global.getSingleton().setServiceConfig(serviceConfig);
		//初始化自定义的类加载器
		logger.info("-----------------[begin]Init DynamicClassLoader to find all jars------------------");
		//自定义类加载器，继承自SecureClassLoader
		DynamicClassLoader classLoader = new DynamicClassLoader();
		//找到各个目录中文件后缀为rar、jar、war、ear的文件路径
		classLoader.addFolder(serviceFolderPath,serviceLibPath,gaeaLibPath);
		logger.info( "已找到的需加载（尚未加载进JVM）的jar文件个数为："+classLoader.getJarList().size() );
		logger.info("-----------------[end]Init DynamicClassLoader finished------------------");

		// init class loader
		logger.info("-----------------[begin]Init GlobalClassLoader to load all jars to JVM------------------");
		//将jar文件通过系统默认的类加载器添加到JVM中
		GlobalClassLoader.addSystemClassPathFolder( classLoader.getJarList() );
		logger.info("-------------------------[end]Init GlobalClassLoader finished-------------------------\n");

		if(new File(serviceFolderPath).isDirectory() || !serviceName.equalsIgnoreCase("error_service_name_is_null")) {
			// load proxy factory
			logger.info("--------------------[begin]loading proxys-------------------");
			IProxyFactory proxyFactory = ProxyFactoryLoader.loadProxyFactory(classLoader);
			Global.getSingleton().setProxyFactory(proxyFactory);
			logger.info("-------------------------[end]loaded proxys-------------------------\n");
			
			// load init beans
			logger.info("-----------------[begin]loading gaea.init beans------------------");
			loadInitBeans(classLoader, serviceConfig);
			logger.info("-------------------------[end]loaded gaea.init beans-------------------------\n");
		}
		
		// load global request-filters
		logger.info("-----------loading global request filters------------");
		List<IFilter> requestFilters = loadFilters(classLoader, serviceConfig, "gaea.filter.global.request");
		for(IFilter filter : requestFilters) {
			Global.getSingleton().addGlobalRequestFilter(filter);
		}
		logger.info("-------------------------end-------------------------\n");
		
		// load global response-filters
		logger.info("-----------loading global response filters-----------");
		List<IFilter> responseFilters = loadFilters(classLoader, serviceConfig, "gaea.filter.global.response");
		for(IFilter filter : responseFilters) {
			Global.getSingleton().addGlobalResponseFilter(filter);
		}
		logger.info("-------------------------end-------------------------\n");
		
		// load connection filters
		logger.info("-----------loading connection filters-----------");
		List<IFilter> connFilters = loadFilters(classLoader, serviceConfig, "gaea.filter.connection");
		for(IFilter filter : connFilters) {
			Global.getSingleton().addConnectionFilter(filter);
		}
		logger.info("-------------------------end-------------------------\n");
		
		// load secureKey 当gaea.secure不为true时不启动权限认证
		logger.info("------------------load secureKey start---------------------");
		if(serviceConfig.getString("gaea.secure") != null && "true".equalsIgnoreCase(serviceConfig.getString("gaea.secure"))) {
			logger.info("gaea.secure:" + serviceConfig.getString("gaea.secure"));
			loadSecureKey(serviceConfig,serviceFolderPath);
		}
		logger.info("------------------load secureKey end----------------------\n");
		
		//注册信号 linux下支持USR2
		logger.info("------------------signal registr start---------------------");
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName != null && osName.indexOf("window") == -1){
			OperateSignal operateSignalHandler = new OperateSignal();
			Signal sig = new Signal("USR2");
			Signal.handle(sig, operateSignalHandler);
		}
		logger.info("------------------signal registr success----------------------\n");
		
		// load servers
		logger.info("------------------ starting servers -----------------");
		loadServers(classLoader, serviceConfig);
		logger.info("-------------------------end-------------------------\n");
		
		// add current service file to monitor
		if(serviceConfig.getBoolean("gaea.hotdeploy")) {
			logger.info("------------------[begin]init file monitor(gaea.hotdeploy is enable)-----------------");
			addFileMonitor(rootPath, serviceConfig.getString("gaea.service.name"));
			logger.info("-------------------------[end]init file monitor-------------------------\n");
		}

		try {
			registerExcetEven();
		} catch (Exception e) {
			logger.error("registerExcetEven error", e);
			System.exit(0);
		}

		//启动监控内存数据的线程
		try {
			monitorCacheData();
		} catch (Exception e){
			logger.warn(e.getMessage() , e);
		}

		logger.info("+++++++++++++++++++++ server start success!!! +++++++++++++++++++++\n");
		while (true) {
			Thread.sleep(1 * 60 * 60 * 1000);
		}
	}
	
	private static void monitorCacheData(){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				while( true ){
					System.out.println(" 系统内存数据监控( 每隔30秒一次 ) ");
					System.out.println(" ObjectSerializer.TypeInfoMap().size = " + ObjectSerializer.getTypeInfoMap().size() );
					System.out.println(" 系统内存数据监控结束");
					try {
						TimeUnit.SECONDS.sleep(30);
					} catch (InterruptedException e) {
					}
				}
			}
		});
	}
	/**
	 * load service config
	 * @param cps
	 * @return
	 * @throws Exception
	 */
	private static ServiceConfig loadServiceConfig(String... cps) throws Exception {
		ServiceConfig sc = ServiceConfig.getServiceConfig(cps);
		if(sc == null) {
			logger.error("ServiceConfig sc is null");
		}
		
		return sc;
	}
	
	/**
	 * 
	 * @param configPath
	 * @param defaultPath
	 * @throws Exception
	 */
	private static void loadLog4jConfig(String configPath, String defaultPath) throws Exception {
		File fLog4jConfig = new File(configPath);
		if(fLog4jConfig.exists()) {
			Log4jConfig.configure(configPath);
		} else {
			Log4jConfig.configure(defaultPath);
		}
		SystemPrintStream.redirectToLog4j();
	}
	
	/**
	 * 初始化Bean是在配置文件的gaea.init中配置的，
	 * 可以配置多个Bean，以逗号分隔。
	 * @param classLoader
	 * @param sc
	 * @throws Exception
	 */
	private static void loadInitBeans(DynamicClassLoader classLoader, ServiceConfig sc) throws Exception{
		List<String> initList = sc.getList("gaea.init", ",");
		if(initList != null) {
			for(String initBeans : initList) {
				try {
					logger.info("load: " + initBeans);
					IInit initBean = (IInit)classLoader.loadClass(initBeans).newInstance();
					Global.getSingleton().addInit(initBean);
					initBean.init();
				} catch(Exception e) {
					logger.error("init " + initBeans + " error!!!", e);
				}
			}
		}
	}
	
	/**
	 * 加载授权文件方法
	 * @param sc
	 * @param path
	 * @throws Exception
	 */
	private static void loadSecureKey(ServiceConfig sc, String path) throws Exception{
		File[] file = new File(path).listFiles();
		for(File f : file){	
			String fName = f.getName();
			if(!f.exists() || fName.indexOf("secure") < 0 || !"xml".equalsIgnoreCase(fName.substring(fName.lastIndexOf(".")+1))){
				continue;
			}
			sc.getSecureConfig(f.getPath());
		}
	}
	
	
	/**
	 * 
	 * @param classLoader
	 * @param sc
	 * @param key
	 * @throws Exception
	 */
	private static List<IFilter> loadFilters(DynamicClassLoader classLoader, ServiceConfig sc, String key) throws Exception {
		List<String> filterList = sc.getList(key, ",");
		List<IFilter> instanceList = new ArrayList<IFilter>();
		if(filterList != null) {
			for(String filterName : filterList) {
				try {
					logger.info("load: " + filterName);
					IFilter filter = (IFilter)classLoader.loadClass(filterName).newInstance();
					instanceList.add(filter);
				} catch(Exception e) {
					logger.error("load " + filterName + " error!!!", e);
				}
			}
		}
		
		return instanceList;
	}
	
	/**
	 * 
	 * @param classLoader
	 * @param sc
	 * @throws Exception
	 */
	private static void loadServers(DynamicClassLoader classLoader, ServiceConfig sc) throws Exception {
		//gaea.servers默认值是gaea.server.tcp , gaea.server.http,gaea.server.telnet
		List<String> servers = sc.getList("gaea.servers", ",");
		if(servers != null) {
			for(String server : servers) {
				try {
					if(sc.getBoolean(server + ".enable")) {
						logger.info(server + " is starting...");
						//gaea.server.tcp.implement    = com.bj58.spat.gaea.server.core.communication.tcp.SocketServer
						//gaea.server.telnet.implement = com.bj58.spat.gaea.server.core.communication.telnet.TelnetServer
						//gaea.server.http.implement   = com.bj58.spat.gaea.server.core.communication.http.HttpServer
						IServer serverImpl = (IServer) classLoader.loadClass(sc.getString(server + ".implement")).newInstance();
						Global.getSingleton().addServer(serverImpl);
						serverImpl.start();
						logger.info(server + "started success!!!\n");
					}
				} catch(Exception ex) {
					
				}
			}
		}
	}
	
	/**
	 * add current service file to file monitor
	 * @param rootPath
	 * @param serviceName
	 * @throws Exception 
	 */
	private static void addFileMonitor(String rootPath, String serviceName) throws Exception {
		FileMonitor.getInstance().addMonitorFile(rootPath + 
												"service/deploy/" + 
												serviceName + 
												"/");
		
		FileMonitor.getInstance().setInterval(5000);
		FileMonitor.getInstance().setNotifyCount(NotifyCount.Once);
		FileMonitor.getInstance().addListener(new HotDeployListener());
		FileMonitor.getInstance().start();
	}
	
	/**
	 * when shutdown server destroyed all socket connection
	 */
	private static void registerExcetEven() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				for(IServer server : Global.getSingleton().getServerList()) {
					try {
						server.stop();
					} catch (Exception e) {
						logger.error("stop server error", e);
					}
				}

				try {
					super.finalize();
				} catch (Throwable e) {
					logger.error("super.finalize() error when stop server", e);
				}
			}
		});
	}
}