package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class CloudUtilTest {
  @ClassRule
  public static BuildWatcher bw = new BuildWatcher();
  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Test
  public void getUniqueCloudName_noSuffix() {
    Assert.assertEquals("SomeDefaultName", CloudUtil.getUniqueCloudName("SomeDefaultName"));
  }

  @Test
  public void getUniqueCloudName_addsSuffix1() {
    Jenkins.get().clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertEquals("SomeDefaultName-1", CloudUtil.getUniqueCloudName("SomeDefaultName"));
  }

  @Test
  public void getUniqueCloudName_addsSuffixOnlyWhenNeeded() {
    Jenkins.get().clouds.add(new EC2FleetCloud("SomeDefaultName-1", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));
    Assert.assertEquals("SomeDefaultName", CloudUtil.getUniqueCloudName("SomeDefaultName"));

    Jenkins.get().clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));
    Assert.assertEquals("SomeDefaultName-2", CloudUtil.getUniqueCloudName("SomeDefaultName"));
  }

  @Test
  public void isCloudNameUniq_true() {
    Jenkins.get().clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertTrue(CloudUtil.isCloudNameUnique("TestCloud"));
  }

  @Test
  public void isCloudNameUniq_false() {
    Jenkins.get().clouds.add(new EC2FleetCloud("SomeDefaultName", null, null, null, null, null,
            "test-label", null, null, false, false,
            0, 0, 0, 0, 0, true, false,
            "-1", false, 0, 0, false,
            10, false));

    Assert.assertFalse(CloudUtil.isCloudNameUnique("SomeDefaultName"));
  }
}
