package org.cpsolver.instructor.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cpsolver.coursett.Constants;
import org.cpsolver.coursett.model.TimeLocation;
import org.cpsolver.coursett.preference.PreferenceCombination;
import org.cpsolver.coursett.preference.SumPreferenceCombination;
import org.cpsolver.ifs.assignment.Assignment;

/**
 * Teaching request. A set of sections of a course to be assigned to an instructor.
 * Each teaching request has a teaching load. The maximal teaching load of an instructor
 * cannot be breached.
 * 
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class TeachingRequest {
    private long iRequestId;
    private Course iCourse;
    private float iLoad;
    private List<Section> iSections = new ArrayList<Section>();
    private List<Preference<Attribute>> iAttributePreferences = new ArrayList<Preference<Attribute>>();
    private List<Preference<Instructor>> iInstructorPreferences = new ArrayList<Preference<Instructor>>();
    private Variable[] iVariables;

    /**
     * Constructor
     * @param requestId teaching request id
     * @param nrVariables number of instructors for this teaching request
     * @param course course
     * @param load teaching load
     * @param sections list of sections
     */
    public TeachingRequest(long requestId, int nrVariables, Course course, float load, Collection<Section> sections) {
        super();
        iRequestId = requestId;
        iCourse = course;
        iLoad = load;
        iSections.addAll(sections);
        iVariables = new Variable[nrVariables];
        for (int i = 0; i < nrVariables; i++)
            iVariables[i] = new Variable(i);
    }

    /**
     * Teaching request id that was provided in the constructor
     * @return request id
     */
    public long getRequestId() {
        return iRequestId;
    }
    
    /**
     * Get single instructor assignment variables
     * @return variables for this request
     */
    public Variable[] getVariables() {
        return iVariables;
    }
    
    /**
     * Get single instructor assignment variable
     * @param index index of the variable
     * @return variable for the index-th instructor assignment
     */
    public Variable getVariable(int index) {
        return iVariables[index];
    }
    
    /**
     * Get number of instructors needed
     * @return number of variables for this request
     */
    public int getNrInstructors() {
        return iVariables.length;
    }

    /**
     * Return attribute preferences for this request
     * @return attribute preferences
     */
    public List<Preference<Attribute>> getAttributePreferences() { return iAttributePreferences; }
    
    /**
     * Add attribute preference
     * @param pref attribute preference
     */
    public void addAttributePreference(Preference<Attribute> pref) { iAttributePreferences.add(pref); }
    
    /**
     * Compute attribute preference for the given instructor and attribute type
     * @param instructor an instructor
     * @param type an attribute type
     * @return combined preference using {@link Attribute.Type#isConjunctive()} and {@link Attribute.Type#isRequired()} properties
     */
    public int getAttributePreference(Instructor instructor, Attribute.Type type) {
        Set<Attribute> attributes = instructor.getAttributes(type);
        boolean hasReq = false, hasPref = false, needReq = false;
        PreferenceCombination ret = new SumPreferenceCombination();
        for (Preference<Attribute> pref: iAttributePreferences) {
            if (!type.equals(pref.getTarget().getType())) continue;
            if (pref.isRequired()) needReq = true;
            if (attributes.contains(pref.getTarget())) {
                if (pref.isProhibited()) return Constants.sPreferenceLevelProhibited;
                else if (pref.isRequired()) hasReq = true;
                else ret.addPreferenceInt(pref.getPreference());
                hasPref = true;
            } else {
                if (pref.isRequired() && type.isConjunctive()) return Constants.sPreferenceLevelProhibited;
            }
        }
        if (needReq && !hasReq) return Constants.sPreferenceLevelProhibited;
        if (type.isRequired() && !hasPref) return Constants.sPreferenceLevelProhibited;
        return ret.getPreferenceInt();
    }
    
    /**
     * Compute attribute preference for the given instructor
     * @param instructor an instructor
     * @return using {@link SumPreferenceCombination} for the preferences of each attribute type (using {@link TeachingRequest#getAttributePreference(Instructor, org.cpsolver.instructor.model.Attribute.Type)})
     */
    public PreferenceCombination getAttributePreference(Instructor instructor) {
        PreferenceCombination preference = new SumPreferenceCombination();
        for (Attribute.Type type: ((InstructorSchedulingModel)getVariables()[0].getModel()).getAttributeTypes())
            preference.addPreferenceInt(getAttributePreference(instructor, type));
        return preference;
    }

    /**
     * Return instructor preferences for this request
     * @return instructor preferences
     */
    public List<Preference<Instructor>> getInstructorPreferences() { return iInstructorPreferences; }
    
    /**
     * Add instructor preference
     * @param pref instructor preference
     */
    public void addInstructorPreference(Preference<Instructor> pref) { iInstructorPreferences.add(pref); }
    
    /**
     * Return instructor preference for the given instructor
     * @param instructor an instructor
     * @return instructor preference for the given instructor
     */
    public Preference<Instructor> getInstructorPreference(Instructor instructor) {
        boolean hasRequired = false;
        for (Preference<Instructor> pref: iInstructorPreferences)
            if (pref.isRequired()) { hasRequired = true; break; }
        for (Preference<Instructor> pref: iInstructorPreferences)
            if (pref.getTarget().equals(instructor)) {
                if (hasRequired && !pref.isRequired()) continue;
                return pref;
            }
        if (hasRequired)
            return new Preference<Instructor>(instructor, Constants.sPreferenceLevelProhibited);
        return new Preference<Instructor>(instructor, Constants.sPreferenceLevelNeutral);
    }
    
    /**
     * Course of the request that was provided in the constructor
     * @return course of the request
     */
    public Course getCourse() {
        return iCourse;
    }

    /**
     * Sections of the request that was provided in the constructor
     * @return sections of the request
     */
    public List<Section> getSections() { return iSections; }

    /**
     * Return teaching load of the request
     * @return teaching load
     */
    public float getLoad() { return iLoad; }
    
    /**
     * Set teaching load of the request
     * @param load teaching load
     */
    public void setLoad(float load) { iLoad = load; }

    @Override
    public String toString() {
        return iCourse.getCourseName() + " " + getSections();
    }
    
    /**
     * Check if the given request fully share the common sections with this request  
     * @param request the other teaching request
     * @return true, if all common sections of this request are also present in the other request
     */
    public boolean sameCommon(TeachingRequest request) {
        for (Section section: getSections())
            if (section.isCommon() && !request.getSections().contains(section))
                return false;
        return true;
    }
    
    /**
     * Count the number of common sections that the given request share with this request
     * @param request the other teaching request
     * @return the number of shared common sections
     */
    public double nrSameLectures(TeachingRequest request) {
        if (!sameCourse(request)) return 0.0;
        double same = 0; int common = 0;
        for (Section section: getSections())
            if (section.isCommon()) {
                common ++;
                if (request.getSections().contains(section)) same++;
            }
        return (common == 0 ? 1.0 : same / common);
    }

    /**
     * Check if this request and the given request are of the same course
     * @param request the other teaching request
     * @return true, if the course of the given request is the same as the course of this request
     */
    public boolean sameCourse(TeachingRequest request) {
        return getCourse().equals(request.getCourse());
    }

    /**
     * Check if this request overlaps with the given one
     * @param request the other teaching request
     * @return true, if there are two sections that are overlapping in time (that are not allowed to overlap)
     */
    public boolean overlaps(TeachingRequest request) {
        for (Section section: getSections()) {
            if (section.isAllowOverlap() || section.getTime() == null || request.getSections().contains(section)) continue;
            for (Section other: request.getSections()) {
                if (other.isAllowOverlap() || other.getTime() == null || getSections().contains(other)) continue;
                if (section.getTime().hasIntersection(other.getTime())) return true;
            }
        }
        return false;
    }
    
    /**
     * Count the number of (allowed) overlapping time slots between this request and the given one
     * @param request the other teaching request
     * @return the number of overlapping time slots
     */
    public int share(TeachingRequest request) {
        int ret = 0;
        for (Section section: getSections())
            ret += section.share(request.getSections());
        return ret;
    }
    
    /**
     * Count the number of overlapping time slots between this request and the given time
     * @param time a time
     * @return the number of overlapping time slots
     */
    public int share(TimeLocation time) {
        int ret = 0;
        for (Section section: getSections())
            ret += section.share(time);
        return ret;
    }

    /**
     * Average value of the back-to-backs between this request and the given one
     * @param request the other teaching request
     * @param diffRoomWeight different room penalty
     * @param diffTypeWeight different instructional type penalty
     * @return average value of {@link Section#countBackToBacks(Collection, double, double)} between the two, common sections are ignored
     */
    public double countBackToBacks(TeachingRequest request, double diffRoomWeight, double diffTypeWeight) {
        double b2b = 0.0;
        int count = 0;
        for (Section section: getSections()) {
            if (!section.isCommon() || !sameCourse(request) || !request.getSections().contains(section)) {
                b2b += section.countBackToBacks(request.getSections(), diffRoomWeight, diffTypeWeight);
                count ++;
            }
        }
        return (count == 0 ? 0.0 : b2b / count);
    }
    
    @Override
    public int hashCode() {
        return (int)(iRequestId ^ (iRequestId >>> 32));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof TeachingRequest)) return false;
        TeachingRequest tr = (TeachingRequest)o;
        return getRequestId() == tr.getRequestId();
    }
    
    /**
     * Single instructor assignment to this teaching request
     */
    public class Variable extends org.cpsolver.ifs.model.Variable<TeachingRequest.Variable, TeachingAssignment> {
        private int iIndex;
        
        /**
         * Constructor 
         * @param index instructor index (if a class can be taught by multiple instructors, the index identifies the particular request)
         */
        public Variable(int index) {
            iId = (iRequestId << 8) + index;
            iIndex = index;
        }

        /**
         * Instructor index that was provided in the constructor
         * @return instructor index
         */
        public int getInstructorIndex() {
            return iIndex;
        }
        
        /**
         * Teaching request for this variable
         * @return teaching request
         */
        public TeachingRequest getRequest() {
            return TeachingRequest.this;
        }
        
        /**
         * Course of the request that was provided in the constructor
         * @return course of the request
         */
        public Course getCourse() {
            return iCourse;
        }
        
        /**
         * Sections of the request that was provided in the constructor
         * @return sections of the request
         */
        public List<Section> getSections() { return iSections; }
        
        @Override
        public List<TeachingAssignment> values(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment) {
            List<TeachingAssignment> values = super.values(assignment);
            if (values == null) {
                values = new ArrayList<TeachingAssignment>();
                for (Instructor instructor: ((InstructorSchedulingModel)getModel()).getInstructors()) {
                    if (instructor.canTeach(getRequest())) {
                        PreferenceCombination attributePref = getAttributePreference(instructor);
                        if (attributePref.isProhibited()) continue;
                        values.add(new TeachingAssignment(this, instructor, attributePref.getPreferenceInt()));
                    }
                }
                setValues(values);
            }
            return values;
        }
        
        @Override
        public void variableAssigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, long iteration, TeachingAssignment ta) {
            super.variableAssigned(assignment, iteration, ta);
            ta.getInstructor().getContext(assignment).assigned(assignment, ta);
        }

        @Override
        public void variableUnassigned(Assignment<TeachingRequest.Variable, TeachingAssignment> assignment, long iteration, TeachingAssignment ta) {
            super.variableUnassigned(assignment, iteration, ta);
            ta.getInstructor().getContext(assignment).unassigned(assignment, ta);
        }
        
        @Override
        public int hashCode() {
            return new Long(iRequestId << 8 + iIndex).hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Variable)) return false;
            Variable tr = (Variable)o;
            return getRequest().getRequestId() == tr.getRequest().getRequestId() && getInstructorIndex() == tr.getInstructorIndex();
        }
        
        @Override
        public String getName() {
            return iCourse.getCourseName() + (getNrInstructors() > 1 ? "[" + getInstructorIndex() + "]" : "") + " " + getSections();
        }
    }
}
