package com.amazon.jenkins.ec2fleet;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;

public class CloudUtil {
  public static String getUniqueCloudName(final String name) {
    final Set<String> jCloudNames = Jenkins.get().clouds != null
            ? Jenkins.get().clouds.stream().map(c -> c.name).collect(Collectors.toSet())
            : Collections.emptySet();

    String uniqName = name;
    int suffix = 1;
    while (jCloudNames.contains(name)) {
      uniqName = name + "-" + suffix++;
    }

    return uniqName;
  }

  public static Boolean isCloudNameUnique(final String name) {
    return !Jenkins.get().clouds.stream().anyMatch(c -> c.name.equals(name));
  }
}
