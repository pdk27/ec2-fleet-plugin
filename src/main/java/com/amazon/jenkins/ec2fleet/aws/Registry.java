package com.amazon.jenkins.ec2fleet.aws;

import com.amazon.jenkins.ec2fleet.EC2FleetCloud;

/**
 * Decouple plugin code from dependencies for easy testing. We cannot just make transient fields
 * in required classes as they usually restored by Jenkins without constructor call. Instead
 * we are using registry pattern.
 *
 * @see EC2FleetCloud
 */
@SuppressWarnings("WeakerAccess")
public class Registry {

    private static EC2Api ec2Api = new EC2Api();
    private static CloudFormationApi cloudFormationApi = new CloudFormationApi();

    public static void setEc2Api(EC2Api ec2Api) {
        Registry.ec2Api = ec2Api;
    }

    public static EC2Api getEc2Api() {
        return ec2Api;
    }

    public static CloudFormationApi getCloudFormationApi() {
        return cloudFormationApi;
    }

    public static void setCloudFormationApi(CloudFormationApi cloudFormationApi) {
        Registry.cloudFormationApi = cloudFormationApi;
    }

}
