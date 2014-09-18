package org.dawb.passerelle.actors.service;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.services.ITransferService;
import org.dawb.passerelle.actors.roi.ROISource;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.ui.services.AbstractServiceFactory;
import org.eclipse.ui.services.IServiceLocator;

public class TransferService extends AbstractServiceFactory implements ITransferService {

	@Override
	public Object createROISource(String name, Object roi) throws Exception {
		final  List<ROISource> list = new ArrayList<ROISource>(1);// They force an ArrayList
		final ROISource source = ROISource.createSource(name, (IROI)roi);
		list.add(source);
		return list;
	}

	@Override
	public Object create(@SuppressWarnings("rawtypes") Class serviceInterface, IServiceLocator parentLocator, IServiceLocator locator) {
		if (serviceInterface==ITransferService.class) {
		    return new TransferService();
		}
		return null;
	}

}
