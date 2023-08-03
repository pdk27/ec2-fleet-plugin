package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.fleet.EC2Fleets;
import com.amazonaws.services.ec2.AmazonEC2;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudUtilTest {
  @ClassRule
  public static BuildWatcher bw = new BuildWatcher();
  @Rule
  public JenkinsRule j = new JenkinsRule();

  @Before
  public void before() {
    final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
    EC2Fleets.setGet(ec2Fleet);
    final EC2Api ec2Api = mock(EC2Api.class);
    Registry.setEc2Api(ec2Api);
    final AmazonEC2 amazonEC2 = mock(AmazonEC2.class);

    when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString()))
            .thenReturn(new FleetStateStats("", 2, FleetStateStats.State.active(), new HashSet<>(Arrays.asList("i-1", "i-2")), Collections.emptyMap()));
    when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);
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
