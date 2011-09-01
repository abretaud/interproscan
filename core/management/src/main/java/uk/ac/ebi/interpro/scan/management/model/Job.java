package uk.ac.ebi.interpro.scan.management.model;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class describes a Job, which is constructed from N steps.
 * Jobs and Steps are templates for analyses.  To actually run
 * analyses against specific proteins (and perhaps specific models)
 * StepInstances are instantiated.  These instances are then
 * run as StepExecutions.  If a StepExecution fails, and the
 * Step is configured to be repeatable, then another attempt
 * to run the instance will be made.
 *
 * NOTE: Instances of Jobs and Steps are defined in Spring XML.  They
 * are NOT persisted to the database - only StepInstances and StepExecutions
 * are persisted.
 *
 * @author Phil Jones
 * @version $Id$
 * @since 1.0-SNAPSHOT
 */
public class Job implements Serializable, BeanNameAware {

    private String id;

    private boolean analysis = false;

    private String description;

    private Map<String,String> mandatoryParameters;

    /**
     * List of steps.  this is transient so they don't all get shoved
     * over the wire when each StepExecution is run.
     */
    private transient List<Step> steps = new ArrayList<Step>();

    public Job() {
    }


    public String getDescription() {
        return description;
    }

    /**
     * A descriptive name for this job.
     * @param description a descriptive (and preferably unique)
     * name for this job.
     */
    @Required
    public void setDescription(String description) {
        this.description = description;
    }

    @Required
    public void setAnalysis(boolean isAnalysis){
        this.analysis = isAnalysis;
    }

    public boolean isAnalysis() {
        return analysis;
    }

    public List<Step> getSteps() {
        return steps;
    }

    void addStep(Step step) {
        steps.add(step);
    }

    public String getId() {
        return id;
    }

    public void setBeanName(String id) {
        this.id = id;
    }

    public Map<String,String> getMandatoryParameters() {
        return mandatoryParameters;
    }

    public void setMandatoryParameters(Map<String,String> mandatoryParameters) {
        this.mandatoryParameters = mandatoryParameters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Job");
        sb.append("{id='").append(id).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", steps=").append(steps);
        sb.append('}');
        return sb.toString();
    }
}
