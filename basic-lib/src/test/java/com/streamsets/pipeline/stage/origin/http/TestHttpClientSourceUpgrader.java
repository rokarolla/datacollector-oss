/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.http;

import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import com.streamsets.pipeline.config.DataFormat;
import com.streamsets.pipeline.config.JsonMode;
import com.streamsets.pipeline.config.upgrade.UpgraderTestUtils;
import com.streamsets.pipeline.lib.http.AuthenticationType;
import com.streamsets.pipeline.lib.http.HttpMethod;
import com.streamsets.pipeline.lib.http.HttpProxyConfigBean;
import com.streamsets.pipeline.lib.http.OAuthConfigBean;
import com.streamsets.pipeline.lib.http.PasswordAuthConfigBean;
import com.streamsets.pipeline.lib.http.SslConfigBean;
import com.streamsets.pipeline.lib.http.logging.JulLogLevelChooserValues;
import com.streamsets.pipeline.lib.http.logging.RequestLoggingConfigBean;
import com.streamsets.pipeline.lib.http.logging.VerbosityChooserValues;
import com.streamsets.pipeline.stage.util.tls.TlsConfigBeanUpgraderTestUtil;
import com.streamsets.pipeline.upgrader.SelectorStageUpgrader;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestHttpClientSourceUpgrader {

  private StageUpgrader upgrader;
  private List<Config> configs;
  private StageUpgrader.Context context;

  @Before
  public void setUp() {
    URL yamlResource = ClassLoader.getSystemClassLoader().getResource("upgrader/HttpClientDSource.yaml");
    upgrader = new SelectorStageUpgrader("stage", new HttpClientSourceUpgrader(), yamlResource);
    configs = new ArrayList<>();
    context = Mockito.mock(StageUpgrader.Context.class);
  }

  @Test
  public void testV1toV2() throws StageException {
    configs.add(new Config("dataFormat", DataFormat.JSON));
    configs.add(new Config("resourceUrl", "stream.twitter.com/1.1/statuses/sample.json"));
    configs.add(new Config("httpMethod", HttpMethod.GET));
    configs.add(new Config("requestData", ""));
    configs.add(new Config("requestTimeoutMillis", 1000L));
    configs.add(new Config("httpMode", HttpClientMode.STREAMING));
    configs.add(new Config("pollingInterval", 5000L));
    configs.add(new Config("isOAuthEnabled", true));
    configs.add(new Config("batchSize", 100));
    configs.add(new Config("maxBatchWaitTime", 5000L));
    configs.add(new Config("consumerKey", "MY_KEY"));
    configs.add(new Config("consumerSecret", "MY_SECRET"));
    configs.add(new Config("token", "MY_TOKEN"));
    configs.add(new Config("tokenSecret", "MY_TOKEN_SECRET"));
    configs.add(new Config("jsonMode", JsonMode.MULTIPLE_OBJECTS));
    configs.add(new Config("entityDelimiter", "\n"));

    Assert.assertEquals(16, configs.size());

    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("a", "b", "c", 1, 2, configs);

    Assert.assertEquals(17, configs.size());

    Map<String, Object> configValues = getConfigsAsMap(configs);

    assertTrue(configValues.containsKey("conf.dataFormat"));
    Assert.assertEquals(DataFormat.JSON, configValues.get("conf.dataFormat"));

    assertTrue(configValues.containsKey("conf.resourceUrl"));
    Assert.assertEquals("stream.twitter.com/1.1/statuses/sample.json", configValues.get("conf.resourceUrl"));

    assertTrue(configValues.containsKey("conf.httpMethod"));
    Assert.assertEquals(HttpMethod.GET, configValues.get("conf.httpMethod"));

    assertTrue(configValues.containsKey("conf.requestData"));
    Assert.assertEquals("", configValues.get("conf.requestData"));

    assertTrue(configValues.containsKey("conf.requestTimeoutMillis"));
    Assert.assertEquals(1000L, configValues.get("conf.requestTimeoutMillis"));

    assertTrue(configValues.containsKey("conf.httpMode"));
    Assert.assertEquals(HttpClientMode.STREAMING, configValues.get("conf.httpMode"));

    assertTrue(configValues.containsKey("conf.pollingInterval"));
    Assert.assertEquals(5000L, configValues.get("conf.pollingInterval"));

    assertTrue(configValues.containsKey("conf.entityDelimiter"));
    Assert.assertEquals("\n", configValues.get("conf.entityDelimiter"));

    assertTrue(configValues.containsKey("conf.authType"));
    Assert.assertEquals(AuthenticationType.OAUTH, configValues.get("conf.authType"));

    assertTrue(configValues.containsKey("conf.dataFormatConfig.jsonContent"));
    Assert.assertEquals(JsonMode.MULTIPLE_OBJECTS, configValues.get("conf.dataFormatConfig.jsonContent"));

    assertTrue(configValues.containsKey("conf.oauth.consumerKey"));
    Assert.assertEquals("MY_KEY", configValues.get("conf.oauth.consumerKey"));

    assertTrue(configValues.containsKey("conf.oauth.consumerKey"));
    Assert.assertEquals("MY_SECRET", configValues.get("conf.oauth.consumerSecret"));

    assertTrue(configValues.containsKey("conf.oauth.token"));
    Assert.assertEquals("MY_TOKEN", configValues.get("conf.oauth.token"));

    assertTrue(configValues.containsKey("conf.oauth.tokenSecret"));
    Assert.assertEquals("MY_TOKEN_SECRET", configValues.get("conf.oauth.tokenSecret"));

    assertTrue(configValues.containsKey("conf.basic.maxBatchSize"));
    Assert.assertEquals(100, configValues.get("conf.basic.maxBatchSize"));

    assertTrue(configValues.containsKey("conf.basic.maxWaitTime"));
    Assert.assertEquals(5000L, configValues.get("conf.basic.maxWaitTime"));

    assertTrue(configValues.containsKey("conf.dataFormatConfig.csvSkipStartLines"));
    Assert.assertEquals(0, configValues.get("conf.dataFormatConfig.csvSkipStartLines"));

  }

  @Test
  public void testV2ToV3() throws StageException {
    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("a", "b", "c", 2, 3, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);

    assertTrue(configValues.containsKey("conf.useProxy"));
    Assert.assertEquals(false, configValues.get("conf.useProxy"));

    assertTrue(configValues.containsKey("conf.proxy.uri"));
    Assert.assertEquals("", configValues.get("conf.proxy.uri"));
    assertTrue(configValues.containsKey("conf.proxy.username"));
    Assert.assertEquals("", configValues.get("conf.proxy.username"));
    assertTrue(configValues.containsKey("conf.proxy.password"));
    Assert.assertEquals("", configValues.get("conf.proxy.password"));
  }

  @Test
  public void testV3ToV4() throws Exception {
    configs.add(new Config("conf.authType", "BASIC"));

    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("a", "b", "c", 3, 4, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    Assert.assertEquals(AuthenticationType.UNIVERSAL, configValues.get("conf.authType"));
  }

  @Test
  public void testV4ToV5() throws Exception {
    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("a", "b", "c", 4, 5, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.headers"));
    Assert.assertEquals(Collections.EMPTY_LIST, configValues.get("conf.headers"));
  }

  @Test
  public void testV5ToV6() throws Exception {
    configs.add(new Config("conf.requestData", ""));

    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 5, 6, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.requestBody"));
    Assert.assertFalse(configValues.containsKey("conf.requestData"));
    Assert.assertEquals(1, configs.size());
  }

  @Test
  public void testV6ToV7() throws Exception {
    configs.add(new Config("conf.requestTimeoutMillis", 1000));
    configs.add(new Config("conf.numThreads", 10));
    configs.add(new Config("conf.authType", AuthenticationType.NONE));
    configs.add(new Config("conf.oauth", new OAuthConfigBean()));
    configs.add(new Config("conf.basicAuth", new PasswordAuthConfigBean()));
    configs.add(new Config("conf.useProxy", false));
    configs.add(new Config("conf.proxy", new HttpProxyConfigBean()));
    configs.add(new Config("conf.sslConfig", new SslConfigBean()));

    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 6, 7, configs);

    for (Config config : configs) {
      switch (config.getName()) {
        case "conf.client.requestTimeoutMillis":
          Assert.assertEquals(1000, config.getValue());
          break;
        case "conf.client.authType":
          Assert.assertEquals(AuthenticationType.NONE, config.getValue());
          break;
        case "conf.client.oauth":
          Assert.assertTrue(config.getValue() instanceof OAuthConfigBean);
          break;
        case "conf.client.basicAuth":
          Assert.assertTrue(config.getValue() instanceof PasswordAuthConfigBean);
          break;
        case "conf.client.useProxy":
          Assert.assertEquals(false, config.getValue());
          break;
        case "conf.client.numThreads":
          Assert.assertEquals(10, config.getValue());
          break;
        case "conf.client.proxy":
          Assert.assertTrue(config.getValue() instanceof HttpProxyConfigBean);
          break;
        case "conf.client.sslConfig":
          Assert.assertTrue(config.getValue() instanceof SslConfigBean);
          break;
        case "conf.client.transferEncoding":
          Assert.assertEquals(RequestEntityProcessing.CHUNKED, config.getValue());
          break;
        default:
          fail();
      }
    }
  }

  @Test
  public void testV7ToV8() throws Exception {
    configs.add(new Config("conf.entityDelimiter", "\\r\\n"));
    configs.add(new Config("conf.client.requestTimeoutMillis", 1000));

    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 7, 8, configs);
    Map<String, Object> configValues = getConfigsAsMap(configs);

    for (Config config : configs) {
      switch (config.getName()) {
        case "conf.client.readTimeoutMillis":
          assertTrue(configValues.containsKey("conf.client.readTimeoutMillis"));
          Assert.assertEquals(1000, config.getValue());
          break;
        case "conf.client.connectTimeoutMillis":
          assertTrue(configValues.containsKey("conf.client.connectTimeoutMillis"));
          Assert.assertEquals(0, config.getValue());
          break;
        case "conf.pagination.mode":
          assertTrue(configValues.containsKey("conf.pagination.mode"));
          Assert.assertEquals(PaginationMode.NONE, config.getValue());
          break;
        case "conf.pagination.startAt":
          assertTrue(configValues.containsKey("conf.pagination.startAt"));
          Assert.assertEquals(0, config.getValue());
          break;
        case "conf.pagination.resultFieldPath":
          assertTrue(configValues.containsKey("conf.pagination.resultFieldPath"));
          Assert.assertEquals("", config.getValue());
          break;
        case "conf.pagination.rateLimit":
          assertTrue(configValues.containsKey("conf.pagination.rateLimit"));
          Assert.assertEquals(2000, config.getValue());
          break;
        default:
          fail();
      }
    }
  }

  @Test
  public void testV8ToV9() throws Exception {
    configs.add(new Config("conf.dataFormatConfig.schemaInMessage", true));

    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 8, 9, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.dataFormatConfig.avroSchema"));
    assertTrue(configValues.containsKey("conf.dataFormatConfig.avroSchemaSource"));
    assertTrue(configValues.containsKey("conf.dataFormatConfig.schemaRegistryUrls"));
    assertTrue(configValues.containsKey("conf.dataFormatConfig.schemaLookupMode"));
    assertTrue(configValues.containsKey("conf.dataFormatConfig.subject"));
    assertTrue(configValues.containsKey("conf.dataFormatConfig.schemaId"));

    for (Config config : configs) {
      switch (config.getName()) {
        case "conf.dataFormatConfig.avroSchema":
          assertNull(config.getValue());
          break;
        case "conf.dataFormatConfig.subject":
          assertEquals("",config.getValue());
          break;
        case "conf.dataFormatConfig.avroSchemaSource":
          assertEquals("SOURCE", config.getValue());
          break;
        case "conf.dataFormatConfig.schemaRegistryUrls":
          assertEquals(new ArrayList<>(), config.getValue());
          break;
        case "conf.dataFormatConfig.schemaLookupMode":
          assertEquals("AUTO", config.getValue());
          break;
        case "conf.dataFormatConfig.schemaId":
          assertEquals(0, config.getValue());
          break;
        default:
          fail();
      }
    }
  }

  @Test
  public void testV9ToV10() throws Exception {
    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("a", "b", "c", 9, 10, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.responseStatusActionConfigs"));
    assertEquals(HttpResponseActionConfigBean.DEFAULT_MAX_NUM_RETRIES,
            configValues.get("conf.responseTimeoutActionConfig.maxNumRetries"));
    assertEquals(HttpResponseActionConfigBean.DEFAULT_BACKOFF_INTERVAL_MS,
            configValues.get("conf.responseTimeoutActionConfig.backoffInterval"));
    assertEquals(HttpTimeoutResponseActionConfigBean.DEFAULT_ACTION,
            configValues.get("conf.responseTimeoutActionConfig.action"));
    List<Map<String, Object>> statusActions =
            (List<Map<String, Object>>) configValues.get("conf.responseStatusActionConfigs");
    Assert.assertEquals(1, statusActions.size());

    Map<String, Object> defaultStatusAction = statusActions.get(0);
    assertEquals(HttpResponseActionConfigBean.DEFAULT_MAX_NUM_RETRIES, defaultStatusAction.get("maxNumRetries"));
    assertEquals(HttpResponseActionConfigBean.DEFAULT_BACKOFF_INTERVAL_MS, defaultStatusAction.get("backoffInterval"));
    assertEquals(HttpStatusResponseActionConfigBean.DEFAULT_ACTION, defaultStatusAction.get("action"));
    assertEquals(HttpStatusResponseActionConfigBean.DEFAULT_STATUS_CODE, defaultStatusAction.get("statusCode"));
  }

  @Test
  public void testV10ToV11() throws Exception {
    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 10, 11, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.client.useOAuth2"));
    Assert.assertEquals(false, configValues.get("conf.client.useOAuth2"));
  }

  @Test
  public void testV11ToV12() throws Exception {
    HttpClientSourceUpgrader upgrader = new HttpClientSourceUpgrader();
    upgrader.upgrade("a", "b", "c", 11, 12, configs);

    Map<String, Object> configValues = getConfigsAsMap(configs);
    assertTrue(configValues.containsKey("conf.pagination.keepAllFields"));
    Assert.assertEquals(false, configValues.get("conf.pagination.keepAllFields"));
  }

  @Test
  public void testV12ToV13() throws Exception {
    TlsConfigBeanUpgraderTestUtil.testHttpSslConfigBeanToTlsConfigBeanUpgrade("conf.client.",
        new HttpClientSourceUpgrader(),
        13
    );
  }

  @Test
  public void testV13ToV14() throws Exception {
    HttpClientSourceUpgrader httpClientSourceUpgrader = new HttpClientSourceUpgrader();
    httpClientSourceUpgrader.upgrade("lib", "stage", "inst", 13, 14, configs);

    UpgraderTestUtils.assertAllExist(configs,
        "conf.client.requestLoggingConfig.enableRequestLogging",
        "conf.client.requestLoggingConfig.logLevel",
        "conf.client.requestLoggingConfig.verbosity",
        "conf.client.requestLoggingConfig.maxEntitySize"
    );

    for (Config config : configs) {
      switch (config.getName()) {
        case "conf.client.requestLoggingConfig.enableRequestLogging":
          Assert.assertEquals(false, config.getValue());
          break;
        case "conf.client.requestLoggingConfig.logLevel":
          Assert.assertEquals(JulLogLevelChooserValues.DEFAULT_LEVEL, config.getValue());
          break;
        case "conf.client.requestLoggingConfig.verbosity":
          Assert.assertEquals(VerbosityChooserValues.DEFAULT_VERBOSITY, config.getValue());
          break;
        case "conf.client.requestLoggingConfig.maxEntitySize":
          Assert.assertEquals(RequestLoggingConfigBean.DEFAULT_MAX_ENTITY_SIZE, config.getValue());
          break;
        default:
          fail();
      }
    }
  }

  @Test
  public void testV14ToV15() {
    Mockito.doReturn(14).when(context).getFromVersion();
    Mockito.doReturn(15).when(context).getToVersion();

    String dataFormatPrefix = "conf.dataFormatConfig.";
    configs.add(new Config(dataFormatPrefix + "preserveRootElement", true));
    configs = upgrader.upgrade(configs, context);

    UpgraderTestUtils.assertExists(configs, dataFormatPrefix + "preserveRootElement", false);
  }

  @Test
  public void testV15ToV16() {
    Mockito.doReturn(15).when(context).getFromVersion();
    Mockito.doReturn(16).when(context).getToVersion();

    String dataFormatPrefix = "conf.client.";
    configs.add(new Config(dataFormatPrefix + "connectTimeoutMillis", 0));
    configs.add(new Config(dataFormatPrefix + "readTimeoutMillis", 0));
    configs = upgrader.upgrade(configs, context);

    UpgraderTestUtils.assertExists(configs, dataFormatPrefix + "connectTimeoutMillis", 250000);
    UpgraderTestUtils.assertExists(configs, dataFormatPrefix + "readTimeoutMillis", 30000);
  }

  @Test
  public void testV16ToV17() {
    Mockito.doReturn(16).when(context).getFromVersion();
    Mockito.doReturn(17).when(context).getToVersion();

    String dataFormatPrefix = "conf.";
    configs = upgrader.upgrade(configs, context);

    UpgraderTestUtils.assertExists(configs, dataFormatPrefix + "propagateAllHttpResponses", false);
    UpgraderTestUtils.assertExists(configs, dataFormatPrefix + "errorResponseField", "outErrorBody");
  }

  @Test
  public void testV17ToV18() {
    Mockito.doReturn(17).when(context).getFromVersion();
    Mockito.doReturn(18).when(context).getToVersion();

    String configPrefix = "conf.client.tlsConfig.";
    configs = upgrader.upgrade(configs, context);

    UpgraderTestUtils.assertExists(configs, configPrefix + "useRemoteKeyStore", false);
    UpgraderTestUtils.assertExists(configs, configPrefix + "privateKey", "");
    UpgraderTestUtils.assertExists(configs, configPrefix + "certificateChain", new ArrayList<>());
    UpgraderTestUtils.assertExists(configs, configPrefix + "trustedCertificates", new ArrayList<>());
  }

  private static Map<String, Object> getConfigsAsMap(List<Config> configs) {
    HashMap<String, Object> map = new HashMap<>();
    for (Config c : configs) {
      map.put(c.getName(), c.getValue());
    }
    return map;
  }
}
