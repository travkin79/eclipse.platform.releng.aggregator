/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.eval;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import junit.framework.Assert;

import org.eclipse.test.internal.performance.Dimensions;
import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.data.PerformanceDataModel;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * @since 3.1
 */
public class Evaluator implements IEvaluator {

//	private static final String PLUGIN_ID= JdtTextTestPlugin.PLUGIN_ID;
//	private static final String DRIVER_OPTION= "/option/driver"; //$NON-NLS-1$
//	private static final String TIMESTAMP_OPTION= "/option/timestamp"; //$NON-NLS-1$
//	
//	private static final String DRIVER_SYSTEM_PROPERTY= "eclipse.performance.reference.driver"; //$NON-NLS-1$
//	private static final String TIMESTAMP_SYSTEM_PROPERTY= "eclipse.performance.reference.timestamp"; //$NON-NLS-1$
	
	private static IEvaluator fgDefaultEvaluator;
	
	private Map fFilterProperties;
	private AssertChecker[] fCheckers;

	public void evaluate(PerformanceMeter performanceMeter) {
		Sample session= ((InternalPerformanceMeter) performanceMeter).getSample();
		Assert.assertTrue("metering session is null", session != null); //$NON-NLS-1$
		Sample reference= getReferenceSession(session.getProperty(InternalPerformanceMeter.TESTNAME_PROPERTY), session.getProperty(InternalPerformanceMeter.HOSTNAME_PROPERTY));
		Assert.assertTrue("reference metering session is null", reference != null); //$NON-NLS-1$
		
		StatisticsSession referenceStats= new StatisticsSession(reference.getDataPoints());
		StatisticsSession measuredStats= new StatisticsSession(session.getDataPoints());
		
		StringBuffer failMesg= new StringBuffer("Performance criteria not met when compared to driver '" + reference.getProperty(InternalPerformanceMeter.DRIVER_PROPERTY) + "' from " + reference.getProperty(InternalPerformanceMeter.RUN_TS_PROPERTY) + ":"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		boolean pass= true;
		for (int i= 0; i < fCheckers.length; i++) {
			pass &= fCheckers[i].test(referenceStats, measuredStats, failMesg);
		}
		
		Assert.assertTrue(failMesg.toString(), pass);
	}

	private Sample getReferenceSession(String testname, String hostname) {
		String specificHost= (String) fFilterProperties.get(InternalPerformanceMeter.HOSTNAME_PROPERTY);
		boolean useSessionsHostname= true;
		if (specificHost != null)
			useSessionsHostname= false;
		
		PerformanceDataModel model= getDataModel();
		Sample[] sessions= model.getMeteringSessions();
		for (int i= 0; i < sessions.length; i++) {
			Sample session= sessions[i];
			boolean match= true;
			
			// check filter properties
			for (Iterator it= fFilterProperties.keySet().iterator(); match && it.hasNext();) {
				String property= (String) it.next();
				String value= session.getProperty(property);
				String filterValue= (String) fFilterProperties.get(property);
				if (!value.equals(filterValue))
					match= false;
			}
			// check properties by specific hostname
			if (match 
					&& testname.equals(session.getProperty(InternalPerformanceMeter.TESTNAME_PROPERTY))
					&& (!useSessionsHostname || hostname.equals(session.getProperty(InternalPerformanceMeter.HOSTNAME_PROPERTY))))
				return session;
		}
		return null;
	}

	protected PerformanceDataModel getDataModel() {
		return PerformanceDataModel.getInstance(null);
	}

	public void setAssertCheckers(AssertChecker[] asserts) {
		fCheckers= asserts;
	}

	public void setReferenceFilterProperties(String driver, String timestamp) {
		fFilterProperties= new HashMap();
		Assert.assertNotNull(driver); // must specify driver;
		fFilterProperties.put(InternalPerformanceMeter.DRIVER_PROPERTY, driver);
		if (timestamp != null)
			fFilterProperties.put(InternalPerformanceMeter.RUN_TS_PROPERTY, timestamp);
	}
	
	public static synchronized IEvaluator getDefaultEvaluator() {
		if (fgDefaultEvaluator == null) {
			fgDefaultEvaluator= new DBEvaluator();
			
//			fgDefaultEvaluator= new Evaluator();
//			
//			String driver= System.getProperty(DRIVER_SYSTEM_PROPERTY);
//			if (driver == null)
//				driver= Platform.getDebugOption(PLUGIN_ID + DRIVER_OPTION);
//			if (driver == null)
//				driver= "3.0"; //$NON-NLS-1$
//			String timestamp= System.getProperty(TIMESTAMP_SYSTEM_PROPERTY);
//			if (timestamp == null)
//				timestamp= Platform.getDebugOption(PLUGIN_ID + TIMESTAMP_OPTION);
//			
//			fgDefaultEvaluator.setReferenceFilterProperties(driver, timestamp);
			
			AssertChecker cpu= new RelativeBandChecker(Dimensions.CPU_TIME, 0.0F, 1.0F);
			//AssertChecker mem= new RelativeBandChecker(Dimensions.USED_JAVA_HEAP, 0.0F, 1.0F);
			//AssertChecker mem= new RelativeBandChecker(Dimensions.WORKING_SET, 0.0F, 1.0F);
			
			fgDefaultEvaluator.setAssertCheckers(new AssertChecker[] { cpu /*, mem */ });
		}

		return fgDefaultEvaluator;
	}

}
