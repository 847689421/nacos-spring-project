/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.spring.context.annotation.config;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.annotation.NacosProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.embedded.web.server.EmbeddedNacosHttpServer;
import com.alibaba.nacos.spring.context.annotation.EnableNacos;
import com.alibaba.nacos.spring.test.AbstractNacosHttpServerTestExecutionListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.alibaba.nacos.embedded.web.server.NacosConfigHttpHandler.CONTENT_PARAM_NAME;
import static com.alibaba.nacos.embedded.web.server.NacosConfigHttpHandler.DATA_ID_PARAM_NAME;
import static com.alibaba.nacos.embedded.web.server.NacosConfigHttpHandler.GROUP_ID_PARAM_NAME;

/**
 * @author <a href="mailto:liaochunyhm@live.com">liaochuntao</a>
 * @since 0.3.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { NacosPropertySource4NacosRefreshTest.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		NacosPropertySource4NacosRefreshTest.class })
@NacosPropertySources({
		@NacosPropertySource(dataId = NacosPropertySource4NacosRefreshTest.DATA_ID, autoRefreshed = true) })
@EnableNacos(globalProperties = @NacosProperties(serverAddr = "${server.addr}", enableRemoteSyncConfig = "true", maxRetry = "5", configRetryTime = "2600", configLongPollTimeout = "26000"))
@Component
public class NacosPropertySource4NacosRefreshTest
		extends AbstractNacosHttpServerTestExecutionListener {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static final String DATA_ID = "app";

	private static final String APP_NAME = "Nacos-Spring";

	private static final String ANOTHER_APP_NAME = "Nacos-Spring-1";

	private static final int VALUE_1 = 1;

	private static final int VALUE_2 = 2;

	private static final int VALUE_3 = 3;

	private static final int VALUE_4 = 4;

	@Override
	public void init(EmbeddedNacosHttpServer httpServer) {
		Map<String, String> config = new HashMap<String, String>(1);
		config.put(DATA_ID_PARAM_NAME, DATA_ID);
		config.put(GROUP_ID_PARAM_NAME, DEFAULT_GROUP);

		config.put(CONTENT_PARAM_NAME, "app.name=" + APP_NAME + LINE_SEPARATOR
				+ "app.nacosFieldIntValueAutoRefreshed=" + VALUE_1 + LINE_SEPARATOR
				+ "app.nacosMethodIntValueAutoRefreshed=" + VALUE_2);
		httpServer.initConfig(config);
	}

	@Override
	protected String getServerAddressPropertyName() {
		return "server.addr";
	}

	@NacosRefresh
	public static class TApp {

		@Value("${app.name}")
		private String name;

		@Value("${app.nacosFieldIntValue:" + VALUE_1 + "}")
		private int nacosFieldIntValue;

		@Value(value = "${app.nacosFieldIntValueAutoRefreshed}")
		private int nacosFieldIntValueAutoRefreshed;

		private int nacosMethodIntValue;

		@Value("${app.nacosMethodIntValue:" + VALUE_2 + "}")
		public void setNacosMethodIntValue(int nacosMethodIntValue) {
			this.nacosMethodIntValue = nacosMethodIntValue;
		}

		private int nacosMethodIntValueAutoRefreshed;

		@Value(value = "${app.nacosMethodIntValueAutoRefreshed}")
		public void setNacosMethodIntValueAutoRefreshed(
				int nacosMethodIntValueAutoRefreshed) {
			this.nacosMethodIntValueAutoRefreshed = nacosMethodIntValueAutoRefreshed;
		}
	}

	@Bean
	public TApp app() {
		return new TApp();
	}

	@NacosInjected
	private ConfigService configService;

	@Autowired
	private TApp app;

	@Autowired
	private Environment environment;

	@Test
	public void testValue() throws NacosException, InterruptedException {
		Assert.assertEquals(APP_NAME, app.name);

		Assert.assertEquals(APP_NAME, environment.getProperty("app.name"));

		Assert.assertEquals(VALUE_1, app.nacosFieldIntValue);

		Assert.assertEquals(VALUE_2, app.nacosMethodIntValue);

		Assert.assertEquals(VALUE_1, app.nacosFieldIntValueAutoRefreshed);

		Assert.assertEquals(VALUE_2, app.nacosMethodIntValueAutoRefreshed);

		configService.publishConfig(DATA_ID, DEFAULT_GROUP, "app.name=" + ANOTHER_APP_NAME
				+ LINE_SEPARATOR + "app.nacosFieldIntValueAutoRefreshed=" + VALUE_3
				+ LINE_SEPARATOR + "app.nacosMethodIntValueAutoRefreshed=" + VALUE_4);

		Thread.sleep(1000);

		Assert.assertEquals(ANOTHER_APP_NAME, environment.getProperty("app.name"));

		Assert.assertEquals(ANOTHER_APP_NAME, app.name);

		Assert.assertEquals(VALUE_1, app.nacosFieldIntValue);

		Assert.assertEquals(VALUE_2, app.nacosMethodIntValue);

		Assert.assertEquals(VALUE_3, app.nacosFieldIntValueAutoRefreshed);

		Assert.assertEquals(VALUE_4, app.nacosMethodIntValueAutoRefreshed);
	}

}
