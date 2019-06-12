/**
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/
package us.mn.state.health.lims.common.provider.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spring.mine.internationalization.MessageUtil;
import spring.service.analysis.AnalysisService;
import spring.util.SpringContext;
import us.mn.state.health.lims.analysis.valueholder.Analysis;
import us.mn.state.health.lims.common.action.IActionConstants;
import us.mn.state.health.lims.common.servlet.reports.ReportsServlet;
import us.mn.state.health.lims.reports.valueholder.resultsreport.ResultsReportTest;
import us.mn.state.health.lims.reports.valueholder.resultsreport.ResultsReportTestComparator;

/**
 * @author benzd1
 * bugzila 2265 implements IActionConstants
 */
public abstract class BaseReportsProvider implements IActionConstants {
	
	protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

	protected ReportsServlet reportsServlet = null;

	/**
	 * @param parameters
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
     * bugzilla 2274: return boolean successful
	 */
	public abstract boolean processRequest(Map parameters,
			HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

	/**
	 * @param rs
	 */
	public void setServlet(ReportsServlet rs) {
		this.reportsServlet = rs;
	}

	/**
	 * @return
	 */
	public ReportsServlet getServlet() {
		return this.reportsServlet;
	}

	//bugzilla 1856
	protected List sortTests(List reportTests) {
		
		//find root level nodes and fill in children for each Test_TestAnalyte
		List rootLevelNodes = new ArrayList();
		for (int i = 0; i < reportTests.size(); i++) {
			ResultsReportTest reportTest = (ResultsReportTest)reportTests.get(i);
			String analysisId = reportTest.getAnalysis().getId();
			
      	    List children = new ArrayList();
            for (int j = 0; j < reportTests.size(); j++) {
            	ResultsReportTest test = (ResultsReportTest)reportTests.get(j);
           	    if (test.getAnalysis().getParentAnalysis() != null && test.getAnalysis().getParentAnalysis().getId().equals(analysisId)) {
                  children.add(test);
 		        }
 		    }
            reportTest.setChildren(children);

			if (reportTest.getAnalysis().getParentAnalysis() == null) {
				rootLevelNodes.add(reportTest);
			} 
		}
		
		//sort rootLevelNodes
		Collections.sort(rootLevelNodes, ResultsReportTestComparator.SORT_ORDER_COMPARATOR);

		reportTests = new ArrayList();
		for (int i = 0; i < rootLevelNodes.size(); i++) {
		   ResultsReportTest reportTest = (ResultsReportTest)rootLevelNodes.get(i);
		   reportTests.add(reportTest);
		   recursiveSort(reportTest, reportTests);
		}
		
	    return reportTests;
	}
		
	//bugzilla 1856 use recursion and sort children
	private void recursiveSort(ResultsReportTest element, List reportTests) {
		List<ResultsReportTest> children = element.getChildren();
 		//sort children
		if (children != null && children.size() > 0) {
		  Collections.sort(children, ResultsReportTestComparator.SORT_ORDER_COMPARATOR);
		}
		for (Iterator<ResultsReportTest> it = children.iterator(); it.hasNext();) {
			ResultsReportTest childElement = it.next();
			reportTests.add(childElement);
			recursiveSort(childElement, reportTests);
		}
	}

	/**
	 * Utility method to simplify the lookup of MessageResource Strings in the
	 * ApplicationResources.properties file for this application.
	 * 
	 * @param request
	 *            the HttpServletRequest
	 * @param messageKey
	 *            the message key to look up
     * bugzilla 2264
	 */
	protected String getMessageForKey(HttpServletRequest request,
			String messageKey) throws Exception {
		if (null == messageKey)
			return null;
		java.util.Locale locale = (java.util.Locale) request.getSession()
				.getAttribute("org.apache.struts.action.LOCALE");
		// Return the message for the user's locale.
		return MessageUtil.getMessage(messageKey);
		//return ResourceLocator.getInstance().getMessageResources().getMessage(
				//locale, messageKey);
	}

    //bugzilla 2264
	protected String getMessageForKey(HttpServletRequest request,
			String messageKey, String arg0) throws Exception {
		if (null == messageKey)
			return null;
		java.util.Locale locale = (java.util.Locale) request.getSession()
				.getAttribute("org.apache.struts.action.LOCALE");
		// Return the message for the user's locale.
		return MessageUtil.getMessage(messageKey);
		//return ResourceLocator.getInstance().getMessageResources().getMessage(
				//locale, messageKey, arg0);
	}
	
	//bugzilla 1856
	protected void moveParentTestsOfCurrentTestsFromPreviouslyReportedTests(List currentTests, List previouslyReportedTests) {
		//create temp list of previously and currently reported tests
		List tempPreviouslyReportedTests = new ArrayList();
		List tempCurrentTests = new ArrayList();
		for (int i = 0; i < previouslyReportedTests.size(); i++) {
			ResultsReportTest test = (ResultsReportTest)previouslyReportedTests.get(i);
			tempPreviouslyReportedTests.add(test);
		}
	    for (int i = 0; i < currentTests.size(); i++) {
        	ResultsReportTest test = (ResultsReportTest)currentTests.get(i);
        	tempCurrentTests.add(test);
        }
	    for (int i = 0; i < tempCurrentTests.size(); i++) {
        	ResultsReportTest test = (ResultsReportTest)tempCurrentTests.get(i);
         	recursiveMoveParentTests(test, currentTests, tempCurrentTests, previouslyReportedTests, tempPreviouslyReportedTests);
        }
	}
	

	//bugzilla 1856
	private void recursiveMoveParentTests(ResultsReportTest test, List currentTests, List tempCurrentTests, List previouslyReportedTests, List tempPreviouslyReportedTests) {
		Iterator it = tempPreviouslyReportedTests.iterator();
		while (it.hasNext()) {
			ResultsReportTest previousTest = (ResultsReportTest)it.next();
			if (test.getAnalysis().getParentAnalysis() != null && test.getAnalysis().getParentAnalysis().getId().equals(previousTest.getAnalysis().getId())) {
				//we are removing this one from previouslyReportedTests - it is a parent of a currently reported test (adding to currentTests)
				currentTests.add(previousTest);
				previouslyReportedTests.remove(previousTest);
				//now the test to find possible further parent of is this parent 
				recursiveMoveParentTests(previousTest, currentTests, tempCurrentTests, previouslyReportedTests, tempPreviouslyReportedTests);
				break;
			} 
		}
	}
	
	//bugzilla 1856 add in "phantom" tests to complete the hierarchy so we can sort correctly (the phantom tests are removed after)
	protected List completeHierarchyOfTestsForSorting(List tests){
		List newTests = new ArrayList();
		for (int i = 0; i < tests.size(); i++) {
			ResultsReportTest test = (ResultsReportTest)tests.get(i);
			newTests.add(test);
			recursiveHierarchyBuild(test, tests, newTests);
		}
		return newTests;
	}
	
	//bugzilla 1856
	protected List removePhantomTests(List tests) {
		Iterator it = tests.iterator();
		tests = new ArrayList();
		while (it.hasNext()) {
			ResultsReportTest test = (ResultsReportTest)it.next();
            if (!test.isPhantom()) {
            	tests.add(test);
			}
		}
		return tests;
	}

	//bugzila 1856 recursively add in phantom tests to build the actual original hierarchy
	//this is so that tests that are part of parent/child relationships can be taken into account for sorting
	private void recursiveHierarchyBuild(ResultsReportTest element, List tests, List newTests){
		if (element != null && element.getAnalysis().getParentAnalysis() != null) {
			//find out if parent is already in the original list
			boolean alreadyInList = false;
			for (int i = 0; i < tests.size(); i++) {
				ResultsReportTest test = (ResultsReportTest)tests.get(i);
				if (element.getAnalysis().getParentAnalysis().getId().equals(test.getAnalysis().getId())) {
					alreadyInList = true;
				}
			}
			if (!alreadyInList) {
				//add phantom test to list
				Analysis analysis = new Analysis();
				analysis.setId(element.getAnalysis().getParentAnalysis().getId());
				analysisService.getData(analysis);
				ResultsReportTest test = new ResultsReportTest();
				test.setAnalysis(analysis);
				test.setPhantom(true);
				newTests.add(test);
				//now check for another parent
				recursiveHierarchyBuild(test, tests, newTests);
			}
		}

	}
	
	
}
