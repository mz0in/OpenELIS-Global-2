package spring.generated.testconfiguration.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import spring.generated.testconfiguration.form.TestOrderabilityForm;
import spring.generated.testconfiguration.validator.TestOrderabilityFormValidator;
import spring.mine.common.controller.BaseController;
import spring.service.test.TestServiceImpl;
import us.mn.state.health.lims.common.services.DisplayListService;
import us.mn.state.health.lims.common.services.TypeOfSampleService;
import us.mn.state.health.lims.common.util.IdValuePair;
import us.mn.state.health.lims.hibernate.HibernateUtil;
import us.mn.state.health.lims.test.beanItems.TestActivationBean;
import us.mn.state.health.lims.test.dao.TestDAO;
import us.mn.state.health.lims.test.daoimpl.TestDAOImpl;
import us.mn.state.health.lims.test.valueholder.Test;

@Controller
public class TestOrderabilityController extends BaseController {

	@Autowired
	TestOrderabilityFormValidator formValidator;

	@RequestMapping(value = "/TestOrderability", method = RequestMethod.GET)
	public ModelAndView showTestOrderability(HttpServletRequest request) {
		TestOrderabilityForm form = new TestOrderabilityForm();
		setupDisplayItems(form);
		return findForward(FWD_SUCCESS, form);
	}

	private void setupDisplayItems(TestOrderabilityForm form) {
		List<TestActivationBean> orderableTestList = createTestList(false);
		form.setOrderableTestList(orderableTestList);
	}

	private List<TestActivationBean> createTestList(boolean refresh) {
		ArrayList<TestActivationBean> testList = new ArrayList<>();

		if (refresh) {
			DisplayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
		}

		List<IdValuePair> sampleTypeList = DisplayListService.getList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);

		for (IdValuePair pair : sampleTypeList) {
			TestActivationBean bean = new TestActivationBean();

			List<Test> tests = TypeOfSampleService.getAllTestsBySampleTypeId(pair.getId());
			List<IdValuePair> orderableTests = new ArrayList<>();
			List<IdValuePair> inorderableTests = new ArrayList<>();

			// initial ordering will be by display order. Inactive tests will then be
			// re-ordered alphabetically
			Collections.sort(tests, new Comparator<Test>() {
				@Override
				public int compare(Test o1, Test o2) {
					// compare sort order
					if (NumberUtils.isNumber(o1.getSortOrder()) && NumberUtils.isNumber(o2.getSortOrder())) {
						return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
						// if o2 has no sort order o1 does, o2 is assumed to be higher
					} else if (NumberUtils.isNumber(o1.getSortOrder())) {
						return -1;
						// if o1 has no sort order o2 does, o1 is assumed to be higher
					} else if (NumberUtils.isNumber(o2.getSortOrder())) {
						return 1;
						// else they are considered equal
					} else {
						return 0;
					}
				}
			});

			for (Test test : tests) {
				if (test.getOrderable()) {
					orderableTests.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
				} else {
					inorderableTests.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
				}
			}

			IdValuePair.sortByValue(orderableTests);

			bean.setActiveTests(orderableTests);
			bean.setInactiveTests(inorderableTests);
			if (!orderableTests.isEmpty() || !inorderableTests.isEmpty()) {
				bean.setSampleType(pair);
				testList.add(bean);
			}
		}

		return testList;
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "testOrderabilityDefinition";
		} else if (FWD_SUCCESS_INSERT.equals(forward)) {
			return "testOrderabilityDefinition";
		} else if (FWD_FAIL_INSERT.equals(forward)) {
			return "testOrderabilityDefinition";
		} else {
			return "PageNotFound";
		}
	}

	@RequestMapping(value = "/TestOrderability", method = RequestMethod.POST)
	public ModelAndView postTestOrderability(HttpServletRequest request,
			@ModelAttribute("form") @Valid TestOrderabilityForm form, BindingResult result) throws Exception {
		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			setupDisplayItems(form);
			return findForward(FWD_FAIL_INSERT, form);
		}

		String changeList = form.getJsonChangeList();

		JSONParser parser = new JSONParser();

		JSONObject obj = (JSONObject) parser.parse(changeList);

		List<String> orderableTestIds = getIdsForActions("activateTest", obj, parser);
		List<String> unorderableTestIds = getIdsForActions("deactivateTest", obj, parser);

		List<Test> tests = getTests(unorderableTestIds, false);
		tests.addAll(getTests(orderableTestIds, true));

		Transaction tx = HibernateUtil.getSession().beginTransaction();

		TestDAO testDAO = new TestDAOImpl();

		try {
			for (Test test : tests) {
				testDAO.updateData(test);
			}

			tx.commit();
		} catch (HibernateException e) {
			tx.rollback();
		} finally {
			HibernateUtil.closeSession();
		}

		TypeOfSampleService.clearCache();

		List<TestActivationBean> orderableTestList = createTestList(true);
		form.setOrderableTestList(orderableTestList);

		return findForward(FWD_SUCCESS_INSERT, form);
	}

	private List<Test> getTests(List<String> testIds, boolean orderable) {
		List<Test> tests = new ArrayList<>();

		for (String testId : testIds) {
			Test test = new TestServiceImpl(testId).getTest();
			test.setOrderable(orderable);
			test.setSysUserId(getSysUserId(request));
			tests.add(test);
		}

		return tests;
	}

	private List<String> getIdsForActions(String key, JSONObject root, JSONParser parser) {
		List<String> list = new ArrayList<>();

		String action = (String) root.get(key);

		try {
			JSONArray actionArray = (JSONArray) parser.parse(action);

			for (int i = 0; i < actionArray.size(); i++) {
				list.add((String) ((JSONObject) actionArray.get(i)).get("id"));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return list;
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
