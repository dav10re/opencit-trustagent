/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.setup.cmd;

import com.intel.mtwilson.datatypes.PcrIndex;
import com.intel.dcg.console.Command;
import com.intel.mtwilson.setup.SetupContext;
import com.intel.mtwilson.validation.Fault;
import com.intel.mtwilson.validation.InvalidModelException;
import com.intel.mtwilson.validation.Model;
import com.intel.mtwilson.validation.Unchecked;
import java.util.List;
import org.apache.commons.configuration.Configuration;

/**
 *
 * @author jbuhacoff
 */
public class TestDataValidation implements Command {
    private SetupContext ctx = null;

//    @Override
    public void setContext(SetupContext ctx) {
        this.ctx = ctx;
    }
    
    private Configuration options = null;
    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }


    @Override
    public void execute(String[] args) throws Exception {
        PcrIndex pcr = new PcrIndex(0);
            PcrIndex invalidPcr = new PcrIndex(-1);
        
        // test methods that accept a single model parameter - works
        test(pcr);
        testUnchecked(pcr);

        try {
            test(invalidPcr);
        }
        catch(InvalidModelException e) {
            // good
            System.out.println("  ... caught exception: "+e.getMessage());
            printFaults(e.getModel());            
        }
        
        testUnchecked(invalidPcr);
        
        // now try methods with multiple parameters
        test(pcr, pcr); // two valid pcr's should be ok
        try {
            test(pcr, invalidPcr); // should throw an exception about the second one
        }
        catch(InvalidModelException e) {
            // good
            System.out.println("  ... caught exception: "+e.getMessage());
            printFaults(e.getModel());
        }
        
        testMixed(pcr, invalidPcr); // should be ok because second parameter is unchecked
        try {
            testMixed(invalidPcr, invalidPcr); // should throw an exception about the first one
        }
        catch(InvalidModelException e) {
            // good
            System.out.println("  ... caught exception: "+e.getMessage());
            printFaults(e.getModel());
        }
        
    }
    
    private void printFaults(@Unchecked Model model) {
        System.out.println("Invalid "+model.getClass().getSimpleName());
        List<Fault> faults = model.getFaults();
        for(Fault fault : faults) {
            System.out.println("- "+fault.toString());
        }
    }
    
    private void test(PcrIndex pcr) {
        System.out.println(String.format("Test1 PCR %d valid? %b", pcr.toInteger(), pcr.isValid()));
    }

    private void test(PcrIndex pcr1, PcrIndex pcr2) {
        System.out.println(String.format("Test2 PCR1 %d valid %b PCR2 %d valid %b", pcr1.toInteger(), pcr1.isValid(), pcr2.toInteger(), pcr2.isValid()));
    }
    
    private void testUnchecked(@Unchecked PcrIndex pcr) {
        System.out.println(String.format("Test Unchecked PCR %d valid? %b", pcr.toInteger(), pcr.isValid()));
    }
    
    private void testMixed(PcrIndex pcr1, @Unchecked PcrIndex pcr2) {
        System.out.println(String.format("Test Unchecked PCR1 %d valid %b PCR2 %d valid %b", pcr1.toInteger(), pcr1.isValid(), pcr2.toInteger(), pcr2.isValid()));
    }
    
}
