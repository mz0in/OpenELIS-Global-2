package spring.mine.testconfiguration.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.controller.BaseController;
import spring.mine.testconfiguration.form.TestRenameEntryForm;
import spring.service.localization.LocalizationService;
import spring.service.test.TestServiceImpl;
import us.mn.state.health.lims.common.log.LogEvent;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.localization.valueholder.Localization;
import us.mn.state.health.lims.test.valueholder.Test;

@Controller
public class TestRenameEntryController extends BaseController {

	@Autowired
	LocalizationService localizationService;

	@RequestMapping(value = "/TestRenameEntry", method = RequestMethod.GET)
	public ModelAndView showTestRenameEntry(HttpServletRequest request) {
		System.out.println("Hibernate Version: " + org.hibernate.Version.getVersionString());
		String forward = FWD_SUCCESS;
		TestRenameEntryForm form = new TestRenameEntryForm();

		form.setTestList(DisplayListService.getList(DisplayListService.ListType.ALL_TESTS));

		return findForward(forward, form);
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "testRenameDefinition";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "redirect:/TestRenameEntry.do";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "testRenameDefinition";
		} else {
			return "PageNotFound";
		}
	}

	@RequestMapping(value = "/TestRenameEntry", method = RequestMethod.POST)
	public ModelAndView updateTestRenameEntry(HttpServletRequest request,
			@ModelAttribute("form") @Valid TestRenameEntryForm form, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}

		form.setCancelAction("CancelDictionary.do");

		String testId = form.getTestId();
		String nameEnglish = form.getNameEnglish();
		String nameFrench = form.getNameFrench();
		String reportNameEnglish = form.getReportNameEnglish();
		String reportNameFrench = form.getReportNameFrench();
		String userId = getSysUserId(request);

		updateTestNames(testId, nameEnglish, nameFrench, reportNameEnglish, reportNameFrench, userId);

		return findForward(FWD_SUCCESS_INSERT, form);
	}

	private void updateTestNames(String testId, String nameEnglish, String nameFrench, String reportNameEnglish,
			String reportNameFrench, String userId) {
		Test test = new TestServiceImpl(testId).getTest();

		if (test != null) {

			Localization name = test.getLocalizedTestName();
			Localization reportingName = test.getLocalizedReportingName();
			name.setEnglish(nameEnglish.trim());
			name.setFrench(nameFrench.trim());
			name.setSysUserId(userId);
			reportingName.setEnglish(reportNameEnglish.trim());
			reportingName.setFrench(reportNameFrench.trim());
			reportingName.setSysUserId(userId);

			try {
				updateTestNames(name, reportingName);
			} catch (HibernateException e) {
				LogEvent.logErrorStack(this.getClass().getSimpleName(), "updateTestNames()", e);
			}

		}

		// Refresh test names
		DisplayListService.getFreshList(DisplayListService.ListType.ALL_TESTS);
		DisplayListService.getFreshList(DisplayListService.ListType.ORDERABLE_TESTS);
	}

	@Transactional
	public void updateTestNames(Localization name, Localization reportingName) {
		localizationService.update(name);
		localizationService.update(reportingName);
	}

	@Override
	protected String getPageTitleKey() {
		return null;
	}

	@Override
	protected String getPageSubtitleKey() {
		return null;
	}
}
