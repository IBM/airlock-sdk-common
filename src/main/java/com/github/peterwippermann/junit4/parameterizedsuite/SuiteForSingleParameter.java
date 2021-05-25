package com.github.peterwippermann.junit4.parameterizedsuite;

import com.github.peterwippermann.junit4.parameterizedsuite.util.BlockJUnit4ClassRunnerWithParametersUtil;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;

/**
 * This extension of {@link Suite} is used by {@link com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite} to fork
 * a run of the suite's children for a single parameter.
 * 
 * @author Peter Wippermann
 *
 */
public final class SuiteForSingleParameter extends Suite {
	private String suiteName;
	private Object[] singleParameter;

	public SuiteForSingleParameter(RunnerBuilder runnerBuilder, Class<?> forkingSuiteClass, Class<?>[] classes,
			String suiteName, Object[] parameter) throws InitializationError {
		/*
		 * By passing "forkingSuiteClass" (which is the forking
		 * ParameterizedSuite), the JUnit framework will build the internal
		 * testClass attribute from the ParameterizedSuite and not from this
		 * virtual, forked Suite. This way @Before/After/Class can be evaluated.
		 */
		super(runnerBuilder, forkingSuiteClass, classes);

		this.suiteName = suiteName;
		this.singleParameter = parameter;
	}

	protected String getName() {
		return this.suiteName;
	}

	protected Statement classBlock(RunNotifier notifier) {
		return BlockJUnit4ClassRunnerWithParametersUtil.buildStatementWithTestRules(childrenInvoker(notifier),
				getTestClass(), getDescription(), singleParameter);
	}

    /**
     * @return The currently active parameter in a normalized form (i.e. an Array). 
     */
    protected Object[] getCurrentlyActiveParameter() {
    	return this.singleParameter;
    }
}
