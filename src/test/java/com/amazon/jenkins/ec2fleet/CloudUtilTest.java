package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CloudUtilTest {
  @Rule
  public JenkinsRule j = new JenkinsRule();

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

  @Test
  public void getUniqDefaultCloudName_noSuffix() {
    final List existingCloudNames = Collections.emptyList();
    Assert.assertEquals("FleetCloud", CloudUtil.getUniqDefaultCloudName(existingCloudNames, "FleetCloud"));
  }

  @Test
  public void getUniqDefaultCloudName_addsSuffixOnlyWhenNeeded() {
    final List existingCloudNames = Arrays.asList("FleetCloud-1");
    Assert.assertEquals("FleetCloud", CloudUtil.getUniqDefaultCloudName(existingCloudNames, "FleetCloud"));
  }

  @Test
  public void getUniqDefaultCloudName_addsSuffixCorrectly() {
    final List existingCloudNames = Arrays.asList("FleetCloud", "FleetCloud-1");
    Assert.assertEquals("FleetCloud-2", CloudUtil.getUniqDefaultCloudName(existingCloudNames, "FleetCloud"));
  }

}
