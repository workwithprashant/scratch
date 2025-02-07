package com.example.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Flips @Test(enabled=false) to enabled=true so the test
 * will be discovered by TestNG, then forcibly skipped by another listener.
 */
public class DisabledToSkippedTransformer implements IAnnotationTransformer {

    private static final String DISABLED_GROUP = "wasDisabled";

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Check if the test is disabled
        if (!annotation.getEnabled()) {
            // Flip it to enabled
            annotation.setEnabled(true);

            // Add a special group so we know it was originally disabled
            String[] existingGroups = annotation.getGroups();
            String[] newGroups = appendGroup(existingGroups, DISABLED_GROUP);
            annotation.setGroups(newGroups);
        }
    }

    /**
     * Helper to append an extra group to the existing groups array.
     */
    private String[] appendGroup(String[] original, String extra) {
        List<String> list = new ArrayList<>(Arrays.asList(original));
        list.add(extra);
        return list.toArray(new String[0]);
    }
}






package com.example.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

/**
 * Listens to test invocation. If the test was originally disabled
 * (detected by group "wasDisabled"), we force a skip so Allure sees it.
 */
public class ForceSkipListener implements IInvokedMethodListener {

    private static final String DISABLED_GROUP = "wasDisabled";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String[] groups = method.getTestMethod().getGroups();
        for (String g : groups) {
            if (DISABLED_GROUP.equals(g)) {
                // Force skip with a message so Allure sees it as "skipped"
                throw new SkipException("Originally @Test(enabled=false); forcing skip for Allure reporting.");
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        // no-op
    }
}
