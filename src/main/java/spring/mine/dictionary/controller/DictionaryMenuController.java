package spring.mine.dictionary.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import spring.mine.common.constants.Constants;
import spring.mine.common.controller.BaseMenuController;
import spring.mine.common.form.MenuForm;
import spring.mine.common.validator.BaseErrors;
import spring.mine.dictionary.form.DictionaryMenuForm;
import spring.mine.dictionary.validator.DictionaryMenuFormValidator;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.common.log.LogEvent;
import us.mn.state.health.lims.common.util.StringUtil;
import us.mn.state.health.lims.common.util.SystemConfiguration;
import us.mn.state.health.lims.dictionary.dao.DictionaryDAO;
import us.mn.state.health.lims.dictionary.daoimpl.DictionaryDAOImpl;
import us.mn.state.health.lims.dictionary.valueholder.Dictionary;
import us.mn.state.health.lims.hibernate.HibernateUtil;

@Controller
public class DictionaryMenuController extends BaseMenuController {

	@Autowired
	DictionaryMenuFormValidator formValidator;

	@RequestMapping(value = { "/DictionaryMenu", "/SearchDictionaryMenu" }, method = RequestMethod.GET)
	public ModelAndView showDictionaryMenu(HttpServletRequest request, RedirectAttributes redirectAttributes)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		DictionaryMenuForm form = new DictionaryMenuForm();

		String forward = performMenuAction(form, request);
		if (FWD_FAIL.equals(forward)) {
			Errors errors = new BaseErrors();
			errors.reject("error.generic");
			redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
			return findForward(forward, form);
		} else {
			request.setAttribute("menuDefinition", "DictionaryMenuDefinition");
			addFlashMsgsToRequest(request);
			return findForward(forward, form);
		}
	}

	@Override
	protected List createMenuList(MenuForm form, HttpServletRequest request) throws Exception {

		List dictionarys = new ArrayList();

		String stringStartingRecNo = (String) request.getAttribute("startingRecNo");
		int startingRecNo = Integer.parseInt(stringStartingRecNo);
		// bugzilla 1413
		String searchString = request.getParameter("searchString");

		String doingSearch = request.getParameter("search");

		DictionaryDAO dictionaryDAO = new DictionaryDAOImpl();

		if (!StringUtil.isNullorNill(doingSearch) && doingSearch.equals(YES)) {
			dictionarys = dictionaryDAO.getPagesOfSearchedDictionarys(startingRecNo, searchString);
		} else {
			dictionarys = dictionaryDAO.getPageOfDictionarys(startingRecNo);
			// end of bugzilla 1413
		}

		request.setAttribute("menuDefinition", "DictionaryMenuDefinition");

		// bugzilla 1411 set pagination variables
		// bugzilla 1413 set pagination variables for searched results
		if (!StringUtil.isNullorNill(doingSearch) && doingSearch.equals(YES)) {
			request.setAttribute(MENU_TOTAL_RECORDS,
					String.valueOf(dictionaryDAO.getTotalSearchedDictionaryCount(searchString)));
		} else {
			request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(dictionaryDAO.getTotalDictionaryCount()));
		}
		request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));
		int numOfRecs = 0;
		if (dictionarys != null) {
			if (dictionarys.size() > SystemConfiguration.getInstance().getDefaultPageSize()) {
				numOfRecs = SystemConfiguration.getInstance().getDefaultPageSize();
			} else {
				numOfRecs = dictionarys.size();
			}
			numOfRecs--;
		}
		int endingRecNo = startingRecNo + numOfRecs;
		request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));
		// end bugzilla 1411

		// bugzilla 1413
		request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "dictionary.dictEntry");
		// bugzilla 1413 set up a seraching mode so the next and previous action will
		// know
		// what to do

		if (!StringUtil.isNullorNill(doingSearch) && doingSearch.equals(YES)) {

			request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");

			request.setAttribute(MENU_SELECT_LIST_HEADER_SEARCH_STRING, searchString);
		}

		return dictionarys;
	}

	@Override
	protected int getPageSize() {
		return SystemConfiguration.getInstance().getDefaultPageSize();
	}

	@Override
	protected String getDeactivateDisabled() {
		return "false";
	}

	@RequestMapping(value = "/DeleteDictionary", method = RequestMethod.POST)
	public ModelAndView showDeleteDictionary(HttpServletRequest request,
			@ModelAttribute("form") @Valid DictionaryMenuForm form, BindingResult result,
			RedirectAttributes redirectAttributes) {
		formValidator.validate(form, result);
		if (result.hasErrors()) {
			saveErrors(result);
			return findForward(FWD_FAIL_INSERT, form);
		}

		String[] selectedIDs = (String[]) form.get("selectedIDs");

		List<Dictionary> dictionarys = new ArrayList<>();
		for (int i = 0; i < selectedIDs.length; i++) {
			Dictionary dictionary = new Dictionary();
			dictionary.setId(selectedIDs[i]);
			dictionary.setSysUserId(getSysUserId(request));
			dictionarys.add(dictionary);
		}

		Transaction tx = HibernateUtil.getSession().beginTransaction();
		try {
			// selectedIDs = (List)PropertyUtils.getProperty(form,
			// "selectedIDs");
			DictionaryDAO dictionaryDAO = new DictionaryDAOImpl();
			dictionaryDAO.deleteData(dictionarys);
			// initialize the form
			tx.commit();
		} catch (LIMSRuntimeException lre) {
			// bugzilla 2154
			LogEvent.logError("DictionaryDeleteAction", "performAction()", lre.toString());
			tx.rollback();

			if (lre.getException() instanceof org.hibernate.StaleObjectStateException) {
				result.reject("errors.OptimisticLockException");
			} else {
				result.reject("errors.DeleteException");
			}
			redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, result);
			return findForward(FWD_FAIL_DELETE, form);

		} finally {
			HibernateUtil.closeSession();
		}

		redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
		return findForward(FWD_SUCCESS_DELETE, form);
	}

	@Override
	protected String findLocalForward(String forward) {
		if (FWD_SUCCESS.equals(forward)) {
			return "masterListsPageDefinition";
		} else if (FWD_FAIL.equals(forward)) {
			return "redirect:/MasterListsPage.do";
		} else if (FWD_SUCCESS_DELETE.equals(forward)) {
			return "redirect:/DictionaryMenu.do";
		} else if (FWD_FAIL_DELETE.equals(forward)) {
			return "redirect:/DictionaryMenu.do";
		} else {
			return "PageNotFound";
		}
	}

	@Override
	protected String getPageTitleKey() {
		return "dictionary.browse.title";
	}

	@Override
	protected String getPageSubtitleKey() {
		return "dictionary.browse.title";
	}

}