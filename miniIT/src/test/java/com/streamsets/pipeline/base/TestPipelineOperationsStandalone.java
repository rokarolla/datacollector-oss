/**
 * (c) 2015 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.base;

import com.streamsets.datacollector.MiniSDC;
import com.streamsets.pipeline.MiniSDCTestingUtility;
import com.streamsets.pipeline.util.VerifyUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * The pipeline and data provider for this test case should make sure that the pipeline runs continuously and the origin
 *  in the pipeline keeps producing records until stopped.
 *
 * To test destinations in the pipeline, use RandomSource which produces continuous data with a delay of 1 second.
 * To test origin, make sure that the data source origin reads from has enough data for the pipeline to keep running.
 * For example to test kafka Origin, a background thread could keep writing to the kafka topic from which the
 * kafka origin reads.
 *
 */
public abstract class TestPipelineOperationsStandalone extends TestPipelineOperationsBase {

  protected static MiniSDCTestingUtility miniSDCTestingUtility;
  protected static URI serverURI;
  protected static MiniSDC miniSDC;


  @Override
  protected List<URI> getWorkerURI() {
    return Collections.EMPTY_LIST;
  }

  @Override
  protected URI getServerURI() {
    return serverURI;
  }

  protected abstract void postPipelineStart();

  /**
   * The extending test must call this method in the method scheduled to run before class
   * @throws Exception
   */
  public static void beforeClass(String pipelineJson) throws Exception {
    System.setProperty("sdc.testing-mode", "true");
    miniSDCTestingUtility = new MiniSDCTestingUtility();
    miniSDC = miniSDCTestingUtility.createMiniSDC(MiniSDC.ExecutionMode.STANDALONE);
    miniSDC.startSDC();
    serverURI = miniSDC.getServerURI();
    miniSDC.createPipeline(pipelineJson);
  }

  /**
   * The extending test must call this method in the method scheduled to run after class
   * @throws Exception
   */
  public static void afterClass() throws Exception {
    miniSDCTestingUtility.stopMiniSDC();
  }

  @Before
  public void setUp() throws Exception {
    miniSDC.startPipeline();
    postPipelineStart();
    Thread.sleep(2000);
  }

  @After
  public void tearDown() throws Exception {
    if ("RUNNING".equals(VerifyUtils.getPipelineState(serverURI))) {
      miniSDC.stopPipeline();
      VerifyUtils.waitForPipelineToStop(serverURI);
    }
  }

  protected  boolean clusterModeTest() {
    return false;
  }

  /**********************************************************/
  /************ Standalone mode specific tests **************/
  /**********************************************************/

  //The following tests can be bumped up to the Base class to be available for cluster mode once we have a way to
  // detect that the pipeline is running in the worker nodes.
  //As of now even though the state says RUNNING the worker nodes may not have started processing the data.

  @Test
  public void testRestartAndHistory() throws Exception {

    VerifyUtils.stopPipeline(serverURI);
    VerifyUtils.waitForPipelineToStop(serverURI);
    Assert.assertEquals("STOPPED", VerifyUtils.getPipelineState(serverURI));

    //clear history
    VerifyUtils.deleteHistory(serverURI, getPipelineName(), getPipelineRev());
    List<Map<String, Object>> history = VerifyUtils.getHistory(serverURI, getPipelineName(), getPipelineRev());
    Assert.assertEquals(0, history.size());

    //fresh start
    VerifyUtils.startPipeline(serverURI, getPipelineName(), getPipelineRev());
    Assert.assertEquals("RUNNING", VerifyUtils.getPipelineState(serverURI));
    VerifyUtils.stopPipeline(serverURI);
    VerifyUtils.waitForPipelineToStop(serverURI);
    Assert.assertEquals("STOPPED", VerifyUtils.getPipelineState(serverURI));

    VerifyUtils.startPipeline(serverURI, getPipelineName(), getPipelineRev());
    Assert.assertEquals("RUNNING", VerifyUtils.getPipelineState(serverURI));
    VerifyUtils.stopPipeline(serverURI);
    VerifyUtils.waitForPipelineToStop(serverURI);
    Assert.assertEquals("STOPPED", VerifyUtils.getPipelineState(serverURI));

    history = VerifyUtils.getHistory(serverURI, getPipelineName(), getPipelineRev());
    Assert.assertEquals(4, history.size());

    //The pipeline must be running at the end of the test
    VerifyUtils.startPipeline(serverURI, getPipelineName(), getPipelineRev());
  }

  @Test
  public void testPreview() throws IOException, InterruptedException {
    URI serverURI = getServerURI();
    Assert.assertEquals("RUNNING", VerifyUtils.getPipelineState(serverURI));
    VerifyUtils.stopPipeline(serverURI);
    VerifyUtils.waitForPipelineToStop(serverURI);
    Assert.assertEquals("STOPPED", VerifyUtils.getPipelineState(serverURI));

    Map<String, Object> preview = VerifyUtils.preview(serverURI, getPipelineName(), getPipelineRev());
    Object batchesOutput = preview.get("batchesOutput");
    Assert.assertTrue(batchesOutput instanceof List);
    //This is list list of StageOuts
    List<?> batchesOutputList = (List<?>) batchesOutput;
    for (Object listOfStageOutput : batchesOutputList) {
      List<Map<String, Object>> stageOutputs = (List<Map<String, Object>>) listOfStageOutput;
      for (Map<String, Object> stageOutput : stageOutputs) {
        Map<String, Object> output = (Map<String, Object>) stageOutput.get("output");
        //output is map of list, lane vs records
        for (Map.Entry<String, Object> e : output.entrySet()) {
          Assert.assertTrue(e.getValue() instanceof List);
          Assert.assertTrue(((List<?>) e.getValue()).size() > 0);
          //This is the list of records
          List<Map<String, Object>> records = (List<Map<String, Object>>) e.getValue();
          for (Map<String, Object> record : records) {
            //each record has header and value
            Map<String, Object> val = (Map<String, Object>) record.get("value");
            Assert.assertNotNull(val);
            //value has root field with path "", and Map with key "text" for the text field
            Assert.assertTrue(val.containsKey("value"));
            Map<String, Map<String, String>> value = (Map<String, Map<String, String>>) val.get("value");
            Assert.assertNotNull(value);
            //The text field in the record [/text]
            if(value.containsKey("text")) {
              //Kafka origin pipelines generate record with text data.
              //Additional tests for those
              Map<String, String> text = value.get("text");
              Assert.assertNotNull(text);
              //Field has type, path and value
              Assert.assertTrue(text.containsKey("value"));
              Assert.assertEquals("Hello Kafka", text.get("value"));
              Assert.assertTrue(text.containsKey("path"));
              Assert.assertEquals("/text", text.get("path"));
              Assert.assertTrue(text.containsKey("type"));
              Assert.assertEquals("STRING", text.get("type"));
            }
          }
        }
      }
    }

    VerifyUtils.startPipeline(serverURI, getPipelineName(), getPipelineRev());
  }
}
