package com.amazon.jenkins.ec2fleet;

import jenkins.model.Jenkins;
import java.util.List;

public class CloudUtil {

  public static Boolean isCloudNameUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }

  public static String getUniqDefaultCloudName(final List<String> existingCloudNames, final String defaultCloudName) {
    String uniqName = defaultCloudName;
    int suffix = 1;
    while (existingCloudNames.contains(uniqName)) {
      uniqName = defaultCloudName + "-" + suffix++;
    }

    return uniqName;
  }
}
