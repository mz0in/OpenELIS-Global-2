package us.mn.state.health.lims.audittrail.dao;

import java.util.List;

import us.mn.state.health.lims.audittrail.valueholder.History;
import us.mn.state.health.lims.common.dao.BaseDAO;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;

public interface HistoryDAO extends BaseDAO<History, String> {
	
	public List getHistoryByRefIdAndRefTableId(String Id, String Table) throws LIMSRuntimeException;
	public List getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException;

}
