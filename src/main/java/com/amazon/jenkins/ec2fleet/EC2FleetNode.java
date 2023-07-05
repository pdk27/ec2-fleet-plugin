package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Failure;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * The {@link EC2FleetNode} represents an agent running on an EC2 instance, responsible for creating {@link EC2FleetNodeComputer}.
 */
public class EC2FleetNode extends Slave implements EphemeralNode {
    private static final Logger LOGGER = Logger.getLogger(EC2FleetNode.class.getName());

    private String cloudName;
    private String instanceId;
    private final int maxTotalUses;
    private int usesRemaining;

    public EC2FleetNode(final String instanceId, final String nodeDescription, final String remoteFS, final int numExecutors,
                        final Mode mode, final String label, final RetentionStrategy retentionStrategy,
                        final List<? extends NodeProperty<?>> nodeProperties, final String cloudName, ComputerLauncher launcher,
                        final int maxTotalUses) throws IOException, Descriptor.FormException {
        //noinspection deprecation
//        System.out.println(super.getLabelString());
        super(instanceId, nodeDescription, remoteFS, numExecutors, mode, label,
                launcher, retentionStrategy, nodeProperties);
        this.cloudName = cloudName;
        this.maxTotalUses = maxTotalUses;
        this.usesRemaining = maxTotalUses;

        LOGGER.fine("*** In EC2FleetNode constructor for " + getDisplayName() + ", " + this);
        LOGGER.fine("cloud: " + getCloud());
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public int getMaxTotalUses() {
        return this.maxTotalUses;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    public void decrementUsesRemaining() {
        this.usesRemaining--;
    }

    @Override
    public Node asNode() {
        return this;
    }

    // pdk: for backwards compatibility - OR release with a new major version
    // pdk: compare
    @Override
    public String getNodeName() {
        return instanceId;
    }

    // pdk: compare
    @Override
    public String getDisplayName() {
        final String name = String.format("%s %s", cloudName, instanceId);
        try {
            Jenkins.checkGoodName(name);
            return name;
        } catch (Failure e) {
            return instanceId;
        }

        // in some multi-thread edge cases cloud could be null for some time, just be ok with that // pdk ?? remove??
//        return (cloudName == "" ? "unknown fleet" : cloudName) + " " + name;
    }

    @Override
    public Computer createComputer() {
        return new EC2FleetNodeComputer(this);
    }

    public AbstractEC2FleetCloud getCloud() {
        return (AbstractEC2FleetCloud) Jenkins.get().getCloud(cloudName); // *** should return latest cloud rather that replying on state
    }

    @Override
    public Node reconfigure(final StaplerRequest req, JSONObject form) throws FormException {
        LOGGER.fine("In EC2FleetNode reconfigure " + this);
        if (form == null) {
            return null;
        }

        EC2FleetNode result = (EC2FleetNode) super.reconfigure(req, form);

        if (result != null) {
//            /* Get rid of the old tags, as represented by ourselves. */
//            clearLiveInstancedata();
//
//            /* Set the new tags, as represented by our successor */
//            result.pushLiveInstancedata();
            return result;
        }
        return null;
//
//        if (!isAlive(true)) {
//            LOGGER.info("EC2 instance terminated externally: " + getInstanceId());
//            try {
//                Jenkins.get().removeNode(this);
//            } catch (IOException ioe) {
//                LOGGER.log(Level.WARNING, "Attempt to reconfigure EC2 instance which has been externally terminated: "
//                        + getInstanceId(), ioe);
//            }

//            return null;
//        }

//        return super.reconfigure(req, form);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
//        public DescriptorImpl() {
//            super();
//        }

        public String getDisplayName() {
            return "Fleet Agent";
        }

        /**
         * We only create this kind of nodes programmatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }
}
