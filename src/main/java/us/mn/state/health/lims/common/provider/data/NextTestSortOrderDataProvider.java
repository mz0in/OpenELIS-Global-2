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
package us.mn.state.health.lims.common.provider.data;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spring.service.test.TestSectionService;
import spring.service.test.TestService;
import spring.util.SpringContext;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.servlet.data.AjaxServlet;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.test.valueholder.Test;
import us.mn.state.health.lims.test.valueholder.TestSection;
/**
 * 
 * @author diane benz
 * bugzilla 2443
 */
public class NextTestSortOrderDataProvider extends BaseDataProvider {
	
	protected TestService testService = SpringContext.getBean(TestService.class);
	protected TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);

	public NextTestSortOrderDataProvider() {
		super();
	}

	public NextTestSortOrderDataProvider(AjaxServlet ajaxServlet) {
		this.ajaxServlet = ajaxServlet;
	}

	public void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String testSectionId = (String) request.getParameter("tsid");
		String formField = (String) request.getParameter("field");
		String result = getData(testSectionId);
		ajaxServlet.sendData(formField, result, request, response);
	}

	// modified for efficiency bugzilla 1367
	/**
	 * getData() - for NextTestSortOrderDataProvider
	 * 
	 * @param testSectionId - String
	 * @return String - data
	 */
	public String getData(String testSectionId) throws LIMSRuntimeException {
        String result = INVALID;

		if (!StringUtil.isNullorNill(testSectionId)) {
			Test test = new Test();
			TestSection testSection = new TestSection();
			testSection.setId(testSectionId);
			testSectionService.getData(testSection);
			
			if (testSection != null && !StringUtil.isNullorNill(testSection.getId())) {
				test.setTestSection(testSection);

				Integer sortOrder = testService.getNextAvailableSortOrderByTestSection(test);
				if (sortOrder != null) {
					result =  sortOrder.toString();
				} else {
					result = "1";
				}
			} 	
		}
		return result;
	}

}
