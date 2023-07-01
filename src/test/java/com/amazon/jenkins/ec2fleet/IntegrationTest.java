package com.amazon.jenkins.ec2fleet;

import com.amazon.jenkins.ec2fleet.aws.CloudFormationApi;
import com.amazon.jenkins.ec2fleet.aws.EC2Api;
import com.amazon.jenkins.ec2fleet.aws.Registry;
import com.amazon.jenkins.ec2fleet.aws.fleet.EC2Fleet;
import com.amazon.jenkins.ec2fleet.aws.fleet.EC2Fleets;
import com.amazon.jenkins.ec2fleet.aws.fleet.EC2SpotFleet;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ActiveInstance;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotFleetRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.ModifySpotFleetRequestRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfig;
import com.amazonaws.services.ec2.model.SpotFleetRequestConfigData;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Base class for integration tests, which require mocked jenkins and utils
 * method to assert node states etc.
 */
@SuppressWarnings("WeakerAccess")
public abstract class IntegrationTest {

    protected static final long JOB_SLEEP_TIME = 30;

    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void after() {
        // restore
        Registry.setEc2Api(new EC2Api());
        Registry.setCloudFormationApi(new CloudFormationApi());
        EC2Fleets.setGet(null);
    }

    protected static void turnOffJenkinsTestTimout() {
        // zero is unlimited timeout
        System.setProperty("jenkins.test.timeout", "0");
    }

    protected static void setJenkinsTestTimoutTo720() {
        System.setProperty("jenkins.test.timeout", "720");
    }

    protected static void assertQueueAndNodesIdle(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertTrue(Queue.getInstance().isEmpty() && node.toComputer().isIdle());
            }
        });
    }

    protected static void assertQueueIsEmpty() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if queue is empty");
                Assert.assertTrue(Queue.getInstance().isEmpty());
            }
        });
    }

    protected void assertAtLeastOneNode() {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("check if non zero nodes!");
                Assert.assertFalse(j.jenkins.getNodes().isEmpty());
            }
        });
    }

    protected void assertLastBuildResult(final Result... lastBuildResults) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                final AbstractBuild lastBuild = j.jenkins.getAllItems(Project.class).get(0).getLastBuild();
                System.out.println("wait until " + Arrays.toString(lastBuildResults) + " current state "
                        + (lastBuild == null ? "<none>" : lastBuild.getResult()));
                Assert.assertNotNull(lastBuild);
                Assert.assertTrue(
                        lastBuild.getResult() + " should be in " + Arrays.toString(lastBuildResults),
                        Arrays.asList(lastBuildResults).contains(lastBuild.getResult()));
            }
        });
    }

    protected void assertNodeIsOnline(final Node node) {
        tryUntil(new Runnable() {
            @Override
            public void run() {
                System.out.println("wait when back online");
                Assert.assertTrue(node.toComputer().isOnline());
            }
        });
    }

    protected static void assertTasksDone(List<QueueTaskFuture> rs) {
        for (final QueueTaskFuture queueTaskFuture : rs) {
            Assert.assertTrue(queueTaskFuture.isDone());
        }
    }

    protected void triggerSuggestReviewNow() {
        triggerSuggestReviewNow("momo");
    }

    protected void triggerSuggestReviewNow(@Nullable final String labelString) {
        final Jenkins jenkins = j.jenkins;
        if (jenkins == null) throw new NullPointerException("No jenkins in j!");

        for (int i = 0; i < 5; i++) {
            if (labelString == null) jenkins.unlabeledNodeProvisioner.suggestReviewNow();
            else jenkins.getLabelAtom(labelString).nodeProvisioner.suggestReviewNow();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static void tryUntil(Runnable r) {
        tryUntil(r, TimeUnit.SECONDS.toMillis(60));
    }

    protected static void tryUntil(Runnable r, long time) {
        long d = TimeUnit.SECONDS.toMillis(5);
        final long start = System.currentTimeMillis();

        while (true) {
            try {
                r.run();
                return;
            } catch (AssertionError e) {
                if (System.currentTimeMillis() - start > time) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(d);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void waitZeroNodes() {
        System.out.println("wait until downscale happens");
        tryUntil(new Runnable() {
            @Override
            public void run() {
                Assert.assertThat(j.jenkins.getLabel("momo").getNodes().size(), Matchers.lessThanOrEqualTo(0));
            }
        }, TimeUnit.MINUTES.toMillis(3));
    }

    protected static void waitFirstStats(final EC2FleetCloud cloud) {
        System.out.println("waiting first stats for cloud");
        tryUntil(new Runnable() {
            @Override
            public void run() {
                // wait first update to speed up test during schedule
                Assert.assertNotNull(cloud.getStats());
            }
        });
    }

    protected void cancelTasks(List<QueueTaskFuture> rs) {
        for (QueueTaskFuture r : rs) {
            r.cancel(true);
        }
    }

    protected List<QueueTaskFuture> enqueTask(int count) throws IOException {
        return enqueTask(count, "momo", JOB_SLEEP_TIME);
    }

    protected List<QueueTaskFuture> enqueTask(int count, final long jobSleepTime) throws IOException {
        return enqueTask(count, "momo", jobSleepTime);
    }

    protected List<QueueTaskFuture> enqueTask(int count, final String labelString, final long jobSleepTime) throws IOException {
        final LabelAtom label = new LabelAtom(labelString);

        final List<QueueTaskFuture> rs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final FreeStyleProject project = j.createFreeStyleProject();
            project.setAssignedLabel(label);
            project.getBuildersList().add(Functions.isWindows() ? new BatchFile("Ping -n " + jobSleepTime + " 127.0.0.1 > nul") : new Shell("sleep " + jobSleepTime));
            rs.add(project.scheduleBuild2(0));
        }

        System.out.println(count + " tasks submitted for label " + labelString);
        return rs;
    }

    protected static Set<String> labelsToNames(Set<Label> labels) {
        Set<String> r = new HashSet<>();
        for (Label l : labels) r.add(l.getName());
        return r;
    }

    protected static Set<String> nodeToNames(Collection<Node> nodes) {
        Set<String> r = new HashSet<>();
        for (Node l : nodes) r.add(l.getNodeName());
        return r;
    }

    protected static void waitJobSuccessfulExecution(final List<QueueTaskFuture> tasks) {
        for (final QueueTaskFuture task : tasks) {
            try {
                Object o = task.get();
                Assert.assertEquals(((Run) o).getResult(), Result.SUCCESS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class AnswerWithDelay<T> implements Answer<T> {

        private final Answer<T> delegate;
        private final long delayMillis;

        public static <T> Answer<T> get(Answer<T> delegate, long delayMillis) {
            if (delayMillis == 0) {
                return delegate;
            } else {
                return new AnswerWithDelay<>(delegate, delayMillis);
            }
        }

        private AnswerWithDelay(Answer<T> delegate, long delayMillis) {
            this.delegate = delegate;
            this.delayMillis = delayMillis;
        }

        @Override
        public T answer(InvocationOnMock invocation) throws Throwable {
            Thread.sleep(delayMillis);
            return delegate.answer(invocation);
        }

    }

    protected void mockEc2FleetApiToEc2SpotFleet(final InstanceStateName instanceStateName) {
        mockEc2FleetApiToEc2SpotFleetWithDelay(instanceStateName, 0, 0);
    }

    protected void mockEc2FleetApiToEc2SpotFleet(final InstanceStateName instanceStateName, final int initialTargetCapacity) {
        mockEc2FleetApiToEc2SpotFleetWithDelay(instanceStateName, initialTargetCapacity, 0);
    }

    protected void mockEc2FleetApiToEc2SpotFleetWithDelay(final InstanceStateName instanceStateName, final long delayMillis) {
        mockEc2FleetApiToEc2SpotFleetWithDelay(instanceStateName, 0, delayMillis);
    }

    protected void mockEc2FleetApiToEc2SpotFleetWithDelay(final InstanceStateName instanceStateName, final int initialTargetCapacity, final long delayMillis) {
        EC2Api ec2Api = spy(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.terminateInstances(any(TerminateInstancesRequest.class)))
                .thenAnswer(AnswerWithDelay.get(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        return new TerminateInstancesResult();
                    }
                }, delayMillis));

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(AnswerWithDelay.get(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        DescribeInstancesRequest request = invocationOnMock.getArgument(0);

                        System.out.println("call describeInstances(" + request.getInstanceIds().size() + ")");
                        final List<Instance> instances = new ArrayList<>();
                        for (final String instanceId : request.getInstanceIds()) {
                            instances.add(new Instance()
                                    .withState(new InstanceState().withName(instanceStateName))
                                    .withPublicIpAddress("public-io")
                                    .withInstanceId(instanceId));
                        }

                        return new DescribeInstancesResult().withReservations(
                                new Reservation().withInstances(instances));
                    }
                }, delayMillis));

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        // force to use ec2 fleet
        EC2Fleets.setGet(new EC2SpotFleet());

        when(amazonEC2.modifySpotFleetRequest(any(ModifySpotFleetRequestRequest.class)))
                .then(AnswerWithDelay.get(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        ModifySpotFleetRequestRequest argument = invocationOnMock.getArgument(0);
                        System.out.println("modifySpotFleetRequest(targetCapacity = " + argument.getTargetCapacity() + ")");
                        targetCapacity.set(argument.getTargetCapacity());
                        return null;
                    }
                }, delayMillis));

        when(amazonEC2.describeSpotFleetInstances(any(DescribeSpotFleetInstancesRequest.class)))
                .then(AnswerWithDelay.get(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final List<ActiveInstance> activeInstances = new ArrayList<>();
                        final int size = targetCapacity.get();
                        for (int i = 0; i < size; i++) {
                            activeInstances.add(new ActiveInstance().withInstanceId("i-" + i));
                        }
                        return new DescribeSpotFleetInstancesResult().withActiveInstances(activeInstances);
                    }
                }, delayMillis));

        when(amazonEC2.describeSpotFleetRequests(any(DescribeSpotFleetRequestsRequest.class)))
                .thenAnswer(AnswerWithDelay.get(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        final DescribeSpotFleetRequestsRequest request = invocationOnMock.getArgument(0);

                        final List<SpotFleetRequestConfig> r = new ArrayList<>();
                        if (request.getSpotFleetRequestIds().size() > 0) {
                            for (String id : request.getSpotFleetRequestIds()) {
                                r.add(new SpotFleetRequestConfig()
                                        .withSpotFleetRequestId(id)
                                        .withSpotFleetRequestState("active")
                                        .withSpotFleetRequestConfig(
                                                new SpotFleetRequestConfigData()
                                                        .withTargetCapacity(targetCapacity.get())));
                            }
                        } else {
                            r.add(new SpotFleetRequestConfig()
                                    .withSpotFleetRequestId("some-id")
                                    .withSpotFleetRequestState("active")
                                    .withSpotFleetRequestConfig(
                                            new SpotFleetRequestConfigData()
                                                    .withTargetCapacity(targetCapacity.get())));
                        }
                        return new DescribeSpotFleetRequestsResult().withSpotFleetRequestConfigs(r);
                    }
                }, delayMillis));
    }

    protected AmazonCloudFormation mockCloudFormationApi() {
        CloudFormationApi cloudFormationApi = spy(CloudFormationApi.class);
        Registry.setCloudFormationApi(cloudFormationApi);

        AmazonCloudFormation amazonCloudFormation = mock(AmazonCloudFormation.class);
        when(cloudFormationApi.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonCloudFormation);

        final Object lock = new Object();
        final Map<String, CreateStackRequest> createStackRequests = new HashMap<>();

        when(amazonCloudFormation.createStack(any(CreateStackRequest.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                synchronized (lock) {
                    CreateStackRequest request = invocation.getArgument(0);
                    createStackRequests.put(request.getStackName(), request);
                }
                return new CreateStackResult();
            }
        });

        when(amazonCloudFormation.describeStacks(any(DescribeStacksRequest.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                final DescribeStacksResult result = new DescribeStacksResult();
                synchronized (lock) {
                    for (Map.Entry<String, CreateStackRequest> entry : createStackRequests.entrySet()) {
                        result.getStacks().add(new Stack()
                                .withStackId(entry.getValue().getStackName())
                                .withStackName(entry.getValue().getStackName())
                                .withStackStatus(StackStatus.CREATE_COMPLETE)
                                .withTags(entry.getValue().getTags())
                                .withOutputs(new Output().withOutputValue(entry.getValue().getStackName() + "-Id"))
                        );
                    }
                }
                return result;
            }
        });

        when(amazonCloudFormation.deleteStack(any(DeleteStackRequest.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                synchronized (lock) {
                    DeleteStackRequest request = invocation.getArgument(0);
                    Iterator<String> iterator = createStackRequests.keySet().iterator();
                    while (iterator.hasNext()) {
                        // since we delete by stack id and create stack name we need contains
                        if (request.getStackName().contains(iterator.next())) {
                            iterator.remove();
                            break;
                        }
                    }
                }
                return new DeleteStackResult();
            }
        });

        return amazonCloudFormation;
    }

    protected void mockEc2FleetApi() {
        EC2Api ec2Api = mock(EC2Api.class);
        Registry.setEc2Api(ec2Api);

        AmazonEC2 amazonEC2 = mock(AmazonEC2.class);
        when(ec2Api.connect(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn(amazonEC2);

        when(amazonEC2.describeInstances(any(DescribeInstancesRequest.class)))
                .then(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) {
                        return new DescribeInstancesResult().withReservations(new Reservation());
                    }
                });

        final AtomicInteger targetCapacity = new AtomicInteger(0);

        final EC2Fleet ec2Fleet = mock(EC2Fleet.class);
        EC2Fleets.setGet(ec2Fleet);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ModifySpotFleetRequestRequest argument = invocation.getArgument(4);
                targetCapacity.set(argument.getTargetCapacity());
                return null;
            }
        }).when(ec2Fleet).modify(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt(), anyInt());

        when(ec2Fleet.getState(anyString(), anyString(), nullable(String.class), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Set<String> instanceIds = new HashSet<>();
                final int size = targetCapacity.get();
                for (int i = 0; i < size; i++) {
                    instanceIds.add("i-" + i);
                }

                return new FleetStateStats("", 0,
                        new FleetStateStats.State(true, false, "active"),
                        instanceIds, Collections.<String, Double>emptyMap());
            }
        });
    }

    public static List<DomElement> getElementsByNameWithoutJdk(HtmlPage page, String name) {
        String jdkCheckUrl = "/jenkins/descriptorByName/hudson.model.JDK/checkName";
        List<DomElement> r = new ArrayList<>();
        for (DomElement domElement : page.getElementsByName(name)) {
            if (!jdkCheckUrl.equals(domElement.getAttribute("checkurl"))) {
                r.add(domElement);
            }
        }
        return r;
    }
}
