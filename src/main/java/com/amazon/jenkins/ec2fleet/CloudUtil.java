package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;

public class CloudUtil {
  public static Boolean isCloudNameUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }
}
